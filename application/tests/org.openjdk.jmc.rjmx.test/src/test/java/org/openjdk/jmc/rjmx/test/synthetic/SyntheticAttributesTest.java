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
package org.openjdk.jmc.rjmx.test.synthetic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.internal.AttributeValueToolkit;
import org.openjdk.jmc.rjmx.test.ServerHandleTestCase;

public class SyntheticAttributesTest extends ServerHandleTestCase {
	private final static String NEW_VALUE = "new value"; //$NON-NLS-1$
	protected IConnectionHandle localConnection;

	@Test
	public void testLookupDomain() throws Exception {
		assertTrue("Could not find the test domain!", containsDomain("org.openjdk.jmc.test")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testFindNonSyntheticDomain() throws Exception {
		assertTrue("Could not find the java.lang domain!", containsDomain("java.lang")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testFindMBean() throws Exception {
		ObjectName mbean = getSyntheticAttributeDescriptor().getObjectName();
		assertTrue("Could not find the test mbean!", containsMBean(mbean)); //$NON-NLS-1$
	}

	@Test
	public void testFindNonSyntheticMBean() throws Exception {
		ObjectName mbean = new ObjectName("java.lang:type=Runtime"); //$NON-NLS-1$
		assertTrue("Could not find the Runtime mbean!", containsMBean(mbean)); //$NON-NLS-1$
	}

	@Test
	public void testGetAttribute() throws Exception {
		MRI descriptor = getSyntheticAttributeDescriptor();
		Object value = getAttributeValue(descriptor);
		assertNotNull("Could not retrieve the attribute value", value); //$NON-NLS-1$
	}

	@Test
	public void testGetCompositeAttribute() throws Exception {
		MRI descriptor = getCompositeAttributeDescriptor();
		Object value = getAttributeValue(descriptor);
		assertNotNull("Could not retrieve the attribute value", value); //$NON-NLS-1$
	}

	@Test
	public void testGetAttributes() throws Exception {
		MRI descriptor = getSyntheticAttributeDescriptor();
		AttributeList list = getAttributeValues(descriptor.getObjectName(), new String[] {descriptor.getDataPath()});
		Object value = list.get(0);
		assertNotNull("Could not retrieve the attribute value", value); //$NON-NLS-1$
	}

	@Test
	public void testGetCompositeAttributes() throws Exception {
		MRI[] descriptors = getCombinedAttributeDescriptors();
		ObjectName objectName = descriptors[0].getObjectName();
		List<String> names = new ArrayList<>();
		for (MRI ad : descriptors) {
			names.add(ad.getDataPath());
		}
		AttributeList list = getAttributeValues(objectName, names.toArray(new String[descriptors.length]));
		assertEquals("Could not retrieve all values", descriptors.length, list.size()); //$NON-NLS-1$
	}

	@Test
	public void testGetCompositeAttributesThroughMBeanHelperService() throws Exception {
		MRI[] descriptors = getCombinedAttributeDescriptors();
		ObjectName objectName = descriptors[0].getObjectName();
		List<String> names = new ArrayList<>();
		for (MRI ad : descriptors) {
			names.add(ad.getDataPath());
		}
		Collection<Object> list = getAttributeValuesThroughMBeanHelperService(objectName, names);
		assertEquals("Could not retrieve all values", descriptors.length, list.size()); //$NON-NLS-1$
	}

	@Test
	public void testGetMultipleAttributes() throws Exception {
		MRI syntheticDescriptor = getExtendedSyntheticAttributeDescriptor();
		MRI nonsyntheticDescriptor = getNonSyntheticDescriptor();
		ObjectName mbean = syntheticDescriptor.getObjectName();
		assertTrue("Not same MBean", mbean.equals(nonsyntheticDescriptor.getObjectName())); //$NON-NLS-1$
		String[] attributes = new String[] {nonsyntheticDescriptor.getDataPath(), syntheticDescriptor.getDataPath()};
		AttributeList values = getAttributeValues(mbean, attributes);
		assertTrue("Not two values", values.size() == 2); //$NON-NLS-1$
		for (Object attribute : values) {
			assertNotNull(((Attribute) attribute).getValue());
		}
	}

	@Test
	public void testGetExtendedAttribute() throws Exception {
		MRI descriptor = getExtendedSyntheticAttributeDescriptor();
		Object value = getAttributeValue(descriptor);
		assertNotNull("Could not retrieve the extended attribute value", value); //$NON-NLS-1$
	}

	@Test
	public void testSetExtendedAttribute() throws Exception {
		MRI descriptor = getExtendedSyntheticAttributeDescriptor();
		String newValue = NEW_VALUE;
		String oldValue = (String) getAttributeValue(descriptor);
		setAttributeValue(descriptor, newValue);
		assertEquals("Could not set the attribute value!", NEW_VALUE, getAttributeValue(descriptor)); //$NON-NLS-1$
		setAttributeValue(descriptor, oldValue);
		assertEquals("Could not restore old attribute value!", oldValue, getAttributeValue(descriptor)); //$NON-NLS-1$
	}

	@Test
	public void testSetAttribute() throws Exception {
		MRI descriptor = getSyntheticAttributeDescriptor();
		String newValue = NEW_VALUE;
		String oldValue = (String) getAttributeValue(descriptor);
		setAttributeValue(descriptor, newValue);
		assertEquals("Could not set the attribute value!", NEW_VALUE, getAttributeValue(descriptor)); //$NON-NLS-1$
		setAttributeValue(descriptor, oldValue);
		assertEquals("Could not restore old attribute value!", oldValue, getAttributeValue(descriptor)); //$NON-NLS-1$
	}

	@Test
	public void testGetProperties() throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, Object> values = (Map<String, Object>) getAttributeValue(
				getPropertiesSyntheticAttributeDescriptor());
		assertEquals("Gegga", values.get("denominator")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Moja", values.get("numerator")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(100, values.get("someinteger")); //$NON-NLS-1$
		assertEquals(0.01f, values.get("factor")); //$NON-NLS-1$
		assertEquals(true, values.get("someboolean")); //$NON-NLS-1$
	}

	@Test
	public void testGetNonSyntheticAttribute() throws Exception {
		MRI descriptor = getNonSyntheticDescriptor();
		Boolean value = (Boolean) getAttributeValue(descriptor);
		assertNotNull(value);
	}

	@Test
	public void testSetNonSyntheticAttribute() throws Exception {
		MRI descriptor = getNonSyntheticDescriptor();
		Boolean value = (Boolean) getAttributeValue(descriptor);
		assertNotNull(value);
		setAttributeValue(descriptor, Boolean.valueOf(!value.booleanValue()));
		assertNotSame(value, getAttributeValue(descriptor));
		setAttributeValue(descriptor, value);
		assertEquals(value, getAttributeValue(descriptor));
	}

	@Test
	public void testSetAttributes() throws Exception {
		MRI synthethicDescriptor = getExtendedSyntheticAttributeDescriptor();
		String newValue = NEW_VALUE;
		String oldValue = (String) getAttributeValue(synthethicDescriptor);

		MRI normalDescriptor = getNonSyntheticDescriptor();
		Boolean value = (Boolean) getAttributeValue(normalDescriptor);
		assertNotNull(value);

		setAttributeValues(new MRI[] {synthethicDescriptor, normalDescriptor},
				new Object[] {newValue, Boolean.valueOf(!value.booleanValue())});
		assertEquals("Could not set the attribute value!", NEW_VALUE, getAttributeValue(synthethicDescriptor)); //$NON-NLS-1$
		assertNotSame(value, getAttributeValue(normalDescriptor));

		setAttributeValues(new MRI[] {synthethicDescriptor, normalDescriptor}, new Object[] {oldValue, value});
		assertEquals("Could not restore old attribute value!", oldValue, getAttributeValue(synthethicDescriptor)); //$NON-NLS-1$
		assertEquals(value, getAttributeValue(normalDescriptor));

	}

	@Test
	public void testSyntheticMetadata() throws Exception {
		MRI descriptor = getSyntheticAttributeDescriptor();
		MBeanInfo info = getMetadata(descriptor);
		assertEquals(2, info.getAttributes().length);
		MBeanAttributeInfo attributeInfo = findCorresponding(info.getAttributes(), descriptor);
		assertTrue(attributeInfo.isReadable());
		assertTrue(attributeInfo.isWritable());
		assertFalse(attributeInfo.isIs());
		assertEquals("java.lang.String", attributeInfo.getType()); //$NON-NLS-1$
	}

	@Test
	public void testExtendedMetadata() throws Exception {
		MRI descriptor = getExtendedSyntheticAttributeDescriptor();
		MBeanInfo info = getMetadata(descriptor);
		assertTrue(info.getDescription().contains("Extended")); //$NON-NLS-1$

		MBeanAttributeInfo extendedInfo = null;
		for (MBeanAttributeInfo attr : info.getAttributes()) {
			if (attr.getName().equals(descriptor.getDataPath())) {
				extendedInfo = attr;
			}
		}
		assertNotNull(extendedInfo);
		MBeanAttributeInfo attributeInfo = findCorresponding(info.getAttributes(), descriptor);
		assertTrue(attributeInfo.isReadable());
		assertTrue(attributeInfo.isWritable());
		assertFalse(attributeInfo.isIs());
		assertEquals("java.lang.String", attributeInfo.getType()); //$NON-NLS-1$
	}

	private MBeanAttributeInfo findCorresponding(MBeanAttributeInfo[] attributes, MRI descriptor) {
		for (MBeanAttributeInfo info : attributes) {
			if (descriptor.getDataPath().equals(info.getName())) {
				return info;
			}
		}
		return null;
	}

	private MRI getPropertiesSyntheticAttributeDescriptor() {
		return MRI.createFromQualifiedName("attribute://org.openjdk.jmc.test:type=Test/Properties"); //$NON-NLS-1$
	}

	private boolean containsMBean(ObjectName mbean) throws Exception {
		MBeanServerConnection connection = getLocalConnection().getServiceOrThrow(MBeanServerConnection.class);
		@SuppressWarnings("rawtypes")
		Set mbeans = connection.queryMBeans(mbean, null);
		return mbeans.size() > 0;
	}

	private MRI getSyntheticAttributeDescriptor() {
		return MRI.createFromQualifiedName("attribute://org.openjdk.jmc.test:type=Test/Test"); //$NON-NLS-1$
	}

	private MRI getExtendedSyntheticAttributeDescriptor() {
		return MRI.createFromQualifiedName("attribute://java.lang:type=ClassLoading/Test"); //$NON-NLS-1$
	}

	private MRI getNonSyntheticDescriptor() {
		return MRI.createFromQualifiedName("attribute://java.lang:type=ClassLoading/Verbose"); //$NON-NLS-1$
	}

	private MRI getCompositeAttributeDescriptor() {
		return MRI.createFromQualifiedName("attribute://java.lang:type=OperatingSystem/TotalPhysicalMemorySize"); //$NON-NLS-1$
	}

	private MRI[] getCombinedAttributeDescriptors() {
		return new MRI[] {MRI.createFromQualifiedName("attribute://java.lang:type=Memory/HeapMemoryUsage/used"), //$NON-NLS-1$
				MRI.createFromQualifiedName("attribute://java.lang:type=Memory/NonHeapMemoryUsage/max"), //$NON-NLS-1$
				MRI.createFromQualifiedName("attribute://java.lang:type=Memory/Verbose")}; //$NON-NLS-1$
	}

	private boolean containsDomain(String checkDomain) throws Exception {
		MBeanServerConnection connection = getLocalConnection().getServiceOrThrow(MBeanServerConnection.class);
		boolean containsDomain = false;
		for (String domain : connection.getDomains()) {
			if (checkDomain.equals(domain)) {
				containsDomain = true;
				break;
			}
		}
		return containsDomain;
	}

	private Object getAttributeValue(MRI descriptor) throws Exception {
		MBeanServerConnection connection = getLocalConnection().getServiceOrThrow(MBeanServerConnection.class);
		Object value = connection.getAttribute(descriptor.getObjectName(), descriptor.getDataPath());
		return value;
	}

	private AttributeList getAttributeValues(ObjectName mbean, String[] attributes) throws Exception {
		MBeanServerConnection connection = getLocalConnection().getServiceOrThrow(MBeanServerConnection.class);
		AttributeList value = connection.getAttributes(mbean, attributes);
		return value;
	}

	private Collection<Object> getAttributeValuesThroughMBeanHelperService(ObjectName mbean, List<String> attributes)
			throws Exception {
		MBeanServerConnection service = getLocalConnection().getServiceOrThrow(MBeanServerConnection.class);
		List<MRI> attributeInfos = new ArrayList<>();
		for (String attribute : attributes) {
			attributeInfos.add(new MRI(Type.ATTRIBUTE, mbean, attribute));
		}

		return AttributeValueToolkit.getAttributes(service, attributeInfos).values();
	}

	private MBeanInfo getMetadata(MRI descriptor) throws Exception {
		IConnectionHandle handle = getLocalConnection();
		MBeanServerConnection connection = handle.getServiceOrThrow(MBeanServerConnection.class);
		MBeanInfo value = connection.getMBeanInfo(descriptor.getObjectName());
		return value;
	}

	private void setAttributeValue(MRI descriptor, Object newValue) throws Exception {
		MBeanServerConnection connection = getLocalConnection().getServiceOrThrow(MBeanServerConnection.class);
		connection.setAttribute(descriptor.getObjectName(), new Attribute(descriptor.getDataPath(), newValue));
	}

	private void setAttributeValues(MRI[] descriptors, Object[] values) throws Exception {
		MBeanServerConnection connection = getLocalConnection().getServiceOrThrow(MBeanServerConnection.class);
		AttributeList list = new AttributeList();
		for (int i = 0; i < descriptors.length; i += 1) {
			list.add(new Attribute(descriptors[i].getDataPath(), values[i]));
		}
		connection.setAttributes(descriptors[0].getObjectName(), list);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		localConnection = getDefaultServer().connect("Test"); //$NON-NLS-1$
	}

	@Override
	public void tearDown() throws Exception {
		localConnection.close();
		super.tearDown();
	}

	protected IConnectionHandle getLocalConnection() {
		return localConnection;
	}
}
