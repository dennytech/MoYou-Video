package com.dennytech.common.service.dataservice.http.impl;

import java.io.InputStream;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;

import com.dennytech.common.service.dataservice.http.FormInputStream;
import com.dennytech.common.service.dataservice.http.HttpRequest;
import com.dennytech.common.service.dataservice.impl.BasicRequest;

/**
 * 基本的Http Request。
 * 
 * @author Jun.Deng
 *
 */
public class BasicHttpRequest extends BasicRequest implements HttpRequest {

	public static final String GET = HttpGet.METHOD_NAME;
	public static final String POST = HttpPost.METHOD_NAME;
	public static final String PUT = HttpPut.METHOD_NAME;
	public static final String DELETE = HttpDelete.METHOD_NAME;

	private String method;
	private InputStream input;
	private List<NameValuePair> headers;

	public BasicHttpRequest(String url, String method, InputStream input) {
		this(url, method, input, null);
	}

	public BasicHttpRequest(String url, String method, InputStream input,
			List<NameValuePair> headers) {
		super(url);
		this.method = method;
		this.input = input;
		this.headers = headers;
	}

	public static HttpRequest httpGet(String url) {
		return new BasicHttpRequest(url, GET, null, null);
	}

	public static HttpRequest httpPost(String url, String... forms) {
		return new BasicHttpRequest(url, POST, new FormInputStream(forms), null);
	}

	@Override
	public String method() {
		return method;
	}

	@Override
	public InputStream input() {
		return input;
	}

	@Override
	public List<NameValuePair> headers() {
		return headers;
	}

	@Override
	public String toString() {
		return method + ": " + super.toString();
	}
}
