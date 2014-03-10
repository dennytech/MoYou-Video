package com.dennytech.wiiivideo.player;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.app.MYActivity;

public class PlayerActivity extends MYActivity {

	private VideoView mVideoView;

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (Build.VERSION.SDK_INT >= 16) {
				View decorView = getWindow().getDecorView();
				if (msg.what == 0) {
					decorView
							.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
					this.sendEmptyMessageDelayed(1, 4000);
					
				} else {
					int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN;
					decorView.setSystemUiVisibility(uiOptions);
				}
			}
		};
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (!LibsChecker.checkVitamioLibs(this))
			return;

		setContentView(R.layout.activity_player);
		mVideoView = (VideoView) findViewById(R.id.surface_view);

		String path = getIntent().getData().getQueryParameter("url");

		if (TextUtils.isEmpty(path)) {
			Toast.makeText(PlayerActivity.this, "illegal url",
					Toast.LENGTH_LONG).show();
			return;

		} else {
			mVideoView.setVideoPath(path);
			mVideoView.setMediaController(new MediaController(this));
			mVideoView.requestFocus();

			mVideoView
					.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared(MediaPlayer mediaPlayer) {
							// optional need Vitamio 4.0
							mediaPlayer.setPlaybackSpeed(1.0f);
							handler.sendEmptyMessage(1);
						}
					});
		}

	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		handler.removeMessages(1);
		handler.sendEmptyMessage(0);
		return super.dispatchTouchEvent(ev);
	}

}
