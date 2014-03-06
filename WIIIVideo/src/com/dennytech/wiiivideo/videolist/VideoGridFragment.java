package com.dennytech.wiiivideo.videolist;

import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import cn.trinea.android.view.autoscrollviewpager.AutoScrollViewPager;

import com.dennytech.common.util.BDUtils;
import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.data.Video;
import com.dennytech.wiiivideo.data.VideoList;
import com.dennytech.wiiivideo.videolist.view.VideoCommendItem;
import com.dennytech.wiiivideo.videolist.view.VideoListItem;
import com.umeng.analytics.MobclickAgent;

public class VideoGridFragment extends VideoListFragment {

	private AutoScrollViewPager pager;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		pager = new AutoScrollViewPager(getActivity());
		pager.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				BDUtils.dip2px(getActivity(), 200)));
		pager.setBackgroundResource(R.drawable.list_item_bg);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (pager != null) {
			pager.startAutoScroll();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (pager != null) {
			pager.stopAutoScroll();
		}
	}

	@Override
	protected Adapter createAdapter() {
		return new MyAdapter();
	}

	@Override
	protected Task createTask() {
		return new MyTask();
	}

	@Override
	public void reset() {
		listView.removeHeaderView(pager);
		super.reset();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Object item = parent.getItemAtPosition(position);
		if (item instanceof Video) {
			playVideo((Video) item);

			HashMap<String, String> map = new HashMap<String, String>();
			map.put("__ct__", String.valueOf(position));
			map.put("title", String.valueOf(((Video) item).title));
			MobclickAgent.onEvent(getActivity(), "video_grid_item_click", map);
		}
	}

	class MyAdapter extends Adapter {

		@Override
		public int getCount() {
			if (videoList.size() == 0) {
				return 1;
			}
			return videoList.size();
		}

		@Override
		public Object getItem(int position) {
			if (videoList.size() < 2) {
				return super.getItem(position);
			}

			if (position == 0) {
				Video[] head = { videoList.get(0), videoList.get(1) };
				return head;
			}

			if (position < videoList.size() - 1) {
				return videoList.get(position + 1);
			}

			return errorMsg == null ? LOADING : ERROR;
		}

		@Override
		protected View createItemViewWithData(int position, Object item,
				View convertView) {
			if (item instanceof Video[]) {
				final Video[] head = (Video[]) item;
				View view = getLayoutInflater(getArguments()).inflate(
						R.layout.layout_video_list_item_2, null);
				VideoListItem item1 = (VideoListItem) view
						.findViewById(R.id.item1);
				VideoListItem item2 = (VideoListItem) view
						.findViewById(R.id.item2);
				item1.setData(head[0]);
				item2.setData(head[1]);

				item1.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						playVideo(head[0]);
						HashMap<String, String> map = new HashMap<String, String>();
						map.put("__ct__", String.valueOf(0));
						map.put("title", String.valueOf(head[0].title));
						MobclickAgent.onEvent(getActivity(), "video_grid_item_click", map);
					}
				});
				item2.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						playVideo(head[1]);
						HashMap<String, String> map = new HashMap<String, String>();
						map.put("__ct__", String.valueOf(1));
						map.put("title", String.valueOf(head[1].title));
						MobclickAgent.onEvent(getActivity(), "video_grid_item_click", map);
					}
				});
				return view;

			} else {
				return super
						.createItemViewWithData(position, item, convertView);
			}
		}
	}

	class MyTask extends Task {
		@Override
		protected void onPostExecute(VideoList result) {
			super.onPostExecute(result);
			if (result.recommend != null && result.recommend.size() > 0) {
				if (listView.getHeaderViewsCount() == 0) {
					listView.addHeaderView(pager);
				}
				pager.setAdapter(new MyPagerAdapter(result.recommend));
				pager.setInterval(2000);
				pager.startAutoScroll();
			} else {
				if (adapter.page == 1) {
					listView.removeHeaderView(pager);
				}
			}
		}
	}

	class MyPagerAdapter extends PagerAdapter {

		List<Video> list;

		public MyPagerAdapter(List<Video> l) {
			this.list = l;
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			if (container instanceof ViewPager && object instanceof View) {
				((ViewPager) container).removeView((View) object);
			}
		}

		@Override
		public Object instantiateItem(ViewGroup container, final int position) {
			final Video video = list.get(position);
			VideoCommendItem view = (VideoCommendItem) LayoutInflater.from(
					getActivity()).inflate(R.layout.layout_commend_item, null);
			view.setData(video);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					playVideo(video);

					HashMap<String, String> map = new HashMap<String, String>();
					map.put("__ct__", String.valueOf(position));
					map.put("title", String.valueOf(video.title));
					MobclickAgent.onEvent(getActivity(),
							"video_pager_item_click", map);
				}
			});
			container.addView(view);
			return view;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

	}

}
