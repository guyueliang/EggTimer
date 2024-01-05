package hutt.tim.eggtimer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CloseAlarmNotification extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent intentService = new Intent(context, AlarmService.class);
        context.stopService(intentService);
    }

}