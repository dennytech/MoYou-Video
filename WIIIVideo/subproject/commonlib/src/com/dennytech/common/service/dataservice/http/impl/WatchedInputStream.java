package com.dennytech.common.service.dataservice.http.impl;

import java.io.IOException;
import java.io.InputStream;

/**
 * 支持监视进度的输入流包装器。一般在POST和PUT的时候使用，在超过一定大小的数据时开启监视。
 * <p>
 * 注：不支持mark
 * 
 * @author Jun.Deng
 * 
 */
public class WatchedInputStream extends InputStream {

	public static interface Listener {
		/**
		 * @param read
		 *            读取到的数据包大小，大多数情况应该和notifyBytes相等
		 */
		void notify(int read);
	}

	private InputStream stream;
	private int remains;
	private int notifyBytes;
	private Listener listener;

	/**
	 * 构造函数。
	 * 
	 * @param instream
	 *            真实的输入流
	 * @param notifyBytes
	 *            触发通知的数据包大小
	 */
	public WatchedInputStream(InputStream instream, int notifyBytes) {
		this.stream = instream;
		this.notifyBytes = notifyBytes;
	}

	@Override
	public int available() throws IOException {
		return stream.available();
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read() throws IOException {
		int r = stream.read();
		if (r < 0 || ++remains > notifyBytes) {
			int sr = remains;
			remains = 0;
			if (listener != null)
				listener.notify(sr);
		}
		return r;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int r = stream.read(b, off, len);
		if (r < 0) {
			int sr = remains;
			remains = 0;
			if (listener != null)
				listener.notify(sr);
			return r;
		}
		remains += r;
		if (remains > notifyBytes) {
			int sr = remains;
			remains = remains % notifyBytes;
			if (listener != null)
				listener.notify(sr - remains);
		}
		return r;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException("not supported operation: reset");
	}

	@Override
	public long skip(long n) throws IOException {
		throw new IOException("not supported operation: skip");
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
}
