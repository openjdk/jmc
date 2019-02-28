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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.internal.AttributeValueToolkit;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.test.internal.RJMXConnectionTest;

/**
 */
public class AttributeValueTest extends RjmxTestCase {

	@Test
	public void testConnect() {
		try {
			assertTrue(m_connectionHandle.isConnected());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetAttributes() {
		try {
			List<MRI> fetchList = createAttributesList(m_connectionHandle);

			long start = System.currentTimeMillis();
			Map<MRI, Object> results = AttributeValueToolkit.getAttributes(getMBeanServerConnection(), fetchList);
			long end = System.currentTimeMillis();
			assertTrue(end - start < 3000);
			assertTrue(fetchList.size() == results.size());
			assertTrue(results.size() > 0);
			for (Object o : results.values()) {
				assertNotNull(o);
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testAndValidateCommonAttributes() {
		try {
			List<MRI> fetchList = createCommonAttributesList(m_connectionHandle);
			long start = System.currentTimeMillis();
			Map<MRI, Object> results = AttributeValueToolkit.getAttributes(getMBeanServerConnection(), fetchList);
			long end = System.currentTimeMillis();
			assertTrue(end - start < 60000);
			assertTrue(fetchList.size() == results.size());
			assertTrue(results.size() > 0);
			for (Object o : results.values()) {
				assertNotNull(o);
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Creates a fetchMap for getting a few attributes (CLASS_LOADING and OS)
	 *
	 * @return
	 */
	private static List<MRI> createAttributesList(IConnectionHandle connectionHandle) throws IOException {
		return Arrays.asList(RJMXConnectionTest.getOSAttributes());
	}

	private static List<MRI> createCommonAttributesList(IConnectionHandle connectionHandle) {
		List<MRI> fetchList = new ArrayList<>();
		fetchList.add(new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad"));
		fetchList.add(new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "ProcessCpuLoad"));
		fetchList.add(new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem",
				"TotalPhysicalMemorySize"));
		fetchList.add(new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem",
				"UsedPhysicalMemorySize"));
		return fetchList;
	}
}
