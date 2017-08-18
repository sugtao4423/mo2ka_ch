package mo2ka.functions;

import java.text.DecimalFormat;

import twitter4j.Status;

public class Memory{

	public Memory(Status status){
		DecimalFormat f1 = new DecimalFormat("#,###MB");
		DecimalFormat f2 = new DecimalFormat("##.#");
		long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
		long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
		long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
		long used = total - free;
		double per = (used * 100 / (double)total);
		String message = "@" + status.getUser().getScreenName() + "\n合計：" + f1.format(total) + " \n使用量：" + f1.format(used) +
				" (" + f2.format(per) + "%)" + "\n使用可能最大：" + f1.format(max);
		new Tweet(message, status.getId());
	}

}
