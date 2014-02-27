package com.dennytech.wiiivideo.data;

public class Video {
	
	public String id;
	public String thumb;
	public String title;
	public String length;
	public String playTimes;
	public String publishTime;

	@Override
	public String toString() {
		return "id:" + id + " thumb:" + thumb + " title:" + title + " length:"
				+ length + " playTimes:" + playTimes + " publishTime:"
				+ publishTime;
	}
	
}
