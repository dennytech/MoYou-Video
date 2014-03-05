package com.baruckis.SlidingMenuImplementation;

/**
 * @author Andrius Baruckis http://www.baruckis.com
 * 
 */
public class SlidingMenuListItem {
	public int id;
	public String name;
	public String icon;
	public String keyword;

	public SlidingMenuListItem() {
	}

	public SlidingMenuListItem(int id, String name, String iconResourceId, String Keyword) {
		this.id = id;
		this.name = name;
		this.icon = iconResourceId;
		this.keyword = Keyword;
	}
}
