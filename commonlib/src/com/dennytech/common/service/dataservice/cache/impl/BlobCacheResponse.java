package com.dennytech.common.service.dataservice.cache.impl;

import com.dennytech.common.service.dataservice.cache.CacheResponse;
import com.dennytech.common.service.dataservice.impl.BasicResponse;

public class BlobCacheResponse extends BasicResponse implements CacheResponse {
	private long time;

	public BlobCacheResponse(long time, byte[] result, Object error) {
		super(result, error);
		this.time = time;
	}

	public byte[] bytes() {
		return (byte[]) result();
	}

	@Override
	public long time() {
		return time;
	}
}
