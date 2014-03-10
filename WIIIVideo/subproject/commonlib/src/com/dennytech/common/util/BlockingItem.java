package com.dennytech.common.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一个线程安全的对象包装器。一般在多线程并发操作一个对象时使用。
 * <p>
 * 注：BlockingItem使用了可重入锁ReentrantLock来处理资源的并发，默认是非公平的（先take的不一定先拿到）。
 * 
 * @author Jun.Deng
 * 
 * @param <T>
 *            需要被包装的对象类型
 */
public class BlockingItem<T> {
	final Lock lock = new ReentrantLock();
	final Condition notEmpty = lock.newCondition();

	private volatile T item;

	public void put(T x) {
		lock.lock();
		try {
			item = x;
			if (x != null)
				notEmpty.signal();
		} finally {
			lock.unlock();
		}
	}

	public T take() throws InterruptedException {
		lock.lock();
		try {
			while (item == null)
				notEmpty.await();
			T t = item;
			item = null;
			return t;
		} finally {
			lock.unlock();
		}
	}

	public T tryTake(long waitMs) throws InterruptedException {
		lock.lock();
		try {
			while (item == null)
				if (!notEmpty.await(waitMs, TimeUnit.MILLISECONDS))
					return null;
			T t = item;
			item = null;
			return t;
		} finally {
			lock.unlock();
		}
	}

	public T peek() {
		return item;
	}
}