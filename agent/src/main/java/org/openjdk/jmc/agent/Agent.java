/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.jmx.AgentManagementFactory;
import org.openjdk.jmc.agent.util.ModuleUtils;

/**
 * Small ASM based byte code instrumentation agent for declaratively adding JFR events.
 */
public class Agent {
	/**
	 * This should be generated as part of the build later.
	 */
	public static final String VERSION = "1.0.0"; //$NON-NLS-1$
	private static boolean loadedDynamically = false;

	/**
	 * This method is run when the agent is started from the command line.
	 *
	 * @param agentArguments
	 *            the arguments to the agent, in this case the path to the config file.
	 * @param instrumentation
	 *            the {@link Instrumentation} instance, provided to us by the kind JVM.
	 */
	public static void premain(String agentArguments, Instrumentation instrumentation) {
		printVersion();
		getLogger().fine("Starting from premain"); //$NON-NLS-1$
		ModuleUtils.openUnsafePackage(instrumentation);
		initializeAgent(agentArguments, instrumentation);
	}

	/**
	 * This method is run when the agent is loaded dynamically.
	 *
	 * @param agentArguments
	 *            the arguments to the agent, in this case the path to the config file.
	 * @param instrumentation
	 *            the {@link Instrumentation} instance, provided to us by the kind JVM.
	 */
	public static void agentmain(String agentArguments, Instrumentation instrumentation) {
		printVersion();
		getLogger().fine("Starting from agentmain"); //$NON-NLS-1$
		loadedDynamically = true;
		initializeAgent(agentArguments, instrumentation);
	}

	/**
	 * This method can be used to initialize the BCI agent when using it as a stand alone library.
	 *
	 * @param configuration
	 *            the configuration options, as XML. The stream will be fully read, but not closed.
	 *            An empty configuration will be used if this argument is <code>null</code>.
	 * @param instrumentation
	 *            the {@link Instrumentation} instance.
	 * @throws XMLStreamException
	 *             if the configuration could not be read.
	 */
	public static void initializeAgent(InputStream configuration, Instrumentation instrumentation)
			throws XMLStreamException {
		TransformRegistry registry = configuration != null ? DefaultTransformRegistry.from(configuration)
				: DefaultTransformRegistry.empty();
		instrumentation.addTransformer(new Transformer(registry), true);
		AgentManagementFactory.createAndRegisterAgentControllerMBean(instrumentation, registry);
		if (loadedDynamically) {
			retransformClasses(registry.getClassNames(), instrumentation);
		}
	}

	/**
	 * @return the Logger to use for agent related status information.
	 */
	public static Logger getLogger() {
		return Logger.getLogger(Agent.class.getName());
	}

	/**
	 * Loads the configuration from the file specified in the agentArguments, and initializes the
	 * agent.
	 *
	 * @param agentArguments
	 *            the file to load from.
	 * @param instrumentation
	 *            the {@link Instrumentation} instance.
	 */
	private static void initializeAgent(String agentArguments, Instrumentation instrumentation) {
		if (agentArguments == null || agentArguments.trim().length() == 0) {
			try {
				initializeAgent((InputStream) null, instrumentation);
			} catch (XMLStreamException e) {
				// noop: null as InputStream causes defaults to be used - the stream will not be used
			}
			return;
		}

		File file = new File(agentArguments);
		try (InputStream stream = new FileInputStream(file)) {
			initializeAgent(stream, instrumentation);
		} catch (XMLStreamException | IOException e) {
			getLogger().log(Level.SEVERE, "Failed to read jfr probe definitions from " + file.getPath(), e); //$NON-NLS-1$
		}
	}

	/**
	 * Retransforms the required classes when the agent is loaded dynamically.
	 *
	 * @param clazzes
	 *            list of names of classes to retransform
	 * @param instrumentation
	 *            the {@link Instrumentation} instance.
	 */
	private static void retransformClasses(Set<String> clazzes, Instrumentation instrumentation) {
		List<Class<?>> classesToRetransform = new ArrayList<Class<?>>();
		for (String clazz : clazzes) {
			try {
				Class<?> classToRetransform = Class.forName(clazz.replace('/', '.'));
				classesToRetransform.add(classToRetransform);
			} catch (ClassNotFoundException cnfe) {
				getLogger().log(Level.SEVERE, "Unable to find class: " + clazz, cnfe);
			}
		}
		try {
			instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[0]));
		} catch (UnmodifiableClassException e) {
			getLogger().log(Level.SEVERE, "Unable to retransform classes", e);
		}
	}

	private static void printVersion() {
		getLogger().info(String.format("JMC BCI agent v%s", VERSION)); //$NON-NLS-1$
	}
}
