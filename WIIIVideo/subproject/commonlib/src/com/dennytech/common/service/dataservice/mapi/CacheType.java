package com.dennytech.common.service.dataservice.mapi;

/**
 * 预定义的缓存类型
 * 
 * @author Jun.Deng
 * 
 */
public enum CacheType {

	/**
	 * 禁用
	 */
	DISABLED,

	/**
	 * 标准缓存，由过期时间控制（一般为5分钟），命中直接返回
	 */
	NORMAL,

	/**
	 * 竞争缓存，缓存有过期时间（一般为5分钟），命中先返回，同时进行网络请求（会有两次数据回调）
	 */
	RIVAL,

	/**
	 * 持久化缓存，命中先返回。缓存不会过期，但是超过一定时间（一般为12小时）会在后台更新缓存内容
	 */
	PERSISTENT,

	/**
	 * 关键缓存，先尝试网络请求，如果失败，则返回缓存
	 */
	CRITICAL
}
