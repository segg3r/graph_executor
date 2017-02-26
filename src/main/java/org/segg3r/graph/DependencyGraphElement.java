package org.segg3r.graph;

import com.google.common.collect.Sets;
import org.segg3r.graph.exception.CircularDependencyException;

import java.util.Optional;
import java.util.Set;

import static org.segg3r.graph.DependencyGraphStringUtil.elementDescriptor;

public class DependencyGraphElement<T> {

	public static <T> DependencyGraphElement<T> emptyElement() {
		return new DependencyGraphElement<>(null, "'");
	}

	public static <T> DependencyGraphElement<T> emptyElementFromRotationWith(T entity) {
		return new DependencyGraphElement<>(null, entity.toString() + " '");
	}

	public static <T> DependencyGraphElement<T> elementWithEntity(T entity) {
		return new DependencyGraphElement<>(entity, entity.toString());
	}

	private final T entity;
	private final Set<DependencyGraphElement<T>> directDependencies = Sets.newHashSet();
	private final Set<DependencyGraphElement<T>> directDependents = Sets.newHashSet();
	private final String identifier;

	private DependencyGraphElement(T entity, String identifier) {
		this.entity = entity;
		this.identifier = identifier;
	}

	public boolean isIndependent() {
		return !getDependencies().stream()
				.filter(dependency -> dependency.getEntity().isPresent())
				.findAny()
				.isPresent();
	}

	public boolean hasDependencies() {
		return !isIndependent();
	}

	public boolean addDependent(DependencyGraphElement<T> dependent) {
		if (dependent.isDependencyOf(this)) throwCircularDependencyException(dependent, this);
		if (isDependencyOf(dependent)) return false;

		this.directDependents.add(dependent);
		dependent.getDependents().forEach(descendant -> descendant.removeDirectDependency(this));
		dependent.addDependency(this);

		return true;
	}

	public boolean addDependency(DependencyGraphElement<T> dependency) {
		if (dependency.dependsOn(this)) throwCircularDependencyException(dependency, this);
		if (dependsOn(dependency)) return false;

		this.directDependencies.add(dependency);
		dependency.getDependencies().forEach(ascendant -> ascendant.removeDirectDependent(this));
		dependency.addDependent(this);

		return true;
	}

	private void throwCircularDependencyException(DependencyGraphElement<T> first, DependencyGraphElement<T> second) {
		Object firstObject = first.getEntity().orElse(null);
		Object secondObject = second.getEntity().orElse(null);
		String message = "Could not create graph. Objects " + firstObject + " and " + secondObject + "have circular dependency on each other.";

		throw new CircularDependencyException(message, firstObject, secondObject);
	}

	public boolean removeDirectDependency(DependencyGraphElement<T> dependency) {
		boolean result = this.directDependencies.remove(dependency);
		if (result) dependency.removeDirectDependent(this);

		return result;
	}

	public boolean removeDirectDependent(DependencyGraphElement<T> dependent) {
		boolean result = this.directDependents.remove(dependent);
		if (result) dependent.removeDirectDependency(this);

		return result;
	}

	public boolean isDirectDependencyOf(DependencyGraphElement<T> dependent) {
		return this.directDependents.contains(dependent);
	}

	public boolean isDirectDependencyOf(T dependent) {
		return isDirectDependencyOf(elementWithEntity(dependent));
	}

	public boolean isDependencyOf(DependencyGraphElement<T> dependent) {
		return getDependents().contains(dependent);
	}

	public boolean isDependencyOf(T dependent) { return isDependencyOf(elementWithEntity(dependent)); }

	public boolean directlyDependsOn(DependencyGraphElement<T> dependency) {
		return this.directDependencies.contains(dependency);
	}

	public boolean directlyDependsOn(T dependency) {
		return directlyDependsOn(elementWithEntity(dependency));
	}

	public boolean dependsOn(DependencyGraphElement<T> dependency) {
		return getDependencies().contains(dependency);
	}

	public boolean dependsOn(T dependency) {
		return dependsOn(elementWithEntity(dependency));
	}

	public Set<DependencyGraphElement<T>> getDependencies() {
		Set<DependencyGraphElement<T>> ascendants = Sets.newHashSet();
		ascendants.addAll(this.directDependencies);

		for (DependencyGraphElement<T> parent : directDependencies) {
			ascendants.addAll(parent.getDependencies());
		}

		return ascendants;
	}

	public Set<DependencyGraphElement<T>> getDependents() {
		Set<DependencyGraphElement<T>> descendants = Sets.newHashSet();
		descendants.addAll(this.directDependents);

		for (DependencyGraphElement<T> child : directDependents) {
			descendants.addAll(child.getDependents());
		}

		return descendants;
	}

	public Set<DependencyGraphElement<T>> getDirectDependencies() {
		return directDependencies;
	}

	public Set<DependencyGraphElement<T>> getDirectDependents() {
		return directDependents;
	}

	public Optional<DependencyGraphElement<T>> findDependent(T dependent) {
		for (DependencyGraphElement<T> child : directDependents) {
			Optional<T> childEntity = child.getEntity();
			if (childEntity.isPresent() && childEntity.get().equals(dependent)) {
				return Optional.of(child);
			}

			Optional<DependencyGraphElement<T>> descendant = child.findDependent(dependent);
			if (descendant.isPresent()) {
				return descendant;
			}
		}

		return Optional.empty();
	}

	public boolean containsEntity() {
		return !isEmpty();
	}

	public boolean isEmpty() {
		return entity == null;
	}

	public Optional<T> getEntity() {
		return Optional.ofNullable(entity);
	}

	public String getIdentifier() {
		return identifier;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DependencyGraphElement<?> that = (DependencyGraphElement<?>) o;

		return identifier != null ? identifier.equals(that.identifier) : that.identifier == null;
	}

	@Override
	public int hashCode() {
		return identifier != null ? identifier.hashCode() : 0;
	}

	@Override
	public String toString() {
		return elementDescriptor(this);
	}
}
