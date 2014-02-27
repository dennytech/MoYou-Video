package com.dennytech.wiiivideo.app;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.dennytech.common.app.CLActivity;
import com.dennytech.wiiivideo.HomeActivity;
import com.dennytech.wiiivideo.R;
import com.umeng.analytics.MobclickAgent;

public class WVActivity extends CLActivity {

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		FrameLayout rootFrame = (FrameLayout) findViewById(android.R.id.content);
		rootFrame.setBackgroundResource(R.drawable.background_tabs);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!(this instanceof HomeActivity)) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	public void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(getClass().getName());
		MobclickAgent.onResume(this);
	}

	public void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd(getClass().getName());
		MobclickAgent.onPause(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
