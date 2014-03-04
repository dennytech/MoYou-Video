package com.dennytech.wiiivideo.app;

import android.content.Context;

import com.dennytech.common.app.CLApplication;
import com.dennytech.common.app.ServiceManager;
import com.dennytech.common.service.configservice.impl.DefaultConfigService;
import com.dennytech.common.service.dataservice.mapi.CacheType;
import com.dennytech.common.service.dataservice.mapi.MApiRequest;
import com.dennytech.common.service.dataservice.mapi.impl.BasicMApiRequest;
import com.dennytech.common.service.dataservice.mapi.impl.DefaultMApiService;

public class MyServiceManager extends ServiceManager {

	public MyServiceManager(Context context) {
		super(context);
	}

	@Override
	protected DefaultMApiService createMApiService() {
		return new DefaultMApiService(context,
				"Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/6.0)");
	}

	@Override
	protected DefaultConfigService createConfigService() {
		return new DefaultConfigService(context, CLApplication.instance()
				.mapiService()) {

			@Override
			protected MApiRequest createRequest() {
				return BasicMApiRequest
						.mapiGet(
								"https://raw.github.com/donotwarry/WIII-Video/master/WIIIVideo/assets/config.json",
								CacheType.DISABLED, null);
			}
		};
	}

}
