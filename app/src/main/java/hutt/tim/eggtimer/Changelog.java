
package hutt.tim.eggtimer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;

// Class that allows us to load a changelog.
public class Changelog
{
	// The versionCode for the app we are currently running. Obtained from the package manager.
	private int mCurrentVersion;
	// The versionCode of the last version they have run, or -1 if they have never run any versions.
	private int mPreviousVersion;

	// The changelog text.
	private String mHeader;
	private String[] mChangelog; // One entry (which may be an empty string) for each version, starting at 0.
	private String mFooter;

	// Load the changelog and read the version details from the prefs. Also updates the prefs to
	// indicate that we have run this version.
	public Changelog(Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		try
		{
			mCurrentVersion = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
			mPreviousVersion = prefs.getInt("previous_version", -1);

			// Record that we have now run this version.
			SharedPreferences.Editor ed = prefs.edit();
			ed.putInt("previous_version", mCurrentVersion);
			ed.commit();
		}
		catch (PackageManager.NameNotFoundException e)
		{
			// This should never happen.
			e.printStackTrace();
			mCurrentVersion = mPreviousVersion = 0;
		}

		// Now load the changelog.
		Resources res = ctx.getResources();
		mHeader = res.getString(R.string.changelog_header);
		mChangelog = res.getStringArray(R.array.changelog);
		mFooter = res.getString(R.string.changelog_footer);
	}

	// Return a string of new changes introduced in the app since last time
	// it was run, or "" if there are none.
	public String getNewChangelog()
	{
		// If it's their first time, everything is new!
		if (isFirstRun())
			return getEntireChangelog();

		if (mPreviousVersion == mCurrentVersion)
			return ""; // Nothing new.

		StringBuilder sb = new StringBuilder();
		for (int i = mPreviousVersion + 1; i <= mCurrentVersion && i < mChangelog.length; ++i)
			sb.append(mChangelog[i]);

		if (sb.length() == 0)
			return "";
		return mHeader + sb.toString() + mFooter;
	}

	public String getEntireChangelog()
	{
		StringBuilder sb = new StringBuilder();
		for (String s : mChangelog)
			sb.append(s);

		if (sb.length() == 0)
			return "";
		return mHeader + sb.toString() + mFooter;
	}

	public boolean isFirstRun()
	{
		return mPreviousVersion == -1;
	}
}
