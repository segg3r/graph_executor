package org.segg3r.graph.execution;

import java.util.function.Consumer;

import static org.segg3r.graph.execution.DependencyGraphProcessingStepStateChange.stateChangeOf;

public interface DependencyGraphProcessingCallback<T> {

	static <TT> DependencyGraphProcessingCallback<TT> processingCallback(Consumer<TT> nodeConsumer) {
		return processingCallback(nodeConsumer, stateChange -> {});
	}

	static <TT> DependencyGraphProcessingCallback<TT> processingCallback(
			Consumer<TT> nodeConsumer, Consumer<DependencyGraphProcessingStepStateChange<TT>> stateConsumer) {
		return new DependencyGraphProcessingCallback<TT>() {
			@Override
			public void processNode(TT node) {
				nodeConsumer.accept(node);
			}

			@Override
			public void onNodeStateChanged(TT node, DependencyGraphProcessingStepState state) {
				stateConsumer.accept(stateChangeOf(node, state));
			}
		};
	}

	default void processNode(T node) {}
	default void onNodeStateChanged(T node, DependencyGraphProcessingStepState state) {}

}
