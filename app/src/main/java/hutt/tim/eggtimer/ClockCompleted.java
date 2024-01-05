package hutt.tim.eggtimer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;


public class ClockCompleted extends Activity {
    PowerManager.WakeLock mWakelock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){
            //api >=27
            setShowWhenLocked(true);
            setTurnScreenOn(true);//必须要有，否则屏幕不亮

        }else{
            Window win = getWindow();
            //其中的两个参数都必须要有，否则手机锁屏的时候屏幕不亮
            win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED); //必须要有，否则屏幕不亮

        }

        //使屏幕保持开启状态，不确定是否有用
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //闹铃的时候可以全屏显示
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_clock_completed);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //停止AlarmService
                Intent intentService = new Intent(ClockCompleted.this, AlarmService.class);
                ClockCompleted.this.stopService(intentService);
                finish();
            }
        });

    }


    @Override
    protected void onResume() {

        //锁屏状态下唤醒屏幕，要在onResume() 方法中启动，并且要在onPause()中释放，否则会出错
        super.onResume();
        //必须要有，onCreate()中的方法只能使在手机锁屏情况下，点亮屏幕，但过一会就会熄屏，此时也会导致震动停止
        //获取唤醒锁，可以使手机锁屏状态下，手机屏幕常亮，并且震动不会停止
        acquireWakeLock();

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseWakeLock();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 唤醒屏幕
     */
    private void acquireWakeLock() {
        if (mWakelock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakelock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.SCREEN_DIM_WAKE_LOCK, this.getClass()
                    .getCanonicalName());
            mWakelock.acquire(10000);
        }
    }

    /**
     * 释放锁屏
     */
    private void releaseWakeLock() {
        if (mWakelock != null && mWakelock.isHeld()) {
            mWakelock.release();
            mWakelock = null;
        }
    }

}