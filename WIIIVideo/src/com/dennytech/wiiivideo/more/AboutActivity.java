package com.dennytech.wiiivideo.more;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.app.WVActivity;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;
import com.umeng.update.UmengUpdateListener;
import com.umeng.update.UpdateResponse;
import com.umeng.update.UpdateStatus;

public class AboutActivity extends WVActivity implements OnClickListener{
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_about);
		setTitle(getText(R.string.action_about));
		
		TextView content = (TextView) findViewById(R.id.about_content);
		content.setText(MobclickAgent.getConfigParams(this, "about_content"));

		TextView version = (TextView) findViewById(R.id.version);
		version.setText("版本：" + getVersionName());
		
		findViewById(R.id.about_check_update).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.about_check_update) {
			UmengUpdateAgent.setUpdateOnlyWifi(false);
			UmengUpdateAgent.setUpdateAutoPopup(false);
			UmengUpdateAgent.setUpdateListener(new UmengUpdateListener() {
				@Override
				public void onUpdateReturned(int updateStatus,
						UpdateResponse updateInfo) {
					dismissDialog();
					switch (updateStatus) {
					case UpdateStatus.Yes: // has update
						UmengUpdateAgent.showUpdateDialog(AboutActivity.this,
								updateInfo);
						break;
					case UpdateStatus.No: // has no update
						showDialog(getString(R.string.app_name),
								getString(R.string.update_no), null);
						break;
					case UpdateStatus.NoneWifi: // none wifi
						Toast.makeText(AboutActivity.this,
								getString(R.string.update_no_wifi),
								Toast.LENGTH_SHORT).show();
						break;
					case UpdateStatus.Timeout: // time out
						Toast.makeText(AboutActivity.this,
								getString(R.string.update_timeout),
								Toast.LENGTH_SHORT).show();
						break;
					}
				}
			});
			UmengUpdateAgent.update(AboutActivity.this);
			showProgressDialog(getString(R.string.update_checking));
			
			MobclickAgent.onEvent(AboutActivity.this, "about_check_update");
		}
	}

	public String getVersionName() {
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			return pi.versionName;
		} catch (NameNotFoundException e) {
			return "1.0";
		}
	}

}
