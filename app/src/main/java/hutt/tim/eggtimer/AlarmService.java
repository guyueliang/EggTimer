package hutt.tim.eggtimer;

import static hutt.tim.eggtimer.MainActivity.CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;

import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AlarmService extends Service {
    SharedPreferences prefs;
    private MediaPlayer mediaPlayer = null; // TODO: Create in another thread?
    private Vibrator mVibrator = null;


    private void startVibrator()
    {
        if (mVibrator == null)
            mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (prefs == null)
            return;
        if (prefs.getBoolean("vibrate", false))
        {
            long[] pattern = {0, 1000, 1000};
            mVibrator.vibrate(pattern, 1);
        }
    }

    private void stopVibrator()
    {
        if (mVibrator == null)
            mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        mVibrator.cancel();
    }
    private void soundKlaxon()
    {
        if (prefs == null)
            return;

        String tone = prefs.getString("tone", "content://settings/system/alarm_alert");

        if (mediaPlayer == null)
        {
            if (tone != null && tone.length() != 0)
            {
                // Use the tone they have given.
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(this, Uri.parse(tone));
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mediaPlayer.setLooping(true);
            }
            else
            {
                // They want silence.
            }
        }

        if (mediaPlayer != null)
        {
            String volSel = prefs.getString("volume", "3");

            if (volSel.equals("0"))
                mediaPlayer.setVolume(0.1f, 0.1f);
            else if (volSel.equals("1"))
                mediaPlayer.setVolume(0.25f, 0.25f);
            else if (volSel.equals("2"))
                mediaPlayer.setVolume(0.5f, 0.5f);
            else
                mediaPlayer.setVolume(1.0f, 1.0f);

            // Doesn't seem to work, at least in the emulator.
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

            try {
                mediaPlayer.prepare();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mediaPlayer.start();
        }
    }

    private void silenceKlaxon()
    {
        if (mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG","service");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);


        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(AlarmService.this, CHANNEL_ID);
        }else {
            builder = new Notification.Builder(AlarmService.this);
        }

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm E");
        Date date1 = new Date(calendar.getTimeInMillis());
        String s = simpleDateFormat.format(date1);

        builder.setSmallIcon(R.drawable.icon);
        builder.setContentTitle("闹钟");
        builder.setContentText(s);
        builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_ALARM);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PRIVATE);
        }


        Intent intentService = new Intent(AlarmService.this, CloseAlarmNotification.class);
        PendingIntent closeAlarmNotification = PendingIntent.getBroadcast(AlarmService.this, 0, intentService, PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(R.drawable.icon,"关闭",closeAlarmNotification);

        Intent fullScreenIntent = new Intent(AlarmService.this, ClockCompleted.class);
        fullScreenIntent.putExtra("AlarmId", "alarm");
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent fullScreenPendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            fullScreenPendingIntent = PendingIntent.getActivity(AlarmService.this, 0,
                    fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            fullScreenPendingIntent = PendingIntent.getActivity(AlarmService.this, 0,
                    fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        builder.setContentIntent(fullScreenPendingIntent);
        builder.setFullScreenIntent(fullScreenPendingIntent,true);

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
        //TODO 如果下一个闹铃触发的时候，之前闹铃界面还在的话，就不要发送通知，直接使用之前的闹铃界面即可；否则就发出通知表示一个新的闹铃触发
        //TODO 这个startForeground必须要执行，否则会报错；这是系统强制性要求
        startForeground(1, builder.build());

        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt("alarm_state", 2);
        ed.commit();
        soundKlaxon();
        startVibrator();


        return START_STICKY;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt("alarm_state", 0);
        ed.commit();
        silenceKlaxon();
        stopVibrator();

    }
}
