package com.dennytech.wiiivideo.app;

import com.dennytech.common.app.CLApplication;
import com.dennytech.common.app.ServiceManager;

public class MYApplication extends CLApplication {
	
	protected ServiceManager createServiceManager() {
		return new MyServiceManager(this);
	}

}
