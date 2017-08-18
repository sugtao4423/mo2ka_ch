package mo2ka.functions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;

import mo2ka.Momoka;
import twitter4j.Status;

public class LearnCount{

	public void learnCount(Status status){
		try{
			ResultSet r = Momoka.stmt.executeQuery(
					"select count(distinct tweetId) from parts;"
				  + "select count(*) from parts;"
				  + "select count(distinct tweetId) from markov;"
				  + "select count(*) from markov;");
			String counts[] = new String[4];
			int i = 0;
			while(r.next())
				counts[i++] = String.format("%,d", r.getInt(1));

			String tweet = String.format("ツイート数 / 全要素数\n品詞: %s / %s\nマルコフ: %s / %s", counts[0], counts[1], counts[2], counts[3]);
			new Tweet("@" + status.getUser().getScreenName() + " " + tweet, status.getId());
			r.close();
		}catch(SQLException e){
			System.err.println("Failed sql query of learnCount()\nMessage: " + e.getMessage());
		}
	}

	public void learnCountFromUser(Status status, String targetUser){
		try{
			String where = " where screen_name = '" + targetUser.replace("@", "") + "';";
			ResultSet r = Momoka.stmt.executeQuery(
					"select count(*) from parts;"
				  + "select count(*) from markov;"
				  + "select count(distinct tweetId) from markov;"
				  + "select count(*) from parts" + where
				  + "select count(distinct tweetId) from markov" + where
				  + "select count(*) from markov" + where);
			int[] counts = new int[6];
			int i = 0;
			while(r.next())
				counts[i++] = r.getInt(1);

			NumberFormat percent = NumberFormat.getPercentInstance();
			percent.setMaximumFractionDigits(5);
			String partsPer = percent.format((double)counts[3] / (double)counts[0]);
			String markovTweetPer = percent.format((double)counts[4] / (double)counts[2]);
			String markovElementsPer = percent.format((double)counts[5] / (double)counts[1]);
			
			String tweet = String.format(targetUser + "さんから学習した件数\n品詞: %s件  %s\nマルコフ: ツイート%s件  %s / 要素%s件  %s",
					String.format("%,d", counts[3]), partsPer,
					String.format("%,d", counts[4]), markovTweetPer, String.format("%,d", counts[5]), markovElementsPer);
			new Tweet("@" + status.getUser().getScreenName() + " " + tweet, status.getId());
			r.close();
		}catch(SQLException e){
			System.err.println("Failed sql query of learnCountFromUser()\nMessage: " + e.getMessage());
		}
	}

}
