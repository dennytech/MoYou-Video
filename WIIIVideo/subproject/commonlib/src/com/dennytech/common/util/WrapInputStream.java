package com.dennytech.common.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream的包装器。
 * <p>
 * 子类可以实现各种表现类型（如String,
 * Map等），在实际读取数据时（一般发生在背景线程中），wrappedInputStream()方法负责创建被包装的真实InputStream。
 * </p>
 * 
 * @author Jun.Deng
 * 
 */
public abstract class WrapInputStream extends InputStream {
	private InputStream ins;
	private IOException ex;
	private int marks;

	/**
	 * 创建被包装的真实InputStream
	 * 
	 * @return
	 * @throws IOException
	 */
	protected abstract InputStream wrappedInputStream() throws IOException;

	private synchronized InputStream inputStream() throws IOException {
		if (ex != null)
			throw ex;
		if (ins == null) {
			try {
				ins = wrappedInputStream();
			} catch (IOException e) {
				ex = e;
				throw ex;
			}
		}
		return ins;
	}

	@Override
	public int available() throws IOException {
		return inputStream().available();
	}

	@Override
	public void close() throws IOException {
		if (ins == null)
			return;
		inputStream().close();
	}

	@Override
	public void mark(int readlimit) {
		InputStream input = ins;
		if (input != null) {
			input.mark(readlimit);
			marks++;
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		if (marks == 0) {
			ins = null;
		} else {
			inputStream().reset();
			marks--;
		}
	}

	@Override
	public boolean markSupported() {
		InputStream input = ins;
		if (input == null) {
			return true;
		} else {
			return input.markSupported();
		}
	}

	@Override
	public int read() throws IOException {
		return inputStream().read();
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		return inputStream().read(buffer, offset, length);
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		return inputStream().read(buffer);
	}

	@Override
	public long skip(long byteCount) throws IOException {
		return inputStream().skip(byteCount);
	}
}
