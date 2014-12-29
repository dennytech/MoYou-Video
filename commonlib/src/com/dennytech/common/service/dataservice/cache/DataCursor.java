package com.dennytech.common.service.dataservice.cache;

public interface DataCursor<E> {

	/**
	 * 返回cursor中的行数。
	 * 
	 * @see {@link android.database.Cursor#getCount}
	 * @return cursor中的行数
	 */
	int getCount();

	/**
	 * 返回当前cursor的位置。
	 * 
	 * @see {@link android.database.Cursor#getPosition}
	 * @return 当前cursor的位置
	 */
	int getPosition();

	/**
	 * 相对当前cursor位置做偏移。
	 * 
	 * @param offset
	 *            偏移量
	 * 
	 * @see {@link android.database.Cursor#move}
	 * @return 偏移操作是否成功
	 */
	boolean move(int offset);

	/**
	 * 移动cursor到指定位置。
	 * 
	 * @param position
	 * 
	 * @see {@link android.database.Cursor#moveToPosition}
	 * @return 操作是否成功
	 */
	boolean moveToPosition(int position);

	/**
	 * 移动cursor到第一行。
	 * 
	 * @see {@link android.database.Cursor#moveToFirst}
	 * @return 移动是否成功
	 */
	boolean moveToFirst();

	/**
	 * 移动cursor到最后一行。
	 * 
	 * @see {@link android.database.Cursor#moveToLast}
	 * @return 移动是否成功
	 */
	boolean moveToLast();

	/**
	 * 移动cursor到下一行。
	 * 
	 * @see {@link android.database.Cursor#moveToNext}
	 * @return 移动是否成功
	 */
	boolean moveToNext();

	/**
	 * 移动cursor到前一行。
	 * 
	 * @see {@link android.database.Cursor#moveToPrevious}
	 * @return 移动是否成功
	 */
	boolean moveToPrevious();

	/**
	 * 返回当前cursor的数据
	 * 
	 * @return
	 */
	E getData();

	/**
	 * 关闭cursor。
	 * 
	 * @see {@link android.database.Cursor#close}
	 */
	void close();

	@SuppressWarnings("rawtypes")
	public static final DataCursor EMPTY_CURSOR = new DataCursor() {
		@Override
		public void close() {
		}

		@Override
		public int getCount() {
			return 0;
		}

		@Override
		public Object getData() {
			return null;
		}

		@Override
		public int getPosition() {
			return -1;
		}

		@Override
		public boolean moveToPosition(int position) {
			return false;
		}

		@Override
		public boolean moveToFirst() {
			return false;
		}

		@Override
		public boolean moveToLast() {
			return false;
		}

		@Override
		public boolean moveToNext() {
			return false;
		}

		@Override
		public boolean moveToPrevious() {
			return false;
		}

		@Override
		public boolean move(int offset) {
			return false;
		}
	};
}
