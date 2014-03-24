package com.dennytech.wiiivideo.playlist;import java.util.ArrayList;import java.util.List;import android.os.Bundle;import android.view.LayoutInflater;import android.view.View;import android.view.ViewGroup;import android.widget.AdapterView;import android.widget.AdapterView.OnItemClickListener;import android.widget.ListView;import com.dennytech.common.adapter.BasicAdapter;import com.dennytech.wiiivideo.R;import com.dennytech.wiiivideo.app.MYFragment;import com.dennytech.wiiivideo.data.Playlist;public class PlaylistFragment extends MYFragment implements OnItemClickListener {	protected ListView listView;	protected Adapter adapter;	@Override	public View onCreateView(LayoutInflater inflater, ViewGroup container,			Bundle savedInstanceState) {		View view = inflater.inflate(R.layout.layout_list, null);		listView = (ListView) view.findViewById(R.id.list);		adapter = new Adapter();		listView.setAdapter(adapter);		listView.setOnItemClickListener(this);		return view;	}	@Override	public void onItemClick(AdapterView<?> parent, View view, int position,			long id) {	}	class Adapter extends BasicAdapter {		List<Playlist> plList = new ArrayList<Playlist>();		@Override		public int getCount() {			return 10;		}		@Override		public Object getItem(int position) {			return null;		}		@Override		public long getItemId(int position) {			return position;		}		@Override		public View getView(int position, View convertView, ViewGroup parent) {			View view = convertView;			if (view == null) {				view = LayoutInflater.from(getActivity()).inflate(						R.layout.layout_playlist_list_item, null);			}			return view;		}	}}