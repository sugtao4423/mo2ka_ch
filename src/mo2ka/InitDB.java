package mo2ka;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.sqlite.JDBC;

import mo2ka.strings.Default;
import mo2ka.strings.Tables;

public class InitDB{

	public static void main(String[] args) throws IOException, SQLException{
		Properties prop = new Properties();
		prop.put("sync_mode", "off");
		prop.put("journal_mode", "wal");
		Connection conn = JDBC.createConnection("jdbc:sqlite:" + Momoka.dbLocation, prop);
		Statement stmt = conn.createStatement();
		stmt.execute(String.format("create table %s(nlText unique)", Tables.NOT_LEARN_TEXT));
		stmt.execute(String.format("create table %s(lVia unique)", Tables.LEARN_VIA));
		stmt.execute(String.format("create table %s(rWord unique)", Tables.REACTION_WORD));

		stmt.execute("create table parts(c, cp, screen_name, tweetId, via, unique(c, cp))");
		stmt.execute("create table markov(c1, cp1, c2, cp2, c3, cp3, screen_name, tweetId, via, unique(c1, cp1, c2, cp2, c3, cp3))");
		stmt.execute("create table talk(t1, tp1, t2, tp2, unique(t1, tp1, t2, tp2))");

		String insertFormat = "insert into %s values('%s')";
		for(String s : Default.NOT_LEARN_TEXT)
			stmt.execute(String.format(insertFormat, Tables.NOT_LEARN_TEXT, s));
		for(String s : Default.LEARN_VIA)
			stmt.execute(String.format(insertFormat, Tables.LEARN_VIA, s));
		for(String s : Default.REACTION_WORDS)
			stmt.execute(String.format(insertFormat, Tables.REACTION_WORD, s));

		stmt.close();
		conn.close();
	}
}
