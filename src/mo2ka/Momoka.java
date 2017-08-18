package mo2ka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import mo2ka.strings.Tables;
import net.reduls.igo.Tagger;
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

	public static final String dbLocation = "/home/tao/mo2ka/momoka.db";
	public static final String igoDicDir = "/home/tao/mo2ka/neologd";
	public static final String myScreenName = "mo2ka_ch";

	private static Connection conn;
	public static Statement stmt;

	public static String[] NOT_LEARN_TEXT, LEARN_VIA, NOT_FAVORITE_USER, REACTION_WORDS;

	public static long startTime;

	public static Twitter twitter;
	public static Tagger tagger;
	public static ArrayList<String> meshiTero;

	public static void main(String[] args){
		startTime = System.currentTimeMillis();
		try{
			prepareSQLite();
			loadSettings();
		}catch(FileNotFoundException | SQLException e){
			System.err.println("Failed Prepare SQLite\nMessage: " + e.getMessage());
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

		//new Tweet(String.format("ももかちゃん起動 (%s)", new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date())));

		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				stream.shutdown();
				try{
					stmt.close();
					conn.close();
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
	}

	public static void loadSettings() throws SQLException{
		NOT_LEARN_TEXT = DBUtils.getOneColumnResult(Tables.NOT_LEARN_TEXT);
		LEARN_VIA = DBUtils.getOneColumnResult(Tables.LEARN_VIA);
		NOT_FAVORITE_USER = DBUtils.getOneColumnResult(Tables.NOT_FAVORITE_USER);
		REACTION_WORDS = DBUtils.getOneColumnResult(Tables.REACTION_WORD);
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
