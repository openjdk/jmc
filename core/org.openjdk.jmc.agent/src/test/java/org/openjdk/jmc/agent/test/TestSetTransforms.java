/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.agent.test;

import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.openjdk.jmc.agent.Agent;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class TestSetTransforms {

	private static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController"; //$NON-NLS-1$

	private static final String XML_DESCRIPTION = "<jfragent>"
			+ "<events>"
			+ "<event id=\"demo.jfr.test1\">"
			+ "<name>JFR Hello World Event 1 %TEST_NAME%</name>"
			+ "<description>Defined in the xml file and added by the agent.</description>"
			+ "<path>demo/jfrhelloworldevent1</path>"
			+ "<stacktrace>true</stacktrace>"
			+ "<class>org.openjdk.jmc.agent.test.InstrumentMe</class>"
			+ "<method>"
			+ "<name>printHelloWorldJFR1</name>"
			+ "<descriptor>()V</descriptor>"
			+ "</method>"
			+ "<location>WRAP</location>"
			+ "</event>"
			+ "</events>"
			+ "</jfragent>";

	@Test
	public void testSetTransforms() throws Exception {
		// Invoke retransform
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName(AGENT_OBJECT_NAME);
		Object[] parameters = {XML_DESCRIPTION};
		String[] signature = {String.class.getName()};
		Class<?>[] clazzes = (Class<?>[]) mbs.invoke(name, "setTransforms", parameters, signature);
		assertNotNull(clazzes);
		if (Agent.getLogger().isLoggable(Level.FINE)) {
			for (Class<?> clazz : clazzes) {
				// If we've asked for verbose information, we write the generated class
				TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
				new CheckClassAdapter(visitor);
				new ClassReader(TestToolkit.getByteCode(clazz));
			}
		}
	}

	@Test
	public void testClearAllTransforms() throws Exception {
		// Invoke retransform
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName(AGENT_OBJECT_NAME);
		Object[] parameters = {""};
		String[] signature = {String.class.getName()};
		Class<?>[] clazzes = (Class<?>[]) mbs.invoke(name, "setTransforms", parameters, signature);
		assertNotNull(clazzes);
		if (Agent.getLogger().isLoggable(Level.FINE)) {
			for (Class<?> clazz : clazzes) {
				// If we've asked for verbose information, we write the generated class
				TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
				new CheckClassAdapter(visitor);
				new ClassReader(TestToolkit.getByteCode(clazz));
			}
		}
	}

	public void test() {
		//Dummy method for instrumentation
	}
}
