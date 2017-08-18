package mo2ka.functions;

import java.sql.ResultSet;
import java.sql.SQLException;

import mo2ka.Momoka;
import twitter4j.Status;

public class WhatIs{

	public WhatIs(Status status, String what){
		try{
			ResultSet r = Momoka.wikiStmt.executeQuery("select abstract from wikipedia where title = '" + what + "'");
			if(r.next()){
				tweet(status, r.getString(1));
				r.close();
				return;
			}

			r = Momoka.wikiStmt.executeQuery("select abstract from wikipedia where title like '" + what + "'");
			if(r.next()){
				tweet(status, r.getString(1));
			}else{
				tweet(status, "単語が見つかりませんでした");
			}
			r.close();
		}catch(SQLException e){
			System.err.println("Failed sql query of class WhatIs constructor\nMessage: " + e.getMessage());
		}
	}

	private void tweet(Status status, String abs){
		int absMaxSize = 140 - status.getUser().getScreenName().length() - 38;
		if(abs.length() > absMaxSize)
			abs = abs.substring(0, absMaxSize - 3) + "...";
		new Tweet("@" + status.getUser().getScreenName() + " " + abs, status.getId());
	}

}
