package com.dennytech.common.util;

public class LinkedListNode<T> {

	public LinkedListNode<T> previous;
	public LinkedListNode<T> next;
	public T object;

	public long time;

	public LinkedListNode(T object, LinkedListNode<T> next,
			LinkedListNode<T> previous) {
		this.object = object;
		this.next = next;
		this.previous = previous;
	}

	public void remove() {
		previous.next = next;
		next.previous = previous;
	}

	public String toString() {
		return String.valueOf(object);
	}
}