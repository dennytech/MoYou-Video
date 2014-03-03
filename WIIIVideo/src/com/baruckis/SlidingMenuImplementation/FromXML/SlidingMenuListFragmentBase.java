package com.baruckis.SlidingMenuImplementation.FromXML;

import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.baruckis.SlidingMenuImplementation.Constants;
import com.baruckis.SlidingMenuImplementation.SlidingMenuListAdapter;
import com.baruckis.SlidingMenuImplementation.SlidingMenuListItem;
import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.data.HomeTag;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

/**
 * @author Andrius Baruckis http://www.baruckis.com
 * 
 */
public abstract class SlidingMenuListFragmentBase extends ListFragment {
	protected List<SlidingMenuListItem> slidingMenuList;
	protected SlidingMenu menu = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// We set here a custom layout which uses holo dark theme colors.
		return inflater.inflate(R.layout.sliding_menu_holo_dark_list, null);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle arguments = getArguments();
		// if there is no bundle passed, that means we did not get any XML
		// resource, so we exit.
		if (arguments == null)
			return;

		// if everything is ok, we use our created formatter to get list from
		// XML.
		SlidingMenuListFormatter slidingMenuListFormatter = new SlidingMenuListFormatter();
		// We open a data stream for reading a raw resource.
		HomeTag[] tags = (HomeTag[]) arguments
				.getParcelableArray(Constants.SLIDING_MENU_LIST_FRAGMENT_JSON);
		// Formatter analyses data stream and generates list for us.
		slidingMenuListFormatter.generate(tags);
		slidingMenuList = slidingMenuListFormatter.getList();

		// We pass our newly generated list to the adapter
		SlidingMenuListAdapter adapter = new SlidingMenuListAdapter(
				getActivity(), R.layout.sliding_menu_holo_dark_list_row,
				slidingMenuList);

		setListAdapter(adapter);
	}

	// It is our base fragment which will be extended, so we can define default
	// actions, which will be called when we press on separate list items.
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		SlidingMenuListItem item = slidingMenuList.get(position);

		CharSequence text;
		Activity activity = getActivity();
	}

	public void setSlidingMenu(SlidingMenu menu) {
		this.menu = menu;
	}
}
