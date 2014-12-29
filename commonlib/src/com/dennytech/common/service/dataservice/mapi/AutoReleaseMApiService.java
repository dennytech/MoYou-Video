package com.dennytech.common.service.dataservice.mapi;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import com.dennytech.common.service.dataservice.RequestHandler;
import com.dennytech.common.util.Log;

public class AutoReleaseMApiService implements MApiService, MApiRequestHandler {
	private Object host;
	private MApiService service;
	private ConcurrentHashMap<MApiRequest, RequestHandler<MApiRequest, MApiResponse>> running;

	public AutoReleaseMApiService(Object host, MApiService service) {
		this.host = host;
		this.service = service;
		running = new ConcurrentHashMap<MApiRequest, RequestHandler<MApiRequest, MApiResponse>>();
	}

	public void onDestory() {
		for (MApiRequest req : running.keySet()) {
			service.abort(req, this, true);
			Log.i("mapi_seal", "Abort leak request " + req);
			Log.w("mapi_seal", "You must abort the request:" + req
					+ " before you destory the host!!!!!!");
		}
	}

	protected boolean isOwnByHost(Object obj, int searchDepth) {
		if (obj == host)
			return true;
		try {
			while ((searchDepth--) > 0) {
				Field f = obj.getClass().getDeclaredField("this$0");
				f.setAccessible(true);
				obj = f.get(obj);
				if (obj == host)
					return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	@Override
	public void exec(MApiRequest req,
			RequestHandler<MApiRequest, MApiResponse> handler) {
		// 有可能是内部类handler
		if (isOwnByHost(handler, 2)) {
			running.put(req, handler);
			service.exec(req, this);
		} else {
			service.exec(req, handler);
		}
	}

	@Override
	public MApiResponse execSync(MApiRequest req) {
		return service.execSync(req);
	}

	@Override
	public void abort(MApiRequest req,
			RequestHandler<MApiRequest, MApiResponse> handler,
			boolean mayInterruptIfRunning) {
		if (running.remove(req, handler)) {
			service.abort(req, this, mayInterruptIfRunning);
		} else {
			service.abort(req, handler, mayInterruptIfRunning);
		}
	}

	@Override
	public void onRequestStart(MApiRequest req) {
		RequestHandler<MApiRequest, MApiResponse> handler = running.get(req);
		if (handler != null) {
			handler.onRequestStart(req);
		} else {
			Log.w("mapi_seal", "Sealed leak on " + req);
		}
	}

	@Override
	public void onRequestProgress(MApiRequest req, int count, int total) {
		RequestHandler<MApiRequest, MApiResponse> handler = running.get(req);
		if (handler != null) {
			handler.onRequestProgress(req, count, total);
		} else {
			Log.w("mapi_seal", "Sealed leak on " + req);
		}
	}

	@Override
	public void onRequestFinish(MApiRequest req, MApiResponse resp) {
		RequestHandler<MApiRequest, MApiResponse> handler;
		if (req.defaultCacheType() == CacheType.RIVAL && resp.isCache()) {
			handler = running.get(req);
		} else {
			handler = running.remove(req);
		}

		if (handler != null) {
			handler.onRequestFinish(req, resp);
		} else {
			Log.w("mapi_seal", "Sealed leak on " + req);
		}
	}

	@Override
	public void onRequestFailed(MApiRequest req, MApiResponse resp) {
		RequestHandler<MApiRequest, MApiResponse> handler = running.remove(req);
		if (handler != null) {
			handler.onRequestFailed(req, resp);
		} else {
			Log.w("mapi_seal", "Sealed leak on " + req);
		}
	}
}
