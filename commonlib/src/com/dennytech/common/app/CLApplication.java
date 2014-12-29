package com.dennytech.common.app;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.dennytech.common.loader.LoaderActivity;
import com.dennytech.common.loader.MappingManager;
import com.dennytech.common.loader.bean.MappingSpec;
import com.dennytech.common.loader.bean.PageSpec;
import com.dennytech.common.service.configservice.ConfigService;
import com.dennytech.common.service.dataservice.cache.CacheService;
import com.dennytech.common.service.dataservice.http.HttpService;
import com.dennytech.common.service.dataservice.image.ImageService;
import com.dennytech.common.service.dataservice.mapi.MApiService;
import com.dennytech.common.util.DateUtil;
import com.dennytech.common.util.Log;

public class CLApplication extends Application {

	private static CLApplication instance;
	private ServiceManager services;
	private String sessionId;

	public static CLApplication instance() {
		if (instance == null) {
			throw new IllegalStateException("Application has not been created");
		}

		return instance;
	}

	static CLApplication _instance() {
		return instance;
	}

	public CLApplication() {
		instance = this;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		if ((getApplicationInfo().flags & 2) != 0) {
			// ApplicationInfo.FLAG_DEBUGGABLE
			Log.LEVEL = Log.VERBOSE;
		} else {
			Log.LEVEL = Integer.MAX_VALUE;
		}

	}

	// **** Application Life Circle **** //

	/**
	 * Application的Activity堆栈第一次不为空时调用
	 * <p>
	 * 在第一个Activity.onCreate()调用之前被调用
	 */
	public void onApplicationStart() {
		Log.i("application", "onApplicationStart");

		sessionId = UUID.randomUUID().toString();
	}

	/**
	 * Application从后台唤醒到前台时调用
	 * <p>
	 * onApplicationStart<br>
	 * |-onApplicationResume<br>
	 * |-onApplicationPause<br>
	 * onApplicationStop
	 */
	public void onApplicationResume() {
		Log.i("application", "onApplicationResume");

//		DateUtil.refreshServerTime(mapiService());
	}

	/**
	 * Application从前台置为后台时调用，比如按Home键
	 * <p>
	 * onApplicationStart<br>
	 * |-onApplicationResume<br>
	 * |-onApplicationPause<br>
	 * onApplicationStop
	 */
	public void onApplicationPause() {
		Log.i("application", "onApplicationPause");
	}

	/**
	 * Application的Activity堆栈第一次为空时调用
	 * <p>
	 * 在最后一个Activity.onDestory()调用之后被调用 <br>
	 * 按Home键返回或被其他应用覆盖不会触发<br>
	 * 只有一直按Back退出才会触发
	 */
	public void onApplicationStop() {
		Log.i("application", "onApplicationStop");

		sessionId = null;

		if (services != null) {
			services.stop();
		}
	}

	private static int liveCounter;
	private static int activeCounter;
	private static Handler handler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				if ((--liveCounter) == 0) {
					CLApplication.instance().onApplicationStop();
				}
			}
			if (msg.what == 2) {
				// skip a event loop in case onPause or onDestory takes too long
				sendEmptyMessageDelayed(3, 100);
			}
			if (msg.what == 3) {
				if ((--activeCounter) == 0) {
					CLApplication.instance().onApplicationPause();
				}
			}
		}
	};

	public void activityOnCreate(Activity a) {
		if (liveCounter++ == 0) {
			onApplicationStart();
		}
	}

	public void activityOnResume(Activity a) {
		if (activeCounter++ == 0) {
			onApplicationResume();
		}
	}

	public void activityOnPause(Activity a) {
		handler.sendEmptyMessage(2);
	}

	public void activityOnDestory(Activity a) {
		handler.sendEmptyMessage(1);
	}

	// **** Application Life Circle END **** //

	public String sessionId() {
		return sessionId;
	}

	public Object getService(String name) {
		if (services == null) {
			services = createServiceManager();
		}
		return services.getService(name);
	}

	protected ServiceManager createServiceManager() {
		return new ServiceManager(this);
	}

	private HttpService httpService;

	public HttpService httpService() {
		if (httpService == null) {
			httpService = (HttpService) getService("http");
		}
		return httpService;
	}

	private ImageService imageService;

	public ImageService imageService() {
		if (imageService == null) {
			imageService = (ImageService) getService("image");
		}
		return imageService;
	}

	private ConfigService configService;

	public ConfigService configService() {
		if (configService == null) {
			configService = (ConfigService) getService("config");
		}
		return configService;
	}

	private CacheService mapiCacheService;

	public CacheService mapiCacheService() {
		if (mapiCacheService == null) {
			mapiCacheService = (CacheService) getService("mapi_cache");
		}
		return mapiCacheService;
	}

	private MApiService mapiService;

	public MApiService mapiService() {
		if (mapiService == null) {
			mapiService = (MApiService) getService("mapi");
		}
		return mapiService;
	}

	// ********* URL Mapping ********** //

	public static final String PRIMARY_SCHEME = "baidulife";

	public Intent urlMap(Intent intent) {
		do {
			Uri uri = intent.getData();
			if (uri == null) {
				break;
			}
			if (uri.getScheme() == null
					|| !PRIMARY_SCHEME.equals(uri.getScheme())) {
				break;
			}

			MappingManager mManager = mappingManager();
			if (mManager == null) {
				break;
			}

			MappingSpec mSpec = mManager.mappingSpec();
			if (mSpec == null) {
				break;
			}

			String host = uri.getHost();
			if (TextUtils.isEmpty(host))
				break;
			host = host.toLowerCase();

			PageSpec page = mSpec.getPage(host);
			if (page == null) {
				Log.w("loader", "host (" + host
						+ ") Can't find the page in mapping.");
				break;
			}
			Class<?> fragment = page.fragment;

			intent.putExtra("_login", page.login);

			Class<?> defaultLoader = mSpec.loader;
			Class<?> loader = null;
			if (page.activity != null) {
				loader = page.activity;

			} else if (defaultLoader != null) {
				loader = defaultLoader;
			}

			if (loader != null) {
				intent.setClass(this, loader);

			} else {
				intent.setClass(this, LoaderActivity.class);
			}

			String query = uri.getQuery();

			uri = Uri.parse(String.format("%s://%s?%s#%s", uri.getScheme(),
					host, query, fragment == null ? "" : fragment.getName()));
			intent.setData(uri);

		} while (false);

		return intent;
	}

	private MappingManager mappingManager;

	public MappingManager mappingManager() {
		if (mappingManager == null) {
			mappingManager = createMappingManager();
		}
		return mappingManager;
	}

	protected MappingManager createMappingManager() {
		return new MappingManager(this);
	}

	public Set<String> getQueryParameterNames(String query) {
		if (query == null) {
			return Collections.emptySet();
		}

		Set<String> names = new LinkedHashSet<String>();
		int start = 0;
		do {
			int next = query.indexOf('&', start);
			int end = (next == -1) ? query.length() : next;

			int separator = query.indexOf('=', start);
			if (separator > end || separator == -1) {
				separator = end;
			}

			String name = query.substring(start, separator);
			names.add(Uri.decode(name));

			// Move start to end of name.
			start = end + 1;
		} while (start < query.length());

		return Collections.unmodifiableSet(names);
	}

}
