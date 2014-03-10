package com.dennytech.common.service.dataservice.mapi;

import java.io.InputStream;

import com.dennytech.common.service.dataservice.http.HttpRequest;

public interface MApiRequest extends HttpRequest {

	/**
	 * HTTP输入流<br>
	 * 如果为GET方法或不需要Body，可为null<br>
	 * <p>
	 * 一般使用MApiFormInputStream来封装URL
	 * Form形式的Body（key1=value1&key2=value2...)<br>
	 * 或使用ChainInputStream组合MApiFormInputStream来封装multipart/form-data，
	 * </p>
	 * 注意如果InputStream不支持mark()，比如ChainInputStream，那么这个HttpRequest只能被执行一次。
	 * 因为流只能够被读取一次，所以当请求被第二次执行时，会遇到Body为空的情况。
	 * 
	 * @return
	 */
	InputStream input();

	/**
	 * 默认的缓存类型<br>
	 * 如果ApiServer不指定缓存类型，则使用该默认值
	 * 
	 * @return
	 */
	CacheType defaultCacheType();
	
	/**
	 * 指定返回数据{@link MApiResponse#result()}的类型。<br>
	 * 如何为NULL，则直接返回json字符串。
	 * 
	 * @return
	 */
	Class<?> resultClazz();

}
