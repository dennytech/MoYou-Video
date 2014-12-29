package com.dennytech.common.service.dataservice.http;

import java.util.List;

import org.apache.http.NameValuePair;

import com.dennytech.common.service.dataservice.Response;

public interface HttpResponse extends Response {

	/**
	 * HTTP请求返回状态值，Status Code
	 * 
	 * @return
	 */
	int statusCode();

	/**
	 * HTTP请求返回头信息
	 * 
	 * @return
	 */
	List<NameValuePair> headers();

}
