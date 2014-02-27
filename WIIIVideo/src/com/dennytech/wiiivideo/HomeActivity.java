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

import com.dennytech.wiiivideo.app.WVActivity;
import com.dennytech.wiiivideo.videolist.VideoListFragment;
import com.dennytech.wiiivideo.widget.PagerSlidingTabStrip;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

public class HomeActivity extends WVActivity {

	private SectionsPagerAdapter mSectionsPagerAdapter;
	private PagerSlidingTabStrip tabs;
	private ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(5);

		tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
		tabs.setViewPager(mViewPager);
		tabs.setIndicatorColor(0xFFC9C9C9);
		tabs.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int index) {
				HashMap<String,String> map = new HashMap<String,String>();
				map.put("page", mSectionsPagerAdapter.getPageTitle(index).toString());
				MobclickAgent.onEvent(HomeActivity.this, "home_page_select", map);
			}
			
			@Override
			public void onPageScrolled(int index, float arg1, int arg2) {
				HashMap<String,String> map = new HashMap<String,String>();
				map.put("page", mSectionsPagerAdapter.getPageTitle(index).toString());
				MobclickAgent.onEvent(HomeActivity.this, "home_page_scroll", map);
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				
			}
		});

		MobclickAgent.updateOnlineConfig(this);
		UmengUpdateAgent.update(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
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

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new VideoListFragment();
			int order = position + 1;
			Bundle args = new Bundle();
			args.putString(
					"url",
					"http://www.soku.com/search_video/q_%E9%AD%94%E5%85%BD%E4%BA%89%E9%9C%B83_orderby_"
							+ order);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 5;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.sort_default).toUpperCase(l);
			case 1:
				return getString(R.string.sort_new).toUpperCase(l);
			case 2:
				return getString(R.string.sort_paly_times).toUpperCase(l);
			case 3:
				return getString(R.string.sort_comments).toUpperCase(l);
			case 4:
				return getString(R.string.sort_collects).toUpperCase(l);
			}
			return null;
		}
	}

}
