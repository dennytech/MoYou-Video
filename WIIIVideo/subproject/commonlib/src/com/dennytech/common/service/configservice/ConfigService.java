package com.dennytech.common.service.configservice;

import com.google.gson.JsonObject;

/**
 * 配置服务
 * <p>
 * 配置由Server提供，服务提供刷新方法，不提供修改方法<br>
 * 配置不需要由调用方缓存，配置服务的实现可以应对频繁调用
 * 
 */
public interface ConfigService {
	/**
	 * 用以订阅任意键值的变化通知
	 * <p>
	 * ConfigService.addListener(ANY, listener)<br>
	 * ConfigService.removeListener(ANY, listener)
	 */
	public static final String ANY = "*";

	boolean getBoolean(String name, boolean defaultValue);

	int getInt(String name, int defaultValue);

	double getDouble(String name, double defaultValue);

	/**
	 * 如果没有name或name为null，则返回defaultValue
	 * <p>
	 * 建议使用TextUtils.isEmpty()判断是否返回空
	 */
	String getString(String name, String defaultValue);
	
	/**
	 * 如果没有name或name对应的值不为JsonObject，则返回null
	 */
	JsonObject getJsonObject(String name);
	
	/**
	 * 读取指定字段的值，以type类型返回，如果值不存在或者解析失败返回null
	 * 
	 * @param name
	 * @param type 指定返回bean类型
	 * @return
	 */
	Object getObject(String name, Class<?> type);

	/**
	 * 获取整个配置文件
	 * <p>
	 * 配置文件是基于JSON的，所以返回JSONObject对象<br>
	 * 可以用来获取JSONObject等复杂类型数据<br>
	 * 修改JSONObject对象不会对配置产生影响
	 */
	JsonObject dump();

	/**
	 * 从服务器获取并更新配置文件
	 */
	void refresh();

	/**
	 * 订阅名字为key的配置项的变化通知
	 * <p>
	 * 注意只能获取boolean, int, double, String四种基本类型的变化<br>
	 * 如果要获取JSONObject等其他复杂类型数据，可以监听ConfigService.ANY获取任意键值的变化
	 * 
	 * @param key
	 *            订阅的配置项的名字，也可以是ConfigService.ANY来获取任意键值的变化
	 * @param l
	 *            配置项发生变化时，回调的Listener
	 */
	void addListener(String key, ConfigChangeListener l);

	/**
	 * 取消订阅名字为key的配置项的变化通知
	 * 
	 * @param key
	 *            取消订阅的配置项的名字，包括ConfigService.ANY
	 * 
	 * @param l
	 *            订阅时传入的Listener
	 */
	void removeListener(String key, ConfigChangeListener l);

}
