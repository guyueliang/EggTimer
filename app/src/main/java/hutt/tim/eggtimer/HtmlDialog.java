
package hutt.tim.eggtimer;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;

// A very simple dialog with an HTML control and a nice white background style.
public class HtmlDialog extends Dialog implements OnClickListener
{
	WebView mWeb;
	LinearLayout mLinear;

	public HtmlDialog(Context context)
	{
		super(context, R.style.HtmlDialog);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mLinear = new LinearLayout(this.getContext());
		mLinear.setOrientation(LinearLayout.VERTICAL);

		mWeb = new WebView(this.getContext());
		LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0); // This is the only way to make the layout work. God knows why.
		l.setMargins(5, 5, 5, 5);
		l.weight = 1.0f;
		mLinear.addView(mWeb, l);

		Button button = new Button(this.getContext());
		button.setText("OK");
		button.setOnClickListener(this);
		l = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		mLinear.addView(button, l);
		
		setContentView(mLinear);
	}

	public void setContent(String content)
	{
		mWeb.clearView();
		mWeb.loadData(content, "text/html", "utf-8");
	}

	public void onClick(View v)
	{
		dismiss();
	}
}
