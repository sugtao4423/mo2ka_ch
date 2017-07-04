package mo2ka.functions;

import java.sql.SQLException;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mo2ka.DBUtils;
import mo2ka.Momoka;
import mo2ka.Word;
import net.reduls.igo.Morpheme;
import twitter4j.ExtendedMediaEntity;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

public class Learn{

	public Learn(Status status){
		String content = deleteUnnecessaryText(status);
		if(content.isEmpty())
			return;

		if(status.getInReplyToStatusId() == -1)
			normalLearn(status, content);
		else
			talkLearn(status, content);
	}

	private void normalLearn(Status status, String content){
		List<Morpheme> morpheme = Momoka.tagger.parse(content);
		Word[] words = new Word[morpheme.size()];
		try{
			for(int i = 0; i < words.length; i++)
				words[i] = new Word(morpheme.get(i).surface, morpheme.get(i).feature.replace(",", "-"));
		}catch(ParseException e){
			System.err.println("Did not parse mecab feature\nMessage: " + e.getMessage());
			return;
		}

		boolean fav = true;
		try{
			DBUtils.insertPartsList(words, status);
			DBUtils.insertMarkov(words, status);
		}catch(SQLException e){
			if(e.getErrorCode() == 19)
				fav = false;
			else
				new Tweet(e.getMessage());
		}catch(ParseException e){
			return;
		}

		for(String s : Momoka.NOT_FAVORITE_USER){
			if(s.equals(status.getUser().getScreenName()))
				fav = false;
		}

		// TODO uncommentout, delline sysout
//		try{
			if(fav)
				System.out.println("learned from @" + status.getUser().getScreenName());
//				Momoka.twitter.createFavorite(status.getId());
//		}catch(TwitterException e){
//		}
	}

	private void talkLearn(Status status, String content){
		// TODO
	}

	private String deleteUnnecessaryText(Status status){
		String result = status.getText();

		UserMentionEntity[] mentionEntity = status.getUserMentionEntities();
		if(mentionEntity != null && mentionEntity.length > 0){
			for(UserMentionEntity mention : mentionEntity){
				Pattern p = Pattern.compile(String.format("\\s*@%s\\s*", mention.getScreenName()));
				Matcher m = p.matcher(result);
				if(m.find())
					result = m.replaceAll("");
			}
		}

		URLEntity[] urlEntity = status.getURLEntities();
		if(urlEntity != null && urlEntity.length > 0){
			for(URLEntity url : urlEntity){
				Pattern p = Pattern.compile(String.format("\\s*%s\\s*", url.getText()));
				Matcher m = p.matcher(result);
				if(m.find())
					result = m.replaceAll("");
			}
		}

		MediaEntity[] mediaEntity = status.getMediaEntities();
		if(mediaEntity != null && mediaEntity.length > 0){
			for(MediaEntity media : mediaEntity){
				Pattern p = Pattern.compile(String.format("\\s*%s\\s*", media.getText()));
				Matcher m = p.matcher(result);
				if(m.find())
					result = m.replaceAll("");
			}
		}

		HashtagEntity[] hashEntity = status.getHashtagEntities();
		if(hashEntity != null && hashEntity.length > 0){
			for(HashtagEntity hash : hashEntity){
				Pattern p = Pattern.compile(String.format("\\s*#%s\\s*", hash.getText()));
				Matcher m = p.matcher(result);
				if(m.find())
					result = m.replaceAll("");
			}
		}

		result = result.replace("‘", "'");
		result = result.replace("’", "'");
		return Normalizer.normalize(result, Normalizer.Form.NFKC);
	}

}
