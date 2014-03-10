package com.dennytech.common.service.dataservice.cache;

import android.database.Cursor;

public abstract class DatabaseCursor<E> implements DataCursor<E> {
	private final Cursor c;

	public DatabaseCursor(Cursor c) {
		this.c = c;
	}

	@Override
	public int getCount() {
		return c.getCount();
	}

	@Override
	public E getData() {
		return getData(c);
	}

	protected abstract E getData(Cursor c);

	@Override
	public int getPosition() {
		return c.getPosition();
	}

	@Override
	public boolean moveToPosition(int position) {
		return c.moveToPosition(position);
	}

	@Override
	public boolean moveToFirst() {
		return c.moveToFirst();
	}

	@Override
	public boolean moveToLast() {
		return c.moveToLast();
	}

	@Override
	public boolean moveToNext() {
		return c.moveToNext();
	}

	@Override
	public boolean moveToPrevious() {
		return c.moveToPrevious();
	}

	@Override
	public boolean move(int offset) {
		return c.move(offset);
	}

	@Override
	public void close() {
		c.close();
	}
}
