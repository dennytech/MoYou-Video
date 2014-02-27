package com.dennytech.wiiivideo.videolist.view;

import android.content.Context;
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
		pubView.setText("发布时间：" + video.publishTime);
		playView.setText("播放次数：" + video.playTimes);
	}

}