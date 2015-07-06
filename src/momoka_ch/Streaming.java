package momoka_ch;

import java.sql.SQLException;
import java.util.Random;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.UserStreamAdapter;

public class Streaming extends UserStreamAdapter{
	
	private Random random = Momoka.random;
	private String MyScreenName = Momoka.MyScreenName;
	
	private int ratio_tweet = 9;
	private int ratio_learn = 6;
	private int ratio_meshi = 2;
	
	@Override
	public void onStatus(Status status){
		//ランダムツイート
		if(random.nextInt(ratio_tweet) == 0){
			try {
				Momoka.randomTweet();
			} catch (SQLException e) {
				Momoka.Tweet(e.toString(), -1);
			}
		}
		if(!status.isRetweet()){
			//学習
			if(random.nextInt(ratio_learn) == 0 && !status.getUser().getScreenName().equals(MyScreenName)){
				try {
					Momoka.learn(status);
				} catch (TwitterException e) {
					Momoka.Tweet(e.toString(), -1);
				}
			}
			//TL反応
			if(status.getText().startsWith("ももか") && status.getText().length() < 10 &&
					!status.getUser().getScreenName().equals(MyScreenName)){
				Momoka.Tweet("@" + status.getUser().getScreenName() + " はいっ！", status.getId());
			}
			//飯
			if(status.getText().matches(".*((おなか|お腹)(すいた|空いた)|空腹|腹減|はらへ).*") &&
					random.nextInt(ratio_meshi) == 0 && !status.getUser().getScreenName().equals(MyScreenName)){
				try {
					Momoka.meshiTero(status);
				} catch (TwitterException e) {
					Momoka.Tweet(e.toString(), -1);
				}
			}
			//学習カウント //ここからelse if続き
			if(status.getText().equals("@" + MyScreenName + " learn")){
				try {
					Momoka.learnCount(status);
				} catch (SQLException e) {
					Momoka.Tweet(e.toString(), -1);
				}
			}
			else if(status.getText().startsWith("@" + MyScreenName + " learn from @")){
				String learnedUser = status.getText().substring(MyScreenName.length() + 14);
				try {
					Momoka.learnCountUser(status, learnedUser);
				} catch (SQLException e) {
					Momoka.Tweet(e.toString(), -1);
				}
			}
			//ping
			else if(status.getText().equals("@" + MyScreenName + " ping")){
				Momoka.ping(status);
			}
			//Memory
			else if(status.getText().equals("@" + MyScreenName + " Memory")){
				Momoka.Memory(status);
			}
			//wakati
			else if(status.getText().startsWith("@" + MyScreenName + " wakati ") && admin(status)){
				String content = status.getText().substring(MyScreenName.length() + 9);
				Momoka.wakati(content, status.getUser().getScreenName(), status.getId());
			}
			//information
			else if(status.getText().startsWith("@" + MyScreenName + " info") && admin(status)){
				Momoka.info(status, ratio_learn, ratio_tweet, ratio_meshi);
			}
			//会話
			else if(status.getText().startsWith("@" + MyScreenName) && !status.getUser().getScreenName().equals(MyScreenName)){
				Momoka.dialogue(status);
			}
		}
	}
	
	public boolean admin(Status status){
		if(!status.isRetweet()){
			if(status.getUser().getScreenName().equals("sugtao4423") || status.getUser().getScreenName().equals("flum_"))
				return true;
		}
		return false;
	}
}