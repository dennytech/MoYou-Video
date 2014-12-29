package com.dennytech.common.service.dataservice.cache;

import com.dennytech.common.service.dataservice.DataService;
import com.dennytech.common.service.dataservice.Request;

/**
 * 以字符串作为键的缓存服务，提供基本的存取和删除功能
 * <p>
 * 基于时间进行批量删除，并支持对象时间的快速查询<br>
 * 时间可以是创建时间，也可以是过期时间<br>
 * 
 * @author Jun.Deng
 *
 */
public interface CacheService extends DataService<Request, CacheResponse> {

	/**
	 * 获取键为key的缓存value
	 * 
	 * @param key
	 * @return 如果不存在或者遇到错误返回null
	 */
	Object get(Request key);

	/**
	 * 键为key的缓存的添加（更新）时间
	 * 
	 * @param key
	 * @return 如果不存在或者遇到错误返回-1
	 */
	long getTime(Request key);

	/**
	 * 添加或更新操作，替换已有内容
	 * 
	 * @param key 请求
	 * @param val 数据，一般为String
	 * @param time 操作时间
	 * @return 操作是否成功
	 */
	boolean put(Request key, Object val, long time);

	/**
	 * 只更新Key的时间，不更新内容<br>
	 * 操作速度较put或insert快
	 * 
	 * @param key
	 * @param time
	 * @return 如果该Key不存在或操作失败，则返回false
	 */
	boolean touch(Request key, long time);

	/**
	 * 移除键为kay的缓存数据
	 * 
	 * @param key
	 */
	void remove(Request key);

	/**
	 * 清除缓存
	 */
	void clear();

}
