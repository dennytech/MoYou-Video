package com.dennytech.common.app;

import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.dennytech.common.service.configservice.ConfigService;
import com.dennytech.common.service.dataservice.cache.CacheService;
import com.dennytech.common.service.dataservice.http.HttpService;
import com.dennytech.common.service.dataservice.image.ImageService;
import com.dennytech.common.service.dataservice.mapi.AutoReleaseMApiService;
import com.dennytech.common.service.dataservice.mapi.MApiService;

public class CLActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		CLApplication.instance().activityOnCreate(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		CLApplication.instance().activityOnResume(this);
	}

	@Override
	protected void onPause() {
		CLApplication.instance().activityOnPause(this);

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		CLApplication.instance().activityOnDestory(this);

		if (autoReleaseMApiService != null) {
			autoReleaseMApiService.onDestory();
		}

		super.onDestroy();
	}

	private AutoReleaseMApiService autoReleaseMApiService;

	public Object getService(String name) {
		if ("mapi".equals(name)) {
			if (autoReleaseMApiService == null) {
				MApiService orig = (MApiService) CLApplication.instance()
						.getService("mapi");
				autoReleaseMApiService = new AutoReleaseMApiService(
						CLActivity.this, orig);
			}
			return autoReleaseMApiService;
		}
		return CLApplication.instance().getService(name);
	}

	private HttpService httpService;

	public HttpService httpService() {
		if (httpService == null) {
			httpService = (HttpService) getService("http");
		}
		return httpService;
	}

	private ImageService imageService;

	public ImageService imageService() {
		if (imageService == null) {
			imageService = (ImageService) getService("image");
		}
		return imageService;
	}

	private CacheService mapiCacheService;

	public CacheService mapiCacheService() {
		if (mapiCacheService == null) {
			mapiCacheService = (CacheService) getService("mapi_cache");
		}
		return mapiCacheService;
	}

	private MApiService mapiService;

	public MApiService mapiService() {
		if (mapiService == null) {
			mapiService = (MApiService) getService("mapi");
		}
		return mapiService;
	}

	private ConfigService configService;

	public ConfigService configService() {
		if (configService == null) {
			configService = (ConfigService) getService("config");
		}
		return configService;
	}

	// ********* URL Mapping ********** //

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		intent = urlMap(intent);
		intent.putExtra("_from", getMyUrl());
		intent.putExtra("_startTime", SystemClock.elapsedRealtime());
		super.startActivityForResult(intent, requestCode);
		overridePendingTransition(0, 0);
	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent,
			int requestCode) {
		intent = urlMap(intent);
		intent.putExtra("_from", getMyUrl());
		intent.putExtra("_startTime", SystemClock.elapsedRealtime());
		super.startActivityFromFragment(fragment, intent, requestCode);
		overridePendingTransition(0, 0);
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, 0);
	}

	public void startActivity(String urlSchema) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlSchema)));
	}

	public void startActivityForResult(String urlSchema, int requestCode) {
		startActivityForResult(
				new Intent(Intent.ACTION_VIEW, Uri.parse(urlSchema)),
				requestCode);
	}

	public Intent urlMap(Intent intent) {
		Application app = getApplication();
		if (app instanceof CLApplication) {
			return ((CLApplication) app).urlMap(intent);
		} else {
			return intent;
		}
	}

	private final static HashMap<String, String> manifestUrlMapping = new HashMap<String, String>();

	protected String getMyUrl() {
		if (getIntent().getDataString() != null)
			return getIntent().getDataString();

		String myClassName = getClass().getName();
		String manifestUrl = manifestUrlMapping.get(myClassName);
		if (manifestUrl != null)
			return manifestUrl;
		try {
			AssetManager am = createPackageContext(getPackageName(), 0)
					.getAssets();
			XmlResourceParser xml = am
					.openXmlResourceParser("AndroidManifest.xml");
			int eventType = xml.getEventType();
			String inActivity = null;
			boolean inIntentFilter = false;
			xmlloop: while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					if (inIntentFilter && myClassName.equals(inActivity)) {
						if (xml.getName().equals("data")) {
							String scheme = xml
									.getAttributeValue(
											"http://schemas.android.com/apk/res/android",
											"scheme");
							String host = xml
									.getAttributeValue(
											"http://schemas.android.com/apk/res/android",
											"host");
							if (scheme != null && host != null
									&& !scheme.startsWith("http")) {
								manifestUrl = scheme + "://" + host;
								break xmlloop;
							}
						}
					}
					if (xml.getName().equals("activity")) {
						inActivity = xml.getAttributeValue(
								"http://schemas.android.com/apk/res/android",
								"name");
						if (inActivity != null && inActivity.startsWith(".")) {
							inActivity = getPackageName() + inActivity;
						}
					}
					if (xml.getName().equals("intent-filter")) {
						inIntentFilter = true;
					}
					break;
				case XmlPullParser.END_TAG:
					if (xml.getName().equals("activity")) {
						inActivity = null;
					}
					if (xml.getName().equals("intent-filter")) {
						inIntentFilter = false;
					}
					break;
				}
				eventType = xml.nextToken();
			}
		} catch (Exception e) {
		}

		if (manifestUrl == null) {
			manifestUrl = "class://" + myClassName;
		}
		manifestUrlMapping.put(myClassName, manifestUrl);
		return manifestUrl;
	}

	// ////////////
	// Dialog相关
	// ////////////

	protected Dialog managedDialog;

	/**
	 * 显示progress对话框 </br></br> progress对话框应该只在<b>主线程</b>中被显示
	 * 
	 * @param message
	 */
	final public void showProgressDialog(String message) {
		dismissDialog();
		ProgressDialog dlg = new ProgressDialog(this);
		dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				onProgressDialogCancel();
			}
		});
		dlg.setMessage(message);

		managedDialog = dlg;
		dlg.show();
	}

	protected void onProgressDialogCancel() {
		// TO OVERRIDE
	}

	final public void showDialog(String title, String message,
			OnClickListener listener) {
		dismissDialog();
		AlertDialog dlg = createDialog(title, message);
		dlg.setButton(DialogInterface.BUTTON_POSITIVE, "确定", listener);

		managedDialog = dlg;
		dlg.show();
	}

	final public void showDialog(String title, String message,
			String positiveText, OnClickListener positiveListener,
			String negativeText, OnClickListener negativeListener,
			OnCancelListener cacelListener) {
		dismissDialog();
		AlertDialog dlg = createDialog(title, message);
		dlg.setButton(DialogInterface.BUTTON_POSITIVE, positiveText,
				positiveListener);
		dlg.setButton(DialogInterface.BUTTON_NEGATIVE, negativeText,
				negativeListener);
		dlg.setOnCancelListener(cacelListener);

		managedDialog = dlg;
		dlg.show();
	}

	final public AlertDialog createDialog(String title, String message) {
		AlertDialog dlg = new AlertDialog.Builder(this).create();
		dlg.setTitle(title);
		dlg.setMessage(message);

		managedDialog = dlg;
		return dlg;
	}

	final public void dismissDialog() {
		if (managedDialog != null && managedDialog.isShowing()) {
			managedDialog.dismiss();
		}
		managedDialog = null;
	}

}
