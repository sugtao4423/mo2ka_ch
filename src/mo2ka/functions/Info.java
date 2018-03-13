package mo2ka.functions;

import java.util.Date;

import mo2ka.Momoka;
import twitter4j.Status;

public class Info{

	public Info(Status status){
		long time = (new Date().getTime() - Momoka.startTime) / 1000;

		long day = time / 86400;
		long hour = (time - day * 86400) / 3600;
		long minute = (time - day * 86400 - hour * 3600) / 60;
		long second = time - day * 86400 - hour * 3600 - minute * 60;

		String result = "";
		if(day != 0L)
			result += day + "日";
		if(hour != 0L)
			result += hour + "時間";
		if(minute != 0L)
			result += minute + "分";
		if(second != 0L)
			result += second + "秒";

		String tweet = "呟く頻度は1/" + Momoka.ratio_tweet + ", 飯テロ頻度は1/" + Momoka.ratio_meshi + "\n連続稼働時間は" + result + "です";
		new Tweet("@" + status.getUser().getScreenName() + " " + tweet, status.getId());
	}

}
