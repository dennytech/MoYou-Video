package com.dennytech.common.service.dataservice.impl;

import com.dennytech.common.service.dataservice.Response;

/**
 * 最基本的返回数据封装
 * 
 * @author Jun.Deng
 *
 */
public class BasicResponse implements Response {
	
	private Object result;
	private Object error;
	
	public BasicResponse(Object result, Object error) {
		this.result = result;
		this.error = error;
	}

	@Override
	public Object result() {
		return result;
	}

	@Override
	public Object error() {
		return error;
	}

}
