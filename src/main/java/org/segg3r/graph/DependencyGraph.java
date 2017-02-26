package org.segg3r.graph;

import com.google.common.collect.Lists;
import org.segg3r.graph.execution.DependencyGraphProcessingContext;
import org.segg3r.graph.execution.DependencyGraphProcessingCallback;
import org.segg3r.graph.execution.step.DependencyGraphProcessingStep;
import org.segg3r.graph.execution.step.SingleDependencyGraphProcessingStep;

import java.util.Optional;
import java.util.Queue;

import static org.segg3r.graph.DependencyGraphElement.*;

public class DependencyGraph<T> {

	public static <TT> DependencyGraph<TT> emptyGraph() {
		return new DependencyGraph<>(emptyElement());
	}

	private DependencyGraphElement<T> head;

	private DependencyGraph(DependencyGraphElement<T> head) {
		this.head = head;
	}

	public void process(DependencyGraphProcessingCallback<T> callback) {
		DependencyGraphProcessingContext<T> context = new DependencyGraphProcessingContext<>(this, callback);
		SingleDependencyGraphProcessingStep<T> headExecutionStep = context.getStep(getHead());

		Queue<DependencyGraphProcessingStep<T>> executionQueue = Lists.newLinkedList();
		executionQueue.add(headExecutionStep);
		executionQueue.addAll(headExecutionStep.getExecutionQueue());

		while (!executionQueue.isEmpty()) {
			executionQueue.poll().execute();
		}
	}

	public DependencyGraphElement<T> getHead() {
		return head;
	}

	public DependencyGraphElement<T> add(T entity) {
		Optional<DependencyGraphElement<T>> existingElement = find(entity);
		if (existingElement.isPresent()) return existingElement.get();

		return attachToHead(entity);
	}

	public Optional<DependencyGraphElement<T>> find(T entity) {
		Optional<T> headEntity = head.getEntity();
		if (headEntity.isPresent() && headEntity.get().equals(entity)) {
			return Optional.of(head);
		}

		return head.findDependent(entity);
	}

	private DependencyGraphElement<T> attachToHead(T entity) {
		DependencyGraphElement<T> entityElement = elementWithEntity(entity);
		this.head.addDependent(entityElement);

		return entityElement;
	}

	public void addDependency(T dependent, T dependency) {
		DependencyGraphElement<T> parentElement = add(dependency);
		DependencyGraphElement<T> childElement = find(dependent).orElse(elementWithEntity(dependent));
		parentElement.addDependent(childElement);
	}

	public boolean contains(T entity) {
		return find(entity).isPresent();
	}

	@Override
	public String toString() {
		return "Head: " + head;
	}

}
