package com.dennytech.common.service.dataservice;

/**
 * 数据获取服务，同时提供同步异步方法
 * 
 * @author Jun.Deng
 * 
 * @param <T>
 *            请求Request的具体类型
 * @param <R>
 *            响应Response的具体类型
 */
public interface DataService<T extends Request, R extends Response> {

	/**
	 * 异步数据请求，最常用的数据请求方式，应该在主线程里面被执行<br>
	 * <br>
	 * 注意：可以通过{@link #abort(Request, RequestHandler, boolean)}
	 * 来取消，被取消的请求不会触发handler的onRequestFailed()回调
	 * 
	 * @param req
	 *            要处理的请求，不可变
	 * @param handler
	 *            回调处理，在主线程中被执行
	 */
	void exec(T req, RequestHandler<T, R> handler);

	/**
	 * 同步数据请求，无法被取消 <br>
	 * 注意：不建议放在主线程中执行
	 * 
	 * @param req
	 *            要处理的请求，不可变
	 * @return 请求结果Response
	 */
	R execSync(T req);

	/**
	 * 取消通过{@link #exec(Request, RequestHandler)}方法执行的异步请求<br>
	 * 不保证请求一定被取消<br>
	 * 
	 * @param req
	 *            被取消的Request请求
	 * @param handler
	 *            指定被取消的Handler。如果为null，则取消所有req请求的handler
	 * @param mayInterruptIfRunning
	 *            是否取消已经在执行中的请求。通常为true。
	 */
	void abort(T req, RequestHandler<T, R> handler,
			boolean mayInterruptIfRunning);
}
