package com.dennytech.common.service.dataservice.image.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dennytech.common.service.dataservice.Request;
import com.dennytech.common.service.dataservice.RequestHandler;
import com.dennytech.common.service.dataservice.Response;
import com.dennytech.common.service.dataservice.cache.CacheResponse;
import com.dennytech.common.service.dataservice.cache.CacheService;
import com.dennytech.common.service.dataservice.http.HttpRequest;
import com.dennytech.common.service.dataservice.http.HttpResponse;
import com.dennytech.common.service.dataservice.http.HttpService;
import com.dennytech.common.service.dataservice.http.impl.BasicHttpRequest;
import com.dennytech.common.service.dataservice.http.impl.DefaultHttpService;
import com.dennytech.common.service.dataservice.image.ImageService;
import com.dennytech.common.service.dataservice.impl.BasicResponse;
import com.dennytech.common.util.BlockingItem;
import com.dennytech.common.util.Daemon;
import com.dennytech.common.util.Log;

/**
 * 默认的图片请求服务
 * <p>
 * 接受ImageRequest中指定的类型，如果不是ImageRequest，默认TYPE_UNKNOWN<br>
 * 延时初始化<br>
 * 
 * @author Jun.Deng
 * 
 */
public class DefaultImageService implements ImageService {
	private static final String TAG = "image";
	private Context context;
	private int poolSize;
	private ImageCacheService cache;
	private DefaultHttpService http;
	private final ConcurrentHashMap<Request, Session> runningSession = new ConcurrentHashMap<Request, DefaultImageService.Session>();

	private static class Session {
		public Request request;
		public RequestHandler<Request, Response> handler;
		/**
		 * 0=idle or canceled<br>
		 * 1=cache<br>
		 * 2=http<br>
		 * 3=decoding<br>
		 * 4=success
		 */
		public int status;
		public Response response;
		public byte[] writeToCache;

		public Session(Request req, RequestHandler<Request, Response> h) {
			request = req;
			handler = h;
		}
	}

	public DefaultImageService(Context context, int poolSize) {
		this.context = context;
		this.poolSize = poolSize;
	}

	public int runningCount() {
		return runningSession.size();
	}

	public synchronized CacheService cache() {
		if (cache == null) {
			cache = new ImageCacheService(context);
		}
		return cache;
	}

	public void asyncTrimToCount(final int type, final int expected) {
		dhandler.post(new Runnable() {
			@Override
			public void run() {
				CacheService c = cache;
				if (c instanceof ImageCacheService) {
					int r = ((ImageCacheService) c).trimToCount(type, expected);
					Log.i(TAG, "trim image cache, type=" + type + ", deleted="
							+ r);
				}
			}
		});
	}

	private synchronized HttpService http() {
		if (http == null) {
			http = new ImageHttpService(context, poolSize);
		}
		return http;
	}

	public synchronized void close() {
		if (cache != null) {
			cache.close();
		}
		if (http != null) {
			http.close();
		}
	}

	/**
	 * 用来对图片进行decode操作的异步任务
	 */
	private final Handler dhandler = new Handler(createLooper("decode")) {
		@Override
		public void handleMessage(Message msg) {
			Session s = (Session) msg.obj;
			byte[] bytes = null;
			Bitmap bmp = null;
			try {
				bytes = (byte[]) s.response.result();
				bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			} catch (Throwable t) {
				if (Log.isLoggable(Log.ERROR)) {
					Log.e(TAG, "unable to decode image " + s.request.url());
				}
			}
			if (s.status == 3) {
				s.response = bmp == null ? new BasicResponse(null,
						"fail to decode bitmap") : new BasicResponse(bmp, null);
				s.writeToCache = bytes;
				s.status = 4;
				mhandler.sendMessage(mhandler.obtainMessage(
						bmp == null ? 3 : 2, s));
			}

			if (bmp != null) {
				if (msg.what == 1) {
					// from cache, touch cache
					chandler.sendMessageDelayed(
							chandler.obtainMessage(msg.what, s), 600);
				} else {
					// from http, save to cache
					chandler.sendMessage(chandler.obtainMessage(msg.what, s));
				}
			}
		}
	};

	/**
	 * 用来对图片进行缓存相关操作的异步任务
	 */
	private final Handler chandler = new Handler(Daemon.looper()) {
		@Override
		public void handleMessage(Message msg) {
			Session s = (Session) msg.obj;

			try {
				if (s.status != 4) {
					return;
				}
				Request req = s.request;
				if (msg.what == 1) {
					// from cache, touch cache
					cache().touch(req, System.currentTimeMillis());
				}
				if (msg.what == 2 && s.writeToCache != null) {
					// from http, save to cache
					byte[] bytes = s.writeToCache;
					cache().put(req, bytes, System.currentTimeMillis());
				}
			} catch (Exception e) {
				Log.e(TAG, "unable to write image cache", e);
			}
		}
	};

	/**
	 * 回调处理，在主线程
	 */
	private final Handler mhandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			Session s = (Session) msg.obj;
			s = runningSession.remove(s.request);
			if (s == null || s.status != 4) {
				// canceled
				return;
			}
			switch (msg.what) {
			case 2:
				// finished
				s.handler.onRequestFinish(s.request, s.response);
				break;
			case 3:
				// failed
				s.handler.onRequestFailed(s.request, s.response);
				break;
			}
		}
	};

	private final RequestHandler<Request, CacheResponse> cacheHandler = new RequestHandler<Request, CacheResponse>() {
		@Override
		public void onRequestStart(Request req) {
		}

		@Override
		public void onRequestProgress(Request req, int count, int total) {
		}

		@Override
		public void onRequestFinish(Request req, CacheResponse response) {
			Session session = runningSession.get(req);
			if (session == null || session.status != 1) {
				// canceled
				return;
			}
			// decode the byte[] in daemon thread
			session.response = response;
			session.status = 3;
			dhandler.sendMessage(dhandler.obtainMessage(1, session));
		}

		@Override
		public void onRequestFailed(Request req, CacheResponse response) {
			Session session = runningSession.get(req);
			if (session == null || session.status != 1) {
				// canceled
				return;
			}
			boolean cacheOnly = req instanceof ImageRequest ? ((ImageRequest) req)
					.cacheOnly() : false;
			if (cacheOnly) {
				runningSession.remove(req, session);
				session.handler.onRequestFailed(req, new BasicResponse(null,
						"cache only"));
			} else {
				session.status = 2;
				http().exec((HttpRequest) req, httpHandler);
			}
		}
	};

	private final RequestHandler<HttpRequest, HttpResponse> httpHandler = new RequestHandler<HttpRequest, HttpResponse>() {
		@Override
		public void onRequestStart(HttpRequest req) {
		}

		@Override
		public void onRequestProgress(HttpRequest req, int count, int total) {
			if (req instanceof ImageRequest
					&& ((ImageRequest) req).type() == ImageRequest.TYPE_PHOTO) {
				Session session = runningSession.get(req);
				if (session == null || session.status != 2) {
					// canceled
					return;
				}
				session.handler.onRequestProgress(req, count, total);
			}
		}

		@Override
		public void onRequestFinish(HttpRequest req, HttpResponse response) {
			Session session = runningSession.get(req);
			if (session == null || session.status != 2) {
				// canceled
				return;
			}
			if (response.statusCode() / 100 == 2) {
				// decode the byte[] in daemon thread
				session.response = response;
				session.status = 3;
				dhandler.sendMessage(dhandler.obtainMessage(2, session));
			} else {
				runningSession.remove(req, session);
				session.handler.onRequestFailed(req, response);
			}
		}

		@Override
		public void onRequestFailed(HttpRequest req, HttpResponse response) {
			Session session = runningSession.remove(req);
			if (session == null || session.status != 2) {
				// canceled
				return;
			}
			session.handler.onRequestFailed(req, response);
		}
	};

	@Override
	public void exec(Request req, RequestHandler<Request, Response> handler) {
		if (!(req instanceof HttpRequest)
				|| !BasicHttpRequest.GET.equals(((HttpRequest) req).method())) {
			throw new IllegalArgumentException(
					"request must be a GET http request");
		}
		handler.onRequestStart(req);
		Session session = new Session(req, handler);
		Session prevs = runningSession.putIfAbsent(req, session);
		if (prevs == null) {
			session.status = 1;
			cache().exec(req, cacheHandler);
		} else {
			Log.e(TAG, "cannot exec duplicate request (same instance)");
		}
	}

	@Override
	public Response execSync(Request req) {
		if (!(req instanceof HttpRequest)
				|| !BasicHttpRequest.GET.equals(((HttpRequest) req).method())) {
			throw new IllegalArgumentException(
					"request must be a GET http request");
		}
		boolean cacheOnly = false;
		if (req instanceof ImageRequest) {
			ImageRequest ir = (ImageRequest) req;
			cacheOnly = ir.cacheOnly();
		}
		{
			CacheResponse resp = cache().execSync(req);
			if (resp.result() instanceof byte[]) {
				byte[] bytes = (byte[]) resp.result();
				try {
					Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0,
							bytes.length);
					return bmp == null ? new BasicResponse(null,
							"fail to decode bitmap") : new BasicResponse(bmp,
							null);
				} catch (Throwable t) {
					return new BasicResponse(null, t);
				}
			}
		}
		if (cacheOnly) {
			return new BasicResponse(null, "cache only");
		}
		HttpResponse resp = http().execSync((HttpRequest) req);
		if (resp.result() instanceof byte[] && resp.statusCode() / 100 == 2) {
			byte[] bytes = (byte[]) resp.result();
			try {
				Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0,
						bytes.length);
				if (bmp != null) {
					cache().put(req, bytes, System.currentTimeMillis());
				}
				return bmp == null ? new BasicResponse(null,
						"fail to decode bitmap") : new BasicResponse(bmp, null);
			} catch (Throwable t) {
				return new BasicResponse(null, t);
			}
		} else {
			return resp;
		}
	}

	@Override
	public void abort(Request req, RequestHandler<Request, Response> handler,
			boolean mayInterruptIfRunning) {
		if (!(req instanceof HttpRequest)
				|| !BasicHttpRequest.GET.equals(((HttpRequest) req).method())) {
			throw new IllegalArgumentException(
					"request must be a GET http request");
		}
		Session session = runningSession.get(req);
		if (session != null && session.handler == handler) {
			runningSession.remove(req, session);
			if (session.status == 2) {
				http().abort((HttpRequest) req, httpHandler, true);
			}
			session.status = 0;
		}
	}

	private class ImageHttpService extends DefaultHttpService {
		public ImageHttpService(Context context, int poolSize) {
			super(context, new ThreadPoolExecutor(poolSize, poolSize,
					Integer.MAX_VALUE, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>()));
		}

		@Override
		protected HttpClient createHttpClient() {
			return new ImageHttpClient();
		}

		@Override
		protected boolean isLoggable() {
			return false;
		}

		@Override
		protected void log(String str) {
		}
	}

	private HttpParams getDefaultHttpParams() {
		BasicHttpParams params = new BasicHttpParams();

		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpConnectionParams.setConnectionTimeout(params, 15000);
		HttpConnectionParams.setSoTimeout(params, 15000);
		return params;
	}

	private class ImageHttpClient extends DefaultHttpClient {
		public ImageHttpClient() {
			super(getDefaultHttpParams());
		}
	}

	private Looper createLooper(String threadName) {
		final BlockingItem<Looper> bl = new BlockingItem<Looper>();
		new Thread(threadName) {
			@Override
			public void run() {
				Looper.prepare();
				Looper l = Looper.myLooper();
				bl.put(l);
				Looper.loop();
			}
		}.start();
		try {
			return bl.take();
		} catch (Exception e) {
			return Daemon.looper();
		}
	}
}
