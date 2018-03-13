package mo2ka;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.sqlite.JDBC;

public class InitDB{

	public static void main(String[] args) throws SQLException{
		Properties prop = new Properties();
		prop.put("sync_mode", "off");
		prop.put("journal_mode", "wal");
		Connection conn = JDBC.createConnection("jdbc:sqlite:" + Momoka.dbLocation, prop);
		Statement stmt = conn.createStatement();

		stmt.execute("create table parts(c, cp, screen_name, tweetId, via, unique(c, cp))");
		stmt.execute("create table markov(c1, cp1, c2, cp2, c3, cp3, screen_name, tweetId, via, unique(c1, cp1, c2, cp2, c3, cp3))");
		stmt.execute("create table talk(t1, tp1, t2, tp2, unique(t1, tp1, t2, tp2))");

		stmt.close();
		conn.close();
	}
}
