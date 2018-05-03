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
package org.openjdk.jmc.flightrecorder.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

@SuppressWarnings("nls")
public class RulesToolkitTest {
	private static final String JAVA_7_14_EA = "Java HotSpot(TM) Client VM (24.0-b36) for windows-x86 JRE (1.7.0_14-ea-b17), built on Mar 20 2013 10:28:38 by \"java_re\" with unknown MS VC++:1600";
	private static final String JAVA_8_40 = "Java HotSpot(TM) 64-Bit Server VM (25.60-b23) for windows-amd64 JRE (1.8.0_60-b31), built on Aug 12 2015 14:45:33 by \"java_re\" with MS VC++ 10.0 (VS2010)";;

	@Test
	public void testMapLin100Straight() {
		assertEquals(0, RulesToolkit.mapLin100(0, 0.25, 0.75), 0);
		assertEquals(1, RulesToolkit.mapLin100(1, 0.25, 0.75), 0);
		assertEquals(12, RulesToolkit.mapLin100(0.12, 0.25, 0.75), 0);
		assertEquals(25, RulesToolkit.mapLin100(0.25, 0.25, 0.75), 0);
		assertEquals(50, RulesToolkit.mapLin100(0.50, 0.25, 0.75), 0);
		assertEquals(75, RulesToolkit.mapLin100(0.75, 0.25, 0.75), 0);
		assertEquals(73, RulesToolkit.mapLin100(0.73, 0.25, 0.75), 0);
	}

	@Test
	public void testMapLin100Multi() {
		assertEquals(0, RulesToolkit.mapLin100(0, 0.1, 0.9), 0);
		assertEquals(1, RulesToolkit.mapLin100(1, 0.1, 0.9), 0);
		assertEquals(25, RulesToolkit.mapLin100(0.1, 0.1, 0.9), 0);
		assertEquals(26.25, RulesToolkit.mapLin100(0.12, 0.1, 0.9), 0);
		assertEquals(34.375, RulesToolkit.mapLin100(0.25, 0.1, 0.9), 0);
		assertEquals(50, RulesToolkit.mapLin100(0.50, 0.1, 0.9), 0);
		assertEquals(65.625, RulesToolkit.mapLin100(0.75, 0.1, 0.9), 0);
		assertEquals(75, RulesToolkit.mapLin100(0.9, 0.1, 0.9), 0);
	}

	@Test
	public void testGetJavaVersionEA() {
		JavaVersion javaVersion = RulesToolkit.getJavaVersion(JAVA_7_14_EA);
		assertTrue(javaVersion.isEarlyAccess());
	}

	@Test
	public void testGetJavaVersion() {
		JavaVersion javaVersion = RulesToolkit.getJavaVersion(JAVA_8_40);
		assertFalse(javaVersion.isEarlyAccess());
	}
}
