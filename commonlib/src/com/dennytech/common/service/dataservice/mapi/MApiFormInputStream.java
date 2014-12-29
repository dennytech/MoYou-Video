package com.dennytech.common.service.dataservice.mapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.dennytech.common.service.dataservice.http.FormInputStream;

/**
 * MApi使用的url-form形式，一般为POST请求使用
 * 
 * @author Jun.Deng
 * 
 */
public class MApiFormInputStream extends FormInputStream {
	public static final String UTF_8 = "UTF-8";
	public static final String DEFAULT_CHARSET = UTF_8;

	public MApiFormInputStream(List<NameValuePair> form) {
		super(form, DEFAULT_CHARSET);
	}

	public MApiFormInputStream(String... keyValues) {
		super(form(keyValues), DEFAULT_CHARSET);
	}

	public MApiFormInputStream(Map<String, ?> forms) {
		super(form(forms), DEFAULT_CHARSET);
	}

	protected static List<NameValuePair> form(Map<String, ?> forms) {
		List<NameValuePair> list = new ArrayList<NameValuePair>();
		Set<String> ks = forms.keySet();
		for (String key : ks) {
			list.add(new BasicNameValuePair(key, String.valueOf(forms.get(key))));
		}
		return list;
	}
}
