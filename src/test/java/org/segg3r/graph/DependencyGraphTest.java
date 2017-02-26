package org.segg3r.graph;

import by.segg3r.testng.util.spring.SpringContextListener;
import org.mockito.InOrder;
import org.segg3r.graph.exception.CircularDependencyException;
import org.segg3r.graph.execution.DependencyGraphProcessingStepStateChange;
import org.segg3r.graph.execution.DependencyGraphProcessingCallback;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.function.Consumer;

import static by.segg3r.expectunit.Expect.expect;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.synchronizedList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.segg3r.graph.DependencyGraph.emptyGraph;
import static org.segg3r.graph.execution.DependencyGraphProcessingCallback.processingCallback;
import static org.segg3r.graph.execution.DependencyGraphProcessingStepState.*;
import static org.segg3r.graph.execution.DependencyGraphProcessingStepStateChange.stateChangeOf;

@Listeners(SpringContextListener.class)
public class DependencyGraphTest {

	@Test(description = "should add element to graph")
	public void testOneItem() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.add(givenEntity("one"));

		expect(graph.contains(givenEntity("one"))).toBeTruthy();
	}

	@Test(description = "should create simple parent->child connection")
	public void testParentChild() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(givenEntity("child"), givenEntity("parent"));

		expect(graph.find(givenEntity("child")).get().directlyDependsOn(givenEntity("parent"))).toBeTruthy();
		expect(graph.find(givenEntity("parent")).get().isDirectDependencyOf(givenEntity("child"))).toBeTruthy();
	}

	@Test(description = "should throw exception in case of circular dependency",
		expectedExceptions = CircularDependencyException.class)
	public void testCircularDependency() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(givenEntity("two"), givenEntity("one"));
		graph.addDependency(givenEntity("three"), givenEntity("two"));
		graph.addDependency(givenEntity("one"), givenEntity("three"));
	}

	@Test(description = "should resolve intermediate dependencies"
			+ " (e.g. 3->2 + 3->1 + 2->1 should remove 3->1 and only remain 3->2")
	public void testIntermediateDependencies() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(givenEntity("three"), givenEntity("two"));
		graph.addDependency(givenEntity("three"), givenEntity("one"));
		graph.addDependency(givenEntity("two"), givenEntity("one"));

		expect(graph.find(givenEntity("two")).get().directlyDependsOn(givenEntity("one"))).toBeTruthy();
		expect(graph.find(givenEntity("three")).get().directlyDependsOn(givenEntity("two"))).toBeTruthy();
		expect(graph.find(givenEntity("three")).get().directlyDependsOn(givenEntity("one"))).toBeFalsy();
		expect(graph.find(givenEntity("three")).get().dependsOn(givenEntity("one"))).toBeTruthy();
	}

	@Test(description = "should threat elements without dependencies as independent")
	public void testIndependency() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(givenEntity("two"), givenEntity("one"));
		graph.add(givenEntity("three"));

		expect(graph.find(givenEntity("two")).get().directlyDependsOn(givenEntity("one"))).toBeTruthy();
		expect(graph.find(givenEntity("three")).get().isIndependent()).toBeTruthy();
		expect(graph.find(givenEntity("three")).get().hasDependencies()).toBeFalsy();
	}

	@Test(description = "should process usual all parallel case")
	public void testAllParallel() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.add(givenEntity("one"));
		graph.add(givenEntity("two"));
		graph.add(givenEntity("three"));

		List<String> result = synchronizedList(newArrayList());
		graph.process(processingCallback(entity -> result.add(entity.getName())));

		expect(result).toContainOnly("one", "two", "three");
	}

	@Test(description = "should process elements in right order")
	public void testExecutionOrder() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(givenEntity("two"), givenEntity("one"));
		graph.addDependency(givenEntity("five"), givenEntity("three"));
		graph.addDependency(givenEntity("four"), givenEntity("two"));
		graph.addDependency(givenEntity("three"), givenEntity("one"));
		graph.addDependency(givenEntity("five"), givenEntity("four"));

		List<String> result = synchronizedList(newArrayList());
		graph.process(processingCallback(entity -> result.add(entity.getName())));

		expect(result.size()).toEqual(5);
		expect(result.get(0)).toEqual("one");
		expect(result.subList(1, 3)).toContainOnly("two", "three"); // can be any order
		expect(result.get(3)).toEqual("four");
		expect(result.get(4)).toEqual("five");
	}

	@Test(description = "should fail dependents")
	public void testFailDependents() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(givenEntity("two"), givenEntity("one"));
		graph.addDependency(givenEntity("five"), givenEntity("three"));
		graph.addDependency(givenEntity("four"), givenEntity("two"));
		graph.addDependency(givenEntity("three"), givenEntity("one"));
		graph.addDependency(givenEntity("five"), givenEntity("four"));

		List<String> result = synchronizedList(newArrayList());
		graph.process(givenFailingCallback("two", entity -> result.add(entity.getName())));

		expect(result.size()).toEqual(2);
		expect(result.get(0)).toEqual("one");
		expect(result.get(1)).toEqual("three");
	}

	@Test(description = "should trigger callbacks in correct order")
	public void testCallbacks() {
		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(givenEntity("two"), givenEntity("one"));
		graph.addDependency(givenEntity("three"), givenEntity("two"));
		graph.addDependency(givenEntity("four"), givenEntity("three"));

		EntityMockedCallback callbackMock = new EntityMockedCallback();

		graph.process(givenFailingCallback("three", callbackMock.processNodeCallback, callbackMock.stateChangeCallback));

		callbackMock.verifySuccessfullyFinished(givenEntity("one"));
		callbackMock.verifySuccessfullyFinished(givenEntity("two"));
		callbackMock.verifyNormallyFailed(givenEntity("three"));
		callbackMock.verifyFailedDependent(givenEntity("four"));
	}

	private static final class EntityMockedCallback {

		private Consumer<DependencyGraphProcessingStepStateChange<Entity>> stateChangeCallback = mock(Consumer.class);
		private Consumer<Entity> processNodeCallback = mock(Consumer.class);
		private InOrder order = inOrder(stateChangeCallback, processNodeCallback);

		private void verifySuccessfullyFinished(Entity entity) {
			order.verify(stateChangeCallback).accept(stateChangeOf(entity, RUNNING));
			order.verify(processNodeCallback).accept(entity);
			order.verify(stateChangeCallback).accept(stateChangeOf(entity, FINISHED));
		}

		public void verifyNormallyFailed(Entity entity) {
			order.verify(stateChangeCallback).accept(stateChangeOf(entity, RUNNING));
			order.verify(stateChangeCallback).accept(stateChangeOf(entity, FAILED));
		}

		public void verifyFailedDependent(Entity entity) {
			order.verify(stateChangeCallback).accept(stateChangeOf(entity, FAILED));
		}

	}

	private Entity givenEntity(String name) {
		return new Entity(name);
	}

	private DependencyGraphProcessingCallback<Entity> givenFailingCallback(
			String failingNodeName, Consumer<Entity> entityConsumer) {
		return givenFailingCallback(failingNodeName, entityConsumer, stateChange -> {
		});
	}

	private DependencyGraphProcessingCallback<Entity> givenFailingCallback(
			String failingNodeName, Consumer<Entity> entityConsumer, Consumer<DependencyGraphProcessingStepStateChange<Entity>> stateConsumer) {
		return processingCallback(entity -> {
			if (failingNodeName.equals(entity.getName())) {
				throw new RuntimeException();
			}
			entityConsumer.accept(entity);
		}, stateConsumer);
	}

}
