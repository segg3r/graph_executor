package org.segg3r.graph.execution.step;

import org.segg3r.graph.execution.DependencyExecutionState;
import org.segg3r.graph.execution.DependencyGraphExecutionCallback;
import org.segg3r.graph.execution.DependencyGraphExecutor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

public class ParallelDependencyGraphExecutionStep<T, S extends DependencyGraphExecutionStep<T>> implements DependencyGraphExecutionStep<T> {

	public static <TT, SS extends DependencyGraphExecutionStep<TT>> ParallelDependencyGraphExecutionStep<TT, SS> parallelExecution(Set<SS> elements) {
		return new ParallelDependencyGraphExecutionStep<>(elements);
	}

	private Set<S> steps;

	private ParallelDependencyGraphExecutionStep(Set<S> steps) {
		this.steps = steps;
	}

	@Override
	public void execute(DependencyGraphExecutionCallback<T> callback) {
		List<CompletableFuture<?>> futures = steps.stream()
				.map(element -> CompletableFuture.runAsync(() -> element.execute(callback)))
				.collect(toList());

		futures.forEach(CompletableFuture::join);
	}

	@Override
	public void setState(DependencyExecutionState state) {
		steps.forEach(step -> step.setState(state));
	}

}
