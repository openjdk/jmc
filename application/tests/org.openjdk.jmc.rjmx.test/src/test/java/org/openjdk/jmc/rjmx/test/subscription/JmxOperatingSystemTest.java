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
package org.openjdk.jmc.rjmx.test.subscription;

import static org.junit.Assert.fail;

import org.junit.Test;

import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.testutil.TestToolkit;

/**
 * Tests the JRockit specific parts of the OSMBean.
 */
public class JmxOperatingSystemTest extends RjmxTestCase {

	@Test
	public void testGetCPULoad() {
		try {
			Object data = getMBeanServerConnection().getAttribute(ConnectionToolkit.OPERATING_SYSTEM_BEAN_NAME,
					"SystemCpuLoad");
			double cpuLoad = ((Number) data).doubleValue();

			// A negative value is returned if load is not available
			assertMax("CPU Load", 100.0, cpuLoad);
			TestToolkit.println("CPU Load: " + cpuLoad);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetCPUJVMLoad() {
		try {
			Object data = getMBeanServerConnection().getAttribute(ConnectionToolkit.OPERATING_SYSTEM_BEAN_NAME,
					"ProcessCpuLoad");
			double jvmLoad = ((Number) data).doubleValue();

			// A negative value is returned if load is not available
			assertMax("JVM Load", 100.0, jvmLoad);
			TestToolkit.println("JVM Load: " + jvmLoad);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetTotalPhysicalMemory() {
		try {
			Object data = getMBeanServerConnection().getAttribute(ConnectionToolkit.OPERATING_SYSTEM_BEAN_NAME,
					"TotalPhysicalMemorySize");
			long totalPhysicalMemory = ((Number) data).longValue();
			assertMin("TotalPhysicalMemory", 256 * 1024 * 1024L, totalPhysicalMemory);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetUsedPhysicalMemory() {
		try {
			Object data = getMBeanServerConnection().getAttribute(ConnectionToolkit.OPERATING_SYSTEM_BEAN_NAME,
					"UsedPhysicalMemorySize");
			long usedPhysicalMemory = ((Number) data).longValue();
			assertMin("UsedPhysicalMemory", 16 * 1024 * 1024L, usedPhysicalMemory);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
