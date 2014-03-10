package com.dennytech.common.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;

import com.dennytech.common.util.Log;

/**
 * 手机相关环境信息
 * 
 * @author Jun.Deng
 * 
 */
public class Environment {

	/**
	 * 渠道号
	 */
	private static String channelID;
	private static String deviceType;
	private static String userAgent;
	private static int versionCode;

	/**
	 * 是否为调试状态
	 * 
	 * @return
	 */
	public static boolean isDebug() {
		return Log.LEVEL < Integer.MAX_VALUE;
	}

	public static String channelID() {
		if (channelID == null) {
			try {
				AssetManager am = CLApplication.instance().getAssets();
				InputStream in;
				in = am.open("channel");
				BufferedReader bin = new BufferedReader(new InputStreamReader(
						in));
				String tempChannel = bin.readLine().toString();
				if (tempChannel != null) {
					channelID = tempChannel;
				}
			} catch (IOException e) {

			} finally {
				if (channelID == null) {
					channelID = "";
				}
			}
		}
		return channelID;
	}

	public static String deviceType() {
		if (deviceType == null) {
			deviceType = Build.MANUFACTURER + " " + Build.MODEL;
		}
		return deviceType;
	}

	public static String userAgent() {
		if (userAgent == null) {
			try {
				Context c = CLApplication.instance();
				PackageInfo packageInfo = c.getPackageManager().getPackageInfo(
						c.getPackageName(), 0);
				StringBuilder sb = new StringBuilder("BDAndroidTuan (");
				sb.append(packageInfo.packageName);
				sb.append(" ").append(packageInfo.versionName);

				String channel = channelID();
				if (!TextUtils.isEmpty(channel))
					sb.append(" ").append(channel);
				else
					sb.append(" null");

				sb.append("; Android ");
				sb.append(Build.VERSION.RELEASE);
				sb.append(")");

				userAgent = sb.toString();
			} catch (Exception e) {
				userAgent = "DT. PUB (www.dennytech.com 1.0; Android "
						+ Build.VERSION.RELEASE + ")";
			}
		}
		return userAgent;
	}

	/**
	 * 当前的sessionId。会在运行过程中发生变化。
	 * <p>
	 * 从打开应用到退出应用（或者应用切换到后台）表示一个session，产品可能需要统计用。
	 */
	public static String sessionId() {
		CLApplication app = CLApplication.instance();
		if (app == null)
			return "";
		return app.sessionId();
	}

	public static int getVersionCode() {
		if (versionCode == 0) {
			try {
				PackageInfo pi = CLApplication
						.instance()
						.getPackageManager()
						.getPackageInfo(
								CLApplication.instance().getPackageName(), 0);
				versionCode = pi.versionCode;
			} catch (NameNotFoundException e) {
				versionCode = -1;
			}
		}
		return versionCode;
	}

}
