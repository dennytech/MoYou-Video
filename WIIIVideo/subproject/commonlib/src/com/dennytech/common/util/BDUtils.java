package com.dennytech.common.util;

import java.security.MessageDigest;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class BDUtils {

	public static int dip2px(Context context, float dipValue) {
		if (context == null) {
			return (int) dipValue;
		}
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

	public static int px2dip(Context context, float pxValue) {
		if (context == null) {
			return (int) pxValue;
		}
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	public static float px2sp(Context context, Float px) {
		if (context == null) {
			return px.intValue();
		}
		float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
		return px / scaledDensity;
	}

	public static float sp2px(Context context, float sp) {
		if (context == null) {
			return sp;
		}
		Resources r = context.getResources();
		float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
				r.getDisplayMetrics());
		return size;
	}

	private static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
			sb.append(HEX_DIGITS[b[i] & 0x0f]);
		}
		return sb.toString();
	}

	public static String md5s(byte[] byteArray) {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			return "";
		}

		byte[] md5Bytes = md5.digest(byteArray);

		StringBuffer hexValue = new StringBuffer();

		for (int i = 0; i < md5Bytes.length; i++) {
			int val = ((int) md5Bytes[i]) & 0xff;
			if (val < 16)
				hexValue.append("0");
			hexValue.append(Integer.toHexString(val));
		}

		return hexValue.toString();
	}

}
