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
		if(random.nextInt(6) == 0 && !status.isRetweet() && !status.getUser().getScreenName().equals(MyScreenName)){
			try {
				Momoka.learn(status);
			} catch (TwitterException e) {
				Momoka.Tweet(e.toString(), -1);
			}
		}
		//ランダムツイート
		if(random.nextInt(9) == 0){
			try {
				Momoka.randomTweet();
			} catch (SQLException e) {
				Momoka.Tweet(e.toString(), -1);
			}
		}
		//TL反応
		if(status.getText().startsWith("ももか") && status.getText().length() < 10 && !status.isRetweet() &&
				!status.getUser().getScreenName().equals(MyScreenName)){
			Momoka.Tweet("@" + status.getUser().getScreenName() + " はいっ！", status.getId());
		}
		//学習カウント
		if(status.getText().equals("@" + MyScreenName + " learn") && !status.isRetweet()){
			try {
				Momoka.learnCount(status);
			} catch (SQLException e) {
				Momoka.Tweet(e.toString(), -1);
			}
		}
		else if(status.getText().startsWith("@" + MyScreenName + " learn from @") && !status.isRetweet()){
			String learnedUser = status.getText().substring(MyScreenName.length() + 14);
			try {
				Momoka.learnCountUser(status, learnedUser);
			} catch (SQLException e) {
				Momoka.Tweet(e.toString(), -1);
			}
		}
		else if(status.getText().startsWith("@" + MyScreenName) && !status.isRetweet()){
			Momoka.dialogue(status);
		}
		//飯
		if(status.getText().matches(".*((おなか|お腹)(すいた|空いた)|空腹|腹減|はらへ).*") &&
				!status.isRetweet() && !status.getUser().getScreenName().equals(MyScreenName)){
			try {
				Momoka.meshiTero(status);
			} catch (TwitterException e) {
				Momoka.Tweet(e.toString(), -1);
			}
		}
	}
}
