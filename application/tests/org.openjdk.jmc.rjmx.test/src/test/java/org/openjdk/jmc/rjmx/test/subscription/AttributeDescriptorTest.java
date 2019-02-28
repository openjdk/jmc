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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import javax.management.ObjectName;

import org.junit.Test;

import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;

/**
 */
public class AttributeDescriptorTest {

	@Test
	public void testHashCode() throws Exception {
		MRI descriptor = createDescriptor1();
		MRI anotherDescriptor = createDescriptor1();
		assertTrue(descriptor.hashCode() == anotherDescriptor.hashCode());

		HashSet<MRI> set = new HashSet<>();
		set.add(descriptor);
		set.add(anotherDescriptor);
		assertTrue(set.size() == 1);

	}

	/*
	 * Class under test for void ConsoleAttributeName(String)
	 */
	@Test
	public void testConsoleAttributeNameString() throws Exception {
		MRI descriptor = createDescriptor1();
		MRI anotherDescriptor = null;
		anotherDescriptor = MRI.createFromQualifiedName(descriptor.getQualifiedName());
		assertTrue(descriptor.equals(anotherDescriptor));
	}

	/*
	 * Class under test for void ConsoleAttributeName(String, String)
	 */
	@Test
	public void testConsoleAttributeNameStringString() throws Exception {
		MRI descriptor = createDescriptor1();
		MRI anotherDescriptor = null;
		anotherDescriptor = new MRI(Type.ATTRIBUTE, descriptor.getObjectName().getCanonicalName(),
				descriptor.getDataPath());
		assertTrue(descriptor.equals(anotherDescriptor));
	}

	/*
	 * Class under test for void ConsoleAttributeName(ObjectName, String)
	 */
	@Test
	public void testConsoleAttributeNameObjectNameString() throws Exception {
		MRI descriptor = createDescriptor1();
		MRI anotherDescriptor = null;
		anotherDescriptor = new MRI(Type.ATTRIBUTE, descriptor.getObjectName(), descriptor.getDataPath());
		assertTrue(descriptor.equals(anotherDescriptor));
	}

	@Test
	public void testGetQualifiedName() throws Exception {
		MRI descriptor = createDescriptor1();
		assertEquals(descriptor.getQualifiedName().length(), (descriptor.getType().getTypeName().length() + 3
				+ descriptor.getObjectName().getCanonicalName().length() + 1 + descriptor.getDataPath().length()));
		// Interned.
		assertTrue(descriptor.getQualifiedName() == createDescriptor1().getQualifiedName());
	}

	/*
	 * Class under test for boolean equals(Object)
	 */
	@Test
	public void testEqualsObject() throws Exception {
		MRI descriptor = createDescriptor1();
		MRI anotherDescriptor = createDescriptor1();
		assertTrue(descriptor.equals(anotherDescriptor));
	}

	/*
	 * Class under test for String toString()
	 */
	@Test
	public void testToString() throws Exception {
		// Should be interned.
		assertTrue(createDescriptor1().toString() == createDescriptor1().toString());
	}

	@Test
	public void testCreateFromQualifiedName1() throws Exception {
		MRI descriptor = MRI.createFromQualifiedName("attribute://java.lang:type=OperatingSystem/Arch");
		assertEquals(createDescriptor1(), descriptor);
	}

	@Test
	public void testCreateFromQualifiedName2() throws Exception {
		MRI descriptor = MRI.createFromQualifiedName("attribute://java.lang:type=OperatingSystem/Arch/Sub");
		assertEquals(Type.ATTRIBUTE, descriptor.getType());
		assertEquals("java.lang:type=OperatingSystem", descriptor.getObjectName().getCanonicalName());
		assertEquals("Arch/Sub", descriptor.getDataPath());
	}

	@Test
	public void testMalformedQualifiedName1() throws Exception {
		try {
			MRI.createFromQualifiedName("smurf://java.lang:type=OperatingSystem/Arch");
		} catch (IllegalArgumentException iae) {
			return;
		}
		assertTrue("Should not be possible to create an attribute with the type smurf!", false);
	}

	@Test
	public void testMalformedQualifiedName2() throws Exception {
		try {
			MRI.createFromQualifiedName("java.lang:type=OperatingSystem/Arch");
		} catch (IllegalArgumentException iae) {
			return;
		}
		assertTrue("Should not be possible to create an attribute name without specifying a type!", false);
	}

	private MRI createDescriptor1() throws Exception {
		return new MRI(Type.ATTRIBUTE, new ObjectName("java.lang:type=OperatingSystem"), "Arch");
	}

}
