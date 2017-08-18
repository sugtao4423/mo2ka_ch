package mo2ka.functions;

import java.util.List;

import mo2ka.Momoka;
import twitter4j.Status;

public class Wakati{

	public Wakati(Status status, String myScreenName){
		String content = status.getText().substring(myScreenName.length() + 9);
		List<String> list = Momoka.tagger.wakati(content);
		String result = list.toString().substring(1);
		result = result.substring(0, result.length() - 1);
		new Tweet("@" + status.getUser().getScreenName() + " " + result, status.getId());
	}

}
