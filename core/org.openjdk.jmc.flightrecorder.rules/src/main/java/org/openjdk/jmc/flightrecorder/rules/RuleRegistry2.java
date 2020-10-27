package org.openjdk.jmc.flightrecorder.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class RuleRegistry2 {

	private static final Collection<IRule> RULES;

	static {
		RULES = new ArrayList<>();
		ServiceLoader<IRule> ruleLoader = ServiceLoader.load(IRule.class, IRule.class.getClassLoader());
		Set<IRule> rules = new HashSet<>();
		Iterator<IRule> ruleIter = ruleLoader.iterator();
		Map<Class<? extends IRule>, IRule> rulesByClass = new HashMap<>();
		Graph<IRule, DefaultEdge> ruleDependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
		while (ruleIter.hasNext()) {
			IRule next = ruleIter.next();
			rules.add(next);
			ruleDependencyGraph.addVertex(next);
			rulesByClass.put(next.getClass(), next);
		}
		for (IRule rule : rules) {
			DependsOn[] dependencies = rule.getClass().getAnnotationsByType(DependsOn.class);
			for (DependsOn dependency : dependencies) {
				ruleDependencyGraph.addEdge(rulesByClass.get(dependency.value()), rule);
			}
		}
		TopologicalOrderIterator<IRule, DefaultEdge> iterator = new TopologicalOrderIterator<>(ruleDependencyGraph);
		while (iterator.hasNext()) {
			IRule rule = iterator.next();
			RULES.add(rule);
		}
	}

	private RuleRegistry2() {
		throw new InstantiationError();
	}

	public static Collection<IRule> getRules() {
		return RULES;
	}

}
