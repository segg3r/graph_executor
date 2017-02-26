package org.segg3r.graph.exception;

public class CircularDependencyException extends RuntimeException {

	private final Object first;
	private final Object second;

	public CircularDependencyException(String message, Object first, Object second) {
		super(message);
		this.first = first;
		this.second = second;
	}

	public Object getFirst() {
		return first;
	}

	public Object getSecond() {
		return second;
	}

}
