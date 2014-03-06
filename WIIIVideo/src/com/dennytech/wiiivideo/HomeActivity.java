package com.dennytech.wiiivideo;

import java.util.HashMap;
import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.baruckis.SlidingMenuImplementation.SlidingMenuListItem;
import com.baruckis.SlidingMenuImplementation.FromXML.ActivityBase;
import com.baruckis.SlidingMenuImplementation.FromXML.SlidingMenuInitialiser;
import com.baruckis.SlidingMenuImplementation.FromXML.SlidingMenuListFragmentConcrete;
import com.dennytech.common.service.configservice.ConfigChangeListener;
import com.dennytech.wiiivideo.data.Home;
import com.dennytech.wiiivideo.data.HomeTag;
import com.dennytech.wiiivideo.videolist.VideoGridFragment;
import com.dennytech.wiiivideo.videolist.VideoListFragment;
import com.dennytech.wiiivideo.widget.PagerSlidingTabStrip;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

public class HomeActivity extends ActivityBase implements ConfigChangeListener,
		OnItemClickListener {

	private View root;
	private SectionsPagerAdapter sPagerAdapter;
	private PagerSlidingTabStrip tabs;
	private ViewPager mViewPager;

	private Home home;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		root = findViewById(R.id.root);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
		home = (Home) configService().getObject("home", Home.class);
		if (home != null) {
			initView();
		} else {
			showProgressDialog(getString(R.string.msg_loading));
		}
		configService().refresh();
		configService().addListener("home", this);

		MobclickAgent.updateOnlineConfig(this);
		// MobclickAgent.setOnlineConfigureListener(this);
		UmengUpdateAgent.update(this);
	}

	private void initView() {
		root.setVisibility(View.VISIBLE);

		sPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		mViewPager.setAdapter(sPagerAdapter);
		mViewPager.setOffscreenPageLimit(5);

		tabs.setViewPager(mViewPager);
		tabs.setIndicatorColor(0xFFC9C9C9);
		tabs.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int index) {
				switch (index) {
				case 0:
					slidingMenuInitialiser.getSlidingMenu().setTouchModeAbove(
							SlidingMenu.TOUCHMODE_FULLSCREEN);
					break;
				default:
					slidingMenuInitialiser.getSlidingMenu().setTouchModeAbove(
							SlidingMenu.TOUCHMODE_MARGIN);
					break;
				}

				HashMap<String, String> map = new HashMap<String, String>();
				map.put("page", sPagerAdapter.getPageTitle(index).toString());
				MobclickAgent.onEvent(HomeActivity.this, "home_page_select",
						map);
			}

			@Override
			public void onPageScrolled(int index, float arg1, int arg2) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("page", sPagerAdapter.getPageTitle(index).toString());
				MobclickAgent.onEvent(HomeActivity.this, "home_page_scroll",
						map);
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});

		slidingMenuInitialiser = new SlidingMenuInitialiser(this);
		if (home != null && home.tags != null) {
			slidingMenuInitialiser.createSlidingMenu(
					SlidingMenuListFragmentConcrete.class, home.tags, this);
		} else {
			HomeTag[] arr = new HomeTag[1];
			arr[0] = new HomeTag("全部视频", "", "魔兽争霸3");
			slidingMenuInitialiser.createSlidingMenu(
					SlidingMenuListFragmentConcrete.class, arr, this);
		}

		slidingMenuInitialiser.getSlidingMenu().setTouchmodeMarginThreshold(10);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	/**
	 * Menu Item Click
	 * 
	 * @param parent
	 * @param view
	 * @param position
	 * @param id
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		slidingMenuInitialiser.getSlidingMenu().toggle();
		slidingMenuInitialiser.getSlidingMenuListFragment().getListAdapter()
				.setSelecte(position);

		SlidingMenuListItem item = (SlidingMenuListItem) parent
				.getItemAtPosition(position);
		sPagerAdapter.setKeyword(item.keyword);

		Fragment current = sPagerAdapter.getCurrentFragment();
		if (current instanceof VideoListFragment) {
			((VideoListFragment) current).setKeyword(item.keyword);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().equals(getString(R.string.action_about))) {
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("wvideo://about")));
			MobclickAgent.onEvent(this, "about");
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		private String keyword;

		public void setKeyword(String kw) {
			this.keyword = kw;
		}

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment;
			if (position == 0) {
				fragment = new VideoGridFragment();
			} else {
				fragment = new VideoListFragment();
			}

			Bundle args = new Bundle();
			args.putString("url", home.sorts[position].url);
			args.putString("keyword", home.tags[0].keyword);
			args.putString("parser", home.sorts[position].parser);

			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			return (home == null || home.sorts == null || home.tags == null) ? 0
					: home.sorts.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			if (home != null && home.sorts != null) {
				return home.sorts[position].title.toUpperCase(l);
			}
			return null;
		}

		private Fragment currentFragment;

		public Fragment getCurrentFragment() {
			return currentFragment;
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position,
				Object object) {
			if (getCurrentFragment() != object) {
				currentFragment = ((Fragment) object);
			}
			if (object instanceof VideoListFragment) {
				VideoListFragment fragment = ((VideoListFragment) object);
				if (keyword != null) {
					fragment.setKeyword(keyword);
				}
			}
			super.setPrimaryItem(container, position, object);
		}
	}

	@Override
	public void onConfigChange(String key, JsonElement from, JsonElement to) {
		if ("home".equals(key)) {
			dismissDialog();
			if (to != null) {
				home = new Gson().fromJson(to, Home.class);
				initView();
			} else {
				showDialog(getString(R.string.app_name), "服务器繁忙，请稍后再试", null);
			}
		}
	}

}
