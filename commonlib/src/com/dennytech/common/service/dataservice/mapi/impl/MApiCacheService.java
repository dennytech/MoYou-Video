package com.dennytech.common.service.dataservice.mapi.impl;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dennytech.common.service.dataservice.Request;
import com.dennytech.common.service.dataservice.RequestHandler;
import com.dennytech.common.service.dataservice.cache.CacheResponse;
import com.dennytech.common.service.dataservice.cache.CacheService;
import com.dennytech.common.service.dataservice.cache.impl.BlobCacheService;
import com.dennytech.common.service.dataservice.mapi.MApiRequest;
import com.dennytech.common.util.Log;

/**
 * 专门为Mapi服务的缓存服务。
 * 
 * @author Jun.Deng
 *
 */
public class MApiCacheService implements CacheService {
	
	private static final String TAG = "mapi";
	private Context context;

	/**
	 * cache0只负责PERSISTENT和CRITICAL类型的缓存
	 * <p>
	 * time表示创建时间
	 */
	private BlobCacheService cache0;

	/**
	 * cache1负责NORMAL和FAST类型的缓存
	 * <p>
	 * time表示过期时间
	 */
	private BlobCacheService cache1;

	public MApiCacheService(Context context) {
		this.context = context;
	}

	public synchronized void close() {
		if (cache0 != null) {
			cache0.close();
			cache0 = null;
		}
		if (cache1 != null) {
			cache1.close();
			cache1 = null;
		}
	}

	public int trimToCount(int expected) {
		if (cache1 != null) {
			return cache1.trimToCount(expected);
		}
		return 0;
	}

	public static File getCacheFile(Context context) {
		return new File(context.getCacheDir(), "mapi.db");
	}

	private synchronized BlobCacheService cache0() {
		if (cache0 == null) {
			File path = getCacheFile(context);
			SQLiteDatabase db = null;
			try {
				db = SQLiteDatabase.openOrCreateDatabase(path, null);
			} catch (Exception e) {
				Log.e(TAG, "cannot open database " + path.getAbsolutePath(), e);
			}
			cache0 = new BlobCacheService(db, "c0");
		}
		return cache0;
	}

	private synchronized BlobCacheService cache1() {
		if (cache1 == null) {
			File path = getCacheFile(context);
			SQLiteDatabase db = null;
			try {
				db = SQLiteDatabase.openOrCreateDatabase(path, null);
			} catch (Exception e) {
				Log.e(TAG, "cannot open database " + path.getAbsolutePath(), e);
			}
			cache1 = new BlobCacheService(db, "c1");
		}
		return cache1;
	}

	private CacheService getCache(Request req) {
		if (req instanceof MApiRequest) {
			MApiRequest mr = (MApiRequest) req;
			switch (mr.defaultCacheType()) {
			case PERSISTENT:
			case CRITICAL:
				return cache0();
			default:
				break;
			}
		}
		return cache1();
	}

	@Override
	public void exec(Request req, RequestHandler<Request, CacheResponse> handler) {
		getCache(req).exec(req, handler);
	}

	@Override
	public CacheResponse execSync(Request req) {
		CacheResponse resp = getCache(req).execSync(req);
		return resp;
	}

	@Override
	public void abort(Request req,
			RequestHandler<Request, CacheResponse> handler,
			boolean mayInterruptIfRunning) {
		getCache(req).abort(req, handler, mayInterruptIfRunning);
	}

	@Override
	public Object get(Request key) {
		Object obj = getCache(key).get(key);
		return obj;
	}

	@Override
	public long getTime(Request key) {
		long time = getCache(key).getTime(key);
		return time;
	}

	@Override
	public boolean put(Request key, Object val, long time) {
		boolean result = getCache(key).put(key, val, time);
		return result;
	}

	@Override
	public boolean touch(Request key, long time) {
		boolean result = getCache(key).touch(key, time);
		return result;
	}

	@Override
	public void remove(Request key) {
		if (Log.isLoggable(Log.DEBUG)) {
			Log.d(TAG, "mapi cache delete " + key.url());
		}
		getCache(key).remove(key);
	}

	@Override
	public void clear() {
		Log.i(TAG, "mapi cache clear");
		cache0().clear();
		cache1().clear();
	}
}
