package com.dennytech.wiiivideo.videolist.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.data.Video;
import com.dennytech.wiiivideo.widget.NetworkThumbView;

public class VideoCommendItem extends FrameLayout {

	private NetworkThumbView icon;
	private TextView title;

	public VideoCommendItem(Context context) {
		super(context);
	}

	public VideoCommendItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		icon = (NetworkThumbView) findViewById(R.id.icon);
		title = (TextView) findViewById(R.id.title);
	}

	public void setData(Video v) {
		icon.setImage(v.thumb);
		title.setText(v.title);
	}
}
