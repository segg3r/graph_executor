package org.segg3r.graph;

import java.util.Optional;

import static org.segg3r.graph.DependencyGraphElement.*;

public class DependencyGraph<T> {

	public static <TT> DependencyGraph<TT> emptyGraph() {
		return new DependencyGraph<>(emptyElement());
	}

	private DependencyGraphElement<T> head;

	private DependencyGraph(DependencyGraphElement<T> head) {
		this.head = head;
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
