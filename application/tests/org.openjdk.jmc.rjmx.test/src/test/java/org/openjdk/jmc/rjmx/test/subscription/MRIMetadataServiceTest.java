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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.Test;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.test.LocalRJMXTestToolkit;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;

/**
 * Tests the basic functionality of the {@link IMRIMetadataService}.
 */
public class MRIMetadataServiceTest extends RjmxTestCase {
	private static final String MOOH = "Mooh!";
	private static final String MY_COW = "MyCow";

	@Test
	public void testGetAttributeInfo() throws Exception {
		@SuppressWarnings("nls")
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		IMRIMetadataService service = LocalRJMXTestToolkit.getInfoService(handle);
		IMRIMetadata info = service
				.getMetadata(new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad"));
		assertNotNull(info);
		assertNotNull(info.getMRI());
		assertNotNull(info.getValueType());
		assertNotNull(info.getDescription());
		handle.close();
	}

	@Test
	public void testGetMetadata() throws Exception {
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		IMRIMetadataService service = LocalRJMXTestToolkit.getInfoService(handle);
		IMRIMetadata info = service
				.getMetadata(new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad"));
		assertNotNull(info);

		String description = info.getDescription();
		assertNotNull(description);
		assertTrue(description.length() > 12);
		handle.close();
	}

	@Test
	public void testGetExtendedProperties() throws Exception {
		try (IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor())
				.connect("Test")) {
			IMRIMetadataService service = LocalRJMXTestToolkit.getInfoService(handle);
			IMRIMetadata info = service
					.getMetadata(new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage/used"));
			assertNotNull(info);
			assertTrue("Should be numerical", MRIMetadataToolkit.isNumerical(info));
			assertNotNull(info.getMetadata("color"));
		}
	}

	@Test
	public void testGetNumericalMetadata() throws Exception {
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		IMRIMetadataService service = LocalRJMXTestToolkit.getInfoService(handle);
		evaluateNumericalMetadata(service, new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage"), false);
		evaluateNumericalMetadata(service,
				new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage/committed"), true);
		handle.close();
	}

	public void evaluateNumericalMetadata(IMRIMetadataService service, MRI mri, boolean isNumerical) {
		IMRIMetadata info = service.getMetadata(mri);
		assertNotNull(info);
		assertTrue(isNumerical == MRIMetadataToolkit.isNumerical(info));
	}

	@Test
	public void testAttributeTypes() throws Exception {
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		IMRIMetadataService service = LocalRJMXTestToolkit.getInfoService(handle);
		evaluateAttributeType(service, new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage"),
				MemoryUsage.class);
		evaluateAttributeType(service, new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage/committed"),
				Long.TYPE);
		evaluateAttributeType(service,
				new MRI(Type.ATTRIBUTE, "JMImplementation:type=MBeanServerDelegate", "ImplementationVendor"),
				String.class);
		evaluateAttributeType(service, new MRI(Type.ATTRIBUTE, "java.lang:type=Runtime", "InputArguments"), List.class);
		evaluateAttributeType(service, new MRI(Type.ATTRIBUTE, "java.lang:type=Runtime", "SystemProperties"),
				Map.class);
		handle.close();
	}

	private void evaluateAttributeType(IMRIMetadataService service, MRI mri, Class<?> clazz) {
		IMRIMetadata info = service.getMetadata(mri);
		assertNotNull(info);
		String typeName = stripGenericType(info.getValueType());
		if (clazz.isPrimitive()) {
			assertTrue("Not assignable!", clazz.getName().equals(typeName));
		} else {
			try {
				assertTrue("Not assignable!", clazz.isAssignableFrom(Class.forName(typeName)));
			} catch (ClassNotFoundException e) {
				assertTrue("Could not instantiate metadata type " + typeName, false);
			}
		}
	}

	private String stripGenericType(String className) {
		int start = className.indexOf('<');
		if (start >= 0) {
			assertTrue(className.charAt(className.length() - 1) == '>');
			return className.substring(0, start);
		}
		return className;
	}

	@Test
	public void testSetMetadata() throws Exception {
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		IMRIMetadataService service = LocalRJMXTestToolkit.getInfoService(handle);
		IMRIMetadata info = service
				.getMetadata(new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad"));
		assertNotNull(info);
		service.setMetadata(info.getMRI(), "testgegga", "Oh, testgegga!");
		String testGegga = (String) service.getMetadata(info.getMRI(), "testgegga");
		assertEquals("Oh, testgegga!", testGegga);
		handle.close();
	}

	@Test
	public void testSetMetadataInDifferentConnections() throws Exception {
		Assume.assumeTrue("Will not pass until BUG XYZ is fixed", false);

		IConnectionHandle handle1 = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor())
				.connect("Test");
		IConnectionHandle handle2 = IServerHandle.create(LocalRJMXTestToolkit.createAlternativeDescriptor())
				.connect("Test");
		IMRIMetadataService service1 = LocalRJMXTestToolkit.getInfoService(handle1);
		IMRIMetadataService service2 = LocalRJMXTestToolkit.getInfoService(handle2);
		MRI mri = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad");
		service1.setMetadata(mri, MY_COW, MOOH);
		assertNotNull(service1.getMetadata(mri, MY_COW));
		assertEquals(MOOH, service1.getMetadata(mri, MY_COW));
		assertNull(service2.getMetadata(mri).getMetadata(MY_COW));
		handle1.close();
		handle2.close();
	}

	@Test
	public void testOverrideDefultMetadata() throws Exception {
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		IMRIMetadataService service = LocalRJMXTestToolkit.getInfoService(handle);
		MRI mri = new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage/committed");
		IMRIMetadata info = service.getMetadata(mri);
		String description = info.getDescription();
		String newDescription = "[ja]" + description;
		service.setMetadata(mri, IMRIMetadataProvider.KEY_DESCRIPTION, newDescription);
		assertEquals("Description not updated", newDescription, info.getDescription());
		handle.close();
	}

	@Test
	public void testCompositeIsChild() throws Exception {
		IConnectionHandle handle = IServerHandle.create(LocalRJMXTestToolkit.createDefaultDescriptor()).connect("Test");
		IMRIMetadataService service = LocalRJMXTestToolkit.getInfoService(handle);
		IMRIMetadata info = service.getMetadata(new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage"));
		assertTrue("MRI is not composite!", MRIMetadataToolkit.isComposite(info));
		int childCount = 0;
		for (MRI mri : handle.getServiceOrThrow(IMRIService.class).getMRIs()) {
			if (info.getMRI().isChild(mri)) {
				childCount++;
			}
		}
		assertEquals("There is not four composite children!", 4, childCount);
		handle.close();
	}
}
