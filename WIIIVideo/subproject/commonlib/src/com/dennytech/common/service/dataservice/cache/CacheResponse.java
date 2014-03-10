package com.dennytech.common.service.dataservice.cache;

import com.dennytech.common.service.dataservice.Response;

public interface CacheResponse extends Response {

	/**
	 * 一般为缓存创建的时间。
	 * 
	 * @return
	 */
	long time();

}
