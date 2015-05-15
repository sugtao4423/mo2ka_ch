package momoka_ch;

import java.sql.SQLException;
import java.util.Random;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.UserStreamAdapter;

public class Streaming extends UserStreamAdapter{
	
	private Random random = Momoka.random;
	private String MyScreenName = Momoka.MyScreenName;
	
	@Override
	public void onStatus(Status status){
		//学習
		if(random.nextInt(3) == 0){
			try {
				Momoka.learn(status);
			} catch (TwitterException e) {
				Momoka.Tweet(e.toString(), -1);
			}
		}
		//ランダムツイート
		if(random.nextInt(5) == 0){
			try {
				Momoka.randomTweet();
			} catch (SQLException e) {
				Momoka.Tweet(e.toString(), -1);
			}
		}
		//TL反応
		if(status.getText().startsWith("ももかちゃん") && status.getText().length() < 8 && !status.isRetweet() && !status.getUser().getScreenName().equals(MyScreenName)){
			Momoka.Tweet("@" + status.getUser().getScreenName() + " はいっ！", status.getId());
		}
		
		if(status.getText().startsWith("@" + MyScreenName) && !status.isRetweet()){
			Momoka.dialogue(status);
		}
	}
}
