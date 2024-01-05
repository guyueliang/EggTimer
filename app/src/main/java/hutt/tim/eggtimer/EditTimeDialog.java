package hutt.tim.eggtimer;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import hutt.tim.eggtimer.widgets.NumberPicker;

// A dialog allowing one to select an amount of seconds, minutes or hours.
public class EditTimeDialog extends Dialog implements OnClickListener
{
	// TODO: Maybe use AlertDialog instead of Dialog?
	Button mOk;
	Button mCancel;
	NumberPicker mPicker;
	Spinner mCombo;

	// Whether ok was pressed. There must be a better way...
	// Ah, of course the better way is to create a callback interface and call it from onClick.
	boolean mOked = false;

	public EditTimeDialog(Context context)
	{
		super(context);
		setContentView(R.layout.edittime);
		setTitle("Choose Time");

		mOk = (Button)findViewById(R.id.ok);
		mOk.setOnClickListener(this);
		mCancel = (Button)findViewById(R.id.cancel);
		mCancel.setOnClickListener(this);

		mPicker = (NumberPicker)findViewById(R.id.number_picker);
		mPicker.setRange(1, 99);
		mPicker.setCurrent(1);

		mCombo = (Spinner)findViewById(R.id.spinner);
		mCombo.setSelection(0);
	}

	public void setValue(int val)
	{
		if (val < 1)
			val = 1;
		if (val > 99)
			val = 99;

		mPicker.setCurrent(val);
	}

	// 0 = seconds, 1 = minutes, 2 = hours, 3 = days.
	public void setUnit(int unit)
	{
		if (unit < 0)
			unit = 0;
		if (unit > 3)
			unit = 3;

		mCombo.setSelection(unit);
	}

	public void onClick(View v)
	{
		if (v == mOk)
		{
			// TODO: Use a callback here.
			mOked = true;
			dismiss();
		}
		cancel();
	}

	public int getValue()
	{
		return mPicker.getCurrent();
	}

	// 0 = seconds, 1 = minutes, 2 = hours, 3 = days.
	public int getUnit()
	{
		return mCombo.getSelectedItemPosition();
	}

	public void setOked(boolean f)
	{
		mOked = f;
	}

	public boolean getOked()
	{
		return mOked;
	}

}
