package com.dennytech.common.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.dennytech.common.loader.bean.MappingSpec;
import com.dennytech.common.util.Log;
import com.google.gson.Gson;

import android.content.Context;

public class MappingManager {
	
	private static final String TAG = MappingManager.class.getSimpleName();
	
	private final Context context;
	private MappingSpec mappingSpec;
	
	public MappingManager(Context ctx) {
		this.context = ctx;
	}
	
	public MappingSpec mappingSpec() {
		if (mappingSpec == null) {
			mappingSpec = read();
		}
		return mappingSpec;
	}
	
	protected MappingSpec read() {
		MappingSpec result = null;
		InputStream is = null;
		ByteArrayOutputStream bos = null;
		try {
			is = context.getResources().getAssets().open("urlmapping.txt");
			bos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int l;
			while ((l = is.read(buf)) != -1) {
				bos.write(buf, 0, l);
			}
			
			byte[] bytes = bos.toByteArray();
			String str = new String(bytes, "UTF-8");
			result = new Gson().fromJson(str, MappingSpec.class);
			
		} catch (IOException e) {
			Log.e(TAG, "read mapping failed", e);
			
		} finally {
			try {
				is.close();
				bos.close();
			} catch (Exception e2) {
			}
		}
		return result;
	}

}
