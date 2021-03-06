package com.baruckis.SlidingMenuImplementation.FromXML;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.AdapterView.OnItemClickListener;

import com.baruckis.SlidingMenuImplementation.Constants;
import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.data.HomeTag;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

/**
 * @author Andrius Baruckis http://www.baruckis.com
 * 
 */
public class SlidingMenuInitialiser {
	private FragmentActivity activity;
	private SlidingMenu menu;

	private SlidingMenuListFragmentBase slidingMenuListFragment;

	/**
	 * Initialiser constructor sets activity, which is going to have sliding
	 * menu created.
	 * 
	 * @param activity
	 *            This is Activity to which sliding menu is attached.
	 * 
	 */
	public SlidingMenuInitialiser(FragmentActivity activity) {
		this.activity = activity;
	}

	public void createSlidingMenu(Class<?> customClass, HomeTag[] tags,
			OnItemClickListener l) {
		menu = new SlidingMenu(activity);
		menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		menu.setShadowWidthRes(R.dimen.sliding_menu_shadow_width);
		menu.setShadowDrawable(R.drawable.sliding_menu_shadow);
		menu.setBehindOffsetRes(R.dimen.sliding_menu_offset);
		menu.setFadeDegree(0.35f);
		menu.attachToActivity(activity, SlidingMenu.SLIDING_CONTENT);
		menu.setMenu(R.layout.sliding_menu_frame);

		Bundle bundle = new Bundle();
		bundle.putParcelableArray(Constants.SLIDING_MENU_LIST_FRAGMENT_JSON,
				tags);

		// Here we create a new instance of a SlidingMenuListFragmentBase with
		// the given class name. This is the same as calling its empty
		// constructor. We also pass bundle with XML resource id value.
		slidingMenuListFragment = (SlidingMenuListFragmentBase) Fragment
				.instantiate(activity, customClass.getName(), bundle);
		// We target sliding menu.
		slidingMenuListFragment.setSlidingMenu(menu);
		slidingMenuListFragment.setOnItemClickListener(l);
		// We replace a FrameLayout, which is a content of sliding menu, with
		// our created list fragment filled with data from XML file.
		activity.getSupportFragmentManager().beginTransaction()
				.replace(R.id.sliding_menu_frame, slidingMenuListFragment)
				.commit();
	}

	public SlidingMenuListFragmentBase getSlidingMenuListFragment() {
		return slidingMenuListFragment;
	}

	public SlidingMenu getSlidingMenu() {
		return menu;
	}
}
