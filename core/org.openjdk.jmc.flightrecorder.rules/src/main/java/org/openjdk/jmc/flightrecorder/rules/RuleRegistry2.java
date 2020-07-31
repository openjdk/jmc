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
	
	private static final Collection<IRule2> RULES;
	
	static {
		RULES = new ArrayList<>();
		ServiceLoader<IRule2> ruleLoader = ServiceLoader.load(IRule2.class, IRule2.class.getClassLoader());
		Set<IRule2> rules = new HashSet<>();
		Iterator<IRule2> ruleIter = ruleLoader.iterator();
		Map<Class<? extends IRule2>, IRule2> rulesByClass = new HashMap<>();
		Graph<IRule2, DefaultEdge> ruleDependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
		while (ruleIter.hasNext()) {
			IRule2 next = ruleIter.next();
			rules.add(next);
			ruleDependencyGraph.addVertex(next);
			rulesByClass.put(next.getClass(), next);
		}
		for (IRule2 rule : rules) {
			DependsOn[] dependencies = rule.getClass().getAnnotationsByType(DependsOn.class);
			for (DependsOn dependency : dependencies) {
				ruleDependencyGraph.addEdge(rulesByClass.get(dependency.value()), rule);
			}
		}
		TopologicalOrderIterator<IRule2, DefaultEdge> iterator = new TopologicalOrderIterator<>(ruleDependencyGraph);
		while (iterator.hasNext()) {
			IRule2 rule = iterator.next();
			RULES.add(rule);
		}
	}
	
	private RuleRegistry2() {
		throw new InstantiationError();
	}
	
	public static Collection<IRule2> getRules() {
		return RULES;
	}
	
}
