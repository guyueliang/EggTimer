package hutt.tim.eggtimer;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity
{
	// Called when the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);
	}
	
	
	@Override
	public void onPause()
	{
		super.onPause();
	}
}
