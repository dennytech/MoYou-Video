package com.dennytech.common.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * 支持拼接的输入流。可以将多个输入流组装到一个流里面。
 * <p>
 * 注：不支持mark操作
 * 
 * @author Jun.Deng
 *
 */
public class ChainInputStream extends InputStream {
	protected InputStream[] streams;
	protected int curoffset;
	protected int curs;

	public ChainInputStream(InputStream... inputStreams) {
		streams = inputStreams;
		curs = 0;
	}

	public InputStream[] streams() {
		return streams;
	}

	@Override
	public int available() throws IOException {
		int c = 0;
		for (int i = curs, n = streams.length; i < n; i++) {
			InputStream s = streams[i];
			int a = s.available();
			if (a <= 0)
				return 0;
			c += a;
		}
		return c - curoffset;
	}

	@Override
	public synchronized void close() throws IOException {
		for (InputStream s : streams) {
			s.close();
		}
		curoffset = 0;
		curs = 0;
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
		InputStream s = streams[curs];
		int r = s.read();
		if (r < 0) {
			if (curs < streams.length - 1) {
				curs++;
				curoffset = 0;
				return read();
			} else {
				return r;
			}
		} else {
			++curoffset;
			return r;
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		InputStream s = streams[curs];
		int r = s.read(b, off, len);
		if (r < 0) {
			if (curs < streams.length - 1) {
				curs++;
				curoffset = 0;
				return read(b, off, len);
			} else {
				return r;
			}
		} else {
			curoffset += r;
			return r;
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public synchronized void reset() throws IOException {
		for (InputStream s : streams) {
			s.reset();
		}
		curoffset = 0;
		curs = 0;
	}

	@Override
	public long skip(long n) throws IOException {
		throw new IOException("unsupported operation: skip");
	}
}
