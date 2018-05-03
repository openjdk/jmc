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
package org.openjdk.jmc.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.jmx.AgentManagementFactory;

/**
 * Small ASM based byte code instrumentation agent for declaratively adding logging and JFR events.
 * Note: This agent is currently work in progress, and it is not supported for production use yet.
 */
public class Agent {
	/**
	 * This should be generated as part of the build later.
	 */
	public final static String VERSION = "0.0.1"; //$NON-NLS-1$
	private final static String DEFAULT_CONFIG = "jfrprobes.xml"; //$NON-NLS-1$

	@SuppressWarnings("unused")
	private static Instrumentation instrumentationInstance;

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
		initializeAgent(agentArguments, instrumentation);
	}

	/**
	 * This method can be used to initialize the BCI agent when using it as a stand alone library.
	 *
	 * @param configuration
	 *            the configuration options, as XML. The stream will be fully read, but not closed.
	 * @param instrumentation
	 *            the {@link Instrumentation} instance.
	 * @throws XMLStreamException
	 *             if the configuration could not be read.
	 */
	public static void initializeAgent(InputStream configuration, Instrumentation instrumentation)
			throws XMLStreamException {
		TransformRegistry registry = DefaultTransformRegistry.from(configuration);
		instrumentationInstance = instrumentation;
		instrumentation.addTransformer(new Transformer(registry));
		AgentManagementFactory.createAndRegisterAgentControllerMBean(instrumentation, registry);
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
			agentArguments = DEFAULT_CONFIG;
		}
		File file = new File(agentArguments);
		try {
			InputStream stream = new FileInputStream(file);
			initializeAgent(stream, instrumentation);
		} catch (FileNotFoundException | XMLStreamException e) {
			getLogger().log(Level.SEVERE, "Failed to read jfr probe definitions from " + file.getPath(), e); //$NON-NLS-1$
		}
	}

	private static void printVersion() {
		Logger.getLogger(Agent.class.getName()).info(String.format("JMC BCI agent v%s", VERSION)); //$NON-NLS-1$
	}
}
