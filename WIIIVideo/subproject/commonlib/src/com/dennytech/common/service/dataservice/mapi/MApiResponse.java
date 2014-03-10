package com.dennytech.common.service.dataservice.mapi;

import com.dennytech.common.service.dataservice.http.HttpResponse;

public interface MApiResponse extends HttpResponse {

	/**
	 * 返回结果对象，一般情况会解析成具体类型的bean。
	 * <p>
	 * 业务层需要做具体判断：<br>
	 * if(response.result instanceof XXXBean){<br>
	 * ...<br>
	 * }
	 * 
	 * @return
	 */
	Object result();

	/**
	 * 发生400或其他错误时，描述错误原因。
	 * 
	 * @return
	 */
	MApiMsg message();

	/**
	 * 是否为缓存。
	 * 
	 * @return
	 */
	boolean isCache();
	
	/**
	 * 原始数据。
	 * 
	 * @return
	 */
	byte[] rawData();

}
