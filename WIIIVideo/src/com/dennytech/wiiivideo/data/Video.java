package com.dennytech.wiiivideo.data;

public class Video {
	
	public String id;
	public String thumb;
	public String title;
	public String length;
	public String playTimes;
	public String publishTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getThumb() {
		return thumb;
	}

	public void setThumb(String thumb) {
		this.thumb = thumb;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLength() {
		return length;
	}

	public void setLength(String length) {
		this.length = length;
	}

	public String getPlayTimes() {
		return playTimes;
	}

	public void setPlayTimes(String playTimes) {
		this.playTimes = playTimes;
	}

	public String getPublishTime() {
		return publishTime;
	}

	public void setPublishTime(String publishTime) {
		this.publishTime = publishTime;
	}

	@Override
	public String toString() {
		return "id:" + id + " thumb:" + thumb + " title:" + title + " length:"
				+ length + " playTimes:" + playTimes + " publishTime:"
				+ publishTime;
	}
	
}
