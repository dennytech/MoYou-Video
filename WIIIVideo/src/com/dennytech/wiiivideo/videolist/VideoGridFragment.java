package com.dennytech.wiiivideo.videolist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.dennytech.wiiivideo.R;
import com.dennytech.wiiivideo.data.Video;
import com.dennytech.wiiivideo.videolist.view.VideoListItem;

public class VideoGridFragment extends VideoListFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.layout_grid, null);
		GridView grid = (GridView) view.findViewById(R.id.grid);
		adapter = createAdapter();
		grid.setAdapter(adapter);
		grid.setOnItemClickListener(this);
		return view;
	}

	@Override
	protected Adapter createAdapter() {
		return new GridAdapter();
	}

	class GridAdapter extends Adapter {

		@Override
		protected View createItemViewWithData(Video item, View convertView) {
			View view = convertView;
			if (!(view instanceof VideoListItem)) {
				view = getLayoutInflater(getArguments()).inflate(
						R.layout.layout_video_grid_item, null);
			}
			((VideoListItem) view).setData(item);
			return view;
		}
	}

}
