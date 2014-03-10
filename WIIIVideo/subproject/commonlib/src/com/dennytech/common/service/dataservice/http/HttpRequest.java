package com.dennytech.common.service.dataservice.http;

import java.io.InputStream;
import java.util.List;

import org.apache.http.NameValuePair;

import com.dennytech.common.service.dataservice.Request;

public interface HttpRequest extends Request {

	/**
	 * HTTP方法，常用的有GET和POST。<br>
	 * 必须为大写
	 * 
	 * @return
	 */
	String method();

	/**
	 * HTTP输入流。<br>
	 * 如果为GET方法或不需要Body，可为null<br>
	 * 
	 * <p>
	 * 一般使用{@link FormInputStream}来封装URL Form形式的Body（key1=value1&key2=value2...)
	 * <br>
	 * 或使用StringInputStream来封装纯字符串Body
	 * </p>
	 * 
	 * <p>
	 * 注意：如果InputStream不支持mark()，那么这个HttpRequest只能被执行一次。因为流只能够被读取一次，所以当请求被第二次执行时
	 * ， 会遇到Body为空的情况。
	 * </p>
	 * 
	 * @return
	 * 
	 */
	InputStream input();

	/**
	 * HTTP头<br>
	 * 如果为空，则没有额外的头信息
	 * 
	 * @return
	 */
	List<NameValuePair> headers();
}
