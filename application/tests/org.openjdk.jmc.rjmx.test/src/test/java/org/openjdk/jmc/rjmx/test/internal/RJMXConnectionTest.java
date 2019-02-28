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
package org.openjdk.jmc.rjmx.test.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.rjmx.internal.DefaultConnectionHandle;
import org.openjdk.jmc.rjmx.internal.JMXConnectionDescriptor;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;

/**
 * Testing the new RJMXConnection.
 */
@SuppressWarnings("nls")
public class RJMXConnectionTest extends RjmxTestCase {
	// The MBEANS vital to console functionality.
	public final static String[] MBEAN_NAMES = {"java.lang:type=OperatingSystem", "java.lang:type=ClassLoading",
			"java.lang:type=Threading", "java.lang:type=Compilation", "java.lang:type=Memory",
			"java.lang:type=Runtime", "java.lang:type=MemoryPool,*", "java.lang:type=GarbageCollector,*",
			"java.lang:type=MemoryManager,*"};

	public final static String[] MBEAN_CLASS_NAMES = {"sun.management.RuntimeImpl"};

	public static final int MIN_CPUS = 1;
	public static final int MAX_CPUS = 1024;

	private MRI[] ATTRIBUTES_OS;

	// Only use this one for testing!
	private final static String[] ATTRIBUTE_SPEC_NAME = {"SpecName"};
	private RJMXConnection m_connection;

	/**
	 * Override to avoid creating RJMXConnectorModels but still use the descriptor from superclass.
	 *
	 * @return null
	 */
	protected DefaultConnectionHandle createConnectorModel(JMXConnectionDescriptor descriptor) {
		return null;
	}

	@Before
	public void setUp() throws Exception {
		m_connection = new RJMXConnection(m_connectionDescriptor, new ServerDescriptor(), null);
		m_connection.connect();
		ATTRIBUTES_OS = getOSAttributes();
	}

	public static MRI[] getOSAttributes() {
		return new MRI[] {new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "SystemCpuLoad"),
				new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "ProcessCpuLoad"),
				new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", "AvailableProcessors")};
	}

	@After
	public void tearDown() throws Exception {
		m_connection.close();
		ATTRIBUTES_OS = null;
	}

	@Test
	public void testConnect() {
		assertTrue(m_connection.isConnected());
	}

	@Test
	public void testGetMBeanNames() throws Exception {
		// <String, ObjectName>
		HashMap<String, ObjectName> names = new HashMap<>();

		for (Object element : m_connection.getMBeanNames()) {
			ObjectName o = (ObjectName) element;
			names.put(o.toString(), o);
		}

		for (String element : MBEAN_NAMES) {
			boolean found = false;
			ObjectName mbeanName = new ObjectName(element);
			for (ObjectName objectName : names.values()) {
				if (mbeanName.apply(objectName)) {
					found = true;
					break;
				}
			}
			assertTrue("MBean names did not contain: " + element, found);
		}
	}

	@Test
	public void testGetMBeanNamesAfterReconnect() throws Exception {
		Set<String> names = getMBeanNameStrings();

		m_connection.close();
		m_connection.connect();

		Set<String> namesAfterReconnection = getMBeanNameStrings();

		if (!(names.containsAll(namesAfterReconnection) && namesAfterReconnection.containsAll(names))) {
			fail("MBeans before (" + names.size() + ") and after (" + namesAfterReconnection.size()
					+ ") reconnect did not match. \nMBeans before: " + names.toString() + "\n MBeans after: "
					+ namesAfterReconnection.toString());
		}
	}

	/**
	 * Returns all available MBean name Strings.
	 *
	 * @throws IOException
	 *             if not connected, or other exception occurred.
	 */
	private Set<String> getMBeanNameStrings() throws IOException {
		HashSet<String> names = new HashSet<>();
		for (Object element : m_connection.getMBeanNames()) {
			ObjectName o = (ObjectName) element;
			names.add(o.toString());
		}
		return names;
	}

	@Test
	public void testGetMBeanInfos() throws Exception {
		// <String, MBeanInfo>
		HashMap<String, MBeanInfo> infos = new HashMap<>();

		@SuppressWarnings("rawtypes")
		Iterator iter = m_connection.getMBeanInfos().values().iterator();
		while (iter.hasNext()) {
			MBeanInfo info = (MBeanInfo) iter.next();
			infos.put(info.getClassName(), info);
		}
		// No longer check all the class names
		for (String element : MBEAN_CLASS_NAMES) {
			assertTrue("Returned infos did not contain MBean class name: " + element, infos
					.containsKey(element));
		}

		MBeanInfo loggingInfo = infos.get(MBEAN_CLASS_NAMES[0]);
		assertNotNull("MBeanInfo was null for " + MBEAN_CLASS_NAMES[0], loggingInfo);
		MBeanAttributeInfo[] attrInfo = loggingInfo.getAttributes();
		assertNotNull("MBeanAttributeInfo was null for " + MBEAN_CLASS_NAMES[0], attrInfo);
	}

	@Test
	public void testGetAttributes() throws Exception {
		for (MRI element : ATTRIBUTES_OS) {
			Object value = m_connection.getAttributeValue(element);
			if (element.equals(ATTRIBUTES_OS[0]) || element.equals(ATTRIBUTES_OS[1])) {
				assertTrue(value != null);
			}
			if (element.equals(ATTRIBUTES_OS[2])) {
				assertBetween(MIN_CPUS, MAX_CPUS, ((Number) value).intValue());
			}
		}
	}

	@Test
	public void testGetRuntimeMBeanAttribute() throws Exception {
		Object attr = m_connection
				.getAttributeValue(new MRI(Type.ATTRIBUTE, getObjectName(MBEAN_NAMES[5]), ATTRIBUTE_SPEC_NAME[0]));
		assertNotNull(attr);
		assertTrue(String.valueOf(attr).contains("Virtual Machine"));
	}

	public void handleRJMXException(Exception exception) {
		fail(exception.getMessage());
	}

	public void handleRJMXNotification(Notification n) {
		// Ignore. Tested by the connection model.
	}

	public ObjectName getObjectName(String name) throws MalformedObjectNameException, NullPointerException {
		ObjectName oname = new ObjectName(name);
		return oname;
	}

}
