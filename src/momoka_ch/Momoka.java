package momoka_ch;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.ResponseList;
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
	
	private static long startTime;
	
	private static Twitter twitter;
	private static TwitterStream stream;
	
	private static Connection conn;
	private static Statement stmt;
	private static Tagger tagger;
	private static String[] NOT_LEARN_TEXT;
	private static String[] NOT_LEARN_VIA;
	private static String[] NOT_FAVORITE_USER;
	private static ResultSet resultSet;
	
	private static Dialogue dialogue;
	private static DialogueRequestParam dialogueParam;
	private static List<String> dialogueContext;
	private static List<String> dialogueContextUser;
	
	private static List<String> meshiText;
	private static Pattern urlPattern, mentionPattern, learnPattern;

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
				"#anime",
				"MinecraftServer",
				"Namer"
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
		NOT_FAVORITE_USER = new String[]{
				"nkpoid"
		};
		InputStream is = Momoka.class.getResourceAsStream("properties");
        Properties prop = new Properties();
        prop.load(is);
        is.close();
        
		dialogueContext = new ArrayList<String>();
		dialogueContextUser = new ArrayList<String>();
		AuthApiKey.initializeAuth(prop.getProperty("docomoAPI"));
		dialogue = new Dialogue();
		dialogueParam = new DialogueRequestParam();
		
		urlPattern = Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+", Pattern.DOTALL);
		mentionPattern = Pattern.compile("@\\w*", Pattern.DOTALL);
		learnPattern = Pattern.compile("(.+),\\s(.+),\\s(.+)", Pattern.DOTALL);
		
        String CK = prop.getProperty("CK");
        String CS = prop.getProperty("CS");
        String AT = prop.getProperty("AT");
        String ATS = prop.getProperty("ATS");
		Configuration jconf = new ConfigurationBuilder()
		.setOAuthConsumerKey(CK)
		.setOAuthConsumerSecret(CS).build();
		AccessToken accessToken = new AccessToken(AT, ATS);
		twitter = new TwitterFactory(jconf).getInstance(accessToken);
		stream = new TwitterStreamFactory(jconf).getInstance(accessToken);
		MyScreenName = twitter.getScreenName();
		twitter.updateStatus("ももかちゃん起動 (" + new SimpleDateFormat("MM/dd HH:mm:ss").format(new Date()) + ")");
		System.out.println(MyScreenName);
		
		startTime = new Date().getTime();
		
		meshiText = new ArrayList<String>();
		ResponseList<Status> meshiResponse = twitter.getUserTimeline("meshiuma_yonaka", new Paging(1, 200));
		for(Status status : meshiResponse){
			MediaEntity[] mentitys = status.getMediaEntities();
			if(mentitys != null && mentitys.length > 0 && !status.isRetweet()){
	            for(int i = 0; i < mentitys.length; i++)
	            	meshiText.add(status.getText());
			}
		}
		meshiResponse.clear();
		
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
			location = "/var/www/html/products/momoka/database.db";
		File DB = new File(location);
		if(!DB.exists()){
			DB.createNewFile();
		}
		conn = DriverManager.getConnection("jdbc:sqlite:" + location);
		stmt = conn.createStatement();
		stmt.execute("create table if not exists momoka(content text, screen_name text, tweetId text, via text)");
	}
	//ツイート
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
		
		if(!NOT_LEARN_TEXT_BOOL && !NOT_LEARN_VIA_BOOL){
			String content;
			
			Matcher URL = urlPattern.matcher(status.getText());
			if(URL.find())
				content = URL.replaceAll("");
			else
				content = status.getText();
			
			Matcher mention = mentionPattern.matcher(content);
			if(mention.find())
				content = mention.replaceAll("");
			
			content = content.replace("#", "");
			
			List<String> list = tagger.wakati(content);
			list.add(0, "[BEGIN]");
			list.add("[END]");
			boolean reallyFav = false;
			for(int i = 2; list.size() > i; i++){
				boolean learn = false;
				try {
					String tango = list.get(i - 2).replace("'", "''") + ", " +
							list.get(i - 1).replace("'", "''") + ", " + 
							list.get(i).replace("'", "''");
					
					resultSet = stmt.executeQuery("select content from momoka where content = '" + tango + "'");
					learn = resultSet.next();
					
					if(!learn){
						stmt.execute("insert into momoka values('" + tango
								+ "', '" + status.getUser().getScreenName() + "', '" + status.getId() + "', '"
								+ status.getSource().replaceAll("<.+?>", "")
								+"')");
						reallyFav = true;
					}
				} catch (SQLException e) {
					Tweet(e.toString(), -1);
				}
			}
			if(reallyFav){
				for(int i = 0; NOT_FAVORITE_USER.length > i; i++){
					if(NOT_FAVORITE_USER[i].equals(status.getUser().getScreenName()))
						reallyFav = false;
				}
				if(reallyFav)
					twitter.createFavorite(status.getId());
			}
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
		resultSet = stmt.executeQuery("select content from momoka where content like '[BEGIN]%'");
		ArrayList<String> array = new ArrayList<String>();
		String sentence;
		while(resultSet.next())
			array.add(resultSet.getString(1));
		String[] elements = string2StringArray(randomArray2String(array));
		sentence = elements[1] + elements[2];
		
		ArrayList<String> selectedWords = new ArrayList<String>();
		for(int i = 0; i < 800; i++){
			array.clear();
			if(elements[2].equals("[END]"))
				break;
			resultSet = stmt.executeQuery("select content from momoka where content like '" + elements[2] + "%'");
			while(resultSet.next())
				array.add(resultSet.getString(1));
			if(array.size() <= 1)
				break;
			String randomString = randomArray2String(array);
			if(selectedWords.indexOf(randomString) == -1)
				selectedWords.add(randomString);
			else
				continue;
			String[] tmp = string2StringArray(randomString);
			if(elements[2].equalsIgnoreCase(tmp[0])){
				elements = tmp;
				sentence += elements[1] + elements[2];
			}
		}
		sentence = sentence.replace("[END]", "");
		Tweet(sentence, -1);
		array.clear();
		selectedWords.clear();
		resultSet.close();
	}
	public static String randomArray2String(ArrayList<String> array){
		return array.get(random.nextInt(array.size() - 1));
	}
	public static String[] string2StringArray(String str){
		Matcher m = learnPattern.matcher(str);
		if(m.find()){
			return new String[]{m.group(1), m.group(2), m.group(3)};
		}else{
			return null;
		}
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
	//飯テロ
	public static void meshiTero(Status status) throws TwitterException{
		int meshiRandom = random.nextInt(meshiText.size() - 1);
		Tweet("@" + status.getUser().getScreenName() + " " + meshiText.get(meshiRandom), status.getId());
	}
	//ping
	public static void ping(Status status){
		long tweetId2time = (status.getId() >> 22) + 1288834974657L;
		long now = new Date().getTime();
		Tweet("@" + status.getUser().getScreenName() + " " + String.valueOf((double)(tweetId2time - now) / 1000), status.getId());
	}
	//Memory
	public static void Memory(Status status){
		long free, total, max, used;
		DecimalFormat f1, f2;
		f1 = new DecimalFormat("#,###MB");
		f2 = new DecimalFormat("##.#");
		free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
		total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
		max = Runtime.getRuntime().maxMemory() /1024 / 1024;
		used = total - free;
		double per = (used * 100 / (double)total);
		String message = "@" + status.getUser().getScreenName() + "\n合計：" + f1.format(total) + " \n使用量：" + f1.format(used) +
				" (" + f2.format(per) + "%)" + "\n使用可能最大：" + f1.format(max);
		Tweet(message, status.getId());
	}
	//wakati
	public static void wakati(String content, String user, long tweetId){
		List<String> list = tagger.wakati(content);
		String result = list.toString().substring(1);
		result = result.substring(0, result.length() - 1);
		Tweet("@" + user + " " + result, tweetId);
	}
	//info
	public static void info(Status status, int ratio_learn, int ratio_tweet, int ratio_meshi){
		long time = (new Date().getTime() - startTime) / 1000;
		
		long day = time / 86400;
		long hour = (time - day * 86400) / 3600;
		long minute = (time - day * 86400 - hour * 3600) / 60;
		long second = time - day * 86400 - hour * 3600 - minute * 60;
		
		String result = "";
		if(day != 0L)
			result += day + "日";
		if(hour != 0L)
			result += hour + "時間";
		if(minute != 0L)
			result += minute + "分";
		if(second != 0L)
			result += second + "秒";
		
		String message = "@" + status.getUser().getScreenName() + " 学習頻度は1/" + ratio_learn + ", 呟く頻度は1/" +
		ratio_tweet + ", 飯テロ頻度は1/" + ratio_meshi + "\n連続稼働時間は" + result + "です";
		Tweet(message, status.getId());
	}
}