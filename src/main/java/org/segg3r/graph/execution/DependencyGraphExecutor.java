package org.segg3r.graph.execution;

import com.google.common.collect.Lists;
import org.segg3r.graph.DependencyGraph;
import org.segg3r.graph.execution.step.DependencyGraphExecutionStep;
import org.segg3r.graph.execution.step.SingleDependencyGraphExecutionStep;

import java.util.Queue;

public abstract class DependencyGraphExecutor<T> implements DependencyGraphExecutionCallback<T> {

	public void executeGraph(DependencyGraph<T> graph) {
		DependencyExecutionContext<T> context = new DependencyExecutionContext<>(graph);
		SingleDependencyGraphExecutionStep<T> headExecutionStep = context.getStep(graph.getHead());

		Queue<DependencyGraphExecutionStep<T>> executionQueue = Lists.newLinkedList();
		executionQueue.add(headExecutionStep);
		executionQueue.addAll(headExecutionStep.getExecutionQueue());

		while (!executionQueue.isEmpty()) {
			executionQueue.poll().execute(this);
		}
	}

}
