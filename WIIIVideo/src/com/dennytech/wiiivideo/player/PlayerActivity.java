package com.dennytech.wiiivideo.player;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.app.WVActivity;

public class PlayerActivity extends WVActivity {

	private VideoView mVideoView;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (!LibsChecker.checkVitamioLibs(this))
			return;
		setContentView(R.layout.activity_player);
		mVideoView = (VideoView) findViewById(R.id.surface_view);

		String path = getIntent().getData().getQueryParameter("url");

		if (TextUtils.isEmpty(path)) {
			Toast.makeText(PlayerActivity.this, "illegal url", Toast.LENGTH_LONG).show();
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
						}
					});
		}

	}

}
