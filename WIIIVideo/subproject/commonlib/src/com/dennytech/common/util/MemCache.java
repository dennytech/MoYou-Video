package com.dennytech.common.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Key-Value缓存，提供内存清理和过期
 * <p>
 * 缓存速度很快，一般在主线程中执行<br>
 * 内存清理在使用量达到100%时触发，默认清理到85%<br>
 * 单个对象的大小默认为1，可以重载sizeOf()函数定制更精细的颗粒大小<br>
 * 过期在get, put, remove等操作时触发，你不会get到过期的对象<br>
 * 
 */
public class MemCache<K, V> implements Map<K, V> {
	protected final Map<K, VCont<K, V>> map = new HashMap<K, VCont<K, V>>(64);

	protected final LinkedList<K> accessList = new LinkedList<K>();;

	protected final LinkedList<K> ageList = new LinkedList<K>();;

	private int maxCacheSize;

	private int cacheSize = 0;

	private long maxLifetime;

	private volatile long cacheHits = 0L, cacheMisses = 0L;

	/**
	 * 构造一个标准的缓存容器
	 * 
	 * @param maxSize
	 *            如果不需要内存清理，设置为-1
	 * @param maxLifetime
	 *            如果不过期，设置为-1
	 */
	public MemCache(int maxSize, long maxLifetime) {
		this.maxCacheSize = maxSize;
		this.maxLifetime = maxLifetime;
	}

	@Override
	public synchronized V put(K key, V value) {
		int valSize = sizeOf(value), oldSize = 0;
		if (maxCacheSize > 0 && valSize > maxCacheSize / 2) {
			VCont<K, V> removed = map.remove(key);
			return removed == null ? null : removed.object;
		}

		VCont<K, V> vc = new VCont<K, V>(value, valSize);
		VCont<K, V> removed = map.put(key, vc);
		LinkedListNode<K> reuseAccess = null, reuseAge = null;
		if (removed != null) {
			removed.accessNode.remove();
			reuseAccess = removed.accessNode;
			removed.accessNode = null;
			removed.ageNode.remove();
			reuseAge = removed.ageNode;
			removed.ageNode = null;
			cacheSize -= removed.size;
		}
		cacheSize += valSize;

		long timestamp = System.currentTimeMillis();
		LinkedListNode<K> accessNode;
		if (reuseAccess == null) {
			accessNode = accessList.addFirst(key);
		} else {
			reuseAccess.object = key;
			accessNode = accessList.addFirst(reuseAccess);
		}
		accessNode.time = timestamp;
		vc.accessNode = accessNode;

		LinkedListNode<K> ageNode;
		if (reuseAge == null) {
			ageNode = ageList.addFirst(key);
		} else {
			reuseAge.object = key;
			ageNode = ageList.addFirst(reuseAge);
		}
		ageNode.time = timestamp;
		vc.ageNode = ageNode;

		if (oldSize < valSize)
			cleanFull();

		return removed == null ? null : removed.object;
	}

	@Override
	public synchronized V get(Object key) {
		cleanExpired();

		VCont<K, V> cacheObject = map.get(key);
		if (cacheObject == null) {
			++cacheMisses;
			return null;
		}

		++cacheHits;
		++(cacheObject.readCount);
		cacheObject.accessNode.time = System.currentTimeMillis();

		cacheObject.accessNode.remove();
		accessList.addFirst(cacheObject.accessNode);

		return cacheObject.object;
	}

	@Override
	public synchronized V remove(Object key) {
		VCont<K, V> removed = map.remove(key);
		if (removed == null) {
			return null;
		}

		removed.accessNode.remove();
		removed.accessNode = null;
		removed.ageNode.remove();
		removed.ageNode = null;
		cacheSize -= removed.size;

		return removed.object;
	}

	public synchronized void clear() {
		for (Object key : map.keySet().toArray()) {
			remove(key);
		}

		map.clear();
		accessList.clear();
		ageList.clear();

		cacheSize = 0;
	}

	public int size() {
		cleanExpired();
		return map.size();
	}

	public boolean isEmpty() {
		cleanExpired();
		return map.isEmpty();
	}

	public Set<Entry<K, V>> entrySet() {
		cleanExpired();
		return entrySet;
	}

	private final EntrySet entrySet = new EntrySet();

	private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public Iterator<java.util.Map.Entry<K, V>> iterator() {
			Iterator<Map.Entry<K, V>> itr = new Iterator<Entry<K, V>>() {
				final Iterator<K> itr = ageList.iterator();
				K nextKey;

				@Override
				public boolean hasNext() {
					synchronized (MemCache.this) {
						if (nextKey == null && itr.hasNext()) {
							nextKey = itr.next();
						}
						return nextKey != null;
					}
				}

				@Override
				public java.util.Map.Entry<K, V> next() {
					final K key = nextKey;
					nextKey = null;
					if (key == null)
						return null;
					return new Map.Entry<K, V>() {
						@Override
						public K getKey() {
							return key;
						}

						@Override
						public V getValue() {
							VCont<K, V> co = map.get(key);
							return co == null ? null : co.object;
						}

						@Override
						public V setValue(V object) {
							VCont<K, V> co = map.get(key);
							if (co == null)
								return null;
							V oldObj = co.object;
							co.object = object;
							return oldObj;
						}
					};
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
			return itr;
		}

		@Override
		public int size() {
			return map.size();
		}
	}

	@Override
	public Set<K> keySet() {
		cleanExpired();
		return keySet;
	}

	private final KeySet keySet = new KeySet();

	private final class KeySet extends AbstractSet<K> {
		@Override
		public Iterator<K> iterator() {
			Iterator<K> itr = new Iterator<K>() {
				final Iterator<K> itr = ageList.iterator();
				K nextKey;

				@Override
				public boolean hasNext() {
					synchronized (MemCache.this) {
						if (nextKey == null && itr.hasNext()) {
							nextKey = itr.next();
						}
						return nextKey != null;
					}
				}

				@Override
				public K next() {
					final K key = nextKey;
					nextKey = null;
					return key;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
			return itr;
		}

		@Override
		public int size() {
			return map.size();
		}
	}

	public Collection<V> values() {
		cleanExpired();
		return values;
	}

	private final ValueCollection values = new ValueCollection();

	private final class ValueCollection extends AbstractCollection<V> {
		@Override
		public Iterator<V> iterator() {
			Iterator<V> itr = new Iterator<V>() {
				final Iterator<K> itr = ageList.iterator();
				V nextVal;

				@Override
				public boolean hasNext() {
					synchronized (MemCache.this) {
						if (nextVal == null) {
							if (itr.hasNext()) {
								VCont<K, V> co = map.get(itr.next());
								nextVal = co == null ? null : co.object;
							}
						}
						return nextVal != null;
					}
				}

				@Override
				public V next() {
					final V val = nextVal;
					nextVal = null;
					return val;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
			return itr;
		}

		@Override
		public int size() {
			return map.size();
		}
	}

	public boolean containsKey(Object key) {
		cleanExpired();
		return map.containsKey(key);
	}

	public void putAll(Map<? extends K, ? extends V> map) {
		for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	public synchronized boolean containsValue(Object value) {
		cleanExpired();

		if (value == null) {
			Iterator<V> itr = values().iterator();
			while (itr.hasNext()) {
				if (itr.next() == null)
					return true;
			}
			return false;
		} else {
			Iterator<V> itr = values().iterator();
			while (itr.hasNext()) {
				if (value.equals(itr.next()))
					return true;
			}
			return false;
		}
	}

	public long getCacheHits() {
		return cacheHits;
	}

	public long getCacheMisses() {
		return cacheMisses;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public int getMaxCacheSize() {
		return maxCacheSize;
	}

	public void setMaxCacheSize(int maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
		cleanFull();
	}

	public long getMaxLifetime() {
		return maxLifetime;
	}

	public void setMaxLifetime(long maxLifetime) {
		this.maxLifetime = maxLifetime;
		cleanExpired();
	}

	protected int sizeOf(Object object) {
		return 1;
	}

	protected void cleanExpired() {
		if (maxLifetime <= 0)
			return;

		long expireTime = System.currentTimeMillis() - maxLifetime;
		LinkedListNode<K> node;
		while ((node = ageList.getLast()) != null) {
			if (expireTime > node.time)
				remove(node.object);
			else
				break;
		}
	}

	protected void cleanFull() {
		cleanFull(0.85);
	}

	protected void cleanFull(double percent) {
		if (maxCacheSize < 0)
			return;

		if (cacheSize >= maxCacheSize) {
			cleanExpired();
			int okaySize = (int) (maxCacheSize * percent);
			while (cacheSize > okaySize) {
				remove(accessList.getLast().object);
			}
		}
	}

	protected static class VCont<K, V> {
		public V object;
		public int size;
		public LinkedListNode<K> accessNode;
		public LinkedListNode<K> ageNode;
		public int readCount = 0;

		public VCont(V object, int size) {
			this.object = object;
			this.size = size;
		}
	}

	@Override
	public String toString() {
		return "MemCache, size=" + getCacheSize() + "/" + getMaxCacheSize()
				+ ", count=" + size() + ", lifetime=" + getMaxLifetime()
				+ ", hits=" + getCacheHits() + ", missses=" + getCacheMisses();
	}
}