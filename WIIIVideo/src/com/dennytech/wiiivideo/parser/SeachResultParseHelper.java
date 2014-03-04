package com.dennytech.wiiivideo.parser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import android.content.Context;
import android.text.TextUtils;

import com.dennytech.common.util.Log;
import com.dennytech.wiiivideo.data.Video;
import com.google.gson.Gson;
import com.umeng.analytics.MobclickAgent;

public class SeachResultParseHelper implements ParseHelper {

	private static final String TAG = "lua_parser";

	protected LuaState luaState;
	private Context context;

	private SeachResultParseHelper(Context ctx) {
		context = ctx;
	}

	private static SeachResultParseHelper instance;

	public synchronized static SeachResultParseHelper instance(Context ctx) {
		if (instance == null) {
			instance = new SeachResultParseHelper(ctx);
		}
		return instance;
	}

	private LuaState createFromAssets(Context ctx, String name) {
		LuaState ls = LuaStateFactory.newLuaState();
		ls.openLibs();
		InputStream is = null;
		ByteArrayOutputStream bos = null;
		try {
			is = ctx.getResources().getAssets().open(name);
			bos = new ByteArrayOutputStream();

			int i = is.read();
			while (i != -1) {
				bos.write(i);
				i = is.read();
			}

			ls.LdoString(bos.toString());

			ls.getField(LuaState.LUA_GLOBALSINDEX, "init");
			ls.pushObjectValue(ctx);
			ls.call(1, 0);

		} catch (Exception e) {
		} finally {
			try {
				is.close();
				bos.close();
			} catch (Exception e2) {
			}
		}

		return ls;
	}

	private LuaState createFromString(Context ctx, String script) {
		LuaState ls = LuaStateFactory.newLuaState();
		ls.openLibs();

		try {
			ls.LdoString(script);
			ls.getField(LuaState.LUA_GLOBALSINDEX, "init");
			ls.pushObjectValue(ctx);
			ls.call(1, 0);

		} catch (Exception e) {
		}

		return ls;
	}

	@Override
	public String parse(String source) {
		try {
			if (luaState == null) {
				String script = MobclickAgent.getConfigParams(context,
						"lua_search_result");
				if (!TextUtils.isEmpty(script)) {
					luaState = createFromString(context, script);
					Log.i(TAG, "create luaState from config");
					
				} else {
					luaState = createFromAssets(context, "search_result.lua");
					Log.i(TAG, "create luaState from assets");
				}
			}

			luaState.getField(LuaState.LUA_GLOBALSINDEX, "parse");
			luaState.pushString(source);
			luaState.call(1, 1);

			luaState.setField(LuaState.LUA_GLOBALSINDEX, "result");
			LuaObject lobj = luaState.getLuaObject("result");
			return lobj.getString();

		} catch (Exception e) {
			Log.e(TAG, "parse failed", e);
		} catch (Error e) {
			Log.e(TAG, "parse failed", e);
		}
		return parseByDefault(source);
	}

	@Override
	public String parseByDefault(String source) {
		Log.i(TAG, "parse search result by default");
		List<Video> videos = new ArrayList<Video>();
		try {
			Document doc = Jsoup.parse(source);
			Element vlist = doc.getElementsByClass("sk-vlist").get(0);
			Elements v = vlist.getElementsByClass("v");
			for (Element element : v) {
				Video video = new Video();
				Element vthumb = element.getElementsByClass("v-thumb").get(0);
				video.thumb = vthumb.getElementsByTag("img").attr("src");
				video.length = vthumb.getElementsByClass("v-time").get(0)
						.childNode(0).toString();

				Element vmeta = element.getElementsByClass("v-meta").get(0);
				video.title = vmeta.getElementsByTag("a").get(0).attr("title");
				video.id = vmeta.getElementsByTag("a").get(0).attr("href")
						.replace("http://v.youku.com/v_show/id_", "")
						.replace(".html", "");

				Elements vmetadata = vmeta.getElementsByClass("v-meta-data");
				video.playTimes = vmetadata.get(1).getElementsByTag("span")
						.text();
				video.publishTime = vmetadata.get(2).getElementsByTag("span")
						.text();

				videos.add(video);
			}

		} catch (Exception e) {
			return null;
		}
		return new Gson().toJson(videos);
	}

}