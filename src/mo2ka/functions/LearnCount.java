package mo2ka.functions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;

import mo2ka.Momoka;
import twitter4j.Status;

public class LearnCount{

	public void learnCount(Status status){
		try{
			String counts[] = new String[4];
			counts[0] = String.format("%,d", getCount("select count(distinct tweetId) from parts"));
			counts[1] = String.format("%,d", getCount("select count(*) from parts"));
			counts[2] = String.format("%,d", getCount("select count(distinct tweetId) from markov"));
			counts[3] = String.format("%,d", getCount("select count(*) from markov"));

			String tweet = String.format("ツイート数 / 全要素数\n品詞: %s / %s\nマルコフ: %s / %s", counts[0], counts[1], counts[2], counts[3]);
			new Tweet("@" + status.getUser().getScreenName() + " " + tweet, status.getId());
		}catch(SQLException e){
			System.err.println("Failed sql query of learnCount()\nMessage: " + e.getMessage());
		}
	}

	public void learnCountFromUser(Status status, String targetUser){
		try{
			String where = " where screen_name = '" + targetUser.replace("@", "") + "';";
			int[] counts = new int[6];
			counts[0] = getCount("select count(*) from parts");
			counts[1] = getCount("select count(*) from markov");
			counts[2] = getCount("select count(distinct tweetId) from markov");
			counts[3] = getCount("select count(*) from parts" + where);
			counts[4] = getCount("select count(distinct tweetId) from markov" + where);
			counts[5] = getCount("select count(*) from markov" + where);

			NumberFormat percent = NumberFormat.getPercentInstance();
			percent.setMaximumFractionDigits(5);
			String partsPer = percent.format((double)counts[3] / (double)counts[0]);
			String markovTweetPer = percent.format((double)counts[4] / (double)counts[2]);
			String markovElementsPer = percent.format((double)counts[5] / (double)counts[1]);

			String tweet = String.format(targetUser + "さんから学習した件数\n品詞: %s件  %s\nマルコフ: ツイート%s件  %s / 要素%s件  %s",
					String.format("%,d", counts[3]), partsPer,
					String.format("%,d", counts[4]), markovTweetPer, String.format("%,d", counts[5]), markovElementsPer);
			new Tweet("@" + status.getUser().getScreenName() + " " + tweet, status.getId());
		}catch(SQLException e){
			System.err.println("Failed sql query of learnCountFromUser()\nMessage: " + e.getMessage());
		}
	}

	private int getCount(String sql) throws SQLException{
		ResultSet r = Momoka.stmt.executeQuery(sql);
		r.next();
		int result = r.getInt(1);
		r.close();
		return result;
	}

}
