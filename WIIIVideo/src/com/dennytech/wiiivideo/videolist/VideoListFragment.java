package com.dennytech.wiiivideo.videolist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.dennytech.common.adapter.BasicAdapter;
import com.dennytech.common.service.dataservice.mapi.CacheType;
import com.dennytech.common.service.dataservice.mapi.MApiRequest;
import com.dennytech.common.service.dataservice.mapi.MApiRequestHandler;
import com.dennytech.common.service.dataservice.mapi.MApiResponse;
import com.dennytech.common.service.dataservice.mapi.impl.BasicMApiRequest;
import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.app.WVFragment;
import com.dennytech.wiiivideo.data.Video;
import com.dennytech.wiiivideo.parser.SeachResultParseHelper;
import com.dennytech.wiiivideo.videolist.view.VideoListItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.umeng.analytics.MobclickAgent;

public class VideoListFragment extends WVFragment implements
		OnItemClickListener, MApiRequestHandler {

	private MApiRequest request;
	private Task task;
	private Adapter adapter;

	private String url;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		url = getArguments().getString("url");
		if (url == null) {
			url = getActivity().getIntent().getData().getQueryParameter("url");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.layout_list, null);
		ListView list = (ListView) view.findViewById(R.id.list);
		adapter = new Adapter();
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onDestroy() {
		if (request != null) {
			mapiService().abort(request, this, true);
		}
		if (task != null) {
			task.cancel(true);
		}
		super.onDestroy();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Object item = parent.getItemAtPosition(position);
		if (item instanceof Video) {
			String url = "http://v.youku.com/player/getM3U8/vid/"
					+ ((Video) item).id + "/type/mp4/v.m3u8";
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("wvideo://player?url=" + url));
			startActivity(intent);

			HashMap<String, String> map = new HashMap<String, String>();
			map.put("__ct__", String.valueOf(position));
			map.put("title", String.valueOf(((Video) item).title));
			MobclickAgent.onEvent(getActivity(), "video_list_item_click", map);
		}

	}

	class Task extends AsyncTask<String, Void, List<Video>> {

		@Override
		protected List<Video> doInBackground(String... params) {
			SeachResultParseHelper helper = SeachResultParseHelper.instance(getActivity());
			String json = helper.parse(params[0]);
			List<Video> result = new Gson().fromJson(json,
					new TypeToken<List<Video>>() {
					}.getType());
			return result;
		}

		@Override
		protected void onPostExecute(List<Video> result) {
			adapter.appendData(result);
		}

	}

	class Adapter extends BasicAdapter {

		List<Video> videoList = new ArrayList<Video>();
		int page = 1;
		String errorMsg;

		public void appendData(List<Video> videos) {
			if (videos == null) {
				setError("数据为空");

			} else {
				videoList.addAll(videos);
				notifyDataSetChanged();
			}
		}

		public void setError(String error) {
			errorMsg = error;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return videoList.size() + 1;
		}

		@Override
		public Object getItem(int position) {
			if (position < videoList.size()) {
				return videoList.get(position);
			}
			return errorMsg == null ? LOADING : ERROR;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Object item = getItem(position);
			if (item == LOADING) {
				if (task != null) {
					task.cancel(true);
				}
				task = new Task();
				if (request != null) {
					mapiService().abort(request, VideoListFragment.this, true);
				}
				request = BasicMApiRequest.mapiGet(url + "_page_" + page + "?",
						CacheType.NORMAL, null);
				mapiService().exec(request, VideoListFragment.this);

				page += 1;
				return getLoadingView(parent, convertView);

			} else if (item == ERROR) {
				return getFailedView(errorMsg, new OnClickListener() {

					@Override
					public void onClick(View v) {
						errorMsg = null;
						notifyDataSetChanged();
					}
				}, parent, convertView);

			} else {
				View view = convertView;
				if (!(view instanceof VideoListItem)) {
					view = getLayoutInflater(getArguments()).inflate(
							R.layout.layout_video_list_item, null);
				}
				((VideoListItem) view).setData((Video) item);
				return view;
			}
		}

	}

	@Override
	public void onRequestStart(MApiRequest req) {
	}

	@Override
	public void onRequestProgress(MApiRequest req, int count, int total) {

	}

	@Override
	public void onRequestFinish(MApiRequest req, MApiResponse resp) {
		if (resp.result() instanceof String) {
			if (task != null) {
				task.cancel(true);
			}
			task = new Task();
			task.execute((String) resp.result());
		}
	}

	@Override
	public void onRequestFailed(MApiRequest req, MApiResponse resp) {
		adapter.setError(resp.message().getErrorMsg());
	}

}
