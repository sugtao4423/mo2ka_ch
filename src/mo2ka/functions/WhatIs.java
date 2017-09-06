package mo2ka.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;

import mo2ka.Momoka;
import twitter4j.Status;

public class WhatIs{

	public WhatIs(Status status, String what){
		try{
			ResultSet r = Momoka.wikiStmt.executeQuery("select * from wikipedia where title = '" + what + "'");
			if(r.next()){
				tweet(status, r.getString(1), r.getString(2));
				r.close();
				return;
			}

			r = Momoka.wikiStmt.executeQuery("select * from wikipedia where title like '" + what + "'");
			if(r.next()){
				tweet(status, r.getString(1), r.getString(2));
			}else{
				tweet(status, null, "単語が見つかりませんでした");
			}
			r.close();
		}catch(SQLException e){
			System.err.println("Failed sql query of class WhatIs constructor\nMessage: " + e.getMessage());
		}
	}

	private void tweet(Status status, String title, String abs){
		String url = "";
		try{
			if(title != null)
				url = "https://ja.wikipedia.org/wiki/" + URLEncoder.encode(title, "UTF-8").replace("+", "%20");
		}catch(UnsupportedEncodingException e){
			System.err.println("Could not encode url\nMessage: " + e.getMessage());
		}
		new Tweet("@" + status.getUser().getScreenName() + " " + abs, url, status.getId());
	}

}
