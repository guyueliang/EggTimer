package hutt.tim.eggtimer;

import java.io.IOException;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

// TODO: Maybe create a desktop widget.

// TODO: I am going to change the architecture so that
// there is only one activity - this one.
// It can however have several states depending on the state of the alarm. These are:
//
// 1. No alarm set: Show timer buttons.
// 2. Alarm set: Show countdown with 'Cancel' and 'Reset' buttons.
// 3. Alarm sounding: Show 'Silence' and 'Reset' buttons and a count up timer.
//
// When the alarm sounds we will unlock the screen.
//
// Instead of abusing the shared preferences we will use the proper method which I
// think should be a bundle of some kind. Nope. Actually we will use sharedpreferences. It seems
// to be the most appropriate place for data that must survive the app's death.
//
// The information we need is everything that we may need if the app is started
// with a timer running. That is:
//
// 1. If an alarm is currently set.
// 2. The time the alarm was set to go off.
// 3. The duration of the alarm (for the reset button).
//
// When the alarm goes off we send an intent to this activity to sound the noise.
//响铃的时候，在Receiver中启动service,然后在service中先发出通知，响铃和振动再运行，在activity中可以关闭服务
public class MainActivity extends Activity implements OnClickListener, OnLongClickListener, Dialog.OnDismissListener
{
	public static final String CHANNEL_ID = "MY_ALARM_SERVICE_CHANNEL";
	PowerManager.WakeLock mWakelock;

	// The number of rows and columns for the buttons.
	final static int NUM_ROWS = 6;
	final static int NUM_COLS = 4;
	final static int NUM_BUTTONS = NUM_ROWS * NUM_COLS;

	// The time of each button in seconds.
	long secondsArray[] = new long[NUM_BUTTONS];
	
	// Control of the actual timer.
	CountDownTimer countdownTextTimer;
	AlarmManager alarmManager;
	PendingIntent alarmIntent;

	// Preferences, and the UTC time of the alarm in milliseconds. 0 means not set.
	SharedPreferences prefs;
	long alarmTime = 0; // Time at which the alarm is set to go off.
	long alarmDuration = 0; // Time in seconds for the previously used alarm (for the 'reset' button).
	final static int ALARM_OFF = 0; // The alarm isn't set and isn't sounding.
	final static int ALARM_SET = 1; // The alarm is set but hasn't gone off yet.
	final static int ALARM_SOUNDING = 2; // The alarm is currently ...alarming!
	int alarmState = ALARM_OFF; // Whether the alarm is set (i.e. currently running) or not.

	// The change-log.
	Changelog changelog;
	
	// Buttons Screen.
	Button buttonArray[] = new Button[NUM_BUTTONS];
	
	// Timer Screen.
	Button cancelButton;
	Button resetButton;
	TextView countdownText;
	
	// Alarm Screen.
	Button silenceButton;
	Button restartButton;
	TextView countupText;
	
	// The three views we switch between.
	LinearLayout buttonsView; // The timer buttons. Simple enough
	View timerView; // The timer view with countdown, cancel and reset buttons.
	View alarmView; // The alarm view with countup, silence and reset buttons.

	// The view switcher to animate between them.
	ViewFlipper viewFlipper;
	
	// Dialog IDs.
	static final int DIALOG_EDITTIME = 0;
	static final int DIALOG_HTML = 1;
	
	// Options menu IDs.
	static final int MENU_QUIT = 0;
	static final int MENU_SETTINGS = 1;
	static final int MENU_HELP = 2;
	static final int MENU_CHANGELOG = 3;
//	static final int MENU_DONATE = 4;

	// Notification ID.
	static final int NOTIFICATION_ID = 0;

	// Unit constants. TODO: Allow any time.
	static final int UNIT_SECOND = 0;
	static final int UNIT_MINUTE = 1;
	static final int UNIT_HOUR = 2;
	static final int UNIT_DAY = 3;

	// Parameters for dialogs (this is a stupid API - why can't I pass them to ShowDialog?)

	// The current button we are editing the time of.
	int currentButton = -1;
	// The HTML content for the HTML dialog.
	String htmlContent;
	
	// Called when the activity is first created, or recreated.
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d("EggTimer", "onCreate()");
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){
			//api >=27
			Log.d("debug","高于android8.1系统");
			setShowWhenLocked(true);
			setTurnScreenOn(true);//必须要有，否则屏幕不亮

		}else{
			Log.d("debug","低于android8.1系统");
			Window win = getWindow();
			//其中的两个参数都必须要有，否则手机锁屏的时候屏幕不亮
			win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
					| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED); //必须要有，否则屏幕不亮

		}

		//使屏幕保持开启状态，不确定是否有用
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
				/*| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON*/ | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); // TODO: Make this configurable.

		// So the volume keys control the alarm volume.
		setVolumeControlStream(AudioManager.STREAM_ALARM);
		
		// The shared preferences. Used for settings and temporary data.
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// The change-log.
		changelog = new Changelog(this);
		// Load the alarm button times.
		loadTimes();

		// Get the info about the alarm state:
		alarmTime = prefs.getLong("alarm_time", 0); // The time the alarm is set to go off, in seconds since epoch.
		alarmState = prefs.getInt("alarm_state", ALARM_OFF); // If the alarm is currently set or sounding.
		alarmDuration = prefs.getLong("alarm_duration", 0); // The length of the alarm used in seconds.
		
		// The alarm manager.
		alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		// This starts the AlarmReceiver, which in turn creates this activity.
//		alarmIntent = PendingIntent.getBroadcast(this, 0, new Intent("hutt.tim.eggtimer.ALARM_WAKE"), 0);

		Intent intent = new Intent(this, AlarmReceiver.class);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			alarmIntent =  PendingIntent.getBroadcast(this, 0,
					intent,  PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);
		}else {
			alarmIntent =  PendingIntent.getBroadcast(this, 0,
					intent, PendingIntent.FLAG_CANCEL_CURRENT );
		}
		
		// The view switcher which changes between out three views.
		viewFlipper = new ViewFlipper(this);

		// Create the views.
		createButtonsView();
		createTimerView();
		createAlarmView();

		// Add them to our switcher. NB order. TODO (Not use magic order).
		viewFlipper.addView(buttonsView);
		viewFlipper.addView(timerView);
		viewFlipper.addView(alarmView);

		// Set the switcher as the main view.
		setContentView(viewFlipper);
		
		if (getIntent() != null
				&& getIntent().getAction() != null
				&& getIntent().getAction().equals("hutt.tim.eggtimer.ALARM_SOUND"))
		{
			//从AlarmReceiver中启动的，应该为响铃状态
			Log.d("EggTimer", "SOUND_ALARM onCreate()");

			showAlarmView();
			startAlarmNoise();
			alarmState = ALARM_SOUNDING;
		}
		else
		{
			showAppropriateView();
			showFirstRunDialog();
			
			if (alarmState == ALARM_SET)
			{
				// Start the countdown timer text.
				startCountdownText(alarmTime - System.currentTimeMillis());
			}
		}
	}
	
	private void showAppropriateView()
	{
		Log.d("EggTimer", "showAppropriateView()");

		
		switch (alarmState)
		{
		default:
		case ALARM_OFF:
			showButtonView();
			Log.d("TAG","buttonView");
			break;
		case ALARM_SET:
			showTimerView();
			Log.d("TAG","timerView");
			break;
		case ALARM_SOUNDING:
			showAlarmView();
			Log.d("TAG","alarmView");
			break;
		}		
	}
	
	protected void onNewIntent(Intent intent)
	{
		
		Log.d("EggTimer", "onNewIntent()");
		if (intent != null
				&& intent.getAction() != null
				&& intent.getAction().equals("hutt.tim.eggtimer.ALARM_SOUND"))
		{
			Log.d("EggTimer", "ALARM_SOUND");

			showAlarmView();
			startAlarmNoise();
			alarmState = ALARM_SOUNDING;
		}
	}

	/**
	 * 创建选择倒计时界面
	 */
	private void createButtonsView()
	{
		int n = 0;

		buttonsView = new LinearLayout(this);
		buttonsView.setOrientation(LinearLayout.VERTICAL);
		for (int r = 0; r < NUM_ROWS; ++r)
		{
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			for (int c = 0; c < NUM_COLS; ++c)
			{
				Button btn = new Button(this);
				btn.setText(secondsToString(secondsArray[n]));
				btn.setBackgroundResource(R.drawable.blue_button);
				btn.setTextColor(0xffeeeeff);
				btn.setTextSize(20.0f);
				btn.setOnClickListener(this);
				btn.setOnLongClickListener(this);

				buttonArray[n] = btn;

				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
				lp.weight = 1.0f;
				lp.setMargins(5, 7, 5, 3);
				row.addView(btn, lp);

				++n;
			}
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
			lp.weight = 1.0f;
			buttonsView.addView(row, lp);
		}
	}

	/**
	 * 创建倒计时界面，倒计时开始，显示该界面
	 */
	void createTimerView()
	{
		timerView = View.inflate(this, R.layout.timer, null);
		cancelButton = (Button)timerView.findViewById(R.id.CancelButton);
		cancelButton.setOnClickListener(this);
		resetButton = (Button)timerView.findViewById(R.id.ResetButton);
		resetButton.setOnClickListener(this);
	    countdownText = (TextView)timerView.findViewById(R.id.CountdownText);
	}

	/**
	 * 创建响铃界面，响铃开始的时候，显示该界面
	 */
	void createAlarmView()
	{
		alarmView = View.inflate(this, R.layout.alarm, null);
		silenceButton = (Button)alarmView.findViewById(R.id.SilenceButton);
		silenceButton.setOnClickListener(this);
		restartButton = (Button)alarmView.findViewById(R.id.RestartButton);
		restartButton.setOnClickListener(this);
		countupText = (TextView)alarmView.findViewById(R.id.CountupText); // Not sure why I made this different to countdownText
	}


	// Show the buttons view.
	private void showButtonView()
	{
		Log.d("EggTimer", "showButtons()");

		if (viewFlipper.getCurrentView() != buttonsView)
			viewFlipper.setDisplayedChild(0);
	}
	// Show the timer view.
	private void showTimerView()
	{
		Log.d("EggTimer", "showTimer()");

		if (viewFlipper.getCurrentView() != timerView)
			viewFlipper.setDisplayedChild(1);
	}	
	private void showAlarmView()
	{
		Log.d("EggTimer", "showAlarm()");

		if (viewFlipper.getCurrentView() != alarmView)
			viewFlipper.setDisplayedChild(2);
	}

	private void showFirstRunDialog()
	{
		if (changelog.isFirstRun())
		{
			showHelp();
		}
		else
		{
			String log = changelog.getNewChangelog();
			if (log.length() != 0)
			{
				htmlContent = log;
				showDialog(DIALOG_HTML);
			}
		}
	}

	private void showHelp()
	{
		htmlContent = getResources().getString(R.string.help);
		showDialog(DIALOG_HTML);
	}

	private void showEntireChangelog()
	{
		String log = changelog.getEntireChangelog();
		if (log.length() == 0)
			log = "<html><body><h1>Changes</h1><p>No changes logged.</p></body></html>";
		showDialog(DIALOG_HTML);
	}
//
//	private void showDonate()
//	{
//		htmlContent = getResources().getString(R.string.donate);
//		showDialog(DIALOG_HTML);
//	}
	
	@Override
	public void onPause()
	{
		Log.d("EggTimer", "onPause()");
		
		super.onPause();
		SharedPreferences.Editor ed = prefs.edit();
		ed.putLong("alarm_time", alarmTime);
		ed.putLong("alarm_duration", alarmDuration);
		ed.putInt("alarm_state", alarmState);
		ed.commit();

		// Saves the times for each button.
		saveTimes();
		releaseWakeLock();
	}
	
	private void startAlarmNoise()
	{
		soundKlaxon();
		startVibrator();
	}
	
	private void stopAlarmNoise()
	{
		silenceKlaxon();
		stopVibrator();
	}
	
	

	// Called when a dialog is dismissed. At the moment it will either be the HTML dialog
	// or the button time editor. Once again I say this is a retarded interface.
	public void onDismiss(DialogInterface dialog)
	{
		EditTimeDialog dlg = (EditTimeDialog)dialog;
		if (dlg != null && dlg.getOked())
		{
			int secs = 1;
			switch (dlg.getUnit())
			{
				case 0:
					secs = dlg.getValue();
					break;
				case 1:
					secs = dlg.getValue() * 60;
					break;
				case 2:
					secs = dlg.getValue() * 60 * 60;
					break;
				case 3:
					secs = dlg.getValue() * 60 * 60 * 24;
					break;
			}

			// TODO: Crash with ArrayIndexOutOfBounds here somewhere.
			// Either currentButton was never changed from -1, or button/secondsArray wasn't populated.
			
			// Meh. Do better fix than this:
			if (currentButton >= 0 && currentButton < buttonArray.length && currentButton < secondsArray.length)
			{
				buttonArray[currentButton].setText(secondsToString(secs));
				secondsArray[currentButton] = secs;
			}
		}
	}


	@Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		switch (id)
		{
		case DIALOG_EDITTIME:
			dialog = new EditTimeDialog(this);
			dialog.setOnDismissListener(this);
			break;
		case DIALOG_HTML:
			dialog = new HtmlDialog(this);
			break;
		}
		return dialog;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog)
	{
		switch (id)
		{
		case DIALOG_EDITTIME:
			EditTimeDialog dlg = (EditTimeDialog)dialog;

			if (currentButton >= 0 && currentButton < secondsArray.length)
			{
				long secs = secondsArray[currentButton];
				dlg.setValue(secondsToVal(secs));
				dlg.setUnit(secontsToUnit(secs));
			}
			dlg.setOked(false);
			break;
		case DIALOG_HTML:
			HtmlDialog hdlg = (HtmlDialog)dialog;
			hdlg.setContent(htmlContent);
			break;
		}
	}

	public boolean onLongClick(View v)
	{
		currentButton = -1;
		for (int i = 0; i < buttonArray.length; ++i)
		{
			if (v == buttonArray[i])
			{
				currentButton = i;
				break;
			}
		}
		if (currentButton < 0)
			return false;
		
		showDialog(DIALOG_EDITTIME);

		return true;
	}
    
	public void onClick(View v)
	{
		// First see if it is the cancel/silence button.
		if (v == cancelButton || v == silenceButton)
		{
			// Cancel any pending intents so the alarm doesn't go off. This is only useful for the cancel button
			// not the silence one, but fortunately this is safe even if it hasn't been started.
			alarmManager.cancel(alarmIntent);
			if (countdownTextTimer != null)
			{
				countdownTextTimer.cancel();
				countdownTextTimer = null;
			}
			countdownText.setText("00:00");
			stopAlarmNoise();
			showButtonView();
			alarmState = ALARM_OFF;
			clearNotification();
			Wakelocker.releaseCpuLock();
		}
		else if (v == resetButton || v == restartButton) // Or the reset button.
		{
			//重新设置或者重新启动闹钟
			alarmManager.cancel(alarmIntent);
			if (countdownTextTimer != null)
			{
				countdownTextTimer.cancel();
				countdownTextTimer = null;
			}
			stopAlarmNoise();
			alarmState = ALARM_SET;
			alarmTime = System.currentTimeMillis() + alarmDuration * 1000;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				//android 6.0以上版本
				alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + alarmDuration * 1000 - 500, alarmIntent);
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				//android 4.4以上版本
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + alarmDuration * 1000 - 500, alarmIntent);
			}else{
				//android 4.4以下版本
				alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + alarmDuration * 1000 - 500, // The 500 is to give the alarm noise time to load and so it doesn't flash to the button screen for a second.
						alarmIntent);
			}
//			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + alarmDuration * 1000 - 500, // The 500 is to give the alarm noise time to load and so it doesn't flash to the button screen for a second.
//					alarmIntent);
			startCountdownText(alarmDuration * 1000);
			showTimerView();
			Wakelocker.releaseCpuLock();
		}
		else
		{
			//开始计时器，即开始闹钟
			// Must be a timer button.
			long seconds = -1;
			for (int i = 0; i < buttonArray.length; ++i)
			{
				if (v == buttonArray[i])
				{
					seconds = secondsArray[i];
					break;
				}
			}
			if (seconds == -1)
				return;


			//设置精确的闹钟,以及可重复的闹钟
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				//android 6.0以上版本
				alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + seconds * 1000 - 500, alarmIntent);
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				//android 4.4以上版本
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + seconds * 1000 - 500, alarmIntent);
			}else{
				//android 4.4以下版本
				alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + seconds * 1000 - 500, // The 500 is to give the alarm noise time to load and so it doesn't flash to the button screen for a second.
						alarmIntent);
			}
			/*alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + seconds * 1000 - 500, // The 500 is to give the alarm noise time to load and so it doesn't flash to the button screen for a second.
					alarmIntent);*/
			startCountdownText(seconds * 1000);
			showTimerView();
			setNotification();
			
			alarmState = ALARM_SET;
			alarmTime = System.currentTimeMillis() + seconds * 1000;
			alarmDuration = seconds;
			Log.d("EggTimer", "Set alarmDuration to " + alarmDuration);
		}
	}
	
	// This is just the aesthetic part.
	private void startCountdownText(long milliSecondsLeft)
	{
		Log.d("EggTimer", "startCoundownText() ; start aesthetic timer text.");

		if (countdownTextTimer != null)
			countdownTextTimer.cancel();

		countdownText.setText(formatTimerString(milliSecondsLeft / 1000));

		countdownTextTimer = new CountDownTimer(milliSecondsLeft, 100) // Use 100 rather than 1000 otherwise it appears to freeze on 1 second. TODO: Better fix.
		{
			public void onTick(long millisUntilFinished)
			{
				countdownText.setText(formatTimerString(millisUntilFinished / 1000));
			}

			public void onFinish()
			{
				countdownText.setText(formatTimerString(0));
			}
		}.start();
	}
	
	private String formatTimerString(long s)
	{
		long days = s / (60*60*24);
		long hours = (s / (60*60)) % 24;
		long mins = (s / 60) % 60;
		long secs = s % 60;
		if (days == 0 && hours == 0)
			return String.format("%02d:%02d", mins, secs);
		if (days == 0)
			return String.format("%d:%02d:%02d", hours, mins, secs);
		return String.format("%d\n%d:%02d:%02d", days, hours, mins, secs);
	}


	
	
	// Options menu.
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
//	    menu.add(0, MENU_DONATE, 0, "Donate").setIcon(android.R.drawable.ic_menu_send);
	    menu.add(0, MENU_SETTINGS, 0, "Settings").setIcon(android.R.drawable.ic_menu_preferences);
//	    menu.add(0, MENU_CHANGELOG, 0, "Changelog");
	    return true;
	}

	// Handles item selections
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId())
	    {
	    case MENU_SETTINGS:
	    	startActivity(new Intent(this, SettingsActivity.class));
	    	return true;
		case MENU_HELP:
			showHelp();
			return true;
		case MENU_CHANGELOG:
			showEntireChangelog();
			return true;
//		case MENU_DONATE:
//			showDonate();
//			return true;
	    }
	    return false;
	}

	
	
	// Utility functions.
	
	// Convert a number of seconds into a string to display on the button.
	static String secondsToString(long s)
	{
		long secs = s % 60;
		long mins = (s/60); // Remember to mod 60 if we are displaying with hours.
		long hours = (s/(60*60));
		long days = s/(60*60*24);

		if (s < 100 && s != 60)
			return Long.toString(s) + "s";
		secs %= 60;
		if (s < 100*60)
		{
			if (secs == 0)
				return Long.toString(mins) + "m";
			return Long.toString(mins) + "m " + Long.toString(secs) + "s";
		}
		mins %= 60;
		if (s < 100*60*60)
		{
			if (mins == 0 && secs == 0)
				return Long.toString(hours) + "h";
			if (secs == 0)
				return Long.toString(hours) + "h " + Long.toString(mins) + "m";
			return Long.toString(hours) + "h " + Long.toString(mins) + "m " + Long.toString(secs) + "s";
		}
		hours %= 24;
		if (s < 100*60*60*24)
		{
			if (hours == 0 && mins == 0 && secs == 0)
				return Long.toString(days) + "d";
			if (mins == 0 && secs == 0)
				return Long.toString(days) + "d " + Long.toString(hours) + "h";
			if (secs == 0)
				return Long.toString(days) + "d " + Long.toString(hours) + "h " + Long.toString(mins) + "m";
			return Long.toString(days) + "d " + Long.toString(hours) + "h " + Long.toString(mins) + "m " + Long.toString(secs) + "s";
		}
		return "\u221E"; // Infinity symbol.
	}

	// Convert '2, minute' to 120.
	static long valUnitToSeconds(int val, int unit)
	{
		long v = val;
		switch (unit)
		{
		case UNIT_DAY:
			v *= 24; // Fallthrough. This is clearly too clever.
		case UNIT_HOUR:
			v *= 60;
		case UNIT_MINUTE:
			v *= 60;
		}
		return v;
	}

	static int secondsToVal(long secs)
	{
		if (secs < 100 && secs != 60)
			return (int)secs;
		if (secs < 100*60)
			return (int)(secs/60);
		if (secs < 100*60*60)
			return (int)(secs/(60*60));
		return (int)(secs/(60*60*24));
	}

	static int secontsToUnit(long secs)
	{
		if (secs < 100 && secs != 60)
			return 0;
		if (secs < 100*60)
			return 1;
		if (secs < 100*60*60)
			return 2;
		return 3;
	}
	
	void loadTimes()
	{
		// Defaults.
		secondsArray[0] = 30;
		secondsArray[1] = 60 * 1;
		secondsArray[2] = 60 * 2;
		secondsArray[3] = 60 * 3;
		secondsArray[4] = 60 * 4;
		secondsArray[5] = 60 * 5;
		secondsArray[6] = 60 * 6;
		secondsArray[7] = 60 * 7;
		secondsArray[8] = 60 * 8;
		secondsArray[9] = 60 * 9;
		secondsArray[10] = 60 * 10;
		secondsArray[11] = 60 * 11;
		secondsArray[12] = 60 * 12;
		secondsArray[13] = 60 * 15;
		secondsArray[14] = 60 * 20;
		secondsArray[15] = 60 * 25;
		secondsArray[16] = 60 * 30;
		secondsArray[17] = 60 * 35;
		secondsArray[18] = 60 * 40;
		secondsArray[19] = 60 * 45;
		secondsArray[20] = 60 * 50;
		secondsArray[21] = 60 * 55;
		secondsArray[22] = 60 * 60;
		secondsArray[23] = 60 * 90;

		if (prefs == null)
			return;
		for (int i = 0; i < secondsArray.length; ++i)
		{
			secondsArray[i] = prefs.getLong("seconds_" + Long.toString(i), secondsArray[i]);
		}
	}
	
	void saveTimes()
	{
		if (prefs == null)
			return;
		SharedPreferences.Editor ed = prefs.edit();
		for (int i = 0; i < secondsArray.length; ++i)
		{
			ed.putLong("seconds_" + Long.toString(i), secondsArray[i]);
		}
		ed.commit();
	}
	

	//************** Vibrator control ************
	
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

	//************** Sound control for the alarm ************
	
	private MediaPlayer mediaPlayer = null; // TODO: Create in another thread?
	
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

	//************** Notification stuff ************
	
	private NotificationManager mNotificationManager = null;
	
	void setNotification()
	{
		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// the next two lines initialize the Notification, using the configurations above
		Notification notification = new Notification(R.drawable.icon, "Egg Timer Running", 0);
//		notification.setLatestEventInfo(this, "Egg Timer Running", null, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
	void clearNotification()
	{
		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		
		mNotificationManager.cancelAll();
	}

	@Override
	protected void onResume() {

		//锁屏状态下唤醒屏幕，要在onResume() 方法中启动，并且要在onPause()中释放，否则会出错
		super.onResume();
		Log.d("TAG","onResume()");
		acquireWakeLock();
		showAppropriateView();

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