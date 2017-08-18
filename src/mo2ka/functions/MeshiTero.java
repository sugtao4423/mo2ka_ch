package mo2ka.functions;

import java.util.Random;

import mo2ka.Momoka;
import twitter4j.Status;

public class MeshiTero{

	public MeshiTero(Status status){
		int i = new Random().nextInt(Momoka.meshiTero.size() - 1);
		new Tweet("@" + status.getUser().getScreenName() + " " + Momoka.meshiTero.get(i), status.getId());
	}

}
