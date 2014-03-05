package com.dennytech.wiiivideo.data;

import java.util.List;

public class VideoList {

	public List<Video> recommend;
	public List<Video> list;

	public List<Video> getRecommend() {
		return recommend;
	}

	public void setRecommend(List<Video> recommend) {
		this.recommend = recommend;
	}

	public List<Video> getList() {
		return list;
	}

	public void setList(List<Video> list) {
		this.list = list;
	}

}
