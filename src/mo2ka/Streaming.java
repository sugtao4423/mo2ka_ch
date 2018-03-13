package mo2ka;

import java.sql.SQLException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mo2ka.functions.CreateTweet;
import mo2ka.functions.Info;
import mo2ka.functions.Learn;
import mo2ka.functions.LearnCount;
import mo2ka.functions.Memory;
import mo2ka.functions.MeshiTero;
import mo2ka.functions.Ping;
import mo2ka.functions.TimeLineReaction;
import mo2ka.functions.Timer;
import mo2ka.functions.Tweet;
import mo2ka.functions.Wakati;
import mo2ka.functions.WhatIs;
import twitter4j.Status;
import twitter4j.UserStreamAdapter;

public class Streaming extends UserStreamAdapter{

	private static Random rnd;
	private static String myScreenName;
	private Pattern preg_timer;
	private Matcher m;

	public Streaming(){
		rnd = new Random();
		myScreenName = Momoka.myScreenName;
		preg_timer = Pattern.compile("^@" + myScreenName + "\\sタイマー(\\d+)(秒|分|時間)");
	}

	@Override
	public void onStatus(Status status){
		// ランダムツイート
		if(rnd.nextInt(Momoka.ratioTweet) == 0){
			try{
				new Tweet(new CreateTweet().getResult());
			}catch(SQLException e){
				System.err.println("Could not create tweet\nMessage: " + e.getMessage());
			}
		}

		// これ以下 リツイート or 自分のツイートには反応しない
		if(status.isRetweet() || status.getUser().getScreenName().equals(myScreenName))
			return;

		// 学習
		if(isLearnTarget(status))
			new Learn(status);

		// TL反応
		if(status.getText().startsWith("ももか") && status.getText().length() < 10
				&& !status.getUser().getScreenName().equals(myScreenName)){
			new TimeLineReaction(status);
		}

		// 飯テロ
		if(status.getText().matches(".*((おなか|お腹)(すいた|空いた)|空腹|腹減|はらへ).*") && rnd.nextInt(Momoka.ratioMeshi) == 0
				&& !status.getUser().getScreenName().equals(myScreenName)){
			new MeshiTero(status);
		}

		// 学習カウント
		if(status.getText().equals("@" + myScreenName + " learn")){
			new LearnCount().learnCount(status);
		}else if(status.getText().startsWith("@" + myScreenName + " learn from ")){
			String learnedUser = status.getText().substring(myScreenName.length() + 13);
			new LearnCount().learnCountFromUser(status, learnedUser);
		}
		// ping
		else if(status.getText().equals("@" + myScreenName + " ping")){
			new Ping(status);
		}
		// memory
		else if(status.getText().equals("@" + myScreenName + " memory")){
			new Memory(status);
		}
		// wakati
		else if(status.getText().startsWith("@" + myScreenName + " wakati ")){
			new Wakati(status, myScreenName);
		}
		// info
		else if(status.getText().equals("@" + myScreenName + " info")){
			new Info(status);
		}
		// what is?
		else if(status.getText().startsWith("@" + myScreenName) && status.getText().endsWith("って何？")){
			String what = status.getText().substring(myScreenName.length() + 2, status.getText().length() - 4);
			new WhatIs(status, what);
		}
		// timer
		else if((m = preg_timer.matcher(status.getText())).find()){
			new Timer(status, m.group(1), m.group(2));
		}
	}

	private boolean isLearnTarget(Status status){
		boolean result = false;
		if(status.getUserMentionEntities().length > 0)
			return false;
		for(String s : Momoka.notLearnUser){
			if(status.getUser().getScreenName().equals(s))
				return false;
		}
		String via = status.getSource().replaceAll("<.+?>", "");
		for(String s : Momoka.learnVia){
			if(via.matches(".*" + s + ".*"))
				result = true;
		}
		for(String s : Momoka.notLearnText){
			if(status.getText().matches(s))
				result = false;
		}
		return result;
	}
}
