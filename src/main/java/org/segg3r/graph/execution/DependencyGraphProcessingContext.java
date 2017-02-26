package org.segg3r.graph.execution;

import com.google.common.collect.Maps;
import org.segg3r.graph.DependencyGraph;
import org.segg3r.graph.DependencyGraphElement;
import org.segg3r.graph.execution.step.SingleDependencyGraphProcessingStep;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class DependencyGraphProcessingContext<T> {

	private final DependencyGraphProcessingCallback<T> callback;
	private Map<DependencyGraphElement<T>, SingleDependencyGraphProcessingStep<T>> executionStepsCache =
			Maps.newHashMap();

	public DependencyGraphProcessingContext(DependencyGraph<T> graph, DependencyGraphProcessingCallback<T> callback) {
		DependencyGraphElement<T> head = graph.getHead();
		createStep(head);
		head.getDependents().forEach(this::createStep);

		this.callback = callback;
	}

	public SingleDependencyGraphProcessingStep<T> getStep(DependencyGraphElement<T> element) {
		return executionStepsCache.get(element);
	}

	public Set<SingleDependencyGraphProcessingStep<T>> getSteps(Set<DependencyGraphElement<T>> elements) {
		return elements.stream()
				.map(this::getStep)
				.collect(toSet());
	}

	public DependencyGraphProcessingCallback<T> getCallback() {
		return callback;
	}

	private void createStep(DependencyGraphElement<T> element) {
		executionStepsCache.put(element, new SingleDependencyGraphProcessingStep<>(element, this));
	}

}
