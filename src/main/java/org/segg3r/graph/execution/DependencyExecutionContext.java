package org.segg3r.graph.execution;

import com.google.common.collect.Maps;
import org.segg3r.graph.DependencyGraph;
import org.segg3r.graph.DependencyGraphElement;
import org.segg3r.graph.execution.step.SingleDependencyGraphExecutionStep;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class DependencyExecutionContext<T> {

	private Map<DependencyGraphElement<T>, SingleDependencyGraphExecutionStep<T>> executionStepsCache =
			Maps.newHashMap();

	public DependencyExecutionContext(DependencyGraph<T> graph) {
		DependencyGraphElement<T> head = graph.getHead();
		createStep(head);
		head.getDependents().forEach(this::createStep);
	}

	public SingleDependencyGraphExecutionStep<T> getStep(DependencyGraphElement<T> element) {
		return executionStepsCache.get(element);
	}

	public Set<SingleDependencyGraphExecutionStep<T>> getSteps(Set<DependencyGraphElement<T>> elements) {
		return elements.stream()
				.map(this::getStep)
				.collect(toSet());
	}

	private void createStep(DependencyGraphElement<T> element) {
		executionStepsCache.put(element, new SingleDependencyGraphExecutionStep<T>(element, this));
	}

}
