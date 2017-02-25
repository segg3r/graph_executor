package org.segg3r.graph;

import by.segg3r.testng.util.spring.SpringContextListener;
import com.google.common.collect.Lists;
import org.mockito.InOrder;
import org.segg3r.graph.execution.DependencyGraphExecutor;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.function.Consumer;

import static by.segg3r.expectunit.Expect.expect;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.segg3r.graph.DependencyGraph.emptyGraph;

@Listeners(SpringContextListener.class)
public class DependencyGraphTest {

	@Test(description = "should add element to graph")
	public void testOneItem() {
		Entity entity = new Entity("head");

		DependencyGraph<Entity> graph = emptyGraph();
		graph.add(entity);

		expect(graph.contains(entity)).toBeTruthy();
	}

	@Test(description = "should create simple parent->child connection")
	public void testParentChild() {
		Entity parent = new Entity("parent");
		Entity child = new Entity("child");

		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(child, parent);

		expect(graph.find(child).get().directlyDependsOn(parent)).toBeTruthy();
		expect(graph.find(parent).get().isDirectDependencyOf(child)).toBeTruthy();
	}

	@Test(description = "should resolve intermediate dependencies"
		+ " (e.g. 3->2 + 3->1 + 2->1 should remove 3->1 and only remain 3->2")
	public void testIntermediateDependencies() {
		Entity one = new Entity("one");
		Entity two = new Entity("two");
		Entity three = new Entity("three");

		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(three, two);
		graph.addDependency(three, one);
		graph.addDependency(two, one);

		expect(graph.find(two).get().directlyDependsOn(one)).toBeTruthy();
		expect(graph.find(three).get().directlyDependsOn(two)).toBeTruthy();
		expect(graph.find(three).get().directlyDependsOn(one)).toBeFalsy();
		expect(graph.find(three).get().dependsOn(one)).toBeTruthy();
	}

	@Test(description = "should threat elements without dependencies as independent")
	public void testIndependency() {
		Entity one = new Entity("one");
		Entity two = new Entity("two");
		Entity three = new Entity("three");

		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(two, one);
		graph.add(three);

		expect(graph.find(two).get().directlyDependsOn(one)).toBeTruthy();
		expect(graph.find(three).get().isIndependent()).toBeTruthy();
		expect(graph.find(three).get().hasDependencies()).toBeFalsy();
	}

	@Test(description = "should execute usual all parallel case")
	public void testAllParallel() {
		Entity one = new Entity("one");
		Entity two = new Entity("two");
		Entity three = new Entity("three");

		DependencyGraph<Entity> graph = emptyGraph();
		graph.add(one);
		graph.add(two);
		graph.add(three);

		List<String> result = Lists.newArrayList();
		new DependencyGraphExecutor<Entity>() {
			@Override public void processNode(Entity node) { result.add(node.getName()); }
		}.executeGraph(graph);

		expect(result).toContainOnly("one", "two", "three");
	}

	@Test(description = "should execute elements in right order")
	public void testExecutionOrder() {
		Entity one = new Entity("one");
		Entity two = new Entity("two");
		Entity three = new Entity("three");
		Entity four = new Entity("four");
		Entity five = new Entity("five");

		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(two, one);
		graph.addDependency(five, three);
		graph.addDependency(four, two);
		graph.addDependency(three, one);
		graph.addDependency(five, four);

		List<String> result = Lists.newArrayList();
		new DependencyGraphExecutor<Entity>() {
			@Override public void processNode(Entity node) { result.add(node.getName()); }
		}.executeGraph(graph);

		expect(result.size()).toEqual(5);
		expect(result.get(0)).toEqual("one");
		expect(result.subList(1, 3)).toContainOnly("two", "three"); // can be any order
		expect(result.get(3)).toEqual("four");
		expect(result.get(4)).toEqual("five");
	}

	@Test(description = "should fail dependents")
	public void testFailDependents() {
		Entity one = new Entity("one");
		Entity two = new Entity("two");
		Entity three = new Entity("three");
		Entity four = new Entity("four");
		Entity five = new Entity("five");

		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(two, one);
		graph.addDependency(five, three);
		graph.addDependency(four, two);
		graph.addDependency(three, one);
		graph.addDependency(five, four);

		List<String> result = Lists.newArrayList();
		new DependencyGraphExecutor<Entity>() {
			@Override
			public void processNode(Entity node) {
				if ("two".equals(node.getName())) {
					throw new RuntimeException();
				}
				result.add(node.getName());
			}
		}.executeGraph(graph);

		expect(result.size()).toEqual(2);
		expect(result.get(0)).toEqual("one");
		expect(result.get(1)).toEqual("three");
	}

	@Test(description = "should trigger callbacks in correct order")
	public void testCallbacks() {
		Entity one = new Entity("one");
		Entity two = new Entity("two");
		Entity three = new Entity("three");
		Entity four = new Entity("four");

		DependencyGraph<Entity> graph = emptyGraph();
		graph.addDependency(two, one);
		graph.addDependency(three, two);
		graph.addDependency(four, three);

		Consumer<Entity> nodeStartedCallback = mock(Consumer.class);
		Consumer<Entity> processNodeCallback = mock(Consumer.class);
		Consumer<Entity> nodeFinishedCallback = mock(Consumer.class);
		Consumer<Entity> nodeFailedCallback = mock(Consumer.class);

		InOrder order = inOrder(nodeStartedCallback, processNodeCallback, nodeFinishedCallback, nodeFailedCallback);

		new DependencyGraphExecutor<Entity>() {
			@Override public void processNode(Entity node) {
				if ("three".equals(node.getName())) throw new RuntimeException();
				processNodeCallback.accept(node);
			}
			@Override public void onNodeStarted(Entity node) { nodeStartedCallback.accept(node); }
			@Override public void onNodeFinished(Entity node) { nodeFinishedCallback.accept(node); }
			@Override public void onNodeFailed(Entity node) { nodeFailedCallback.accept(node); }
		}.executeGraph(graph);

		order.verify(nodeStartedCallback).accept(one);
		order.verify(processNodeCallback).accept(one);
		order.verify(nodeFinishedCallback).accept(one);
		order.verify(nodeStartedCallback).accept(two);
		order.verify(processNodeCallback).accept(two);
		order.verify(nodeFinishedCallback).accept(two);
		order.verify(nodeStartedCallback).accept(three);
		order.verify(nodeFailedCallback).accept(three);
		order.verify(nodeFailedCallback).accept(four);
	}

}
