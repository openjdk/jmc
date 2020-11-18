/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.jmx;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementPermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;

public class AgentController implements AgentControllerMXBean {

	private static final Logger logger = Logger.getLogger(AgentController.class.getName());

	private final Instrumentation instrumentation;
	private final TransformRegistry registry;

	public AgentController(Instrumentation instrumentation, TransformRegistry registry) {
		this.instrumentation = instrumentation;
		this.registry = registry;
	}

	public void defineEventProbes(String xmlDescription) throws Exception {
		checkSecurity();
		Class<?>[] classesToRetransformArray;
		boolean revertAll = xmlDescription == null ? true : xmlDescription.isEmpty();
		if (revertAll) {
			classesToRetransformArray = retransformClasses(registry.clearAllTransformData());
			registry.setCurrentConfiguration("");
		} else {
			Set<String> initialClasses = new HashSet<>(registry.getClassNames());
			Set<String> modifiedClasses = registry.modify(xmlDescription);
			if (modifiedClasses == null) {
				logger.log(Level.SEVERE, "Failed to identify transformations: " + xmlDescription);
				return;
			} else {
				modifiedClasses.addAll(initialClasses);
				classesToRetransformArray = retransformClasses(modifiedClasses);
			}
		}
		registry.setRevertInstrumentation(true);
		instrumentation.retransformClasses(classesToRetransformArray);
		registry.setRevertInstrumentation(false);
	}

	private Class<?>[] retransformClasses(Set<String> classNames) {
		Set<Class<?>> classesToRetransform = new HashSet<>();
		for (String className : classNames) {
			try {
				Class<?> classToRetransform = Class.forName(className.replace('/', '.'));
				classesToRetransform.add(classToRetransform);
			} catch (ClassNotFoundException cnfe) {
				logger.log(Level.SEVERE, "Unable to find class: " + className, cnfe);
			}
		}
		return classesToRetransform.toArray(new Class<?>[0]);
	}

	public JFRTransformDescriptor[] retrieveCurrentTransforms() {
		checkSecurity();
		Set<String> classNames = registry.getClassNames();
		List<TransformDescriptor> tds = new ArrayList<>();
		for (String className : classNames) {
			tds.addAll(registry.getTransformData(className));
		}

		List<JFRTransformDescriptor> jfrTds = new ArrayList<>();
		for (TransformDescriptor td : tds) {
			jfrTds.add((JFRTransformDescriptor) td);
		}
		return (jfrTds.toArray(new JFRTransformDescriptor[0]));
	}

	public String retrieveEventProbes() {
		checkSecurity();
		return registry.getCurrentConfiguration();
	}

	private void checkSecurity() {
		SecurityManager secMan = System.getSecurityManager();
		if (secMan != null) {
			secMan.checkPermission(new ManagementPermission("control"));
		}
	}

}
