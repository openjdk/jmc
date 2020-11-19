package org.openjdk.jmc.flightrecorder.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.flightrecorder.rules.internal.IRuleProvider;

public class RuleRegistry {

	private static final Collection<IRule> RULES;

	private static class Vertex {
		private boolean temporaryMark = false;
		private boolean permanentMark = false;
		private final IRule rule;
		private final Collection<Vertex> edges = new ArrayList<>();

		Vertex(IRule rule) {
			this.rule = rule;
		}

		boolean isTemporarilyMarked() {
			return temporaryMark;
		}

		boolean isPermanentlyMarked() {
			return permanentMark;
		}

		void temporarilyMark() {
			this.temporaryMark = true;
		}

		void removeTemporaryMark() {
			temporaryMark = false;
		}

		void permanentlyMark() {
			this.permanentMark = true;
		}
	}

	private static class Graph {
		private final Map<IRule, Vertex> vertices = new HashMap<>();

		Collection<IRule> getTopologicalOrder() {
			int permanentlyMarkedVertices = 0;
			List<IRule> orderedList = new ArrayList<>();
			for (Vertex vertex : vertices.values()) {
				if (permanentlyMarkedVertices == vertices.size()) {
					return orderedList;
				}
				visit(vertex, orderedList);
			}
			return orderedList;
		}

		void visit(Vertex vertex, List<IRule> orderedList) {
			if (vertex.isPermanentlyMarked()) {
				return;
			}
			if (vertex.isTemporarilyMarked()) {
				throw new RuntimeException("Non-DAG IRule dependency graph detected!"); //$NON-NLS-1$
			}
			vertex.temporarilyMark();
			for (Vertex v : vertex.edges) {
				visit(v, orderedList);
			}
			vertex.removeTemporaryMark();
			vertex.permanentlyMark();
			orderedList.add(0, vertex.rule);
		}

		void addVertex(IRule rule) {
			vertices.put(rule, new Vertex(rule));
		}

		void addDependency(IRule dependee, IRule depender) {
			Vertex vertex = new Vertex(depender);
			vertices.put(depender, vertex);
			vertices.get(dependee).edges.add(vertex);
		}
	}

	static {
		ServiceLoader<IRule> ruleLoader = ServiceLoader.load(IRule.class, IRule.class.getClassLoader());
		Set<IRule> rules = new HashSet<>();
		Iterator<IRule> ruleIter = ruleLoader.iterator();
		while (ruleIter.hasNext()) {
			try {
				rules.add(ruleIter.next());
			} catch (ServiceConfigurationError e) {
				getLogger().log(Level.WARNING, "Could not create IRule instance specified in a JSL services file", e); //$NON-NLS-1$
			}
		}
		ServiceLoader<IRuleProvider> providerLoader = ServiceLoader.load(IRuleProvider.class,
				IRuleProvider.class.getClassLoader());
		Iterator<IRuleProvider> providerIter = providerLoader.iterator();
		while (providerIter.hasNext()) {
			try {
				IRuleProvider provider = providerIter.next();
				for (IRule rule : provider.getRules()) {
					rules.add(rule);
				}
			} catch (ServiceConfigurationError e) {
				getLogger().log(Level.WARNING,
						"Could not create IRuleProvider instance specified in a JSL services file", e); //$NON-NLS-1$
			}
		}
		Map<Class<? extends IRule>, IRule> rulesByClass = new HashMap<>();
		Graph g = new Graph();
		for (IRule rule : rules) {
			g.addVertex(rule);
			rulesByClass.put(rule.getClass(), rule);
		}
		for (IRule rule : rules) {
			DependsOn[] dependencies = rule.getClass().getAnnotationsByType(DependsOn.class);
			for (DependsOn dependency : dependencies) {
				g.addDependency(rulesByClass.get(dependency.value()), rule);
			}
		}
		Collection<IRule> topologicalOrder = g.getTopologicalOrder();
		RULES = Collections.unmodifiableCollection(topologicalOrder);
	}

	private static Logger getLogger() {
		return Logger.getLogger("org.openjdk.jmc.flightrecorder.rules"); //$NON-NLS-1$
	}

	private RuleRegistry() {
		throw new InstantiationError();
	}

	public static Collection<IRule> getRules() {
		return RULES;
	}

}
