package com.dennytech.wiiivideo.videolist.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.data.Video;
import com.dennytech.wiiivideo.widget.NetworkThumbView;

public class VideoListItem extends LinearLayout {

	private NetworkThumbView thumbView;
	private TextView titleView;
	private TextView pubView;
	private TextView playView;

	public VideoListItem(Context context) {
		super(context);
	}

	public VideoListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		thumbView = (NetworkThumbView) findViewById(R.id.thumb);
		titleView = (TextView) findViewById(R.id.title);
		pubView = (TextView) findViewById(R.id.publish);
		playView = (TextView) findViewById(R.id.play);
	}
	
	public void setData(Video video) {
		thumbView.setImage(video.thumb);
		titleView.setText(video.title);
		if (!TextUtils.isEmpty(video.publishTime)) {
			pubView.setText(video.publishTime + "发布");
		}
		playView.setText(video.playTimes + "次播放");
	}

}
