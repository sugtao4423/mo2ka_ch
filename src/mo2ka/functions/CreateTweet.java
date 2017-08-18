package mo2ka.functions;

import java.sql.ResultSet;
import java.sql.SQLException;

import mo2ka.Momoka;

public class CreateTweet{

	private String result = null;

	public CreateTweet() throws SQLException{
		create();
	}

	private void create() throws SQLException{
		ResultSet r = Momoka.stmt.executeQuery("select * from parts order by random() limit 1");
		String[] partsList;
		if(r.next()){
			partsList = r.getString(2).split(",");
		}else{
			r.close();
			return;
		}

		String beginParts = "[BEGIN]-[BEGIN]-[BEGIN]-[BEGIN]-[BEGIN]-[BEGIN]";
		String endParts = "[END]-[END]-[END]-[END]-[END]-[END]";

		r = Momoka.stmt.executeQuery(
				String.format("select c2, c3 from markov where cp1='%s' and cp2='%s' order by random() limit 1",
						beginParts, partsList[0]));
		String c2, c3;
		if(r.next()){
			c2 = r.getString(1);
			c3 = r.getString(2);
			result = c2 + c3;
		}else{
			r.close();
			return;
		}

		boolean isEnd = false;
		for(int i = 1; i < partsList.length - 2; i+=2){
			r = Momoka.stmt.executeQuery(
					String.format("select c2, c3 from markov where c1='%s' and cp1='%s' order by random() limit 1",
							c3, partsList[i]));
			if(r.next()){
				c2 = r.getString(1);
				c3 = r.getString(2);
				if(c3.equals("[END]")){
					result += c2;
					isEnd = true;
					break;
				}
				result += c2 + c3;
			}else{
				result = null;
				return;
			}
		}

		if(isEnd)
			return;

		if(partsList.length - 2 < 0){
			result = null;
			return;
		}

		r = Momoka.stmt.executeQuery(
				String.format("select c2 from markov where c1='%s' and cp1='%s' and cp3='%s' order by random() limit 1",
						c3, partsList[partsList.length - 2], endParts));
		if(r.next()){
			result += r.getString(1);
		}else{
			r = Momoka.stmt.executeQuery(
					String.format("select c3 from markov where c2='%s' and cp2='%s' and cp3='%s' order by random() limit 1",
							c3, partsList[partsList.length - 1], endParts));
			if(!r.next()){
				result = null;
				return;
			}
		}

	}

	public String getResult() throws SQLException{
		while(result == null)
			create();
		return result;
	}

}
