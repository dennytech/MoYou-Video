package com.dennytech.common.loader.bean;

public class PageSpec {

	public String host;
	public Class<?> fragment;
	public Class<?> activity;
	public boolean login;

	public PageSpec(String host, Class<?> fragment, Class<?> activity,
			boolean login) {
		this.host = host;
		this.fragment = fragment;
		this.activity = activity;
		this.login = login;
	}
}
