package com.dennytech.common.service.dataservice.http.impl;

import java.util.List;

import org.apache.http.NameValuePair;

import com.dennytech.common.service.dataservice.http.HttpResponse;
import com.dennytech.common.service.dataservice.impl.BasicResponse;

/**
 * 基本Http请求响应。增加了状态码和头信息
 * 
 * @author Jun.Deng
 *
 */
public class BasicHttpResponse extends BasicResponse implements HttpResponse {

	private int statusCode;
	private List<NameValuePair> headers;

	public BasicHttpResponse(int statusCode, Object result,
			List<NameValuePair> headers, Object error) {
		super(result, error);
		this.statusCode = statusCode;
		this.headers = headers;
	}

	@Override
	public int statusCode() {
		return statusCode;
	}

	@Override
	public List<NameValuePair> headers() {
		return headers;
	}
}
