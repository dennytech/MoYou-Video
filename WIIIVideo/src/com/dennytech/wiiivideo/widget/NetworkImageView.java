package com.dennytech.wiiivideo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.dennytech.common.app.CLApplication;
import com.dennytech.common.service.dataservice.Request;
import com.dennytech.common.service.dataservice.RequestHandler;
import com.dennytech.common.service.dataservice.Response;
import com.dennytech.common.service.dataservice.image.ImageService;
import com.dennytech.common.service.dataservice.image.impl.ImageRequest;
import com.dennytech.common.util.BDUtils;
import com.dennytech.wiiivideo.R;

public class NetworkImageView extends ImageView implements
		RequestHandler<Request, Response> {
	private ImageService imageService;

	protected String url;
	public boolean requireBeforeAttach;
	/**
	 * default thumbnail
	 */
	public boolean isPhoto;

	protected boolean attached;
	protected Boolean imageRetrieve;
	protected ImageRequest request;

	protected Task localImageTask;

	public int placeholderEmpty, placeholderLoading, placeholderError;

	protected boolean hasSavedScaleType;
	protected ScaleType savedScaleType;
	protected boolean currentPlaceholder;

	public NetworkImageView(Context context) {
		this(context, null);
	}

	public NetworkImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NetworkImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.NetworkImageView);
		requireBeforeAttach = a.getBoolean(
				R.styleable.NetworkImageView_requireBeforeAttach, false);
		placeholderEmpty = a.getResourceId(
				R.styleable.NetworkImageView_placeholderEmpty, 0);
		placeholderLoading = a.getResourceId(
				R.styleable.NetworkImageView_placeholderLoading, 0);
		placeholderError = a.getResourceId(
				R.styleable.NetworkImageView_placeholderError, 0);
		a.recycle();
	}

	public void setLocalBitmap(Bitmap bmp) {
		setImageBitmap(bmp);

		url = "local_bitmap";
		imageRetrieve = true;
	}

	public void setPlaceHolder(int resId) {
		if (!currentPlaceholder) {
			if (!hasSavedScaleType) {
				savedScaleType = getScaleType();
				hasSavedScaleType = true;
			}
			setScaleType(ScaleType.CENTER_INSIDE);
		}
		currentPlaceholder = true;
		super.setImageResource(resId);
	}

	public void setImageBitmap(Bitmap bmp) {
		if (hasSavedScaleType) {
			setScaleType(savedScaleType);
		}
		currentPlaceholder = false;
		super.setImageBitmap(bmp);
	}

	public void setDrawable(Drawable d, boolean isPlaceholder) {
		if (currentPlaceholder != isPlaceholder) {
			if (isPlaceholder) {
				if (!hasSavedScaleType) {
					savedScaleType = getScaleType();
					hasSavedScaleType = true;
				}
				setScaleType(ScaleType.CENTER_INSIDE);
			} else {
				if (hasSavedScaleType) {
					setScaleType(savedScaleType);
				}
			}
		}
		currentPlaceholder = isPlaceholder;
		setImageDrawable(d);
		if (isPlaceholder && d instanceof AnimationDrawable) {
			((AnimationDrawable) d).start();
		}
	}

	protected ImageService imageService() {
		synchronized (NetworkImageView.class) {
			if (imageService == null) {
				imageService = (ImageService) CLApplication.instance()
						.getService("image");
			}
		}
		return imageService;
	}

	protected boolean require() {
		if (!(attached || requireBeforeAttach)) {
			return false;
		}
		if (url == null) {
			setPlaceHolder(placeholderEmpty);
			imageRetrieve = true;
			return true;
		}
		if (url != null && imageRetrieve == null) {
			setPlaceHolder(placeholderLoading);

			if (url.startsWith("http://") || url.startsWith("https://")) {
				request = new ImageRequest(url,
						isPhoto ? ImageRequest.TYPE_PHOTO
								: ImageRequest.TYPE_THUMBNAIL);
				imageService().exec(request, this);
			} else {
				localImageTask = new Task();
				localImageTask.execute();
			}
			imageRetrieve = false;
			return true;
		}

		return false;
	}

	protected boolean discard() {
		if (url != null && Boolean.FALSE.equals(imageRetrieve)) {
			if (url.startsWith("http://")) {
				if (request != null) {
					imageService().abort(request, this, true);
					request = null;
				}
			} else {
				if (localImageTask != null) {
					localImageTask.cancel(true);
					localImageTask = null;
				}
			}

			imageRetrieve = null;
			return true;
		}
		return false;
	}

	public void setImage(String url) {
		if (url != null && url.length() == 0)
			url = null;
		if (url == null && this.url == null)
			return;
		if (url != null && url.equals(this.url))
			return;
		discard();
		imageRetrieve = null;
		this.url = url;

		require();
		if (imageRetrieve == null) {
			setImageDrawable(null);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		attached = true;
		require();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		attached = false;
		discard();
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		if (visibility == View.VISIBLE) {
			attached = true;
			require();
		} else {
			attached = false;
			discard();
		}
	}

	@Override
	public void onRequestStart(Request req) {
	}

	@Override
	public void onRequestProgress(Request req, int count, int total) {
	}

	@Override
	public void onRequestFinish(Request req, Response response) {
		if (Boolean.FALSE == imageRetrieve && req == request) {
			Bitmap bmp = (Bitmap) response.result();
			setImageBitmap(bmp);
			imageRetrieve = true;
			request = null;
		}
	}

	@Override
	public void onRequestFailed(Request req, Response response) {
		if (Boolean.FALSE == imageRetrieve && req == request) {
			setPlaceHolder(placeholderError);
			imageRetrieve = true;
			request = null;
		}
	}

	class Task extends AsyncTask<Void, Void, Bitmap> {

		@Override
		protected Bitmap doInBackground(Void... params) {

			int sampling = 1;
			Bitmap bitmap = null;

			int imgSize = BDUtils.dip2px(getContext(), 76);

			try {

				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inJustDecodeBounds = true;

				BitmapFactory.decodeFile(url, opt);

				int size = opt.outWidth > opt.outHeight ? opt.outWidth
						: opt.outHeight;
				sampling = (size / imgSize) + 1;

			} catch (Exception e) {
				return null;
			}

			try {
				// FileInputStream ins = new FileInputStream(imgUri);
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inSampleSize = sampling;
				bitmap = BitmapFactory.decodeFile(url, opt);
				// ins.close();
			} catch (OutOfMemoryError oom) {
				System.gc();
				showToast("out of memory");
				return null;
			} catch (Exception e) {
				return null;
			}
			return bitmap;
		}

		Toast toast;

		protected void showToast(String msg) {
			if (toast == null) {
				toast = Toast.makeText(getContext(), msg, Toast.LENGTH_LONG);
			} else {
				toast.setText(msg);
			}
			toast.show();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result == null) {
				if (Boolean.FALSE == imageRetrieve && localImageTask == this) {
					setPlaceHolder(placeholderError);
					imageRetrieve = true;
					localImageTask = null;
				}
			} else {
				if (Boolean.FALSE == imageRetrieve && localImageTask == this) {

					setImageBitmap(result);
					imageRetrieve = true;
					localImageTask = null;
				}
			}
		}
	}

}
