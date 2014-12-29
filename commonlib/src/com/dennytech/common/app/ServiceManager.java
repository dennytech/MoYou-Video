package com.dennytech.common.app;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;

import com.dennytech.common.service.configservice.impl.DefaultConfigService;
import com.dennytech.common.service.dataservice.http.impl.DefaultHttpService;
import com.dennytech.common.service.dataservice.image.impl.DefaultImageService;
import com.dennytech.common.service.dataservice.image.impl.ImageRequest;
import com.dennytech.common.service.dataservice.mapi.MApiRequest;
import com.dennytech.common.service.dataservice.mapi.impl.DefaultMApiService;
import com.dennytech.common.util.Log;

public class ServiceManager {

	protected final Context context;
	private DefaultHttpService http;
	private DefaultImageService image;
	private DefaultMApiService mapi;
	private DefaultConfigService config;

	public ServiceManager(Context context) {
		this.context = context;
	}

	public synchronized void stop() {
		if (image != null) {
			image.asyncTrimToCount(ImageRequest.TYPE_THUMBNAIL, 250);
			image.asyncTrimToCount(ImageRequest.TYPE_PHOTO, 40);
		}
		if (mapi != null) {
			mapi.asyncTrimToCount(160);
		}
	}

	public synchronized Object getService(String name) {
		if ("http".equals(name)) {
			if (http == null) {
				Executor executor = new ThreadPoolExecutor(2, 6, 60,
						TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
				http = new DefaultHttpService(context, executor);
			}
			return http;
		}
		if ("image".equals(name)) {
			if (image == null) {
				image = new DefaultImageService(context, 2);
			}
			return image;
		}
		if ("image_cahce".equals(name)) {
			if (image == null) {
				getService("image");
			}
			return image.cache();
		}
		if ("mapi".equals(name)) {
			if (mapi == null) {
				getService("statistics");
				mapi = createMApiService();
			}
			return mapi;
		}
		if ("mapi_cache".equals(name)) {
			if (mapi == null) {
				getService("mapi");
			}
			return mapi.cache();
		}
		if ("config".equals(name)) {
			if (config == null) {
				getService("mapi");
				config = createConfigService();
			}
			return config;
		}

		Log.e("unknown service \"" + name + "\"");
		return null;
	}

	protected DefaultMApiService createMApiService() {
		return new DefaultMApiService(context, Environment.userAgent());
	}

	protected DefaultConfigService createConfigService() {
		return new DefaultConfigService(context, mapi) {

			@Override
			protected MApiRequest createRequest() {
				return null;
			}
		};
	}

}
