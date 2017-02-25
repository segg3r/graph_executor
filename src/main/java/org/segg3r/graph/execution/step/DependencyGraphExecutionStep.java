package org.segg3r.graph.execution.step;

import org.segg3r.graph.execution.DependencyExecutionState;
import org.segg3r.graph.execution.DependencyGraphExecutionCallback;
import org.segg3r.graph.execution.DependencyGraphExecutor;

public interface DependencyGraphExecutionStep<T> {

	void execute(DependencyGraphExecutionCallback<T> callback);

	void setState(DependencyExecutionState state);

}
