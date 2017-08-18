package mo2ka.functions;

import java.util.Date;

import twitter4j.Status;

public class Ping{

	public Ping(Status status){
		long tweetId2time = (status.getId() >> 22) + 1288834974657L;
		long now = new Date().getTime();
		new Tweet("@" + status.getUser().getScreenName() + " " + String.valueOf((double)(now - tweetId2time) / 1000), status.getId());
	}

}
