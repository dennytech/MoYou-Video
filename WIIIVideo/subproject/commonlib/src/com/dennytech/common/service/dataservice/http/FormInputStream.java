package com.dennytech.common.service.dataservice.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.dennytech.common.util.WrapInputStream;

/**
 * 用来封装URL Form形式的Body的输入流。
 * <p>
 * 标准HTTP的application/x-www-form-urlencoded协议，默认编码采用ISO-8859-1。
 * </p>
 * 
 * @author Jun.Deng
 * 
 */
public class FormInputStream extends WrapInputStream {
	public static final String ISO_8859_1 = "ISO-8859-1";
	public static final String DEFAULT_CHARSET = ISO_8859_1;

	private List<NameValuePair> form;
	private String charsetName;

	public FormInputStream(List<NameValuePair> form) {
		this(form, DEFAULT_CHARSET);
	}

	public FormInputStream(List<NameValuePair> form, String charsetName) {
		this.form = form;
		this.charsetName = charsetName;
	}

	/**
	 * 构造函数。
	 * 
	 * @param keyValues
	 *            一个key跟着一个value（key，value，key，value...）
	 */
	public FormInputStream(String... keyValues) {
		this.form = form(keyValues);
		this.charsetName = DEFAULT_CHARSET;
	}
	
	protected static List<NameValuePair> form(String... keyValues) {
		int n = keyValues.length / 2;
		ArrayList<NameValuePair> list = new ArrayList<NameValuePair>(n);
		for (int i = 0; i < n; i++) {
			list.add(new BasicNameValuePair(keyValues[i * 2],
					keyValues[i * 2 + 1]));
		}
		return list;
	}

	public List<NameValuePair> form() {
		return form;
	}

	public String charsetName() {
		return charsetName;
	}

	private String encode() throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		for (NameValuePair e : form) {
			if (sb.length() > 0)
				sb.append('&');
			sb.append(e.getName());
			sb.append('=');
			if (e.getValue() != null)
				sb.append(URLEncoder.encode(e.getValue(), charsetName));
		}
		return sb.toString();
	}

	@Override
	protected InputStream wrappedInputStream() throws IOException {
		try {
			String str = encode();
			byte[] bytes = str.getBytes(charsetName);
			return new ByteArrayInputStream(bytes);
		} catch (UnsupportedCharsetException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public String toString() {
		try {
			return encode();
		} catch (Exception e) {
			return "";
		}
	}
}
