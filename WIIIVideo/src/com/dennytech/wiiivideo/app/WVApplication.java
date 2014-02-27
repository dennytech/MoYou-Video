package com.dennytech.wiiivideo.app;

import com.dennytech.common.app.CLApplication;
import com.dennytech.common.app.ServiceManager;

public class WVApplication extends CLApplication {
	
	protected ServiceManager createServiceManager() {
		return new MyServiceManager(this);
	}

}
