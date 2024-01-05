package hutt.tim.eggtimer;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class Wakelocker
{
	private static final String TAG = "EggTimer";
	private static PowerManager.WakeLock sCpuWakeLock;

	static void acquireCpuWakeLock(Context context)
	{
		Log.d(TAG, "Acquiring cpu wake lock");
		if (sCpuWakeLock != null)
			return;
		
		PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

		sCpuWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, TAG);
		sCpuWakeLock.acquire();
	}

	static void releaseCpuLock()
	{
		Log.d(TAG, "Releasing cpu wake lock");
		if (sCpuWakeLock != null)
		{
			sCpuWakeLock.release();
			sCpuWakeLock = null;
		}
	}

}
