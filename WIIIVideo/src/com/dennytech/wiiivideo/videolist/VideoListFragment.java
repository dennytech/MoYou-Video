package com.dennytech.wiiivideo.videolist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.dennytech.common.adapter.BasicAdapter;
import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.app.WVFragment;
import com.dennytech.wiiivideo.data.Video;
import com.dennytech.wiiivideo.videolist.view.VideoListItem;

public class VideoListFragment extends WVFragment implements
		OnItemClickListener {

	private Task task;
	private Adapter adapter;

	private int order;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		order = getArguments().getInt("order");
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
		}

	}

	class Task extends AsyncTask<String, Void, List<Video>> {

		@Override
		protected List<Video> doInBackground(String... params) {
			List<Video> videos = new ArrayList<Video>();
			try {
				Document doc = Jsoup.connect(params[0]).userAgent("Mozilla")
						.get();
				Element vlist = doc.getElementsByClass("sk-vlist").get(0);
				Elements v = vlist.getElementsByClass("v");
				for (Element element : v) {
					Video video = new Video();
					Element vthumb = element.getElementsByClass("v-thumb").get(
							0);
					video.thumb = vthumb.getElementsByTag("img").attr("src");
					video.length = vthumb.getElementsByClass("v-time").get(0)
							.childNode(0).toString();

					Element vmeta = element.getElementsByClass("v-meta").get(0);
					video.title = vmeta.getElementsByTag("a").get(0)
							.attr("title");
					video.id = vmeta.getElementsByTag("a").get(0).attr("href")
							.replace("http://v.youku.com/v_show/id_", "")
							.replace(".html", "");

					Elements vmetadata = vmeta
							.getElementsByClass("v-meta-data");
					video.playTimes = vmetadata.get(1).getElementsByTag("span")
							.text();
					video.publishTime = vmetadata.get(2)
							.getElementsByTag("span").text();

					videos.add(video);
				}

			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			return videos;
		}

		@Override
		protected void onPostExecute(List<Video> result) {
			adapter.appendData(result);
		}

	}

	class Adapter extends BasicAdapter {

		List<Video> videoList = new ArrayList<Video>();
		int page = 1;

		public void appendData(List<Video> videos) {
			videoList.addAll(videos);
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
			return LOADING;
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
				task.execute("http://www.soku.com/search_video/q_%E9%AD%94%E5%85%BD%E4%BA%89%E9%9C%B83_orderby_"
						+ order + "_page_" + page + "?");
				page += 1;
				return getLoadingView(parent, convertView);
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

}
