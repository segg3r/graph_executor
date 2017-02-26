package org.segg3r.graph.execution;

public class DependencyGraphProcessingStepStateChange<T> {

	public static <TT> DependencyGraphProcessingStepStateChange<TT> stateChangeOf(TT node, DependencyGraphProcessingStepState state) {
		return new DependencyGraphProcessingStepStateChange<>(node, state);
	}

	private final T node;
	private final DependencyGraphProcessingStepState state;

	public DependencyGraphProcessingStepStateChange(T node, DependencyGraphProcessingStepState state) {
		this.node = node;
		this.state = state;
	}

	public T getNode() {
		return node;
	}

	public DependencyGraphProcessingStepState getState() {
		return state;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DependencyGraphProcessingStepStateChange<?> that = (DependencyGraphProcessingStepStateChange<?>) o;

		if (node != null ? !node.equals(that.node) : that.node != null) return false;
		return state == that.state;

	}

	@Override
	public int hashCode() {
		int result = node != null ? node.hashCode() : 0;
		result = 31 * result + (state != null ? state.hashCode() : 0);
		return result;
	}

}
