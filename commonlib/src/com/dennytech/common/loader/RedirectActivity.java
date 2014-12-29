package com.dennytech.common.loader;

import java.util.List;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dennytech.common.app.CLActivity;
import com.dennytech.common.app.CLApplication;
import com.dennytech.common.app.Environment;
import com.dennytech.common.util.Log;

/**
 * 对外部URI Scheme请求做拦截，并重定向到指定页面。
 * 
 * 错误码：<br>
 * -402 重定向遇到异常exception<br>
 * -499 当前Application类型不符<br>
 * 
 * @author Jun.Deng
 *
 */
public class RedirectActivity extends CLActivity {

	private FrameLayout rootView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		rootView = new FrameLayout(this);
		rootView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		rootView.setId(android.R.id.primary);
		setContentView(rootView);

		if (!(getApplication() instanceof CLApplication)) {
			setError(499, null);
			return;
		}

		doRedirect();
	}

	protected void doRedirect() {
		Intent orig = getIntent();
		Intent intent = new Intent(orig.getAction(), orig.getData());
		intent.putExtras(orig);
		intent = urlMap(intent);
		try {
			// 避免进入死循环
			List<ResolveInfo> l = getPackageManager().queryIntentActivities(
					intent, 0);
			if (l.size() == 1) {
				ResolveInfo ri = l.get(0);
				if (getPackageName().equals(ri.activityInfo.packageName)) {
					if (getClass().getName().equals(ri.activityInfo.name)) {
						throw new Exception("infinite loop");
					}
				}
			} else if (l.size() > 1) {
				// should not happen, do we allow this?
			}
			startActivity(intent);
			finish();
		} catch (Exception e) {
			setError(402, e);
			Log.e("app", "unable to redirect " + getIntent(), e);
		}
	}
	
	private void setError(int errorCode, Exception e) {
		rootView.removeAllViews();
		TextView text = new TextView(this);
		text.setText("载入页面失败 (" + (errorCode > 0 ? errorCode : -1) + ")");
		if (Environment.isDebug()) {
			if (e != null) {
				text.append("\n");
				text.append(e.toString());
			}
		}
		text.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		rootView.addView(text);
	}

}
