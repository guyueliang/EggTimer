package hutt.tim.eggtimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		// 1. Grab the wakelock so the phone doesn't fall asleep (damn you phone!)
//		Wakelocker.acquireCpuWakeLock(context);
		
		Log.d("EggTimer", "AlarmReceiver::onReceive()");
		
//		// 2. Start the MainActivity and tell it to show and sound the alarm.
//		Intent ma = new Intent("hutt.tim.eggtimer.ALARM_SOUND");
//		ma.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Necessary apparently.
//		context.startActivity(ma);

		Intent intentService = new Intent(context, AlarmService.class);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
			context.startForegroundService(intentService);
		}else{
			context.startService(intentService);
		}
	}

}
