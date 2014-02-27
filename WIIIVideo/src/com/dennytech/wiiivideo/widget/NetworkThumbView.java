package com.dennytech.wiiivideo.widget;

import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.dennytech.common.service.dataservice.Request;
import com.dennytech.common.service.dataservice.Response;
import com.dennytech.common.util.MemCache;

public class NetworkThumbView extends NetworkImageView {

	public NetworkThumbView(Context context) {
		super(context);
	}

	public NetworkThumbView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NetworkThumbView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private static final int MEM_CACHE_SIZE = 4 * 0x400 * 0x400; //
	private static final int MEM_CACHE_LIFETIME = -1; // unlimited

	private final static MemCache<String, Bitmap> memcache = new MemCache<String, Bitmap>(
			MEM_CACHE_SIZE, MEM_CACHE_LIFETIME) {

		@Override
		protected int sizeOf(Object object) {
			if (object instanceof Bitmap) {
				Bitmap bmp = (Bitmap) object;
				return bmp.getHeight() * bmp.getRowBytes();
			} else {
				return super.sizeOf(object);
			}
		}
	};

	public static Map<String, Bitmap> memcache() {
		return memcache;
	}

	@Override
	public void setImage(String url) {
		Bitmap cache = memcache().get(url);
		if (cache != null) {
			if (cache.isRecycled()) {
				memcache().remove(url);
				super.setImage(url);
				return;
			}

			setImageBitmap(cache);
			this.url = url;
			imageRetrieve = true;
			return;
		}

		super.setImage(url);
	}

	@Override
	public void onRequestFinish(Request req, Response response) {
		if (Boolean.FALSE == imageRetrieve && req == request) {
			Bitmap bmp = (Bitmap) response.result();
			memcache().put(req.url(), bmp);
			setImageBitmap(bmp);
			imageRetrieve = true;
			request = null;
		}
	}

}
