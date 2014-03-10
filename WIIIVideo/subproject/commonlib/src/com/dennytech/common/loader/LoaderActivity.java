package com.dennytech.common.loader;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dennytech.common.app.CLActivity;
import com.dennytech.common.app.Environment;
import com.dennytech.common.util.Log;

/**
 * 负责装载Fragment的容器
 * 
 * <p>
 * 错误对照表: <br>
 * - 400 uri为空<br>
 * - 401 fragmentName为空<br>
 * - 402 遇到exception<br>
 * 
 * @author Jun.Deng
 * 
 */
public class LoaderActivity extends CLActivity {

	private FrameLayout rootView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		rootView = new FrameLayout(this);
		rootView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		rootView.setId(getRootViewID());
		setContentView(rootView);

		Fragment fragment = null;
		Uri uri = getIntent().getData();
		if (uri == null) {
			setError(400, null);
			return;
		}

		String fragmentName = uri.getFragment();
		if (fragmentName == null) {
			setError(401, null);
			return;
		}

		try {
			fragment = (Fragment) getClassLoader().loadClass(fragmentName)
					.newInstance();
		} catch (Exception e) {
			setError(402, e);
			Log.e("loader", "load fragment failed", e);
			return;
		}

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(getRootViewID(), fragment);
		ft.commit();

	}
	
	/**
	 * 提供根布局的ID
	 * 
	 * @return
	 */
	protected int getRootViewID() {
		return android.R.id.primary;
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
