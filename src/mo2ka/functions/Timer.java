package mo2ka.functions;

import java.util.concurrent.TimeUnit;

import twitter4j.Status;

public class Timer{

	public Timer(Status status, String time, String timeUnit){
		this(status, Integer.parseInt(time), timeUnit);
	}

	public Timer(Status status, int time, String timeUnit){
		new Thread(new Runnable(){

			@Override
			public void run(){
				timer(status, time, timeUnit);
			}
		}).start();
	}

	private void timer(Status status, int time, String timeUnit){
		try{
			switch(timeUnit){
			case "秒":
				TimeUnit.SECONDS.sleep(time);
				break;
			case "分":
				TimeUnit.MINUTES.sleep(time);
				break;
			case "時間":
				TimeUnit.HOURS.sleep(time);
				break;
			}
		}catch(InterruptedException e){
			new Tweet("@" + status.getUser().getScreenName() + " タイマーの処理に失敗しました", status.getId());
			return;
		}
		new Tweet("@" + status.getUser().getScreenName() + " " + time + timeUnit + "経ちました", status.getId());
	}

}
