package com.dennytech.common.service.dataservice.image.impl;

import com.dennytech.common.service.dataservice.http.impl.BasicHttpRequest;

public class ImageRequest extends BasicHttpRequest {
	/**
	 * TYPE_UNKNOWN will not be cached
	 */
	public static final int TYPE_UNKNOWN = 0;
	/**
	 * TYPE_THUMBNAIL is normally under 25k
	 */
	public static final int TYPE_THUMBNAIL = 1;
	/**
	 * TYPE_LARGE_PHOTO is limited to 250k
	 */
	public static final int TYPE_PHOTO = 2;
	private int type;
	private boolean cacheOnly;

	public ImageRequest(String url, int type) {
		this(url, type, false);
	}

	public ImageRequest(String url, int type, boolean cacheOnly) {
		super(url, GET, null);
		this.type = type;
		this.cacheOnly = cacheOnly;
	}

	public int type() {
		return type;
	}

	public boolean cacheOnly() {
		return cacheOnly;
	}
}
