package com.dennytech.common.service.dataservice.mapi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpException;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;

import com.dennytech.common.service.dataservice.Request;
import com.dennytech.common.service.dataservice.RequestHandler;
import com.dennytech.common.service.dataservice.cache.CacheResponse;
import com.dennytech.common.service.dataservice.cache.CacheService;
import com.dennytech.common.service.dataservice.http.FormInputStream;
import com.dennytech.common.service.dataservice.http.HttpRequest;
import com.dennytech.common.service.dataservice.http.HttpResponse;
import com.dennytech.common.service.dataservice.http.HttpService;
import com.dennytech.common.service.dataservice.http.impl.BasicHttpRequest;
import com.dennytech.common.service.dataservice.http.impl.BasicHttpResponse;
import com.dennytech.common.service.dataservice.http.impl.DefaultHttpService;
import com.dennytech.common.service.dataservice.mapi.CacheType;
import com.dennytech.common.service.dataservice.mapi.MApiFormInputStream;
import com.dennytech.common.service.dataservice.mapi.MApiMsg;
import com.dennytech.common.service.dataservice.mapi.MApiRequest;
import com.dennytech.common.service.dataservice.mapi.MApiRequestHandler;
import com.dennytech.common.service.dataservice.mapi.MApiResponse;
import com.dennytech.common.service.dataservice.mapi.MApiService;
import com.dennytech.common.util.BlockingItem;
import com.dennytech.common.util.Daemon;
import com.dennytech.common.util.IntegerAdapter;
import com.dennytech.common.util.Log;
import com.dennytech.common.util.LongAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 默认的MApi请求服务，支持各种缓存类型
 * <p>
 * http协议，域名为promo.lbc.baidu.com
 * 
 * @author Jun.Deng
 * 
 */
public class DefaultMApiService implements MApiService {

	private static final String TAG = "mapi";

	private Context context;
	private String userAgent;
	private MApiCacheService cache;
	private DefaultHttpService http;
	private final ConcurrentHashMap<MApiRequest, Session> runningSession = new ConcurrentHashMap<MApiRequest, Session>();

	private static class Session {
		public MApiRequest request;
		public RequestHandler<MApiRequest, MApiResponse> handler;
		/**
		 * 0=idle or canceled<br>
		 * 1=cache, 2=http, 3=cacheBack
		 */
		public int status;
		/**
		 * <0表示开始时间, >0表示请求执行花费时间。
		 */
		public long time;
		public CacheResponse cacheResponse;
		public HttpResponse httpResponse;
		public byte[] writeToCache;
		/**
		 * 为true代表该session还需要经过二次处理，还不能被回收掉<br>
		 * PERSISTENT、RIVAL类型缓存会使用到。
		 */
		public boolean doNotReturn;

		public Session(MApiRequest r,
				RequestHandler<MApiRequest, MApiResponse> h) {
			request = r;
			handler = h;
		}
	}

	public DefaultMApiService(Context context, String userAgent) {
		this.context = context;
		this.userAgent = userAgent;
	}

	public synchronized CacheService cache() {
		if (cache == null) {
			cache = new MApiCacheService(context);
		}
		return cache;
	}

	/**
	 * 异步裁剪Mapi缓存到期望个数
	 * 
	 * @param expected
	 *            期望个数
	 */
	public void asyncTrimToCount(final int expected) {
		new Handler(Daemon.looper()).post(new Runnable() {
			@Override
			public void run() {
				CacheService c = cache;
				if (c instanceof MApiCacheService) {
					int r = ((MApiCacheService) c).trimToCount(expected);
					Log.i(TAG, "trim mapi cache, deleted=" + r);
				}
			}
		});
	}

	public synchronized void close() {
		if (cache != null) {
			cache.close();
		}
	}

	private synchronized HttpService http() {
		if (http == null) {
			http = new MApiHttpService(context);
		}
		return http;
	}

	@Override
	public void exec(MApiRequest req,
			RequestHandler<MApiRequest, MApiResponse> handler) {
		handler.onRequestStart(req);
		if (req.defaultCacheType() == CacheType.NORMAL
				|| req.defaultCacheType() == CacheType.RIVAL
				|| req.defaultCacheType() == CacheType.PERSISTENT) {
			Session session = new Session(req, handler);
			Session prevs = runningSession.putIfAbsent(req, session);
			if (prevs == null) {
				session.status = 1;
				cache().exec(req, cacheHandler);
			} else {
				Log.e("mapi", "cannot exec duplicate request (same instance)");
			}

		} else {
			Session session = new Session(req, handler);
			Session prevs = runningSession.putIfAbsent(req, session);
			if (prevs == null) {
				session.status = 2;
				http().exec(req, httpHandler);
			} else {
				Log.e("mapi", "cannot exec duplicate request (same instance)");
			}
		}

	}

	@Override
	public MApiResponse execSync(MApiRequest req) {
		Log.w(TAG,
				"MApiService.execSync() is a temporary solution, use it as your own risk (TIP: do not try to abort sync request)");
		final BlockingItem<MApiResponse> item = new BlockingItem<MApiResponse>();
		exec(req, new MApiRequestHandler() {
			@Override
			public void onRequestStart(MApiRequest req) {
			}

			@Override
			public void onRequestProgress(MApiRequest req, int count, int total) {
			}

			@Override
			public void onRequestFinish(MApiRequest req, MApiResponse resp) {
				item.put(resp);
			}

			@Override
			public void onRequestFailed(MApiRequest req, MApiResponse resp) {
				item.put(resp);
			}
		});
		try {
			MApiResponse resp = item.take();
			return resp;
		} catch (Exception e) {
			return new BasicMApiResponse(0, null, null,
					Collections.<NameValuePair> emptyList(), e, false);
		}
	}

	@Override
	public void abort(MApiRequest req,
			RequestHandler<MApiRequest, MApiResponse> handler,
			boolean mayInterruptIfRunning) {
		Session session = runningSession.get(req);
		if (session != null && session.handler == handler) {
			runningSession.remove(req, session);
			if (session.status == 2) {
				http().abort((HttpRequest) req, httpHandler, true);
			} else if (session.status == 1) {
				if (Log.isLoggable(Log.DEBUG)) {
					Log.d(TAG, "abort (cache." + req.defaultCacheType() + ") "
							+ req.url());
				}
			}
			session.status = 0;
		}
	}

	/**
	 * 创建回传给业务层使用的数据格式。
	 * <p>
	 * 这里回传了String，业务层采用gson创建具体bean<br>
	 * 例：gson.fromJson(resp.result(), xxx.class);
	 * 
	 * @param bytes
	 * @return
	 * @throws Exception
	 */
	private Object getResult(byte[] bytes, Class<?> clazz) throws Exception {
		String jsonStr = new String(bytes);
		if (clazz != null) {
			return new Gson().fromJson(jsonStr, clazz);
		}
		return new String(bytes);
	}

	/**
	 * 缓存请求回来的数据（异步）
	 */
	private final Handler dhandler = new Handler(Daemon.looper()) {

		public void handleMessage(android.os.Message msg) {
			Session s = (Session) msg.obj;
			if (s.writeToCache != null) {
				cache().put(s.request, s.writeToCache,
						System.currentTimeMillis());

				// 翻页请求的缓存需要注意，在缓存新的当前页数据的时候，
				// 我们需要把老的下一页的数据清除掉，因为它已经失效了。
				if ((s.request.defaultCacheType() == CacheType.NORMAL || s.request
						.defaultCacheType() == CacheType.RIVAL)
						&& s.writeToCache.length > 0) {

					String cacheStr = new String(s.writeToCache);
					Uri reqUri = Uri.parse(s.request.url());

					// 服务端协议存在两种写法，且对该字段大小写敏感，因此需要都检查。
					String param = "startindex";
					String startIndexStr = reqUri.getQueryParameter(param);

					if (startIndexStr == null) {
						param = "startIndex";
						startIndexStr = reqUri.getQueryParameter(param);
					}

					if (startIndexStr != null) {
						try {
							int startIndex = Integer.parseInt(startIndexStr);
							JsonParser jp = new JsonParser();
							JsonObject cacheObj = (JsonObject) jp
									.parse(cacheStr);
							int nextStartIndex = cacheObj.get("startindex")
									.getAsInt();

							if (nextStartIndex > startIndex) {
								String url = s.request.url();
								String oldSW = param + "=" + startIndex;
								String newSW = param + "=" + nextStartIndex;
								if (url.endsWith(oldSW)) {
									String newUrl = url.replaceFirst(oldSW,
											newSW);
									cache.remove(BasicMApiRequest.mapiGet(
											newUrl, CacheType.NORMAL, null));
								} else if (url.contains(oldSW + "&")) {
									String newUrl = url.replaceFirst(oldSW
											+ "&", newSW + "&");
									cache.remove(BasicMApiRequest.mapiGet(
											newUrl, CacheType.NORMAL, null));
								}
							}

						} catch (Exception e) {
							Log.w(TAG, "clear invalid page list cache failed",
									e);
						}
					}
				}
			}

		};

	};

	/**
	 * 缓存数据读取回调
	 */
	private final RequestHandler<Request, CacheResponse> cacheHandler = new RequestHandler<Request, CacheResponse>() {

		@Override
		public void onRequestStart(Request req) {
		}

		@Override
		public void onRequestProgress(Request req, int count, int total) {
		}

		@Override
		public void onRequestFinish(Request req, CacheResponse resp) {
			Session session = runningSession.get(req);
			if (session == null
					|| !(session.status == 1 || session.status == 3)) {
				// canceled
				return;
			}
			session.cacheResponse = resp;
			MApiRequest mreq = (MApiRequest) req;
			MApiResponse r;
			try {
				byte[] bytes = (byte[]) resp.result();
				if (session.status == 1
						&& mreq.defaultCacheType() == CacheType.NORMAL) {
					long dt = System.currentTimeMillis() - resp.time();
					if (dt < 0 || dt > 300000) { // 5 minutes
						session.status = 2;
						http().exec(mreq, httpHandler);
						if (Log.isLoggable(Log.DEBUG)) {
							Log.d(TAG, "expired (cache.NORMAL) " + req.url());
						}
						return;
					}
				}
				if (session.status == 1
						&& mreq.defaultCacheType() == CacheType.PERSISTENT) {
					long dt = System.currentTimeMillis() - resp.time();
					if (dt < 0 || dt > 43200000) { // 12 hours
						session.status = 2;
						http().exec(mreq, httpHandler);
						if (Log.isLoggable(Log.DEBUG)) {
							Log.d(TAG,
									"refresh (cache.PERSISTENT) " + req.url());
						}
						session.doNotReturn = true;
					}
				}

				// RIVAL缓存策略的处理
				if (session.status == 1
						&& mreq.defaultCacheType() == CacheType.RIVAL) {
					long dt = System.currentTimeMillis() - resp.time();
					if (dt < 0 || dt > 300000) {
						session.status = 2;
						http().exec(mreq, httpHandler);
						if (Log.isLoggable(Log.DEBUG)) {
							Log.d(TAG, "expired (cache.RIVAL) " + req.url());
						}
						return;
					}
				}

				Object result = getResult(bytes, mreq.resultClazz());
				r = new BasicMApiResponse(0, bytes, result,
						Collections.<NameValuePair> emptyList(), null, true);
			} catch (Exception e) {
				Log.e("mapi", "exception when processing cached data, ignored",
						e);
				if (session.status == 1) {
					session.status = 2;
					http().exec(mreq, httpHandler);
				}
				return;
			}

			if (session.status == 1
					&& mreq.defaultCacheType() == CacheType.RIVAL) {
				session.status = 2;
				http().exec(mreq, httpHandler);
				if (Log.isLoggable(Log.DEBUG)) {
					Log.d(TAG, "continue (cache.RIVAL) " + req.url());
				}
				session.doNotReturn = true;
			}

			if (!session.doNotReturn) {
				runningSession.remove(req, session);
			}

			session.handler.onRequestFinish(mreq, r);
			if (Log.isLoggable(Log.DEBUG)) {
				Log.d(TAG, "finish (cache." + mreq.defaultCacheType() + ") "
						+ req.url());
			}
		}

		@Override
		public void onRequestFailed(Request req, CacheResponse resp) {
			Session session = runningSession.get(req);
			if (session == null) {
				// canceled
				return;
			}

			session.cacheResponse = resp;
			if (session.status == 1) {
				session.status = 2;
				http().exec((MApiRequest) req, httpHandler);

			} else if (session.status == 3) {
				MApiResponse r = new BasicMApiResponse(
						session.httpResponse.statusCode(), null, null,
						session.httpResponse.headers(),
						session.httpResponse.error(), true);
				session.handler.onRequestFailed((MApiRequest) req, r);

				if (Log.isLoggable(Log.DEBUG)) {
					Log.d(TAG, "fail (cache.CRITICAL) " + req.url());
				}
			}
		}

	};

	/**
	 * 网络数据读取回调
	 */
	private final RequestHandler<HttpRequest, HttpResponse> httpHandler = new RequestHandler<HttpRequest, HttpResponse>() {

		@Override
		public void onRequestStart(HttpRequest req) {
			Session session = runningSession.get(req);
			if (session == null || session.status != 2 || session.doNotReturn) {
				// canceled
				return;
			}
			session.time = -SystemClock.elapsedRealtime();
		}

		@Override
		public void onRequestProgress(HttpRequest req, int count, int total) {
			Session session = runningSession.get(req);
			if (session == null || session.status != 2 || session.doNotReturn) {
				// canceled
				return;
			}
			session.handler.onRequestProgress((MApiRequest) req, count, total);
		}

		@Override
		public void onRequestFinish(HttpRequest req, HttpResponse resp) {
			Session session = runningSession.get(req);
			if (session == null || session.status != 2) {
				// canceled
				return;
			}
			MApiResponse failResponse = null;
			MApiResponse successResponse = null;
			byte[] successResponse_bytes = null;
			boolean malformed = false;

			session.httpResponse = resp;
			if (session.time < 0) {
				session.time += SystemClock.elapsedRealtime();
			}
			MApiRequest mreq = (MApiRequest) req;
			if (resp.statusCode() / 100 == 2) {
				try {
					Object error = null;
					byte[] rawData;
					Object result;
					// 如果指定了返回bean类型，在http请求的异步操作中即完成了解析工作，并封装成了MApiResponse返回
					if (mreq.resultClazz() == null) {
						rawData = (byte[]) resp.result();
						result = getResult((byte[]) resp.result(), mreq.resultClazz());
						
					} else {
						if (resp instanceof MApiResponse) {
							result = resp.result();
							error = resp.error();
							rawData = ((MApiResponse) resp).rawData();

						} else {
							int errNo = 0;
							String errMsg = null;
							JsonParser jp = new JsonParser();
							JsonObject jsonObj = (JsonObject) jp
									.parse(new String((byte[]) resp.result()));
							if (jsonObj.has("code")) {
								errNo = jsonObj.get("code").getAsInt();
							}
							if (jsonObj.has("message")) {
								errMsg = jsonObj.get("message").getAsString();
							}
							if (errNo != 0) {
								error = new MApiMsg(errNo, errMsg);
							}
							rawData = (byte[]) resp.result();
							result = getResult(rawData, mreq.resultClazz());
						}
					}

					if (error != null) {
						failResponse = new BasicMApiResponse(resp.statusCode(),
								rawData, null, resp.headers(), error, false);
					} else {
						successResponse = new BasicMApiResponse(
								resp.statusCode(), rawData, result,
								resp.headers(), null, false);
						successResponse_bytes = rawData;
					}

				} catch (Exception e) {
					Log.e(TAG, "malformed content", e);
					failResponse = new BasicMApiResponse(resp.statusCode(),
							null, null, resp.headers(),
							BasicMApiResponse.ERROR_MALFORMED, false);
					malformed = true;
					if (Log.isLoggable(Log.ERROR)) {
						StringBuilder sb = new StringBuilder();
						sb.append("malformed (");
						sb.append(req.method()).append(',');
						sb.append(failResponse.statusCode()).append(',');
						sb.append(session.time).append("ms");
						sb.append(") ").append(req.url());
						Log.d(TAG, sb.toString());

						if (req.input() instanceof FormInputStream) {
							FormInputStream form = (FormInputStream) req
									.input();
							Log.d(TAG, "    " + form.toString());
						}
					}
				}

			} else {
				failResponse = new BasicMApiResponse(resp.statusCode(), null,
						null, resp.headers(), BasicMApiResponse.ERROR_STATUS,
						false);
			}

			if (successResponse != null) {
				runningSession.remove(req, session);
				if (!session.doNotReturn
						|| mreq.defaultCacheType() == CacheType.RIVAL) {
					session.handler.onRequestFinish(mreq, successResponse);
				}

				session.writeToCache = successResponse_bytes;
				dhandler.sendMessage(dhandler.obtainMessage(1, session));

				// statistics will be finished in dhandler (getIp)

				if (Log.isLoggable(Log.DEBUG)) {
					StringBuilder sb = new StringBuilder();
					sb.append("finish (");
					sb.append(req.method()).append(',');
					sb.append(successResponse.statusCode()).append(',');
					sb.append(session.time).append("ms");
					sb.append(") ").append(req.url());
					Log.d(TAG, sb.toString());

					if (req.input() instanceof FormInputStream) {
						FormInputStream form = (FormInputStream) req.input();
						Log.d(TAG, "    " + form.toString());
					}
				}
				return;
			}

			if (session.doNotReturn) {
				runningSession.remove(req, session);
				return;
			}

			// expired cache data is also accepted when http fail
			if (mreq.defaultCacheType() == CacheType.NORMAL
					&& session.cacheResponse != null) {
				try {
					byte[] bytes = (byte[]) session.cacheResponse.result();
					Object obj = getResult(bytes, mreq.resultClazz());
					MApiResponse r = new BasicMApiResponse(0, bytes, obj,
							Collections.<NameValuePair> emptyList(), null, true);

					runningSession.remove(req, session);
					session.handler.onRequestFinish(mreq, r);

					if (Log.isLoggable(Log.DEBUG)) {
						Log.d(TAG, "finish (cache." + mreq.defaultCacheType()
								+ ") " + req.url());
						Log.d(TAG,
								"    expired cache is also accepted when http fail");
					}

					return;
				} catch (Exception e) {
				}
			}

			// critical cache
			if (mreq.defaultCacheType() == CacheType.CRITICAL) {
				session.status = 3;
				cache().exec(req, cacheHandler);
				return;
			}

			runningSession.remove(req, session);
			session.handler.onRequestFailed(mreq, failResponse);

			if (Log.isLoggable(Log.DEBUG) && !malformed) {
				StringBuilder sb = new StringBuilder();
				sb.append("fail (");
				sb.append(req.method()).append(',');
				sb.append(failResponse.statusCode()).append(',');
				sb.append(session.time).append("ms");
				sb.append(") ").append(req.url());
				Log.d(TAG, sb.toString());

				if (req.input() instanceof FormInputStream) {
					FormInputStream form = (FormInputStream) req.input();
					Log.d(TAG, "    " + form.toString());
				}

				Object err = failResponse.error();
				if (err instanceof String) {
					Log.d(TAG, "    " + err);
				}
			}

		}

		@Override
		public void onRequestFailed(HttpRequest req, HttpResponse resp) {
			Session session = runningSession.get(req);
			if (session == null || session.status != 2) {
				// canceled
				return;
			}

			if (session.doNotReturn) {
				runningSession.remove(req, session);
				return;
			}

			MApiRequest mreq = (MApiRequest) req;
			session.httpResponse = resp;

			// expired cache data is also accepted when http fail
			if (mreq.defaultCacheType() == CacheType.NORMAL
					&& session.cacheResponse != null) {
				MApiResponse r = null;
				try {
					byte[] bytes = (byte[]) session.cacheResponse.result();
					Object obj = getResult(bytes, mreq.resultClazz());
					r = new BasicMApiResponse(0, bytes, obj,
							Collections.<NameValuePair> emptyList(), null, true);
				} catch (Exception e) {
				}
				if (r != null) {
					runningSession.remove(req, session);
					session.handler.onRequestFinish(mreq, r);

					if (Log.isLoggable(Log.DEBUG)) {
						Log.d(TAG, "finish (cache." + mreq.defaultCacheType()
								+ ") " + req.url());
						Log.d(TAG,
								"    expired cache is also accepted when http fail");
					}
					return;
				}
			}

			// critical cache
			if (mreq.defaultCacheType() == CacheType.CRITICAL) {
				session.status = 3;
				cache().exec(req, cacheHandler);
				return;
			}

			MApiResponse r = new BasicMApiResponse(resp.statusCode(), null,
					null, resp.headers(), resp.error(), false);
			runningSession.remove(req, session);
			session.handler.onRequestFailed(mreq, r);

			if (Log.isLoggable(Log.DEBUG)) {
				StringBuilder sb = new StringBuilder();
				sb.append("fail (");
				sb.append(req.method()).append(',');
				sb.append(resp.statusCode()).append(',');
				sb.append(session.time).append("ms");
				sb.append(") ").append(req.url());
				Log.d(TAG, sb.toString());

				if (req.input() instanceof FormInputStream) {
					FormInputStream form = (FormInputStream) req.input();
					Log.d(TAG, "    " + form.toString());
				}

				Log.d(TAG, "    " + resp.error());
			}
		}

	};

	private class MApiHttpService extends DefaultHttpService {

		public MApiHttpService(Context context) {
			super(context, new ThreadPoolExecutor(3, 6, 30, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>()));
		}

		@Override
		protected HttpClient createHttpClient() {
			return new MapiHttpClient();
		}

		@Override
		protected Task createTask(HttpRequest req,
				RequestHandler<HttpRequest, HttpResponse> handler) {
			return new MyTask(req, handler);
		}

		@Override
		protected HttpParams getDefaultHttpParams() {
			BasicHttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setUserAgent(params, userAgent);
			HttpConnectionParams.setConnectionTimeout(params, 15000);
			HttpConnectionParams.setSoTimeout(params, 15000);
			return params;
		}

		private class MapiHttpClient extends MyHttpClient implements
				HttpRequestInterceptor {

			@Override
			public void process(org.apache.http.HttpRequest request,
					HttpContext context) throws HttpException, IOException {
				super.process(request, context);
				if (!request.containsHeader("pragma-os")) {
					request.addHeader("pragma-os", userAgent);
				}
			}

		}

		private class MyTask extends Task {

			public MyTask(HttpRequest req,
					RequestHandler<HttpRequest, HttpResponse> handler) {
				super(req, handler);
			}

			@Override
			public HttpResponse doInBackground(Void... params) {
				HttpResponse resp = super.doInBackground(params);
				if (resp.error() != null) {
					return resp;
				}

				if (resp.statusCode() / 100 == 2
						&& ((MApiRequest) req).resultClazz() != null) {
					try {
						JsonParser parser = new JsonParser();
						JsonElement element = parser.parse(new String(
								(byte[]) resp.result()));
						JsonObject object = element.getAsJsonObject();
						int errorNo = 0;
						String errorMsg = null;
						if (object.has("code")) {
							errorNo = object.get("code").getAsInt();
						}
						if (object.has("message")) {
							errorMsg = object.get("message").getAsString();
						}

						IntegerAdapter integerAdapter = new IntegerAdapter();
						LongAdapter longAdapter = new LongAdapter();
						Gson gson = new GsonBuilder()
								.registerTypeAdapter(int.class, integerAdapter)
								.registerTypeAdapter(Integer.class,
										integerAdapter)
								.registerTypeAdapter(long.class, longAdapter)
								.registerTypeAdapter(Long.class, longAdapter)
								.create();

						Object result = gson.fromJson(element,
								((MApiRequest) req).resultClazz());
						return new BasicMApiResponse(statusCode,
								(byte[]) resp.result(), result, resp.headers(),
								errorNo == 0 ? null : new MApiMsg(errorNo,
										errorMsg), false);
					} catch (Exception e) {
						Log.e(TAG, "deserialize failed", e);
						return new BasicHttpResponse(0, null, null, e);
					}
				}

				return resp;
			}

			@Override
			protected HttpUriRequest getUriRequest() throws Exception {
				HttpUriRequest request = null;
				if (BasicHttpRequest.GET.equals(req.method())) {
					request = new HttpGet(urlAppendBasicParams(req.url()));

				} else if (BasicHttpRequest.POST.equals(req.method())) {
					HttpPost post = new HttpPost(req.url());
					InputStream ins = req.input();
					if (ins instanceof MApiFormInputStream) {
						post.setEntity(new UrlEncodedFormEntity(
								((MApiFormInputStream) ins).form(), "UTF-8"));
						request = post;
					}

				} else if (BasicHttpRequest.PUT.equals(req.method())) {
					throw new IllegalArgumentException(
							"unsupported http method " + req.method());
				} else if (BasicHttpRequest.DELETE.equals(req.method())) {
					throw new IllegalArgumentException(
							"unsupported http method " + req.method());
				}

				if (request == null) {
					request = super.getUriRequest();
				}

				HttpUriRequest transferReq = transferUriRequest(request);

				if (Log.isLoggable(Log.DEBUG)) {
					StringBuilder sb = new StringBuilder();
					sb.append("transfer (");
					sb.append(transferReq.getMethod());
					sb.append(") ").append(transferReq.getURI());
					Log.d(TAG, sb.toString());
				}

				return transferReq;
			}

		}

	}

	/**
	 * 调试入口。
	 * <p>
	 * 调试器可以对原始request进行修改达到切换api服务器目的
	 * 
	 * @param request
	 *            原始request
	 * @return 加工后的request
	 * @throws Exception
	 */
	protected HttpUriRequest transferUriRequest(HttpUriRequest request)
			throws Exception {
		return request;
	}

	private String urlAppendBasicParams(String url) throws Exception {
		Uri uri = Uri.parse(url);
		String query = uri.getQuery();
		String[] params = query.split("&");
		List<NameValuePair> forms = new ArrayList<NameValuePair>();
		for (String param : params) {
			String[] kv = param.split("=");
			if (kv.length != 2) {
				continue;
			}
			forms.add(new BasicNameValuePair(kv[0], kv[1]));
		}

		StringBuilder urlSb = new StringBuilder();
		int index = url.indexOf('?');
		if (index < 0) {
			urlSb.append(url).append("?");
		} else {
			urlSb.append(url.substring(0, index)).append("?");
		}

		urlSb.append(URLEncodedUtils.format(forms, HTTP.UTF_8));
		return urlSb.toString();
	}

}
