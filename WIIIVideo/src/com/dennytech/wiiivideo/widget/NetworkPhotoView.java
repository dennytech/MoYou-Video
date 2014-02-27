package com.dennytech.wiiivideo.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.dennytech.common.service.dataservice.Request;

public class NetworkPhotoView extends NetworkImageView {

	public NetworkPhotoView(Context context) {
		this(context, null);
	}

	public NetworkPhotoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NetworkPhotoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		isPhoto = true;
		progressTextColor = context.getResources().getColor(android.R.color.darker_gray);
	}

	protected Bitmap bitmap;
	int currentBytes;
	int totalBytes;
	String percent = " ";
	private Paint defaultPaint;
	private int progressTextColor;
	
	public void setProgressTextColor(int color) {
		this.progressTextColor = color;
	}

	@Override
	public void setDrawable(Drawable d, boolean isPlaceholder) {
		super.setDrawable(d, isPlaceholder);
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
		if (!isPlaceholder && d instanceof BitmapDrawable) {
			bitmap = ((BitmapDrawable) d).getBitmap();
		}
	}

	@Override
	protected boolean discard() {
		currentBytes = 0;
		totalBytes = 0;
		return super.discard();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (Boolean.FALSE.equals(imageRetrieve) && totalBytes > 0) {
			if (percent == "flag" || percent == "")
				percent = "";
			else
				percent = currentBytes * 100 / totalBytes + "%";
			if (defaultPaint == null) {
				defaultPaint = new Paint();
				defaultPaint.setAntiAlias(true);

				defaultPaint.setColor(progressTextColor);
				defaultPaint.setTextAlign(Paint.Align.CENTER);
				defaultPaint.setTextSize(12 * getResources()
						.getDisplayMetrics().density);
			}
			canvas.drawText(percent, getWidth() / 2, getHeight() / 2
					- defaultPaint.ascent(), defaultPaint);
		}
	}

	@Override
	public void onRequestProgress(Request req, int count, int total) {
		totalBytes = total;
		currentBytes = count;
		invalidate();
	}

}
