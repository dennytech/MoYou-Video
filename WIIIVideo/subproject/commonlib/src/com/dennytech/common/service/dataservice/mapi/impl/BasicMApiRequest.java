package com.dennytech.common.service.dataservice.mapi.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.text.TextUtils;

import com.dennytech.common.service.dataservice.http.impl.BasicHttpRequest;
import com.dennytech.common.service.dataservice.mapi.CacheType;
import com.dennytech.common.service.dataservice.mapi.MApiFormInputStream;
import com.dennytech.common.service.dataservice.mapi.MApiRequest;

/**
 * 基本的MapiRequest实现，提供了默认的GET、POST静态工厂方法。
 * <p>
 * 注意：request构建这里是直接给出完整的url，对测试服务器的切换，是在MapiService里面完成的。
 * 
 * @author Jun.Deng
 * 
 */
public class BasicMApiRequest extends BasicHttpRequest implements MApiRequest {
	private CacheType defaultCacheType;
	private Class<?> resultClazz;

	public BasicMApiRequest(String url, String method, InputStream input,
			CacheType defaultCacheType, Class<?> resultClazz,
			List<NameValuePair> headers) {
		super(url, method, input, headers);
		this.defaultCacheType = defaultCacheType;
		this.resultClazz = resultClazz;
	}

	/**
	 * GET类型的MApiRequest静态工厂构造器
	 * <p>
	 * 注意：url应该为一个基本的请求地址，不包含参数（http://promo.lbc.baidu.com/xxx?）
	 * 
	 * 
	 * @param url
	 *            完整的请求地址, url里面不能包含参数
	 * @param defaultCacheType
	 *            缓存类型
	 * @param resultClazz
	 *            如果为NULL，则表示不指定返回类型，默认返回String
	 * @param forms
	 *            可以以变长参数形似传入url参数，将会补全到url中（key，value，key，value...）
	 * @return
	 */
	public static MApiRequest mapiGet(String url, CacheType defaultCacheType,
			Class<?> resultClazz, String... forms) {
		BasicMApiRequest r = new BasicMApiRequest(appendForms(url, forms), GET,
				null, defaultCacheType, resultClazz, null);
		return r;
	}

	/**
	 * GET类型的MApiRequest静态工厂构造器
	 * <p>
	 * 注意：url应该为一个基本的请求地址，不包含参数（http://promo.lbc.baidu.com/xxx?）
	 * 
	 * 
	 * @param url
	 *            完整的请求地址, url里面不能包含参数
	 * @param defaultCacheType
	 *            缓存类型
	 * @param resultClazz
	 *            如果为NULL，则表示不指定返回类型，默认返回String
	 * @param forms
	 *            包含参数的Map对象，以Key-Value的键值对存储
	 * @return
	 */
	public static MApiRequest mapiGet(String url, CacheType defaultCacheType,
			Class<?> resultClazz, Map<String, ?> forms) {
		BasicMApiRequest r = new BasicMApiRequest(appendForms(url, forms), GET,
				null, defaultCacheType, resultClazz, null);
		return r;
	}

	/**
	 * POST类型的MApiRequest静态工厂构造器
	 * 
	 * @param url
	 *            请求地址
	 * @param resultClazz
	 *            如果为NULL，则表示不指定返回类型，默认返回String
	 * @param forms
	 *            表单，数据以key-value形式交替给出（key，value，key，value...）
	 * @return
	 */
	public static MApiRequest mapiPost(String url, Class<?> resultClazz,
			String... forms) {
		BasicMApiRequest r = new BasicMApiRequest(url, POST,
				new MApiFormInputStream(forms), CacheType.DISABLED,
				resultClazz, null);
		return r;
	}

	/**
	 * POST类型的MApiRequest静态工厂构造器
	 * 
	 * @param url
	 *            请求地址
	 * @param resultClazz
	 *            如果为NULL，则表示不指定返回类型，默认返回String
	 * @param forms
	 *            表单，数据以key-value形式交替给出（key，value，key，value...）
	 * @return
	 */
	public static MApiRequest mapiPost(String url, Class<?> resultClazz,
			Map<String, ?> forms) {
		List<NameValuePair> formList = new ArrayList<NameValuePair>();
		for (String key : forms.keySet()) {
			formList.add(new BasicNameValuePair(key, String.valueOf(forms
					.get(key))));
		}

		BasicMApiRequest r = new BasicMApiRequest(url, POST,
				new MApiFormInputStream(formList), CacheType.DISABLED,
				resultClazz, null);
		return r;
	}

	@Override
	public CacheType defaultCacheType() {
		return defaultCacheType;
	}

	@Override
	public Class<?> resultClazz() {
		return resultClazz;
	}

	public static String appendForms(String url, String... forms) {
		if (forms == null || forms.length == 0) {
			if (!url.contains("?")) {
				return url + "?";
			}
			return url;
		}

		if (forms.length % 2 != 0) {
			throw new IllegalArgumentException(
					"Do you miss a parameter in forms?");
		}

		Map<String, String> formMap = new HashMap<String, String>();
		for (int i = 0; i < forms.length - 1; i += 2) {
			formMap.put(forms[i], forms[i + 1]);
		}

		return appendForms(url, formMap);
	}

	public static String appendForms(String url, Map<String, ?> forms) {
		StringBuilder sb = new StringBuilder(url);
		if (!url.contains("?")) {
			sb.append("?");
		}

		if (forms == null || forms.isEmpty()) {
			return sb.toString();
		}

		for (String key : forms.keySet()) {
			String value = String.valueOf(forms.get(key));
			if (TextUtils.isEmpty(value)) {
				continue;
			}
			sb.append("&").append(key).append("=").append(value);
		}

		return sb.toString();
	}

}
