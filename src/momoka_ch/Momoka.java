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
import java.util.Collections;
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
import net.reduls.igo.Morpheme;
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
	public static String SQLITE_RANDOM = " ORDER BY RANDOM() LIMIT 1";
	
	private static long startTime;
	
	private static Twitter twitter;
	private static TwitterStream stream;
	
	private static Connection conn, wikiconn;
	private static Statement stmt, wikistmt;
	private static Tagger tagger;
	private static String[] NOT_LEARN_TEXT, NOT_LEARN_VIA, NOT_FAVORITE_USER, REACTION_WORDS;
	private static ResultSet resultSet;
	
	private static Dialogue dialogue;
	private static DialogueRequestParam dialogueParam;
	private static List<String> dialogueContext, dialogueContextUser, meshiText;
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
				"osu! notification via tao",
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
		REACTION_WORDS = new String[]{
				"はいっ！",
				"お呼びですかご主人さま？",
				"え〜、やだ、なんですか〜？",
				"なにかありましたぁ？",
				"お呼びでしょうか？",
				"あなたの元気な顔が見れて嬉しいです",
				"わわっ、びっくりしました…",
				"今、呼びました？",
				"なにかご用ですか？",
				"あ、あの、何か…？",
				"ど、どうしました？"
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
		
		urlPattern = Pattern.compile("(\\s)?http(s)?://[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+", Pattern.DOTALL);
		mentionPattern = Pattern.compile("@[0-9a-zA-Z_]+(\\s)?", Pattern.DOTALL);
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
			location = "/Users/tao/Desktop/momoka.db";
		else
			location = "/home/tao/data/momoka.db";
		File DB = new File(location);
		if(!DB.exists()){
			DB.createNewFile();
		}
		conn = DriverManager.getConnection("jdbc:sqlite:" + location);
		stmt = conn.createStatement();
		stmt.execute("create table if not exists momoka(content unique, screen_name, tweetId, via)");
		stmt.execute("create table if not exists talk(t1, t2, unique(t1, t2))");
		
		wikiconn = DriverManager.getConnection("jdbc:sqlite:/home/tao/data/wikiData.db");
		wikistmt = wikiconn.createStatement();
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
		for(String s : NOT_LEARN_TEXT){
			if(status.getText().matches(s))
				NOT_LEARN_TEXT_BOOL = true;
		}
		for(String s : NOT_LEARN_VIA){
			if(status.getSource().replaceAll("<.+?>", "").equals(s))
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
			boolean reallyFav = true;
			for(int i = 2; i < list.size(); i++){
				try {
					String tango = list.get(i - 2).replace("'", "''") + ", " +
							list.get(i - 1).replace("'", "''") + ", " + 
							list.get(i).replace("'", "''");
					
					stmt.execute("insert into momoka values('" + tango + "', '"
							+ status.getUser().getScreenName() + "', '" + status.getId() + "', '"
							+ status.getSource().replaceAll("<.+?>", "")
							+"')");
				} catch (SQLException e) {
					if(e.getErrorCode() == 19)
						reallyFav = false;
					else
						Tweet(e.toString(), -1);
				}
			}
			if(reallyFav){
				for(String noFavUser : NOT_FAVORITE_USER){
					if(noFavUser.equals(status.getUser().getScreenName()))
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
			return;
		}
		if(userIndex != -1){
			dialogueContext.set(userIndex, resultData.getContext());
		}else{
			dialogueContextUser.add(status.getUser().getScreenName());
			dialogueContext.add(resultData.getContext());
		}
		Tweet("@" + status.getUser().getScreenName() + " " + resultData.getUtt(), status.getId());
		//会話を2段階で保存
		ArrayList<String> receiveUtt = new ArrayList<String>();
		ArrayList<String> sendUtt = new ArrayList<String>();
		
		Matcher urlMatch = urlPattern.matcher(content);
		if(urlMatch.find())
			content = urlMatch.replaceAll("");
		
		Matcher mentionMatch = mentionPattern.matcher(content);
		if(mentionMatch.find())
			content = mentionMatch.replaceAll("");
		
		content = content.replace("#", "");
		
		String resultUtt = resultData.getUtt();
		urlMatch = urlPattern.matcher(resultUtt);
		if(urlMatch.find())
			resultUtt = urlMatch.replaceAll("");
		
		mentionMatch = mentionPattern.matcher(resultUtt);
		if(mentionMatch.find())
			resultUtt = mentionMatch.replaceAll("");
		
		resultUtt = resultUtt.replace("#", "");
		
		List<Morpheme> receive = tagger.parse(content);
		List<Morpheme> send = tagger.parse(resultUtt);
		boolean receive_noun = false, send_noun = false;
		for(Morpheme m : receive){
			if(m.feature.startsWith("名詞")){
				receive_noun = true;
				receiveUtt.add(m.surface);
			}
		}
		for(Morpheme m : send){
			if(m.feature.startsWith("名詞")){
				send_noun = true;
				sendUtt.add(m.surface);
			}
		}
		if(!receive_noun){
			int i = random.nextInt(receive.size());
			receiveUtt.add(receive.get(i).surface);
		}
		if(!send_noun){
			int i = random.nextInt(send.size());
			sendUtt.add(send.get(i).surface);
		}
		String insertReceive = "";
		String insertSend = "";
		for(String s : receiveUtt)
			insertReceive += s.replace("'", "''") + ",";
		for(String s : sendUtt)
			insertSend += s.replace("'", "''") + ",";
		try{
			stmt.execute("insert into talk values('" + insertReceive + "', '" + insertSend + "')");
		} catch (SQLException e) {
			if(e.getErrorCode() != 19)
				Tweet(e.toString(), -1);
		}
	}
	//会話を単語で学習してしゃべる
	//今のところadmin、すなわち、@sugtao4423(←ノンケ) と @flum_(←ホモ)専用
	public static void learnedTalk(Status status){
		String content = status.getText().substring(MyScreenName.length() + 2);
		List<Morpheme> contentWords = tagger.parse(content);
		ArrayList<String> words = new ArrayList<String>();
		boolean find_noun = false;
		for(Morpheme m : contentWords){
			if(m.feature.startsWith("名詞")){
				find_noun = true;
				words.add(m.surface);
			}
		}
		String oneWord;
		if(!find_noun){
			int i = random.nextInt(contentWords.size());
			oneWord = contentWords.get(i).surface;
		}else{
			int i = random.nextInt(words.size());
			oneWord = words.get(i);
		}
		
		try {
			resultSet = stmt.executeQuery("select t2 from talk where t1 like '%" + oneWord + "%'");
			if(!resultSet.next()){
				dialogue(status);
				return;
			}
			ArrayList<String> result = new ArrayList<String>();
			String[] t2 = resultSet.getString(1).split(",", 0);
			int index = random.nextInt(t2.length);
			resultSet = stmt.executeQuery("select content from momoka where content like '%" + t2[index] + "'" + SQLITE_RANDOM);
			if(!resultSet.next()){
				dialogue(status);
				return;		
			}
			String[] c = string2StringArray(resultSet.getString(1));
			if(c[0].equals("[BEGIN]")){
				result.add(c[2]);
				result.add(c[1]);
			}else{
				result.add(c[2]);
				result.add(c[1]);
				result.add(c[0]);
				for(int i = 0; i < 800; i++){
					resultSet = stmt.executeQuery("select content from momoka where content like '%" + c[0] + "'" + SQLITE_RANDOM);
					if(!resultSet.next())
						break;
					c = string2StringArray(resultSet.getString(1));
					if(c[0].equals("[BEGIN]")){
						result.add(c[1]);
						break;
					}
					result.add(c[1]);
					result.add(c[0]);
				}
				Collections.reverse(result);
				resultSet = stmt.executeQuery("select content from momoka where content like '" + t2[index] + "%'" + SQLITE_RANDOM);
				if(!resultSet.next()){
					c = string2StringArray(resultSet.getString(1));
					if(c[2].equals("[END]")){
						result.add(c[1]);
					}else{
						result.add(c[1]);
						result.add(c[2]);
						for(int i = 0; i < 800; i++){
							resultSet = stmt.executeQuery("select content from momoka where content like '" + c[2] + "%'" + SQLITE_RANDOM);
							if(!resultSet.next())
								break;
							c = string2StringArray(resultSet.getString(1));
							if(c[2].equals("[END]")){
								result.add(c[1]);
								break;
							}
							result.add(c[1]);
							result.add(c[2]);
						}
					}
				}
			}
			String tweet = "";
			for(String s : result)
				tweet += s;
			Tweet("@" + status.getUser().getScreenName() + " " + tweet, status.getId());
		} catch (SQLException e) {
			Tweet(e.toString(), -1);
		}
	}
	
	//乱数によってツイート
	public static void randomTweet() throws SQLException{
		resultSet = stmt.executeQuery("select content from momoka where content like '[BEGIN]%'" + SQLITE_RANDOM);
		String sentence;
		if(!resultSet.next())
			return;
		String[] elements = string2StringArray(resultSet.getString(1));
		sentence = elements[1] + elements[2];
		
		ArrayList<String> selectedWords = new ArrayList<String>();
		for(int i = 0; i < 800; i++){
			if(elements[2].equals("[END]"))
				break;
			resultSet = stmt.executeQuery("select content from momoka where content like '" + elements[2] + "%'" + SQLITE_RANDOM);
			if(!resultSet.next())
				break;
			String randomString = resultSet.getString(1);
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
		selectedWords.clear();
		resultSet.close();
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
	//会話の学習個数
	public static void learnTalkCount(Status status){
		try {
			resultSet = stmt.executeQuery("select count(*) from talk");
			if(!resultSet.next()){
				Tweet("@" + status.getUser().getScreenName() + " 学習した会話の単語セットは0個です", status.getId());
			}else{
				String s = resultSet.getString(1);
				Tweet("@" + status.getUser().getScreenName() + " 学習した会話の単語セットは" + s + "個です", status.getId());
			}
		} catch (SQLException e) {
			Tweet(e.toString(), -1);
		}
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
	//TL反応
	public static void timeLineReaction(Status status){
		int i = random.nextInt(REACTION_WORDS.length);
		String tweet = "@" + status.getUser().getScreenName() + " " + REACTION_WORDS[i];
		Tweet(tweet, status.getId());
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
	public static void wakati(Status status){
		String content = status.getText().substring(MyScreenName.length() + 9);
		List<String> list = tagger.wakati(content);
		String result = list.toString().substring(1);
		result = result.substring(0, result.length() - 1);
		Tweet("@" + status.getUser().getScreenName() + " " + result, status.getId());
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
	//○○って何？
	public static void whatIs(Status status){
		String what = status.getText().substring(MyScreenName.length() + 2, status.getText().length() - 4);
		
		try {
			String tweet = "";
			resultSet = wikistmt.executeQuery("select * from wiki where title = '" + what + "'");
			if(resultSet.next()){
				String description = resultSet.getString(2);
				if(description.equals("")){
					tweet = resultSet.getString(1) + " " + resultSet.getString(3);
				}else{
					int head = status.getUser().getScreenName().length() + 2;
					String title = resultSet.getString(1);
					String url = resultSet.getString(3);
					
					int descriptionSize;
					if(description.startsWith(title))
						descriptionSize = 140 - head - 36;
					else
						descriptionSize = 140 - head - title.length() - 37;
					
					if(description.length() > descriptionSize)
						description = description.substring(0, descriptionSize - 3) + "...";
					
					if(description.startsWith(title))
						tweet = description + " " + url;
					else
						tweet = title + "：" + description + " " + url;
				}
			}else{
				resultSet = wikistmt.executeQuery("select title from wiki where description like '%" + what + "%'");
				String[] arr = new String[5];
				boolean isfind = false;
				for(int i = 0; i < 5; i++){
					if(resultSet.next()){
						isfind = true;
						arr[i] = resultSet.getString(1);
					}else break;
				}
				if(isfind){
					for(String s : arr){
						if(s != null)
							tweet += s + ", ";
					}
					tweet = "単語が見つかりませんでした。もしかして：" + tweet.substring(0, tweet.length() - 2);
				}else{
					tweet = "単語や、関連するような単語も見つかりませんでした";
				}
			}
			twitter.updateStatus(new StatusUpdate("@" + status.getUser().getScreenName() + " " + tweet).inReplyToStatusId(status.getId()));
		} catch (SQLException | TwitterException e) {
			Tweet(e.toString(), -1);
		}
	}
}