package hutt.tim.eggtimer;

import android.content.Context;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class VolumePreference extends Preference implements OnSeekBarChangeListener
{
	AudioManager audioManager = null;
	int maxVol = 0;

	public VolumePreference(Context context)
	{
		super(context);
	}

	public VolumePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public VolumePreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView(ViewGroup parent)
	{
		Context ctx = getContext();
		
		audioManager = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		if (audioManager != null)
			maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
		
		LinearLayout layout = new LinearLayout(ctx);

		layout.setOrientation(LinearLayout.VERTICAL);
		
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		
		titleParams.gravity = Gravity.LEFT;
		titleParams.weight = 1.0f;

		LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		
		sliderParams.gravity = Gravity.CENTER;
		sliderParams.weight = 1.0f;

		layout.setPadding(15, 5, 10, 5);

		TextView title = new TextView(ctx);
		
		title.setText(getTitle());
		title.setTextSize(22);
		title.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
		title.setTextColor(0xFFFFFFFF);
		title.setGravity(Gravity.LEFT);
		title.setLayoutParams(titleParams);

		SeekBar slider = new SeekBar(ctx);
		
		slider.setMax(maxVol);
		slider.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_ALARM));
		slider.setLayoutParams(sliderParams);
		slider.setOnSeekBarChangeListener(this);

		layout.addView(title);
		layout.addView(slider);
		layout.setId(android.R.id.widget_frame);

		return layout;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser)
	{
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
		if (audioManager == null)
			return;
		
		audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
				seekBar.getProgress(), AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
		
		notifyChanged();
	}
}