package mo2ka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.sqlite.JDBC;

import mo2ka.functions.CreateTweet;
import mo2ka.functions.Tweet;
import mo2ka.strings.Keys;
import mo2ka.strings.JsonKeys;
import net.reduls.igo.Tagger;
import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class Momoka{

	public static final String confLocation = "/home/tao/mo2ka/mo2ka.conf.json";
	public static final String dbLocation = "/home/tao/mo2ka/momoka.db";
	public static final String wikiLocation = "/home/tao/mo2ka/wiki.db";
	public static final String igoDicDir = "/home/tao/mo2ka/neologd";
	public static final String myScreenName = "mo2ka_ch";

	private static Connection conn, wikiConn;
	public static Statement stmt, wikiStmt;

	public static String[] NOT_LEARN_USER, NOT_LEARN_TEXT, LEARN_VIA, REACTION_WORDS;
	public static int ratio_tweet;
	public static int ratio_meshi;

	public static long startTime;

	public static Twitter twitter;
	public static Tagger tagger;
	public static ArrayList<String> meshiTero;

	public static void main(String[] args){
		startTime = System.currentTimeMillis();
		try{
			prepareSQLite();
		}catch(FileNotFoundException | SQLException e){
			System.err.println("Failed Prepare SQLite\nMessage: " + e.getMessage());
			System.exit(1);
		}
		try{
			loadSettings();
		}catch(IOException | JSONException e){
			System.err.println("Failed Load Configuration file\nMessage: " + e.getMessage());
			System.exit(1);
		}

		Configuration conf = new ConfigurationBuilder()
				.setOAuthConsumerKey(Keys.consumerKey)
				.setOAuthConsumerSecret(Keys.consumerSecret)
				.setTweetModeExtended(true).build();
		AccessToken at = new AccessToken(Keys.accessToken[0], Keys.accessToken[1]);
		twitter = new TwitterFactory(conf).getInstance(at);

		loadMeshiTeroBot();

		try{
			tagger = new Tagger(igoDicDir);
		}catch(IOException e){
			System.err.println("Could not create instance of Tagger\nMessage: " + e.getMessage());
			System.exit(1);
		}

		TwitterStream stream = new TwitterStreamFactory(conf).getInstance(at);
		stream.addListener(new Streaming());
		stream.user();

		new Tweet(String.format("ももかちゃん起動 (%s)", new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date())));

		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				stream.shutdown();
				try{
					stmt.close();
					conn.close();
					wikiStmt.close();
					wikiConn.close();
				}catch(SQLException e){
				}
				System.out.println("mo2ka stopped");
				return;
			}
		});

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try{
			for(String line = br.readLine(); line != null; line = br.readLine()){
				switch(line){
				case "stop":
				case "exit":
					System.exit(0);
					break;
				case "create-sentence":
					try{
						String str = new CreateTweet().getResult();
						System.out.println(str);
					}catch(SQLException e){
						System.err.println("Could not create tweet\nMessage: " + e.getMessage());
					}
					break;
				case "update-tagger":
					try{
						tagger = new Tagger(igoDicDir);
						System.out.println("Update igo dic OK");
					}catch(IOException e){
						System.err.println("Could not create instance of Tagger\nMessage: " + e.getMessage());
					}
					break;
				case "reload-config":
					try{
						loadSettings();
						Thread.sleep(500);
						System.out.println("Reload config OK");
					}catch (IOException | JSONException | InterruptedException e) {
						System.err.println("Could not reload configuration file\nMessage: " + e.getMessage());
					}
				}
			}
		}catch(IOException e){
			System.err.println("Could not BufferedReader#readLine()\nMessage: " + e.getMessage());
		}
	}

	public static void prepareSQLite() throws FileNotFoundException, SQLException{
		if(!new File(dbLocation).exists())
			throw new FileNotFoundException("Not found SQLite3 database file\nPlease run `java -cp {This jar} mo2ka.InitDB`");
		Properties prop = new Properties();
		prop.put("sync_mode", "off");
		prop.put("journal_mode", "wal");
		conn = JDBC.createConnection("jdbc:sqlite:" + dbLocation, prop);
		stmt = conn.createStatement();

		if(!new File(wikiLocation).exists())
			throw new FileNotFoundException("Not found SQLite3 Wikipedia database");
		wikiConn = JDBC.createConnection("jdbc:sqlite:" + wikiLocation, prop);
		wikiStmt = wikiConn.createStatement();
	}

	public static void loadSettings() throws IOException, JSONException{
		File conf = new File(confLocation);
		if(!conf.exists()){
			conf.createNewFile();
			FileWriter fw = new FileWriter(conf);
			fw.write("{}");
			fw.close();
		}
		BufferedReader br = new BufferedReader(new FileReader(conf));
		StringBuilder sb = new StringBuilder();
		String str;
		while((str = br.readLine()) != null)
			sb.append(str);
		br.close();
		str = sb.toString();
		JSONObject jsonObj = new JSONObject(str);
		ratio_tweet = jsonObj.has(JsonKeys.RATIO_TWEET) ? jsonObj.getInt(JsonKeys.RATIO_TWEET) : 10;
		ratio_meshi = jsonObj.has(JsonKeys.RATIO_MESHI) ? jsonObj.getInt(JsonKeys.RATIO_MESHI) : 3;
		JSONArray notLearnUser = jsonObj.has(JsonKeys.NOT_LEARN_USER) ? jsonObj.getJSONArray(JsonKeys.NOT_LEARN_USER) : null;
		JSONArray notLearnText = jsonObj.has(JsonKeys.NOT_LEARN_TEXT) ? jsonObj.getJSONArray(JsonKeys.NOT_LEARN_TEXT) : null;
		JSONArray learnVia = jsonObj.has(JsonKeys.LEARN_VIA) ? jsonObj.getJSONArray(JsonKeys.LEARN_VIA) : null;
		JSONArray reactionWord = jsonObj.has(JsonKeys.REACTION_WORD) ? jsonObj.getJSONArray(JsonKeys.REACTION_WORD) : null;
		NOT_LEARN_USER = getArrayFromJSONArray(notLearnUser);
		NOT_LEARN_TEXT = getArrayFromJSONArray(notLearnText);
		LEARN_VIA = getArrayFromJSONArray(learnVia);
		REACTION_WORDS = getArrayFromJSONArray(reactionWord);
	}

	public static String[] getArrayFromJSONArray(JSONArray arr) throws JSONException{
		if(arr == null)
			return new String[0];
		String[] result = new String[arr.length()];
		for(int i = 0; i < result.length; i++)
			result[i] = arr.getString(i);
		return result;
	}

	public static void loadMeshiTeroBot(){
		meshiTero = new ArrayList<String>();
		try{
			for(Status status : twitter.getUserTimeline("meshiuma_yonaka", new Paging(1, 200))){
				MediaEntity[] mentitys = status.getMediaEntities();
				if(mentitys != null && mentitys.length > 0 && !status.isRetweet()){
					for(int i = 0; i < mentitys.length; i++)
						meshiTero.add(status.getText());
				}
			}
		}catch(TwitterException e){
			System.err.println("Failed Load MeshiTeroBot\nMessage: " + e.getMessage());
			System.exit(1);
		}
	}
}
