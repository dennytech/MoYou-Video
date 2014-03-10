package com.dennytech.common.loader.bean;

import android.text.TextUtils;

public class MappingSpec {

	public Class<?> loader;
	public PageSpec[] pages;

	public MappingSpec(Class<?> loader, PageSpec[] pages) {
		this.loader = loader;
		this.pages = pages;
	}

	public PageSpec getPage(String host) {
		if (TextUtils.isEmpty(host))
			return null;
		for (PageSpec page : pages) {
			if (page == null) {
				continue;
			}
			if (host.equalsIgnoreCase(page.host)) {
				return page;
			}
		}
		return null;
	}

}
