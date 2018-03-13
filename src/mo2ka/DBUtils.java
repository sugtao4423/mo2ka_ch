package mo2ka;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;

import twitter4j.Status;

public class DBUtils{

	public static void insertPartsList(Word[] words, Status status) throws SQLException{
		String contents = "";
		String parts = "";
		for(Word w : words){
			contents += escape(w.getWord().replace(",", "\\,")) + ",";
			parts += String.format("%s-%s-%s-%s-%s-%s,", w.getType1(), w.getType2(), w.getType3(), w.getType4(), w.getType5(), w.getType6());
		}
		contents = contents.substring(0, contents.length() - 1);
		parts = parts.substring(0, parts.length() - 1);
		Momoka.stmt.execute(
				String.format("insert into parts values('%s', '%s', '%s', %d, '%s')",
						contents, parts,
						status.getUser().getScreenName(), status.getId(), getVia(status)));
	}

	public static void insertMarkov(Word[] words, Status status) throws ParseException, SQLException{
		ArrayList<Word> list = new ArrayList<Word>();
		for(Word w : words)
			list.add(w);
		list.add(0, new Word("[BEGIN]", "[BEGIN]-[BEGIN]-[BEGIN]-[BEGIN]-[BEGIN]-[BEGIN]"));
		list.add(new Word("[END]", "[END]-[END]-[END]-[END]-[END]-[END]"));

		for(int i = 2; i < list.size(); i++){
			Momoka.stmt.execute(
					String.format("insert into markov values('%s', '%s', '%s', '%s', '%s', '%s', '%s', %d, '%s')",
					escape(list.get(i - 2).getWord()), getHyphenType(list.get(i - 2)),
					escape(list.get(i - 1).getWord()), getHyphenType(list.get(i - 1)),
					escape(list.get(i).getWord()), getHyphenType(list.get(i)),
					status.getUser().getScreenName(), status.getId(), getVia(status)));
		}
	}

	private static String getHyphenType(Word w){
		return String.format("%s-%s-%s-%s-%s-%s", w.getType1(), w.getType2(), w.getType3(), w.getType4(), w.getType5(), w.getType6());
	}

	private static String escape(String str){
		return str.replace("'", "''");
	}

	private static String getVia(Status status){
		return status.getSource().replaceAll("<.+?>", "");
	}

}
