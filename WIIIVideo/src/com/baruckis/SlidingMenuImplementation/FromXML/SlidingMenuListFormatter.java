package com.baruckis.SlidingMenuImplementation.FromXML;

import java.util.ArrayList;
import java.util.List;

import com.baruckis.SlidingMenuImplementation.SlidingMenuListItem;
import com.dennytech.wiiivideo.data.HomeTag;

/**
 * @author Andrius Baruckis http://www.baruckis.com
 * 
 */
public class SlidingMenuListFormatter {
	private final List<SlidingMenuListItem> list;

	public SlidingMenuListFormatter() {
		list = new ArrayList<SlidingMenuListItem>();
	}

	public List<SlidingMenuListItem> getList() {
		return list;
	}

	// This generates a list from a given data stream of a raw resource.
	public void generate(HomeTag[] tagsArr) {
		for (int i = 0; i < tagsArr.length; i++) {
			HomeTag tag = tagsArr[i];
			// Creates new object using these values.
			SlidingMenuListItem slidingMenuListItem = new SlidingMenuListItem(
					i, tag.title, tag.icon, tag.keyword);

			// Fills the list.
			list.add(slidingMenuListItem);
		}
	}
}
