/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, SAP SE. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

import javax.management.JMX;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.agent.jmx.AgentControllerMXBean;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class TestDynamicallyLoadedClasses {

	private static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController";

	private static final String XML_TEST_DESCRIPTION = "<jfragent>" + "<config>" + "<classprefix>"
			+ "__JFRTestDynamicallyLoadedClasses" + "</classprefix>" + "<allowconverter>" + true + "</allowconverter>"
			+ "</config>" + "<events>" + "<event id=\"demo.jfr.test.dynamic\">" + "<label>" + "JFR Dynamic" + "</label>"
			+ "<description>" + "desc" + "</description>" + "<path>" + "demo/dynamic" + "</path>" + "<class>"
			+ Target.class.getName() + "</class>" + "<method>" + "<name>" + "testStaticWithParameter" + "</name>"
			+ "<descriptor>" + "(I)Ljava/lang/Object;" + "</descriptor>" + "<returnvalue>" + "<name>" + "val"
			+ "</name>" + "<description>" + "value" + "</description>" + "<converter>"
			+ TestDynamicallyLoadedClasses.class.getName() + "</converter>" + "</returnvalue>" + "</method>"
			+ "<location>" + "WRAP" + "</location>" + "</event>" + "</events>" + "</jfragent>";

	private static int sum = 0;

	@Test
	public void testIntrumentationOfDynamicallyLoadedClass() throws Exception {
		// Load the class when we are not tracking it yet.
		ClassLoader c = new TargetLoader();
		Class<?> cls = c.loadClass(Target.class.getName());
		Method m = cls.getDeclaredMethod("testStaticWithParameter", int.class);

		Assert.assertNotEquals(Target.class.getClassLoader(), cls.getClassLoader());

		AgentControllerMXBean mbean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
				new ObjectName(AGENT_OBJECT_NAME), AgentControllerMXBean.class, false);
		mbean.defineEventProbes(XML_TEST_DESCRIPTION);

		// Use the convert method to determine if we instrumented both classes.
		m.invoke(null, Integer.valueOf(1));
		Target.testStaticWithParameter(2);

		// Check both calls were tracked
		Assert.assertEquals(3, sum);
	}

	public static int convert(Object o) {
		int v = ((Integer) o).intValue();
		sum += v;
		return v;
	}

	private static class TargetLoader extends ClassLoader {
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (name.equals(Target.class.getName())) {
				try {
					return defineClass(Target.class.getName(), TestToolkit.getByteCode(Target.class), 0,
							TestToolkit.getByteCode(Target.class).length);
				} catch (ClassFormatError | IOException e) {
					throw new ClassNotFoundException();
				}
			}

			return TargetLoader.class.getClassLoader().loadClass(name);
		}
	}

	public static class Target {
		public static Object testStaticWithParameter(int p) {
			System.out.println("Called with parameter " + p);
			return Integer.valueOf(p);
		}
	}
}
