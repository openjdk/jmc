/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2020, Red Hat Inc. All rights reserved.
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

import java.lang.management.ManagementFactory;

import javax.management.JMX;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.agent.jmx.AgentControllerMXBean;

public class TestRetrieveEventProbes {

	private static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController"; //$NON-NLS-1$

	private static final String XML_TEST_DESCRIPTION = "<jfragent>" + "<events>" + "<event id=\"demo.jfr.test1\">"
			+ "<label>JFR Hello World Event 1 </label>"
			+ "<description>Defined in the xml file and added by the agent.</description>"
			+ "<path>demo/jfrhelloworldevent1</path>" + "<stacktrace>true</stacktrace>"
			+ "<class>org.openjdk.jmc.agent.test.InstrumentMe</class>" + "<method>" + "<name>printHelloWorldJFR1</name>"
			+ "<descriptor>()V</descriptor>" + "</method>" + "<location>WRAP</location>" + "</event>" + "</events>"
			+ "</jfragent>";

	@Test
	public void testRetrieveEventProbes() throws Exception {
		AgentControllerMXBean mbean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
				new ObjectName(AGENT_OBJECT_NAME), AgentControllerMXBean.class, false);

		Assert.assertNotEquals(mbean.retrieveEventProbes(), XML_TEST_DESCRIPTION);
		mbean.defineEventProbes(XML_TEST_DESCRIPTION);
		Assert.assertEquals(mbean.retrieveEventProbes(), XML_TEST_DESCRIPTION);
	}

	@Test
	public void testRetrieveInvalidConfiguration() throws Exception {
		AgentControllerMXBean mbean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
				new ObjectName(AGENT_OBJECT_NAME), AgentControllerMXBean.class, false);

		String initialConfiguration = mbean.retrieveEventProbes();
		String invalidConfiguration = XML_TEST_DESCRIPTION.concat("</jfragent>");
		try {
			mbean.defineEventProbes(invalidConfiguration);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Assert.assertEquals(mbean.retrieveEventProbes(), initialConfiguration);
	}

	public void test() {
		//Dummy method for instrumentation
	}
}
