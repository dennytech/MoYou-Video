package com.dennytech.common.service.dataservice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.UnsupportedCharsetException;

import com.dennytech.common.util.WrapInputStream;

/**
 * 标准的文本流，默认的编码为UTF-8
 * 
 * @author Jun.Deng
 *
 */
public class StringInputStream extends WrapInputStream {
	public static final String UTF_8 = "UTF-8";
	public static final String DEFAULT_CHARSET = UTF_8;

	private String data;
	private String charsetName;

	public StringInputStream(String str, String charsetName) {
		data = str;
		this.charsetName = charsetName;
	}

	public StringInputStream(String str) {
		this(str, DEFAULT_CHARSET);
	}

	public String data() {
		return data;
	}

	public String charsetName() {
		return charsetName;
	}

	@Override
	protected InputStream wrappedInputStream() throws IOException {
		try {
			byte[] bytes = data.getBytes(charsetName);
			return new ByteArrayInputStream(bytes);
		} catch (UnsupportedCharsetException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public String toString() {
		return data;
	}
}
