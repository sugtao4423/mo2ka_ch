package mo2ka;

import java.sql.SQLException;
import java.util.Random;

import mo2ka.functions.CreateTweet;
import mo2ka.functions.Info;
import mo2ka.functions.Learn;
import mo2ka.functions.LearnCount;
import mo2ka.functions.Memory;
import mo2ka.functions.MeshiTero;
import mo2ka.functions.Ping;
import mo2ka.functions.TimeLineReaction;
import mo2ka.functions.Tweet;
import mo2ka.functions.Wakati;
import mo2ka.functions.WhatIs;
import twitter4j.Status;
import twitter4j.UserStreamAdapter;

public class Streaming extends UserStreamAdapter{

	private static Random rnd;
	private static String myScreenName;

	private int ratio_tweet = 10;
	private int ratio_meshi = 3;

	public Streaming(){
		rnd = new Random();
		myScreenName = Momoka.myScreenName;
	}

	@Override
	public void onStatus(Status status){
		// ランダムツイート
		if(rnd.nextInt(ratio_tweet) == 0){
			try{
				String str = new CreateTweet().getResult();
				new Tweet(str + "\nvia new mo2ka");
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
		if(status.getText().matches(".*((おなか|お腹)(すいた|空いた)|空腹|腹減|はらへ).*") && rnd.nextInt(ratio_meshi) == 0
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
			new Info(status, ratio_tweet, ratio_meshi);
		}
		// what is?
		else if(status.getText().startsWith("@" + myScreenName) && status.getText().endsWith("って何？")){
			String what = status.getText().substring(myScreenName.length() + 2, status.getText().length() - 4);
			new WhatIs(status, what);
		}
	}

	private boolean isLearnTarget(Status status){
		boolean result = false;
		if(status.getUserMentionEntities().length > 0)
			return false;
		String via = status.getSource().replaceAll("<.+?>", "");
		for(String s : Momoka.LEARN_VIA){
			if(via.matches(".*" + s + ".*"))
				result = true;
		}
		for(String s : Momoka.NOT_LEARN_TEXT){
			if(status.getText().matches(s))
				result = false;
		}
		return result;
	}
}
