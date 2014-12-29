package com.dennytech.common.service.dataservice.http;

import com.dennytech.common.service.dataservice.DataService;

/**
 * 基于HTTP协议的网络服务<br>
 * <p>
 * 当statusCode = 2xx | 4xx | 5xx时，均表示成功<br>
 * 失败的情况一般有无网络或网络超时<br>
 * </p>
 * 所有异步调用的发起必须在主线程中执行，回调方法也在主线程中执行<br>
 * 同步方法不允许在主线程中调用
 * 
 * @author Jun.Deng
 *
 */
public interface HttpService extends DataService<HttpRequest, HttpResponse> {

}
