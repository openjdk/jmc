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
package org.openjdk.jmc.common.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JavaVersionTest {

	@Test
	public void testJava9NewShortGEQJava9OldVersion() {
		JavaVersion ver1 = new JavaVersion("9"); //$NON-NLS-1$
		JavaVersion ver2 = new JavaVersion("1.9.foo"); //$NON-NLS-1$

		assertTrue(ver1.isGreaterOrEqualThan(ver2));
	}

	@Test
	public void testJava9NewShortEQJava9NewLongSameVersion() {
		JavaVersion version9 = new JavaVersion("9-ea"); //$NON-NLS-1$
		JavaVersion version9000 = new JavaVersion("9.0.0.0-ea"); //$NON-NLS-1$

		assertTrue(version9000.equals(version9));
	}

	@Test
	public void testJava9NewShortWithExtraEQJava9NewLongSameVersion() {
		JavaVersion version190 = new JavaVersion("9-ea+19-BR-435345"); //$NON-NLS-1$
		JavaVersion version900 = new JavaVersion("9.0.0.0-ea"); //$NON-NLS-1$

		assertTrue(version900.equals(version190));
	}

	@Test
	public void testJava9NewWithExtraGEQJava9OldVersion() {
		JavaVersion version913 = new JavaVersion("9.1.3.0"); //$NON-NLS-1$
		JavaVersion version190 = new JavaVersion("1.9.0-ea+19-BR-435345"); //$NON-NLS-1$

		assertTrue(version913.isGreaterOrEqualThan(version190));
	}

	/*
	 * NOTE: This test does not really have to pass when the new Java 9 version is committed, but
	 * since we are using this for testing before that happens, we need this to work.
	 */
	@Test
	public void testJava9OldVersionGEQJava9NewWithExtra() {
		JavaVersion version913 = new JavaVersion("9.1.3.0"); //$NON-NLS-1$
		JavaVersion version192 = new JavaVersion("1.9.2-ea+19-BR-435345"); //$NON-NLS-1$

		assertTrue(version192.isGreaterOrEqualThan(version913));
	}

	@Test
	public void testJava9LongVersionisGreaterOrEqualsJava9ShortVersion() {
		JavaVersion version9 = new JavaVersion("9"); //$NON-NLS-1$
		JavaVersion version921 = new JavaVersion("9.2.1.0"); //$NON-NLS-1$

		assertTrue(version921.isGreaterOrEqualThan(version9));
	}

	@Test
	public void testJava9isGreaterOrEqualsVersion() {
		JavaVersion version913 = new JavaVersion("9.1.3.0-ea"); //$NON-NLS-1$
		JavaVersion version921 = new JavaVersion("9.2.1.0"); //$NON-NLS-1$

		assertTrue(version921.isGreaterOrEqualThan(version913));
	}

	@Test
	public void testJava9isGreaterOrEqualsVersionEA() {
		JavaVersion version921ea = new JavaVersion("9.2.1.0-ea"); //$NON-NLS-1$
		JavaVersion version921 = new JavaVersion("9.2.1.0"); //$NON-NLS-1$

		assertTrue(version921.isGreaterOrEqualThan(version921ea));
		assertTrue(!version921ea.isGreaterOrEqualThan(version921));
	}

	@Test
	public void testGreaterOrEqualsMediumVsLongVersion() {
		JavaVersion version123 = new JavaVersion("1.2.3"); //$NON-NLS-1$
		JavaVersion version12245 = new JavaVersion("1.2.2.4.5"); //$NON-NLS-1$

		assertTrue(version123.isGreaterOrEqualThan(version12245));
	}

	@Test
	public void testGreaterOrEqualsLongVsMediumVersion() {
		JavaVersion version12345 = new JavaVersion("1.2.3.4.5"); //$NON-NLS-1$
		JavaVersion version12 = new JavaVersion("1.2"); //$NON-NLS-1$

		assertTrue(version12345.isGreaterOrEqualThan(version12));
	}

	@Test
	public void testJava9isGreaterOrEqualsThanJava8Version() {
		JavaVersion version903 = new JavaVersion("9.0.3.0-ea"); //$NON-NLS-1$
		JavaVersion version8u40 = new JavaVersion("1.8.0_40"); //$NON-NLS-1$

		assertTrue(version903.isGreaterOrEqualThan(version8u40));
	}

	@Test
	public void testGEQ() {
		JavaVersion version16 = new JavaVersion("1.6.0_14ea"); //$NON-NLS-1$
		JavaVersion version17 = new JavaVersion("1.7.0_0"); //$NON-NLS-1$

		assertTrue(version17.isGreaterOrEqualThan(version16));
		assertFalse(version16.isGreaterOrEqualThan(version17));
		assertTrue(version16.isGreaterOrEqualThan(version16));
		assertTrue(version17.equals(version17));
	}

	@Test
	public void testGEQJava7EA() {
		JavaVersion version17ea = new JavaVersion("1.7.0_0ea"); //$NON-NLS-1$
		JavaVersion version17 = new JavaVersion("1.7.0_0"); //$NON-NLS-1$

		assertTrue(version17.isGreaterOrEqualThan(version17ea));
		assertFalse(version17ea.isGreaterOrEqualThan(version17));
	}

	@Test
	public void testGEQWithMicro() {
		JavaVersion version142 = new JavaVersion("1.4.2_14"); //$NON-NLS-1$
		JavaVersion version131 = new JavaVersion("1.3.1_67"); //$NON-NLS-1$

		assertTrue(version142.isGreaterOrEqualThan(version131));
		assertFalse(version131.isGreaterOrEqualThan(version142));
		assertTrue(version131.isGreaterOrEqualThan(version131));
		assertTrue(version142.equals(version142));
	}

	@Test
	public void testGEQCropped() {
		JavaVersion version17 = new JavaVersion("1.7"); //$NON-NLS-1$
		JavaVersion version17u12 = new JavaVersion("1.7.0_12"); //$NON-NLS-1$

		assertTrue(version17u12.isGreaterOrEqualThan(version17));
		assertFalse(version17u12.equals(version17));
	}

	@Test
	public void testIsReverseNumbers() {
		JavaVersion version17u13 = new JavaVersion("1.7.12_13ea"); //$NON-NLS-1$
		JavaVersion version17u31 = new JavaVersion("1.7.12_31"); //$NON-NLS-1$

		assertTrue(version17u31.isGreaterOrEqualThan(version17u13));
		assertFalse(version17u31.equals(version17u13));
	}

	@Test
	public void testOldVersionShortAndLongEquals() {
		JavaVersion version17u0 = new JavaVersion("1.7.0_0"); //$NON-NLS-1$
		JavaVersion version17 = new JavaVersion("1.7"); //$NON-NLS-1$

		assertTrue(version17u0.equals(version17));
	}
	
	@Test
	public void testNullStringArgument() {
		JavaVersion nullVersion = new JavaVersion((String) null);
		assertEquals(JavaVersion.UNKNOWN, nullVersion.getMajorVersion());
	}
}
