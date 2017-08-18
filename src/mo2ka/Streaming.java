package mo2ka;

import java.sql.SQLException;
import java.util.Random;

import mo2ka.functions.CreateTweet;
import mo2ka.functions.Learn;
import mo2ka.functions.Tweet;
import twitter4j.Status;
import twitter4j.UserStreamAdapter;

public class Streaming extends UserStreamAdapter{

	private static Random rnd;
	private static String myScreenName;

	private int ratio_tweet = 10;
	private int ratio_meshi = 3;

	public Streaming(){
		rnd = new Random();
		myScreenName = Momoka.myScreenName;
	}

	@Override
	public void onStatus(Status status){
		// ランダムツイート
		if(rnd.nextInt(ratio_tweet) == 0){
			try{
				String str = new CreateTweet().getResult();
				new Tweet(str + "\nvia new mo2ka");
			}catch(SQLException e){
				System.err.println("Could not create tweet\nMessage: " + e.getMessage());
			}
		}

		if(status.isRetweet() || status.getUser().getScreenName().equals(myScreenName))
			return;

		if(isLearnTarget(status))
			new Learn(status);
	}

	private boolean isLearnTarget(Status status){
		boolean result = false;
		if(status.getUserMentionEntities().length > 0)
			return false;
		String via = status.getSource().replaceAll("<.+?>", "");
		for(String s : Momoka.LEARN_VIA){
			if(via.matches(".*" + s + ".*"))
				result = true;
		}
		for(String s : Momoka.NOT_LEARN_TEXT){
			if(status.getText().matches(s))
				result = false;
		}
		return result;
	}
}
