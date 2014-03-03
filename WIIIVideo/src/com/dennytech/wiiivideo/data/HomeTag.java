package com.dennytech.wiiivideo.data;

import android.os.Parcel;
import android.os.Parcelable;

public class HomeTag implements Parcelable {

	public String title;
	public String icon;
	public String keyword;

	public HomeTag(String title, String icon, String keyword) {
		this.title = title;
		this.icon = icon;
		this.keyword = keyword;
	}

	public HomeTag(Parcel in) {
		title = in.readString();
		icon = in.readString();
		keyword = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeString(icon);
		dest.writeString(keyword);
	}

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public HomeTag createFromParcel(Parcel in) {
			return new HomeTag(in);
		}

		public HomeTag[] newArray(int size) {
			return new HomeTag[size];
		}
	};

}
