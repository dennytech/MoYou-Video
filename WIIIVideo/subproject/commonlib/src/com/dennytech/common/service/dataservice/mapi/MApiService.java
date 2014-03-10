package com.dennytech.common.service.dataservice.mapi;

import com.dennytech.common.service.dataservice.DataService;

/**
 * 基于MApi协议的网络服务。<br>
 * <p>
 * 当statusCode = 2xx时，表示成功<br>
 * 当statusCode = 400或发生其他网络和数据错误时，返回失败，MApiResponse.message()中包含错误信息<br>
 * </p>
 * 所有异步调用的发起必须在主线程中执行，回调方法也在主线程中执行<br>
 * 同步方法不允许在主线程中调用
 * 
 * @author Jun.Deng
 * 
 */
public interface MApiService extends DataService<MApiRequest, MApiResponse> {

}
