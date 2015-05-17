package momoka_ch;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ne.docomo.smt.dev.common.exception.SdkException;
import jp.ne.docomo.smt.dev.common.exception.ServerException;
import jp.ne.docomo.smt.dev.common.http.AuthApiKey;
import jp.ne.docomo.smt.dev.dialogue.Dialogue;
import jp.ne.docomo.smt.dev.dialogue.data.DialogueResultData;
import jp.ne.docomo.smt.dev.dialogue.param.DialogueRequestParam;
import net.reduls.igo.Tagger;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class Momoka {
	public static String MyScreenName;
	public static Random random;
	
	private static Twitter twitter;
	private static TwitterStream stream;
	
	private static Statement stmt;
	private static Tagger tagger;
	private static String[] NOT_LEARN_TEXT;
	private static String[] NOT_LEARN_VIA;
	private static List<String> learned;
	private static ResultSet resultSet;
	
	private static Dialogue dialogue;
	private static DialogueRequestParam dialogueParam;
	private static List<String> dialogueContext;
	private static List<String> dialogueContextUser;

	private static Connection conn;
	private static boolean debug = false;
	
	public static void main(String[] args) throws Exception{
		NOT_LEARN_TEXT = new String[]{
				"9oo.me",
				"tinyurl.com",
				"#SleepMeister",
				"#tweetbatt",
				"#RakutenIchiba",
				"#rbooks",
				"#news",
				"#anime"
		};
		NOT_LEARN_VIA = new String[]{
				"たおっぱいのNamer",
				"ツイ廃あらーと",
				"TweetMag1c for Android",
				"twittbot.net",
				"Tweet Battery",
				"autotweety.net",
				"午前3時の茨城県",
				"ニコニコ動画",
				"Foursquare",
				"リプライ数チェッカ"
		};
		InputStream is = Momoka.class.getResourceAsStream("properties");
        Properties prop = new Properties();
        prop.load(is);
        is.close();
        
		learned = new ArrayList<String>();
		dialogueContext = new ArrayList<String>();
		dialogueContextUser = new ArrayList<String>();
		AuthApiKey.initializeAuth(prop.getProperty("docomoAPI"));
		dialogue = new Dialogue();
		dialogueParam = new DialogueRequestParam();
		
        String CK = prop.getProperty("CK");
        String CS = prop.getProperty("CS");
        String AT = prop.getProperty("AT");
        String ATS = prop.getProperty("ATS");
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(CK)
		.setOAuthConsumerSecret(CS);
		Configuration jconf = builder.build();
		TwitterFactory factory = new TwitterFactory(jconf);
		TwitterStreamFactory streamFactory = new TwitterStreamFactory(jconf);
		AccessToken accessToken = new AccessToken(AT, ATS);
		twitter = factory.getInstance(accessToken);
		stream = streamFactory.getInstance(accessToken);
		MyScreenName = twitter.getScreenName();
		twitter.updateStatus("ももかちゃん起動 (" + new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date()) + ")");
		System.out.println(MyScreenName);
		
		random = new Random();
		tagger = new Tagger("ipadic");
		prepareSQL();
		stream.addListener(new Streaming());
		stream.user();
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				stream.shutdown();
				try {
					conn.close();
					stmt.close();
				} catch (SQLException e) {}
			}
		});
	}
	public static void prepareSQL() throws Exception{
		Class.forName("org.sqlite.JDBC");
		String location;
		if(debug)
			location = "/Users/tao/Desktop/database.db";
		else
			location = "/var/www/html/momoka/database.db";
		File DB = new File(location);
		if(!DB.exists()){
			DB.createNewFile();
		}
		conn = DriverManager.getConnection("jdbc:sqlite:" + location);
		stmt = conn.createStatement();
		stmt.execute("create table if not exists momoka(content text, screen_name text, tweetId text, via text)");
		resultSet = stmt.executeQuery("select * from momoka");
		while(resultSet.next()){
			learned.add(resultSet.getString(1));
		}
		resultSet.close();
	}
	public static void Tweet(String TweetText, long ReplyTweetId){
		try {
			if(TweetText.length() > 140){
				TweetText = TweetText.substring(0, 140);
			}
			
			if(ReplyTweetId == -1){
				twitter.updateStatus(TweetText);
			}else{
				twitter.updateStatus(new StatusUpdate(TweetText).inReplyToStatusId(ReplyTweetId));
			}
		} catch (TwitterException e) {}
	}
	
	
	//学習
	public static void learn(Status status) throws TwitterException{
		boolean NOT_LEARN_TEXT_BOOL = false;
		boolean NOT_LEARN_VIA_BOOL = false;
		for(int i = 0; NOT_LEARN_TEXT.length > i; i++){
			if(status.getText().matches(NOT_LEARN_TEXT[i]))
				NOT_LEARN_TEXT_BOOL = true;
		}
		for(int i = 0; NOT_LEARN_VIA.length > i; i++){
			if(status.getSource().replaceAll("<.+?>", "").equals(NOT_LEARN_VIA[i]))
				NOT_LEARN_VIA_BOOL = true;
		}
		
		if(!status.isRetweet() && !NOT_LEARN_TEXT_BOOL && !NOT_LEARN_VIA_BOOL && !status.getUser().getScreenName().equals(MyScreenName)){
			String content;
			Matcher URL = Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+", Pattern.DOTALL)
					.matcher(status.getText());
			if(URL.find()){
				content = URL.replaceAll("");
			}else{
				content = status.getText();
			}
			Matcher mention = Pattern.compile("@\\w*", Pattern.DOTALL).matcher(content);
			if(mention.find()){
				content = mention.replaceAll("");
			}
			content.replace("#", "");
			
			List<String> list = tagger.wakati(content);
			list.add(0, "[BEGIN]");
			list.add("[END]");
			for(int i = 2; list.size() > i; i++){
				boolean learn = false;
				try {
					String tango = list.get(i - 2) + ", " + list.get(i - 1) + ", " + list.get(i);
					if(learned.indexOf(tango) != -1)
						learn = true;
					
					if(!learn){
						stmt.execute("insert into momoka values('" + tango
								+ "', '" + status.getUser().getScreenName() + "', '" + status.getId() + "', '"
								+ status.getSource().replaceAll("<.+?>", "")
								+"')");
						learned.add(tango);
					}
				} catch (SQLException e) {
					Tweet(e.toString(), -1);
				}
			}
			twitter.createFavorite(status.getId());
			list.clear();
		}
	}
	//docomoAPIを使った会話
	public static void dialogue(Status status){
		String content = status.getText().substring(MyScreenName.length() + 2);
		
		String context = null;
		DialogueResultData resultData = null;
		
		int userIndex = dialogueContextUser.indexOf(status.getUser().getScreenName());
		if(userIndex != -1)
			context = dialogueContext.get(userIndex).toString();
		
		dialogueParam.setUtt(content);
		dialogueParam.setContext(context);
		
		try {
			resultData = dialogue.request(dialogueParam);
		} catch (SdkException | ServerException e) {
			Tweet("@" + status.getUser().getScreenName() + " " + e.getErrorMessage(), status.getId());
		}
		if(resultData != null){
			if(userIndex != -1){
				dialogueContext.set(userIndex, resultData.getContext());
			}else{
				dialogueContextUser.add(status.getUser().getScreenName());
				dialogueContext.add(resultData.getContext());
			}
			Tweet("@" + status.getUser().getScreenName() + " " + resultData.getUtt(), status.getId());
		}
	}
	//乱数によってツイート
	public static void randomTweet() throws SQLException{
		resultSet = stmt.executeQuery("select * from momoka where content like '[BEGIN]%'");
		ArrayList<String> array = new ArrayList<String>();
		String sentence;
		while(resultSet.next())
			array.add(resultSet.getString(1));
		String[] elements = string2StringArray(randomArray2String(array));
		sentence = elements[1] + elements[2];
		
		resultSet.close();
		String[] tmp;
		for(int i = 0; i < 100; i++){
			array.clear();
			if(elements[2].equals("[END]"))
				break;
			resultSet = stmt.executeQuery("select * from momoka where content like '" + elements[2] + "%'");
			while(resultSet.next())
				array.add(resultSet.getString(1));
			if(array.size() <= 1)
				break;
			tmp = string2StringArray(randomArray2String(array));
			if(elements[2].equalsIgnoreCase(tmp[0])){
				elements = tmp;
				sentence += elements[1] + elements[2];
			}
		}
		if(sentence.endsWith("[END]"))
			sentence = sentence.replace("[END]", "");
		Tweet(sentence, -1);
		
		resultSet.close();
	}
	public static String randomArray2String(ArrayList<String> array){
		return array.get(random.nextInt(array.size() - 1));
	}
	public static String[] string2StringArray(String str){
		return (String[])Arrays.asList(str.split(", ")).toArray();
	}
	
	//学習要素数
	public static void learnCount(Status status) throws SQLException{
		String learnCountContent = "学習したすべてのツイートは" + learnTweets() + "個です。"
				+ "すべての学習要素数は" + learnElements(null) + "個です。";
		Tweet("@" + status.getUser().getScreenName() + " " + learnCountContent, status.getId());
		resultSet.close();
	}
	//ユーザーによる学習要素数
	public static void learnCountUser(Status status, String targetUser) throws SQLException{
		long learnTweets = learnTweets();
		long learnTweetsUser = learnTweetsUser(targetUser);
		if(learnTweetsUser == 0){
			Tweet("@" + status.getUser().getScreenName() + " データは存在しません。", status.getId());
		}else{
			NumberFormat nf = NumberFormat.getPercentInstance();
			nf.setMaximumFractionDigits(5);
			String ratio = nf.format((double)learnTweetsUser / (double)learnTweets);
			
			String learnCountUserContent = "@" + targetUser + "さんから学習したツイートは" + learnTweetsUser +
					"個です。その中の学習要素数は" + learnElements(targetUser) + "個です。学習比率は全体の" + ratio + "です。";
			Tweet("@" + status.getUser().getScreenName() + " " + learnCountUserContent, status.getId());
		}
		resultSet.close();
	}
	//要素数返却（ユーザーがnullの場合は全体の要素数を返却）
	public static long learnElements(String user) throws SQLException{
		if(user == null){
			resultSet = stmt.executeQuery("select count(content) from momoka");
		}else{
			resultSet = stmt.executeQuery("select count(content) from momoka where screen_name = '" + user + "'");
		}
		return Long.parseLong(resultSet.getString(1));
	}
	//学習した総ツイート数
	public static long learnTweets() throws SQLException{
		resultSet = stmt.executeQuery("select count(distinct tweetId) from momoka");
		return Long.parseLong(resultSet.getString(1));
	}
	//学習したユーザーの総ツイート数
	public static long learnTweetsUser(String user) throws SQLException{
		resultSet = stmt.executeQuery("select count(distinct tweetId) from momoka where screen_name = '" + user + "'");
		return Long.parseLong(resultSet.getString(1));
	}
}