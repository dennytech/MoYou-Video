package com.dennytech.common.service.dataservice.impl;

import com.dennytech.common.service.dataservice.Request;

/**
 * 最基本的包含url的Request<br>
 * <br>
 * 不包含比较函数，只有当引用相等时才相等
 * 
 * @author Jun.Deng
 * 
 */
public class BasicRequest implements Request {

	private String url;

	public BasicRequest(String url) {
		this.url = url;
	}

	@Override
	public String url() {
		return url;
	}

	@Override
	public String toString() {
		return url;
	}

}
