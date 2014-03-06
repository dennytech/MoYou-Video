package com.dennytech.wiiivideo.app;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;

import com.dennytech.common.app.CLActivity;
import com.dennytech.wiiivideo.HomeActivity;
import com.umeng.analytics.MobclickAgent;

public class WVActivity extends CLActivity {

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!(this instanceof HomeActivity)) {
			ActionBar actionBar = getActionBar();
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			}
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
