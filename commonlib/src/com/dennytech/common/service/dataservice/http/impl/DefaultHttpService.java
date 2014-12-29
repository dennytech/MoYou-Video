package com.dennytech.common.service.dataservice.http.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.SystemClock;

import com.dennytech.common.service.dataservice.RequestHandler;
import com.dennytech.common.service.dataservice.http.FormInputStream;
import com.dennytech.common.service.dataservice.http.HttpRequest;
import com.dennytech.common.service.dataservice.http.HttpResponse;
import com.dennytech.common.service.dataservice.http.HttpService;
import com.dennytech.common.service.dataservice.http.NetworkInfoHelper;
import com.dennytech.common.util.Log;
import com.dennytech.common.util.MyTask;

/**
 * 最基本的HTTP服务。包含自动WAP代理，不包含缓存等其他逻辑，不提供进度通知。
 * 
 * @author Jun.Deng
 * 
 */
public class DefaultHttpService implements HttpService {
	private static final String TAG = "http";
	private Context context;
	private Executor executor;
	private NetworkInfoHelper networkInfo;
	private final ConcurrentLinkedQueue<HttpClient> httpClients = new ConcurrentLinkedQueue<HttpClient>();
	private final ConcurrentHashMap<HttpRequest, Task> runningTasks = new ConcurrentHashMap<HttpRequest, DefaultHttpService.Task>();

	public DefaultHttpService(Context context, Executor executor) {
		this.context = context;
		this.executor = executor;
		this.networkInfo = new NetworkInfoHelper(this.context);
	}

	public synchronized void close() {
		// TODO:
	}

	protected Task createTask(HttpRequest req,
			RequestHandler<HttpRequest, HttpResponse> handler) {
		return new Task(req, handler);
	}

	protected boolean isLoggable() {
		return Log.isLoggable(Log.DEBUG);
	}

	protected void log(String str) {
		Log.d(TAG, str);
	}

	@Override
	public void exec(HttpRequest req,
			RequestHandler<HttpRequest, HttpResponse> handler) {
		Task task = createTask(req, handler);
		Task prev = runningTasks.putIfAbsent(req, task);
		if (prev == null) {
			task.executeOnExecutor(executor);
		} else {
			Log.e(TAG, "cannot exec duplicate request (same instance)");
		}
	}

	@Override
	public HttpResponse execSync(HttpRequest req) {
		HttpResponse resp = createTask(req, null).doInBackground();
		return resp;
	}

	@Override
	public void abort(HttpRequest req,
			RequestHandler<HttpRequest, HttpResponse> handler,
			boolean mayInterruptIfRunning) {
		Task task = runningTasks.get(req);
		if (task != null && task.handler == handler) {
			runningTasks.remove(req, task);
			task.cancel(mayInterruptIfRunning);
		}
	}

	public int runningTasks() {
		return runningTasks.size();
	}

	protected HttpClient createHttpClient() {
		return new DefaultHttpClient();
	}

	private HttpClient getHttpClient() {
		HttpClient hc = httpClients.poll();
		if (hc == null) {
			hc = createHttpClient();
		}
		return hc;
	}

	private void recycleHttpClient(HttpClient hc) {
		if (httpClients.size() < 4) {
			httpClients.add(hc);
		}
	}

	protected HttpParams getDefaultHttpParams() {
		BasicHttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		return params;
	}

	protected class MyHttpClient extends DefaultHttpClient implements
			HttpRequestInterceptor {

		public MyHttpClient() {
			super(getDefaultHttpParams());
			addRequestInterceptor(this);
		}

		@Override
		public void process(org.apache.http.HttpRequest request,
				HttpContext context) throws HttpException, IOException {
			if (!request.containsHeader("Accept-Encoding")) {
				request.addHeader("Accept-Encoding", "gzip");
			}
		}

	}

	protected class Task extends MyTask<Void, Void, HttpResponse> implements
			WatchedInputStream.Listener {
		protected final HttpRequest req;
		protected final RequestHandler<HttpRequest, HttpResponse> handler;
		protected HttpUriRequest request;

		protected int statusCode;

		protected boolean isUploadProgress;
		// GET
		protected int contentLength;
		protected int receivedBytes;
		// POST and PUT
		protected int availableBytes;
		protected int sentBytes;
		protected long prevProgressTime;

		// time
		protected long startTime;

		public Task(HttpRequest req,
				RequestHandler<HttpRequest, HttpResponse> handler) {
			this.req = req;
			this.handler = handler;
		}

		@Override
		protected void onPreExecute() {
			handler.onRequestStart(req);
			startTime = SystemClock.elapsedRealtime();
		}

		/**
		 * 创建Http请求。子类可以重载该方法对输入流进行加密相关操作。
		 * 
		 * @return
		 * @throws Exception
		 */
		protected HttpUriRequest getUriRequest() throws Exception {
			HttpUriRequest request;
			if (BasicHttpRequest.GET.equals(req.method())) {
				request = new HttpGet(req.url());
			} else if (BasicHttpRequest.POST.equals(req.method())) {
				HttpPost post = new HttpPost(req.url());
				InputStream ins = req.input();
				if (ins != null) {
					if (ins instanceof FormInputStream) {
						post.setEntity(new UrlEncodedFormEntity(
								((FormInputStream) ins).form(), "UTF-8"));
					} else {
						availableBytes = ins.available();
						sentBytes = 0;
						if (availableBytes > 4096) {
							WatchedInputStream wis = new WatchedInputStream(
									ins, 4096);
							wis.setListener(this);
							ins = wis;
							isUploadProgress = true;
						}
						post.setEntity(new InputStreamEntity(ins, ins
								.available()));
					}
				}
				request = post;
			} else if (BasicHttpRequest.PUT.equals(req.method())) {
				HttpPut put = new HttpPut(req.url());
				InputStream ins = req.input();
				if (ins != null) {
					availableBytes = ins.available();
					sentBytes = 0;
					if (availableBytes > 4096) {
						WatchedInputStream wis = new WatchedInputStream(ins,
								4096);
						wis.setListener(this);
						ins = wis;
						isUploadProgress = true;
					}
					put.setEntity(new InputStreamEntity(ins, ins.available()));
				}
				request = put;
			} else if (BasicHttpRequest.DELETE.equals(req.method())) {
				request = new HttpDelete(req.url());
			} else {
				throw new IllegalArgumentException("unknown http method "
						+ req.method());
			}
			if (req.headers() != null) {
				for (NameValuePair e : req.headers()) {
					request.setHeader(e.getName(), e.getValue());
				}
			}
			HttpHost proxy = networkInfo.getProxy();
			ConnRouteParams.setDefaultProxy(request.getParams(), proxy);
			return request;
		}

		@Override
		public HttpResponse doInBackground(Void... params) {
			HttpClient httpClient = null;
			InputStream resetStream = null;
			try {
				InputStream input = this.req.input();
				if (input != null && input.markSupported()) {
					input.mark(64 * 0x100); // 64k
					resetStream = input;
				}
				this.request = getUriRequest();

				httpClient = getHttpClient();
				org.apache.http.HttpResponse response = httpClient
						.execute(request);

				statusCode = response.getStatusLine().getStatusCode();

				HttpEntity entity = response.getEntity();
				byte[] buffer;
				{
					long len = entity.getContentLength();
					if (len > Integer.MAX_VALUE || len < 0)
						len = -1;
					contentLength = (int) len;
					receivedBytes = 0;
				}

				String streamType = null;
				if ((response.getHeaders("Content-Encoding") != null)) {
					Header[] headers = response.getHeaders("Content-Encoding");
					for (int i = 0; i < headers.length; ++i) {
						if (headers[i].getValue() != null
								&& headers[i].getValue().length() > 0) {
							streamType = headers[i].getValue();
							break;
						}
					}
				}

				if (BasicHttpRequest.GET.equals(req.method())
						&& contentLength >= 4096 && handler != null) {
					ByteArrayBuffer buf = new ByteArrayBuffer(contentLength);
					byte[] tmp = new byte[4096];
					int l;
					long pt = 0;
					InputStream ins = entity.getContent();
					if (streamType != null && "gzip".equals(streamType)) {
						ins = new GZIPInputStream(ins);
					}
					while ((l = ins.read(tmp, 0, tmp.length)) != -1) {
						buf.append(tmp, 0, l);
						receivedBytes += l;
						if (receivedBytes >= contentLength) {
							publishProgress();
						} else {
							long ct = SystemClock.elapsedRealtime();
							if (ct - pt > 200) {
								publishProgress();
								pt = ct;
							}
						}
					}
					buffer = buf.toByteArray();
					ins.close();

				} else {
					buffer = EntityUtils.toByteArray(entity);
					contentLength = buffer.length;
					if (streamType != null && "gzip".equals(streamType)) {
						ByteArrayInputStream bis = new ByteArrayInputStream(
								buffer);
						GZIPInputStream gzips = new GZIPInputStream(bis);
						byte[] buf = new byte[4096];
						int l;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						while ((l = gzips.read(buf, 0, buf.length)) != -1) {
							baos.write(buf, 0, l);
						}
						buffer = baos.toByteArray();
						baos.flush();
						baos.close();
						gzips.close();
						bis.close();
					}
				}
				entity.consumeContent();
				ArrayList<NameValuePair> headers = new ArrayList<NameValuePair>(
						8);
				for (Header h : response.getAllHeaders()) {
					headers.add(new BasicNameValuePair(h.getName(), h
							.getValue()));
				}
				return new BasicHttpResponse(statusCode, buffer, headers, null);
			} catch (Exception e) {
				return new BasicHttpResponse(0, null, null, e);
			} finally {
				if (httpClient != null) {
					recycleHttpClient(httpClient);
				}
				if (resetStream != null) {
					try {
						resetStream.reset();
					} catch (Exception e) {
					}
				}
			}
		}

		@Override
		public void notify(int read) {
			if (handler == null || !isUploadProgress)
				return;
			sentBytes += read;
			if (sentBytes >= availableBytes) {
				publishProgress();
			} else {
				long ct = SystemClock.elapsedRealtime();
				if (ct - prevProgressTime > 200) {
					publishProgress();
					prevProgressTime = ct;
				}
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			if (isUploadProgress) {
				handler.onRequestProgress(req, sentBytes, availableBytes);
			} else {
				handler.onRequestProgress(req, receivedBytes, contentLength);
			}
		}

		@Override
		protected void onCancelled() {
			if (isLoggable()) {
				long elapse = SystemClock.elapsedRealtime() - startTime;
				StringBuilder sb = new StringBuilder();
				sb.append("abort (");
				sb.append(req.method()).append(',');
				sb.append(statusCode).append(',');
				sb.append(elapse).append("ms");
				sb.append(") ").append(req.url());
				log(sb.toString());

				if (req.input() instanceof FormInputStream) {
					FormInputStream form = (FormInputStream) req.input();
					log("    " + form.toString());
				}
			}
			if (request != null) {
				request.abort();
			}
		}

		@Override
		protected void onPostExecute(HttpResponse result) {
			if (runningTasks.remove(req, this)) {
				if (result.result() != null) {
					handler.onRequestFinish(req, result);
				} else {
					handler.onRequestFailed(req, result);
				}
				if (isLoggable()) {
					long elapse = SystemClock.elapsedRealtime() - startTime;
					StringBuilder sb = new StringBuilder();
					if (result.result() != null) {
						sb.append("finish (");
					} else {
						sb.append("fail (");
					}
					sb.append(req.method()).append(',');
					sb.append(statusCode).append(',');
					sb.append(elapse).append("ms");
					sb.append(") ").append(req.url());
					log(sb.toString());

					if (req.input() instanceof FormInputStream) {
						FormInputStream form = (FormInputStream) req.input();
						log("    " + form.toString());
					}

					if (result.result() == null) {
						log("    " + result.error());
					}
				}
			}
		}
	}
}
