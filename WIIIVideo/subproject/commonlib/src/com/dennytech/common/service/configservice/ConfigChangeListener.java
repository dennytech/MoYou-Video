package com.dennytech.common.service.configservice;

import com.google.gson.JsonElement;

/**
 * 配置项更新Listener
 * <p>
 * 如需关注某个配置项的变化，需要创建Listner，并调用subscribe，不再关注时调用unsubscribe
 */
public interface ConfigChangeListener {

	/**
	 * 名字为 key 的配置项变化时被调用
	 *
	 * @param from 变化前的值，需要调用者通过'getAsJsonXXX'转换为Integer或者String，变化前无该配置项时from为null
	 * @param to   变化后的值，需要调用者通过'getAsJsonXXX'转换为Integer或者String，变化后删除该配置项时to为null
	 */
	void onConfigChange(String key, JsonElement from, JsonElement to);

}