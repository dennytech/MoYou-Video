package com.dennytech.common.service.dataservice.image;

import com.dennytech.common.service.dataservice.DataService;
import com.dennytech.common.service.dataservice.Request;
import com.dennytech.common.service.dataservice.Response;

/**
 * 图片服务，Response.result为Bitmap对象。<br>
 * 包含本地缓存，不包含MemCache
 * 
 * <p>
 * 注：该服务只提供图片的获取，不提供图片上传，上传使用MApiService<br>
 * 
 * @author Jun.Deng
 * 
 */
public interface ImageService extends DataService<Request, Response> {

}
