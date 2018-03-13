package mo2ka.functions;

import java.util.Random;

import mo2ka.Momoka;
import twitter4j.Status;

public class TimeLineReaction{

	public TimeLineReaction(Status status){
		int i = new Random().nextInt(Momoka.reactionWord.length);
		String tweet = "@" + status.getUser().getScreenName() + " " + Momoka.reactionWord[i];
		new Tweet(tweet, status.getId());
	}

}
