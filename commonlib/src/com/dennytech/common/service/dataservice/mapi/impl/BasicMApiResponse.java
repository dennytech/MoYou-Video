package com.dennytech.common.service.dataservice.mapi.impl;

import java.util.List;

import org.apache.http.NameValuePair;

import android.text.TextUtils;

import com.dennytech.common.service.dataservice.http.impl.BasicHttpResponse;
import com.dennytech.common.service.dataservice.mapi.MApiMsg;
import com.dennytech.common.service.dataservice.mapi.MApiResponse;

/**
 * 最基本的MapiResponse实现。result为String字符串(json)，需要业务层自己解析。
 * 
 * @author Jun.Deng
 * 
 */
public class BasicMApiResponse extends BasicHttpResponse implements
		MApiResponse {
	public static final Object ERROR_STATUS = "server status error";
	public static final Object ERROR_MALFORMED = "malformed content";
	public static final String MSG_ERROR_NET = "网络不给力~";
	public static final String MSG_ERROR_SERVER = "服务器繁忙，稍后再试~";
	private byte[] rawData;
	private boolean isCache;

	public BasicMApiResponse(int statusCode, byte[] rawData, Object result,
			List<NameValuePair> headers, Object error, boolean isCache) {
		super(statusCode, result, headers, error);
		this.rawData = rawData;
		this.isCache = isCache;
	}

	@Override
	public MApiMsg message() {
		Object error = error();
		if (error instanceof MApiMsg) {
			MApiMsg errMsg = (MApiMsg) error;
			if (TextUtils.isEmpty(errMsg.getErrorMsg())) {
				errMsg.setErrorMsg(MSG_ERROR_SERVER);
			}
			return (MApiMsg) error;
		}

		if (result() != null) {
			return null;
		}

		return new MApiMsg(-1, MSG_ERROR_NET);
	}

	@Override
	public byte[] rawData() {
		return rawData;
	}

	@Override
	public boolean isCache() {
		return isCache;
	}

}
