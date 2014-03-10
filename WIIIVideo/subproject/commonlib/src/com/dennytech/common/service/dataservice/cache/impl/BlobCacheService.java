package com.dennytech.common.service.dataservice.cache.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dennytech.common.service.dataservice.Request;
import com.dennytech.common.service.dataservice.RequestHandler;
import com.dennytech.common.service.dataservice.cache.CacheResponse;
import com.dennytech.common.service.dataservice.cache.CacheService;
import com.dennytech.common.service.dataservice.cache.DataCursor;
import com.dennytech.common.service.dataservice.cache.DatabaseCursor;
import com.dennytech.common.util.Daemon;
import com.dennytech.common.util.Log;

/**
 * 存储byte[]的SQLite数据库实现
 * <p>
 * 基于时间和对象个数管理，不提供自动清理和过期功能
 * 
 * @author Jun.Deng
 * 
 */
public class BlobCacheService implements CacheService {
	private static final String TAG = "cache";
	// TABLE: K:TEXT, T:INT8, V:BLOB
	private SQLiteDatabase db;

	/**
	 * 当该值>0时，表示有线程正在使用数据库，此时调用{@link #close()}会等待其他线程释放数据库的持有
	 */
	private final AtomicInteger ops;

	private boolean drain;
	private final String cacheName;
	private final AtomicInteger count = new AtomicInteger();
	private Statement queryTimeStat, deleteStat, touchStat, updateStat;
	private DatabaseUtils.InsertHelper insertHelper;
	private int iKey, iTime, iVal;
	private final ConcurrentHashMap<Request, Session> runningSession = new ConcurrentHashMap<Request, BlobCacheService.Session>();

	private static class Session {
		public Request request;
		public RequestHandler<Request, CacheResponse> handler;
		public CacheResponse response;

		public Session(Request r, RequestHandler<Request, CacheResponse> h) {
			request = r;
			handler = h;
		}
	}

	/**
	 * db传null时，整个cache不工作
	 */
	public BlobCacheService(SQLiteDatabase db, String cacheName) {
		this.db = db;
		this.cacheName = cacheName;
		this.ops = new AtomicInteger();

		if (db == null)
			return;

		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ").append(cacheName).append(" (");
		sb.append("K TEXT PRIMARY KEY, ");
		sb.append("T INT8, ");
		sb.append("V BLOB);");

		db.execSQL(sb.toString());

		Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + cacheName, null);
		try {
			if (c.moveToFirst()) {
				count.set(c.getInt(0));
			}
		} finally {
			c.close();
		}

		queryTimeStat = new Statement("SELECT T FROM " + cacheName
				+ " WHERE K=?");

		deleteStat = new Statement("DELETE FROM " + cacheName + " WHERE K=?");

		touchStat = new Statement("UPDATE " + cacheName + " SET T=? WHERE K=?");

		updateStat = new Statement("UPDATE " + cacheName
				+ " SET T=?,V=? WHERE K=?");

		insertHelper = new DatabaseUtils.InsertHelper(db, cacheName);
		iKey = insertHelper.getColumnIndex("K");
		iTime = insertHelper.getColumnIndex("T");
		iVal = insertHelper.getColumnIndex("V");
	}

	private SQLiteDatabase retain() {
		synchronized (ops) {
			if (drain)
				return null;
			ops.incrementAndGet();
			return db;
		}
	}

	private void release(SQLiteDatabase db) {
		synchronized (ops) {
			ops.decrementAndGet();
		}
	}

	@Override
	public long getTime(Request key) {
		return getTime(key.url());
	}

	public long getTime(String key) {
		SQLiteDatabase db = retain();
		if (db == null)
			return -1;
		SQLiteStatement stmt = null;
		try {
			stmt = queryTimeStat.create();
			stmt.bindString(1, key);
			long l = stmt.simpleQueryForLong();
			return l;
		} catch (Exception e) {
			return -1;
		} finally {
			if (stmt != null)
				queryTimeStat.dispose(stmt);
			release(db);
		}
	}

	@Override
	public Object get(Request key) {
		return get(key.url());
	}

	public Object get(String key) {
		SQLiteDatabase db = retain();
		if (db == null)
			return null;
		try {
			String sql = "SELECT V FROM " + cacheName + " WHERE K=\"" + key
					+ "\"";
			Cursor c = db.rawQuery(sql, null);
			if (c.moveToFirst()) {
				byte[] blob = c.getBlob(0);
				c.close();
				return blob;
			} else {
				c.close();
				return null;
			}
		} catch (Exception e) {
			return null;
		} finally {
			release(db);
		}
	}

	@Override
	public void remove(Request key) {
		remove(key.url());
	}

	public void remove(String key) {
		SQLiteDatabase db = retain();
		if (db == null)
			return;
		SQLiteStatement stmt = null;
		try {
			stmt = deleteStat.create();
			stmt.bindString(1, key);
			long row = stmt.executeInsert();
			if (row > 0)
				count.decrementAndGet();
		} catch (Exception e) {
		} finally {
			if (stmt != null)
				deleteStat.dispose(stmt);
			release(db);
		}
	}

	/**
	 * 添加操作，不替换已有内容
	 * 
	 * @param key
	 * @param val
	 * @param time
	 * @return 如果该Key已存在或操作失败，返回false
	 */
	public boolean insert(String key, byte[] val, long time) {
		SQLiteDatabase db = retain();
		if (db == null)
			return false;
		synchronized (insertHelper) {
			try {
				insertHelper.prepareForInsert();
				insertHelper.bind(iKey, key);
				insertHelper.bind(iTime, time);
				insertHelper.bind(iVal, val);
				if (insertHelper.execute() < 0) {
					return false;
				} else {
					count.incrementAndGet();
					return true;
				}
			} catch (Exception e) {
				return false;
			} finally {
				release(db);
			}
		}
	}

	public int count() {
		return count.get();
	}

	@Override
	public boolean put(Request key, Object val, long time) {
		return put(key.url(), val, time);
	}

	public boolean put(String key, Object val, long time) {
		if (val instanceof byte[]) {
			if (getTime(key) < 0) {
				return insert(key, (byte[]) val, time);
			} else {
				SQLiteDatabase db = retain();
				if (db == null)
					return false;
				SQLiteStatement stmt = null;
				try {
					stmt = updateStat.create();
					stmt.bindLong(1, time);
					stmt.bindBlob(2, (byte[]) val);
					stmt.bindString(3, key);
					long row = stmt.executeInsert();
					return row > 0;
				} catch (Exception e) {
					return false;
				} finally {
					if (stmt != null)
						updateStat.dispose(stmt);
					release(db);
				}
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean touch(Request key, long time) {
		return touch(key.url(), time);
	}

	public boolean touch(String key, long time) {
		SQLiteDatabase db = retain();
		if (db == null)
			return false;
		SQLiteStatement stmt = null;
		try {
			stmt = touchStat.create();
			stmt.bindLong(1, time);
			stmt.bindString(2, key);
			long row = stmt.executeInsert();
			return row > 0;
		} catch (Exception e) {
			return false;
		} finally {
			if (stmt != null)
				touchStat.dispose(stmt);
			release(db);
		}
	}

	@Override
	public void clear() {
		SQLiteDatabase db = retain();
		if (db == null)
			return;
		try {
			db.delete(cacheName, "1", null);
			count.set(0);
		} catch (Exception e) {
		} finally {
			release(db);
		}
	}

	public synchronized void close() {
		synchronized (ops) {
			drain = true;
		}
		while (ops.get() > 0) {
			Thread.yield();
		}
		if (drain || db == null)
			return;
		try {
			insertHelper.close();
			queryTimeStat.close();
			deleteStat.close();
			touchStat.close();
			updateStat.close();
			db.close();
		} catch (Exception e) {
		}
		db = null;
	}

	/**
	 * 返回Key的遍历器，按时间顺序排列（最早的到最近的）<br>
	 * !!线程不安全!!
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public DataCursor<String> getIteratorByTime() {
		if (drain || db == null)
			return DataCursor.EMPTY_CURSOR;
		String query = "SELECT K FROM " + cacheName + " ORDER BY T ASC";
		Cursor c = db.rawQuery(query, null);
		return new DatabaseCursor<String>(c) {
			@Override
			protected String getData(Cursor c) {
				return c.getString(0);
			}
		};
	}

	/**
	 * 按时间顺序裁剪缓存，使包含的对象个数小于指定的值<br>
	 * 最早的对象最先被删除
	 * 
	 * @param expected
	 *            希望被裁剪到的个数
	 * @return 实际被裁剪的个数
	 */
	public synchronized int trimToCount(int expected) {
		SQLiteDatabase db = retain();
		if (db == null)
			return 0;
		final int d = count() - expected;
		if (d <= 0)
			return 0;
		long time;
		try {
			String sql = "SELECT T FROM " + cacheName
					+ " ORDER BY T ASC LIMIT 1 OFFSET " + d;
			Cursor c = db.rawQuery(sql, null);
			if (c.moveToFirst()) {
				time = c.getLong(0);
				c.close();
			} else {
				c.close();
				return 0;
			}
		} catch (Exception e) {
			return 0;
		} finally {
			release(db);
		}
		return trimToTime(time);
	}

	/**
	 * 按时间裁剪缓存，小于该时间的对象均被删除
	 * 
	 * @param expected
	 *            希望被裁剪的时间，小于该值的均被删除
	 * @return 实际被裁剪的个数
	 */
	public synchronized int trimToTime(long expected) {
		SQLiteDatabase db = retain();
		if (db == null)
			return 0;
		try {
			int i = db.delete(cacheName, "T < " + expected, null);
			if (i > 0) {
				count.addAndGet(-i);
			}
			return i;
		} catch (Exception e) {
			return 0;
		} finally {
			release(db);
		}
	}

	@Override
	public BlobCacheResponse execSync(Request req) {
		SQLiteDatabase db = retain();
		if (db == null)
			return new BlobCacheResponse(0, null, "db closed");
		try {
			Cursor c = db.rawQuery("SELECT T,V FROM " + cacheName
					+ " WHERE K=\"" + req.url() + "\"", null);
			if (c.moveToFirst()) {
				long time = c.getLong(0);
				byte[] blob = c.getBlob(1);
				c.close();
				return new BlobCacheResponse(time, blob, null);
			} else {
				c.close();
				return new BlobCacheResponse(0, null, "not found: " + req.url());
			}
		} catch (Exception e) {
			return new BlobCacheResponse(0, null, e);
		} finally {
			release(db);
		}
	}

	@Override
	public void exec(Request req, RequestHandler<Request, CacheResponse> handler) {
		Session session = new Session(req, handler);
		Session prevs = runningSession.putIfAbsent(req, session);
		if (prevs == null) {
			handler.onRequestStart(req);
			this.dhandler.sendMessage(this.dhandler.obtainMessage(0, session));
		} else {
			Log.e(TAG, "cannot exec duplicate request (same instance)");
		}
	}

	@Override
	public void abort(Request req,
			RequestHandler<Request, CacheResponse> handler,
			boolean mayInterruptIfRunning) {
		Session session = runningSession.get(req);
		if (session != null && session.handler == handler) {
			runningSession.remove(req, session);
		}
	}

	public int runningCount() {
		return runningSession.size();
	}

	private final Handler dhandler = new Handler(Daemon.looper()) {
		@Override
		public void handleMessage(Message msg) {
			Session s = (Session) msg.obj;
			s = runningSession.get(s.request);
			if (s == null) {
				// canceled
				return;
			}
			CacheResponse resp = execSync(s.request);
			s.response = resp;
			mhandler.sendMessage(mhandler.obtainMessage(0, s));
		}
	};

	private final Handler mhandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			Session s = (Session) msg.obj;
			s = runningSession.remove(s.request);
			if (s == null) {
				// canceled
				return;
			}
			CacheResponse resp = s.response;
			if (resp.result() != null) {
				// finished
				s.handler.onRequestFinish(s.request, resp);
				Log.d(TAG, "[hit cache] " + s.request);
			} else {
				// failed;
				s.handler.onRequestFailed(s.request, resp);
				Log.d(TAG, "[miss cache] " + resp.error());
			}
		}
	};

	private class Statement {
		private final String sql;
		private SQLiteStatement stmt;

		public Statement(String sql) {
			this.sql = sql;
		}

		public SQLiteStatement create() {
			SQLiteStatement s = null;
			synchronized (this) {
				if (stmt != null) {
					s = stmt;
					stmt = null;
					return s;
				}
			}
			s = db.compileStatement(sql);
			return s;
		}

		public void dispose(SQLiteStatement s) {
			synchronized (this) {
				if (stmt == null) {
					stmt = s;
					return;
				}
			}
			s.close();
		}

		public void close() {
			synchronized (this) {
				if (stmt != null) {
					stmt.close();
					stmt = null;
				}
			}
		}
	}
}
