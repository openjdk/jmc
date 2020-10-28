/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 Red Hat Inc. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.logging.Logger;

import javax.management.JMX;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.Transformer;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.jmx.AgentControllerMXBean;
import org.openjdk.jmc.agent.test.util.TestToolkit;

/***
 * JMC-6895 When the agent instruments a class being loaded by a custom classloader, it will first
 * instrument it in the AppClassloader. Since the agent then checks on future retransforms if the
 * class is already there, it will use this version when it is called from the custom ClassLoader's
 * loadClass chain. When invoking the instrumented method the old pre-instrumented method will be
 * run instead.
 */
public class TestCustomClassloader {

	private static Logger logger = Logger.getLogger(TestCustomClassloader.class.getName());

	private static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController"; //$NON-NLS-1$
	private static final String EVENT_ID = "demo.jfr.test6";
	private static final String EVENT_NAME = "JFR Hello World Event 1 %TEST_NAME%";
	private static final String EVENT_DESCRIPTION = "JFR Hello World Event 1 %TEST_NAME%";
	private static final String EVENT_PATH = "demo/jfrhelloworldevent";
	private static final String EVENT_CLASS_NAME = "org.openjdk.jmc.agent.test.TestDummy";
	private static final String METHOD_NAME = "testWithoutException";
	private static final String METHOD_DESCRIPTOR = "()V";

	private static final String XML_DESCRIPTION = "<jfragent>" + "<events>" + "<event id=\"" + EVENT_ID + "\">"
			+ "<name>" + EVENT_NAME + "</name>" + "<description>" + EVENT_DESCRIPTION + "</description>" + "<path>"
			+ EVENT_PATH + "</path>" + "<stacktrace>true</stacktrace>" + "<class>" + EVENT_CLASS_NAME + "</class>"
			+ "<method>" + "<name>" + METHOD_NAME + "</name>" + "<descriptor>" + METHOD_DESCRIPTOR + "</descriptor>"
			+ "</method>" + "<location>WRAP</location>" + "</event>" + "</events>" + "</jfragent>";

	@Test
	public void testCorrectMethodDescriptor() throws Exception {
		try {
			ClassLoader c = new CustomClassLoader();
			Class reproducer = c.loadClass(TestDummy.class.getName());
			for (int i = 0; i < 10; i++) {
				reproducer.getDeclaredMethod("testWithoutException")
						.invoke(reproducer.getDeclaredConstructor().newInstance());
			}
		} catch (ClassNotFoundException e) {
			logger.severe("===================================" + e.toString());
			Assert.fail();
		}
	}

	private void doDefineEventProbes(String xmlDescription) throws Exception {
		AgentControllerMXBean mbean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
				new ObjectName(AGENT_OBJECT_NAME), AgentControllerMXBean.class, false);
		mbean.defineEventProbes(xmlDescription);
	}

	private class CustomClassLoader extends ClassLoader {

		@Override
		protected Class<?> findClass(String moduleName, String name) {
			try {
				if (name.equals(TestDummy.class.getName())) {
					try {
						return defineClass(TestDummy.class.getName(), TestToolkit.getByteCode(TestDummy.class), 0,
								TestToolkit.getByteCode(TestDummy.class).length);
					} catch (IOException e) {
						System.err.println("Could not define class");
						return null;
					}
				} else {
					return loadClass(name, false);
				}
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (name.equals(TestDummy.class.getName())) {
				return findClass("", name);
			}
			return loadClass(name, false);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			try {
				ClassLoader.getPlatformClassLoader().loadClass(name);
			} catch (Exception e) {
				logger.severe("Exception thrown: " + e.toString());
			}
			return ClassLoader.getPlatformClassLoader().loadClass(name);
		}
	}
}
