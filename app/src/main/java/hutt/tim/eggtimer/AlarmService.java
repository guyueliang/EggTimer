package hutt.tim.eggtimer;

import static hutt.tim.eggtimer.MainActivity.CHANNEL_ID;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class AlarmService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG","service");

//        Notification builder1 = new NotificationCompat.Builder(AlarmService.this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_stat_name)
//                .setContentTitle(alarmTime)
//                .setContentText(alarmTitle)
//                .setDefaults(NotificationCompat.DEFAULT_ALL)
//                .setPriority(NotificationCompat.PRIORITY_MAX)
////                                .setContentIntent(fullScreenPendingIntent)
//                .setAutoCancel(true)
//                .setCategory(NotificationCompat.CATEGORY_ALARM)
//                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
//                .addAction(R.drawable.close_notification, "关闭", closeAlarmNotification)
//                .setFullScreenIntent(fullScreenPendingIntent, true)
//                .build();
//        //TODO 如果下一个闹铃触发的时候，之前闹铃界面还在的话，就不要发送通知，直接使用之前的闹铃界面即可；否则就发出通知表示一个新的闹铃触发
//        //TODO 这个startForeground必须要执行，否则会报错；这是系统强制性要求
//        startForeground(1, builder1);

        return START_STICKY;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
