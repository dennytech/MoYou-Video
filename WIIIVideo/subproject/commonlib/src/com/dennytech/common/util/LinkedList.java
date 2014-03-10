package com.dennytech.common.util;

import java.util.Iterator;

public class LinkedList<T> implements Iterable<T> {

	private LinkedListNode<T> head;

	public LinkedList() {
		head = new LinkedListNode<T>(null, null, null);
		head.next = head.previous = head;
	}

	public LinkedListNode<T> getFirst() {
		LinkedListNode<T> node = head.next;
		if (node == head) {
			return null;
		}
		return node;
	}

	public LinkedListNode<T> getLast() {
		LinkedListNode<T> node = head.previous;
		if (node == head) {
			return null;
		}
		return node;
	}

	public LinkedListNode<T> addFirst(LinkedListNode<T> node) {
		node.next = head.next;
		node.previous = head;
		node.previous.next = node;
		node.next.previous = node;
		return node;
	}

	public LinkedListNode<T> addFirst(T object) {
		LinkedListNode<T> node = new LinkedListNode<T>(object, head.next, head);
		node.previous.next = node;
		node.next.previous = node;
		return node;
	}

	public LinkedListNode<T> addLast(LinkedListNode<T> node) {
		node.next = head;
		node.previous = head.previous;
		node.previous.next = node;
		node.next.previous = node;
		return node;
	}

	public LinkedListNode<T> addLast(T object) {
		LinkedListNode<T> node = new LinkedListNode<T>(object, head,
				head.previous);
		node.previous.next = node;
		node.next.previous = node;
		return node;
	}

	public void clear() {
		LinkedListNode<T> node = getLast();
		while (node != null) {
			node.remove();
			node = getLast();
		}

		head.next = head.previous = head;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			LinkedListNode<T> node = head;

			@Override
			public boolean hasNext() {
				return node.next != head;
			}

			@Override
			public T next() {
				node = node.next;
				return node.object;
			}

			@Override
			public void remove() {
				node.remove();
			}
		};
	}

	public Iterator<LinkedListNode<T>> nodeIterator() {
		return new Iterator<LinkedListNode<T>>() {
			LinkedListNode<T> node = head;

			@Override
			public boolean hasNext() {
				return node.next != head;
			}

			@Override
			public LinkedListNode<T> next() {
				node = node.next;
				return node;
			}

			@Override
			public void remove() {
				node.remove();
			}
		};
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		boolean b = false;
		for (T obj : this) {
			if (b) {
				sb.append(", ");
			} else {
				sb.append('[');
				b = true;
			}
			sb.append(String.valueOf(obj));
		}
		sb.append(']');
		return sb.toString();
	}
}