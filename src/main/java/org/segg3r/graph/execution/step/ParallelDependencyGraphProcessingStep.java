package org.segg3r.graph.execution.step;

import org.segg3r.graph.execution.DependencyGraphProcessingStepState;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

public class ParallelDependencyGraphProcessingStep<T, S extends DependencyGraphProcessingStep<T>> implements DependencyGraphProcessingStep<T> {

	public static <TT, SS extends DependencyGraphProcessingStep<TT>> ParallelDependencyGraphProcessingStep<TT, SS> parallelExecution(Set<SS> elements) {
		return new ParallelDependencyGraphProcessingStep<>(elements);
	}

	private Set<S> steps;

	private ParallelDependencyGraphProcessingStep(Set<S> steps) {
		this.steps = steps;
	}

	@Override
	public void execute() {
		List<CompletableFuture<?>> futures = steps.stream()
				.map(element -> CompletableFuture.runAsync(() -> element.execute()))
				.collect(toList());

		futures.forEach(CompletableFuture::join);
	}

	@Override
	public void setState(DependencyGraphProcessingStepState state) {
		steps.forEach(step -> step.setState(state));
	}

}
