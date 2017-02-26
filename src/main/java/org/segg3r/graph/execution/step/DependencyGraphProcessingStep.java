package org.segg3r.graph.execution.step;

import org.segg3r.graph.execution.DependencyGraphProcessingStepState;

public interface DependencyGraphProcessingStep<T> {

	void execute();

	void setState(DependencyGraphProcessingStepState state);

}
