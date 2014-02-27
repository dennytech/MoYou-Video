package com.dennytech.wiiivideo.app;

import android.widget.FrameLayout;

import com.dennytech.common.app.CLActivity;
import com.dennytech.wiiivideo.R;

public class WVActivity extends CLActivity {
	
	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		FrameLayout rootFrame = (FrameLayout) findViewById(android.R.id.content);
		rootFrame.setBackgroundResource(R.drawable.background_tabs);
	}

}
