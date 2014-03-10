package com.dennytech.common.service.configservice.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.os.Looper;

import com.dennytech.common.service.configservice.ConfigChangeListener;
import com.dennytech.common.service.configservice.ConfigService;
import com.dennytech.common.service.dataservice.mapi.MApiRequest;
import com.dennytech.common.service.dataservice.mapi.MApiRequestHandler;
import com.dennytech.common.service.dataservice.mapi.MApiResponse;
import com.dennytech.common.service.dataservice.mapi.MApiService;
import com.dennytech.common.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class DefaultConfigService implements ConfigService,
		MApiRequestHandler {
	private Context context;
	private MApiService mapiService;
	private JsonObject root;
	private HashMap<String, ArrayList<ConfigChangeListener>> listeners;
	private MApiRequest request;

	public DefaultConfigService(Context context, MApiService mapiService) {
		this.context = context;
		this.mapiService = mapiService;
		listeners = new HashMap<String, ArrayList<ConfigChangeListener>>();
	}

	/**
	 * 创建mapi请求
	 */
	protected abstract MApiRequest createRequest();

	private File getConfigDir() {
		File dir = new File(context.getFilesDir(), "config");
		if (!dir.isDirectory()) {
			dir.delete();
			dir.mkdir();
		}

		return dir;
	}

	private File getConfigFile() {
		return new File(getConfigDir(), "1"); // 1 is a base version code
	}

	// read from file, return null if fail
	@SuppressWarnings("resource")
	private JsonObject read() {
		File file = getConfigFile();
		if (file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				if (fis.available() > 1000000)
					throw new IOException();
				byte[] buf = new byte[fis.available()];
				fis.read(buf);
				fis.close();
				String str = new String(buf, "UTF-8");
				JsonParser parser = new JsonParser();
				JsonElement element = parser.parse(str);
				JsonObject json = element.getAsJsonObject();
				return json;
			} catch (Exception e) {
			}
		} else {
		}
		return null;
	}

	private boolean write(JsonObject json, File file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(json.toString().getBytes());
			fos.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private JsonObject root() {
		if (root == null) {
			JsonObject dump = read();
			if (dump == null)
				dump = new JsonObject();
			if (root == null) {
				root = dump;
			}
		}
		return root;
	}

	@Override
	public JsonObject dump() {
		try {
			JsonParser parser = new JsonParser();
			JsonElement element = parser.parse(root().getAsString());
			return element.getAsJsonObject();
		} catch (Exception e) {
			return new JsonObject();
		}
	}

	@Override
	public boolean getBoolean(String name, boolean defaultValue) {
		JsonElement element = root().get(name);
		try {
			return element == null ? defaultValue : element.getAsBoolean();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	@Override
	public int getInt(String name, int defaultValue) {
		JsonElement element = root().get(name);
		try {
			return element == null ? defaultValue : element.getAsInt();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	@Override
	public double getDouble(String name, double defaultValue) {
		JsonElement element = root().get(name);
		try {
			return element == null ? defaultValue : element.getAsDouble();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	@Override
	public String getString(String name, String defaultValue) {
		JsonElement element = root().get(name);
		try {
			return element == null ? defaultValue : element.getAsString();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	@Override
	public JsonObject getJsonObject(String name) {
		try {
			return root().getAsJsonObject(name);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Object getObject(String name, Class<?> type) {
		try {
			return new Gson().fromJson(root().get(name), type);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void addListener(String key, ConfigChangeListener l) {
		synchronized (listeners) {
			ArrayList<ConfigChangeListener> list = listeners.get(key);
			if (list == null) {
				list = new ArrayList<ConfigChangeListener>();
				listeners.put(key, list);
			}
			list.add(l);
		}
	}

	@Override
	public void removeListener(String key, ConfigChangeListener l) {
		synchronized (listeners) {
			ArrayList<ConfigChangeListener> list = listeners.get(key);
			if (list != null) {
				list.remove(l);
				if (list.isEmpty()) {
					listeners.remove(key);
				}
			}
		}
	}

	@Override
	public void refresh() {
		if (request != null) {
			mapiService.abort(request, this, true);
		}
		request = createRequest();
		if (request != null) {
			mapiService.exec(request, this);
		} else {
			Log.w("config",
					"there is no request supply for the config service, refresh failed");
		}
	}

	@Override
	public void onRequestStart(MApiRequest req) {
	}

	@Override
	public void onRequestProgress(MApiRequest req, int count, int total) {
	}

	@Override
	public void onRequestFinish(MApiRequest req, MApiResponse resp) {
		if (resp.result() instanceof String) {
			String str = (String) resp.result();
			try {
				JsonParser parser = new JsonParser();
				JsonElement element = parser.parse(str);
				JsonObject json = element.getAsJsonObject().getAsJsonObject(
						"data");
				setConfig(json);
			} catch (Exception e) {
				Log.w("config", "result from " + req + " is not a json object",
						e);
			}
		} else {
			Log.w("config", "result from " + req + " is not a string");
		}
	}

	@Override
	public void onRequestFailed(MApiRequest req, MApiResponse resp) {
		Log.i("config", "fail to refresh config from " + req);
	}

	public void setConfig(JsonObject root) {
		if (root == null)
			return;

		if (Thread.currentThread().getId() != Looper.getMainLooper()
				.getThread().getId()) {
			Log.w("config", "setConfig must be run under main thread");
			if (Log.LEVEL < Integer.MAX_VALUE) {
				throw new RuntimeException(
						"setConfig must be run under main thread");
			} else {
				return;
			}
		}
		File file = new File(getConfigDir(), new Random(
				System.currentTimeMillis()).nextInt()
				+ ".tmp");
		if (!write(root, file)) {
			Log.w("config", "fail to write config to " + file);
			return;
		}
		if (!file.renameTo(getConfigFile())) {
			Log.w("config", "fail to move config file " + file);
			return;
		}
		JsonObject old = this.root;
		this.root = root;

		ArrayList<ConfigChangeListener> list = listeners.get(ANY);
		if (list != null) {
			for (ConfigChangeListener l : list) {
				l.onConfigChange(ANY, old, root);
			}
		}
		for (Map.Entry<String, ArrayList<ConfigChangeListener>> e : listeners
				.entrySet()) {
			String key = e.getKey();
			if (ANY.equals(key))
				continue;
			JsonElement v1 = old.get(key);
			JsonElement v2 = root.get(key);
			boolean eq = (v1 == null) ? (v2 == null) : (v1.equals(v2));
			if (eq)
				continue;
			list = e.getValue();
			Log.i("config", "config changed, " + key + " has " + list.size()
					+ " listeners");
			for (ConfigChangeListener l : list) {
				l.onConfigChange(key, v1, v2);
			}
		}
	}
}
