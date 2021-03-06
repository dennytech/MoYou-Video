package com.baruckis.SlidingMenuImplementation.FromXML;

import android.view.View;
import android.widget.ListView;

/**
 * @author Andrius Baruckis http://www.baruckis.com
 * 
 */
public class SlidingMenuListFragmentConcrete extends
		SlidingMenuListFragmentBase {

	// We can define actions, which will be called, when we press on separate
	// list items. These actions can override default actions defined inside
	// base fragment. Also, you can create new actions, which will added to the
	// default ones.
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}

}
