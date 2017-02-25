package org.segg3r.graph.execution.step;

import com.google.common.collect.Lists;
import org.segg3r.graph.DependencyGraphElement;
import org.segg3r.graph.execution.DependencyExecutionContext;
import org.segg3r.graph.execution.DependencyExecutionState;
import org.segg3r.graph.execution.DependencyGraphExecutionCallback;

import java.util.Queue;
import java.util.Set;

import static org.segg3r.graph.execution.DependencyExecutionState.*;

public class SingleDependencyGraphExecutionStep<T> implements DependencyGraphExecutionStep<T> {

	private final DependencyGraphElement<T> element;
	private final DependencyExecutionContext<T> context;
	private DependencyExecutionState state = WAITING;

	public SingleDependencyGraphExecutionStep(DependencyGraphElement<T> element, DependencyExecutionContext<T> context) {
		this.element = element;
		this.context = context;
	}

	@Override
	public void execute(DependencyGraphExecutionCallback<T> callback) {
		if (!isReadyForExecution()) return;

		if (element.isEmpty()) {
			state = FINISHED;
			return;
		}

		T entity = element.getEntity().get();

		state = RUNNING;
		callback.onNodeStarted(entity);
		try {
			callback.processNode(entity);
			state = FINISHED;
			callback.onNodeFinished(entity);
		} catch (Exception e) {
			state = FAILED;
			callback.onNodeFailed(entity);

			getDependents().forEach(dependent -> {
				dependent.setState(FAILED);
				dependent.element.getEntity().ifPresent(callback::onNodeFailed);
			});
		}
	}

	public Queue<DependencyGraphExecutionStep<T>> getExecutionQueue() {
		Set<SingleDependencyGraphExecutionStep<T>> directDependents = getDirectDependents();

		Queue<DependencyGraphExecutionStep<T>> queue = Lists.newLinkedList();
		if (directDependents.isEmpty()) return queue;

		queue.add(ParallelDependencyGraphExecutionStep.parallelExecution(directDependents));
		directDependents.forEach(dependent -> queue.addAll(dependent.getExecutionQueue()));

		return queue;
	}

	@Override
	public void setState(DependencyExecutionState state) {
		this.state = state;
	}

	public boolean isReadyForExecution() {
		return isWaiting()
				&& !hasUnfinishedDependencies();
	}

	public boolean hasUnfinishedDependencies() {
		Set<SingleDependencyGraphExecutionStep<T>> directDependencies = getDirectDependencies();

		return directDependencies.stream()
				.filter(dependency -> !dependency.isFinished())
				.findAny()
				.isPresent();
	}

	private Set<SingleDependencyGraphExecutionStep<T>> getDirectDependents() {
		return context.getSteps(element.getDirectDependents());
	}

	private Set<SingleDependencyGraphExecutionStep<T>> getDirectDependencies() {
		return context.getSteps(element.getDirectDependencies());
	}

	private Set<SingleDependencyGraphExecutionStep<T>> getDependents() {
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
