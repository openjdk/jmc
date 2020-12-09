/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

		void setTemporarilyMark() {
			this.temporaryMark = true;
		}

		void resetTemporaryMark() {
			temporaryMark = false;
		}

		void setPermanentlyMark() {
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
			vertex.setTemporarilyMark();
			for (Vertex v : vertex.edges) {
				visit(v, orderedList);
			}
			vertex.resetTemporaryMark();
			vertex.setPermanentlyMark();
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
