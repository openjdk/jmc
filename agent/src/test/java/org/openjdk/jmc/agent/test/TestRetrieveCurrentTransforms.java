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
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.jmx.AgentControllerMXBean;

public class TestRetrieveCurrentTransforms {

	private static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController"; //$NON-NLS-1$
	private static final String EVENT_ID = "demo.jfr.test1"; //$NON-NLS-1$
	private static final String METHOD_NAME = "printHelloWorldJFR1"; //$NON-NLS-1$
	private static final String FIELD_NAME = "'InstrumentMe.STATIC_STRING_FIELD'"; //$NON-NLS-1$

	@Test
	public void testRetreiveCurrentTransforms() throws Exception {
		JFRTransformDescriptor[] jfrTds = doRetrieveCurrentTransforms();
		Assert.assertTrue(jfrTds.length == 1);
		for (JFRTransformDescriptor jfrTd : jfrTds) {
			Assert.assertEquals(EVENT_ID, jfrTd.getId());
			Assert.assertEquals(METHOD_NAME, jfrTd.getMethod().getName());
			Assert.assertEquals(FIELD_NAME, jfrTd.getFields().get(0).getName());
		}
	}

	private JFRTransformDescriptor[] doRetrieveCurrentTransforms() throws Exception {
		AgentControllerMXBean mbean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
				new ObjectName(AGENT_OBJECT_NAME), AgentControllerMXBean.class, false);
		return mbean.retrieveCurrentTransforms();
	}

	public void test() {
		//Dummy method for instrumentation
	}
}
