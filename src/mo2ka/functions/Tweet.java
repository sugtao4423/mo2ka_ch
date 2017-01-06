package mo2ka.functions;

import mo2ka.Momoka;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;

public class Tweet{

	public Tweet(String text){
		try{
			if(text.length() > 140)
				text = text.substring(0, 140);
			Momoka.twitter.updateStatus(text);
		}catch(TwitterException e){
		}
	}

	public Tweet(String text, long replyId){
		try{
			if(text.length() > 140)
				text = text.substring(0, 140);
			Momoka.twitter.updateStatus(new StatusUpdate(text).inReplyToStatusId(replyId));
		}catch(TwitterException e){
		}
	}

}
