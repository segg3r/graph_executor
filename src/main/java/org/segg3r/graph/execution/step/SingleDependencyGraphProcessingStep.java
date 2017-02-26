package org.segg3r.graph.execution.step;

import com.google.common.collect.Lists;
import org.segg3r.graph.DependencyGraphElement;
import org.segg3r.graph.execution.DependencyGraphProcessingContext;
import org.segg3r.graph.execution.DependencyGraphProcessingStepState;
import org.segg3r.graph.execution.DependencyGraphProcessingCallback;

import java.util.Queue;
import java.util.Set;

import static org.segg3r.graph.execution.DependencyGraphProcessingStepState.*;

public class SingleDependencyGraphProcessingStep<T> implements DependencyGraphProcessingStep<T> {

	private final DependencyGraphElement<T> element;
	private final DependencyGraphProcessingContext<T> context;
	private DependencyGraphProcessingStepState state = WAITING;

	public SingleDependencyGraphProcessingStep(DependencyGraphElement<T> element, DependencyGraphProcessingContext<T> context) {
		this.element = element;
		this.context = context;
	}

	@Override
	public void execute() {
		if (!isReadyForExecution()) return;

		if (element.isEmpty()) {
			state = FINISHED;
			return;
		}

		T entity = element.getEntity().get();
		DependencyGraphProcessingCallback<T> callback = context.getCallback();

		state = RUNNING;
		callback.onNodeStateChanged(entity, RUNNING);
		try {
			callback.processNode(entity);
			state = FINISHED;
			callback.onNodeStateChanged(entity, FINISHED);
		} catch (Exception e) {
			state = FAILED;
			callback.onNodeStateChanged(entity, FAILED);

			getDependents().forEach(dependent -> {
				dependent.setState(FAILED);
				dependent.element.getEntity().ifPresent(element -> callback.onNodeStateChanged(element, FAILED));
			});
		}
	}

	public Queue<DependencyGraphProcessingStep<T>> getExecutionQueue() {
		Set<SingleDependencyGraphProcessingStep<T>> directDependents = getDirectDependents();

		Queue<DependencyGraphProcessingStep<T>> queue = Lists.newLinkedList();
		if (directDependents.isEmpty()) return queue;

		queue.add(ParallelDependencyGraphProcessingStep.parallelExecution(directDependents));
		directDependents.forEach(dependent -> queue.addAll(dependent.getExecutionQueue()));

		return queue;
	}

	@Override
	public void setState(DependencyGraphProcessingStepState state) {
		this.state = state;
	}

	public boolean isReadyForExecution() {
		return isWaiting()
				&& !hasUnfinishedDependencies();
	}

	public boolean hasUnfinishedDependencies() {
		Set<SingleDependencyGraphProcessingStep<T>> directDependencies = getDirectDependencies();

		return directDependencies.stream()
				.filter(dependency -> !dependency.isFinished())
				.findAny()
				.isPresent();
	}

	private Set<SingleDependencyGraphProcessingStep<T>> getDirectDependents() {
		return context.getSteps(element.getDirectDependents());
	}

	private Set<SingleDependencyGraphProcessingStep<T>> getDirectDependencies() {
		return context.getSteps(element.getDirectDependencies());
	}

	private Set<SingleDependencyGraphProcessingStep<T>> getDependents() {
		return context.getSteps(element.getDependents());
	}

	public boolean isWaiting() {
		return state == WAITING;
	}

	public boolean isFinished() {
		return state == FINISHED;
	}

	@Override
	public String toString() {
		return element.toString() + " : " + state;
	}

}
