package com.dennytech.common.service.dataservice.mapi;

/**
 * MApi调试面板工具
 * <p>
 * 域名切换：切换到指定域名<br>
 * 延时请求：每个请求都会延时返回<br>
 * 请求失败：模拟网络失败 50% 请求失败：下N次网络请求模拟失败
 * 
 */
public interface MApiDebugAgent {

	String baseDomain();

	void setBaseDomain(String to);
	
	String updateDomain();
	
	void setUpdateDomain(String to);
	
	String oneKeyRegisterDomain();
	
	void setOneKeyRegisterDomain(String to);

	long delay();

	void setDelay(long delay);

	boolean failHalf();

	void setFailHalf(boolean f);
	
	int nextFail();

	void addNextFail(int n);

}
