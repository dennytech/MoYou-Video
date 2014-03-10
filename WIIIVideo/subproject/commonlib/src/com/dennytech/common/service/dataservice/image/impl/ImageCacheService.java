package com.dennytech.common.service.dataservice.image.impl;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dennytech.common.service.dataservice.Request;
import com.dennytech.common.service.dataservice.RequestHandler;
import com.dennytech.common.service.dataservice.cache.CacheResponse;
import com.dennytech.common.service.dataservice.cache.CacheService;
import com.dennytech.common.service.dataservice.cache.impl.BlobCacheResponse;
import com.dennytech.common.service.dataservice.cache.impl.BlobCacheService;
import com.dennytech.common.util.Log;

public class ImageCacheService implements CacheService {
	private static final String TAG = "image";
	private Context context;
	private BlobCacheService thumbnailCache;
	private BlobCacheService photoCache;
	private final ConcurrentHashMap<Request, RequestHandler<Request, CacheResponse>> runningFails = new ConcurrentHashMap<Request, RequestHandler<Request, CacheResponse>>();

	public ImageCacheService(Context context) {
		this.context = context;
	}

	public static File getThunbnailCacheFile(Context context) {
		File path = new File(context.getCacheDir(), "thumb.db");
		return path;
	}

	private synchronized BlobCacheService thumbnailCache() {
		if (thumbnailCache == null) {
			File path = getThunbnailCacheFile(context);
			SQLiteDatabase db = null;
			try {
				db = SQLiteDatabase.openOrCreateDatabase(path, null);
			} catch (Exception e) {
				Log.e(TAG, "cannot open database " + path.getAbsolutePath(), e);
			}
			thumbnailCache = new BlobCacheService(db, "thumb");
		}
		return thumbnailCache;
	}

	public static File getPhotoCacheFile(Context context) {
		File path = new File(context.getCacheDir(), "photo.db");
		return path;
	}

	private synchronized BlobCacheService photoCache() {
		if (photoCache == null) {
			File path = getPhotoCacheFile(context);
			SQLiteDatabase db = null;
			try {
				db = SQLiteDatabase.openOrCreateDatabase(path, null);
			} catch (Exception e) {
				Log.e(TAG, "cannot open database " + path.getAbsolutePath(), e);
			}
			photoCache = new BlobCacheService(db, "photo");
		}
		return photoCache;
	}

	public synchronized void close() {
		if (thumbnailCache != null) {
			thumbnailCache.close();
		}
		if (photoCache != null) {
			photoCache.close();
		}
	}

	public int trimToCount(int type, int expected) {
		switch (type) {
		case ImageRequest.TYPE_THUMBNAIL:
			if (thumbnailCache != null) {
				return thumbnailCache.trimToCount(expected);
			}
			break;
		case ImageRequest.TYPE_PHOTO:
			if (photoCache != null) {
				return photoCache.trimToCount(expected);
			}
			break;
		}
		return 0;
	}

	private CacheResponse getFailResponse(Request req) {
		return new BlobCacheResponse(0, null, "not found (type=0): "
				+ req.url());
	}

	private int typeOf(Request req) {
		if (req instanceof ImageRequest) {
			ImageRequest ir = (ImageRequest) req;
			return ir.type();
		} else {
			return ImageRequest.TYPE_UNKNOWN;
		}
	}

	private final Handler mhandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			Request req = (Request) msg.obj;
			RequestHandler<Request, CacheResponse> handler = runningFails
					.remove(req);
			if (handler != null) {
				handler.onRequestFailed(req, getFailResponse(req));
			}
		}
	};

	@Override
	public void exec(Request req, RequestHandler<Request, CacheResponse> handler) {
		int type = typeOf(req);
		switch (type) {
		case ImageRequest.TYPE_THUMBNAIL:
			thumbnailCache().exec(req, handler);
			break;
		case ImageRequest.TYPE_PHOTO:
			photoCache().exec(req, handler);
			break;
		case ImageRequest.TYPE_UNKNOWN:
		default:
			// default is no cache
			runningFails.put(req, handler);
			mhandler.sendMessage(mhandler.obtainMessage(0, req));
			break;
		}
	}

	@Override
	public CacheResponse execSync(Request req) {
		int type = typeOf(req);
		switch (type) {
		case ImageRequest.TYPE_THUMBNAIL:
			return thumbnailCache().execSync(req);
		case ImageRequest.TYPE_PHOTO:
			return photoCache().execSync(req);
		case ImageRequest.TYPE_UNKNOWN:
		default:
			return getFailResponse(req);
		}
	}

	@Override
	public void abort(Request req,
			RequestHandler<Request, CacheResponse> handler,
			boolean mayInterruptIfRunning) {
		runningFails.remove(req, handler);
	}

	@Override
	public Object get(Request key) {
		int type = typeOf(key);
		switch (type) {
		case ImageRequest.TYPE_THUMBNAIL:
			return thumbnailCache().get(key);
		case ImageRequest.TYPE_PHOTO:
			return photoCache().get(key);
		case ImageRequest.TYPE_UNKNOWN:
		default:
			return null;
		}
	}

	@Override
	public long getTime(Request key) {
		int type = typeOf(key);
		switch (type) {
		case ImageRequest.TYPE_THUMBNAIL:
			return thumbnailCache().getTime(key);
		case ImageRequest.TYPE_PHOTO:
			return photoCache().getTime(key);
		case ImageRequest.TYPE_UNKNOWN:
		default:
			return 0;
		}
	}

	@Override
	public boolean put(Request key, Object val, long time) {
		int type = typeOf(key);
		switch (type) {
		case ImageRequest.TYPE_THUMBNAIL:
			return thumbnailCache().put(key, val, time);
		case ImageRequest.TYPE_PHOTO:
			return photoCache().put(key, val, time);
		case ImageRequest.TYPE_UNKNOWN:
		default:
			return true;
		}
	}

	@Override
	public boolean touch(Request key, long time) {
		int type = typeOf(key);
		switch (type) {
		case ImageRequest.TYPE_THUMBNAIL:
			return thumbnailCache().touch(key, time);
		case ImageRequest.TYPE_PHOTO:
			return photoCache().touch(key, time);
		case ImageRequest.TYPE_UNKNOWN:
		default:
			return true;
		}
	}

	@Override
	public void remove(Request key) {
		int type = typeOf(key);
		switch (type) {
		case ImageRequest.TYPE_THUMBNAIL:
			thumbnailCache().remove(key);
			return;
		case ImageRequest.TYPE_PHOTO:
			photoCache().remove(key);
			return;
		case ImageRequest.TYPE_UNKNOWN:
		default:
			return;
		}
	}

	@Override
	public void clear() {
		thumbnailCache().clear();
		photoCache().clear();
	}
}
