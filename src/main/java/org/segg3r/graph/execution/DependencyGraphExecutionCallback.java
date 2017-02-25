package org.segg3r.graph.execution;

public interface DependencyGraphExecutionCallback<T> {

	default void onNodeStarted(T node) {}
	default void processNode(T node) {}
	default void onNodeFinished(T node) {}
	default void onNodeFailed(T node) {}

}
