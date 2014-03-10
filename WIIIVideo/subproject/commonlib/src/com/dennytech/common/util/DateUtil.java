package com.dennytech.common.util;

import org.json.JSONObject;

import com.dennytech.common.service.dataservice.mapi.CacheType;
import com.dennytech.common.service.dataservice.mapi.MApiRequest;
import com.dennytech.common.service.dataservice.mapi.MApiRequestHandler;
import com.dennytech.common.service.dataservice.mapi.MApiResponse;
import com.dennytech.common.service.dataservice.mapi.MApiService;
import com.dennytech.common.service.dataservice.mapi.impl.BasicMApiRequest;

public class DateUtil implements MApiRequestHandler {

	private static final String TAG = DateUtil.class.getSimpleName();
	private static long TIME_DIFFERENCE = 0;

	private static DateUtil instance;

	private synchronized static DateUtil instance() {
		if (instance == null) {
			instance = new DateUtil();
		}
		return instance;
	}

	private DateUtil() {
	}

	/**
	 * 刷新服务器时间。应该在主线程中调用。
	 * 
	 * @param mapi
	 */
	public static void refreshServerTime(MApiService mapi) {
		MApiRequest request = BasicMApiRequest
				.mapiGet(
						"http://promo.lbc.baidu.com/promov1/subject/server",
						CacheType.DISABLED, null);
		mapi.exec(request, instance());
	}

	public static void setServerTime(long time) {
		TIME_DIFFERENCE = time * 1000 - System.currentTimeMillis();
	}

	/**
	 * 获取服务器时间（校准过后的）。
	 */
	public static long serverTimeMillis() {
		return System.currentTimeMillis() + TIME_DIFFERENCE;
	}

	@Override
	public void onRequestStart(MApiRequest req) {
	}

	@Override
	public void onRequestProgress(MApiRequest req, int count, int total) {
	}

	@Override
	public void onRequestFinish(MApiRequest req, MApiResponse resp) {
		if (resp.result() instanceof String) {
			String result = (String) resp.result();
			try {
				JSONObject jo = new JSONObject(result);
				setServerTime(jo.optLong("server_time"));
			} catch (Exception e) {
				Log.w(TAG, "get server time fail", e);
			}
		}
	}

	@Override
	public void onRequestFailed(MApiRequest req, MApiResponse resp) {
	}

}
