package mo2ka;

import java.io.IOException;
import java.sql.SQLException;

import mo2ka.functions.CreateTweet;
import net.reduls.igo.Tagger;
import twitter4j.JSONException;

public class Cli{

	public Cli(String cmd){
		switch(cmd){
		case "stop":
		case "exit":
			System.exit(0);
			break;
		case "create-sentence":
			createSentence();
			break;
		case "update-tagger":
			updateTagger();
			break;
		case "reload-config":
			reloadConfig();
			break;
		}
	}

	public void createSentence(){
		try{
			String str = new CreateTweet().getResult();
			System.out.println(str);
		}catch(SQLException e){
			System.err.println("Could not create tweet\nMessage: " + e.getMessage());
		}
	}

	public void updateTagger(){
		try{
			Momoka.tagger = new Tagger(Momoka.igoDicDir);
			System.out.println("Update igo dic OK");
		}catch(IOException e){
			System.err.println("Could not create instance of Tagger\nMessage: " + e.getMessage());
		}
	}

	public void reloadConfig(){
		try{
			Momoka.loadSettings();
			Thread.sleep(500);
			System.out.println("Reload config OK");
		}catch(IOException | JSONException | InterruptedException e){
			System.err.println("Could not reload configuration file\nMessage: " + e.getMessage());
		}
	}

}
