package org.segg3r.graph;

import java.util.Set;

import static java.util.stream.Collectors.joining;

public class DependencyGraphStringUtil {

	public static <T> String elementsToString(Set<DependencyGraphElement<T>> elements) {
		return "["
				+ elements.stream()
						.map(DependencyGraphElement::getIdentifier)
						.collect(joining(", "))
				+ "]";
	}

	public static <T> String elementDescriptor(DependencyGraphElement<T> element) {
		return elementsToString(element.getDirectDependencies())
				+ " <-- " + element.getIdentifier()
				+ " <-- " + elementsToString(element.getDirectDependents());
	}

}
