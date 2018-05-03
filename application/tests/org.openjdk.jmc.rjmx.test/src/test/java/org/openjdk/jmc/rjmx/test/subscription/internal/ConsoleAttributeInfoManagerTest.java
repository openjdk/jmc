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
package org.openjdk.jmc.rjmx.test.subscription.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.internal.IIntervalUpdatePolicy;
import org.openjdk.jmc.rjmx.subscription.internal.UpdatePolicyToolkit;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;

/**
 * Tests the attribute manager.
 */
public class ConsoleAttributeInfoManagerTest extends RjmxTestCase {
	IMRIMetadataService m_manager;

	@Test
	public void testGetAttributeInfo() {
		MRI descriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=OperatingSystem", //$NON-NLS-1$
				"AvailableProcessors"); //$NON-NLS-1$
		IMRIMetadata info = m_manager.getMetadata(descriptor);
		assertNotNull(info);

		assertEquals(5000,
				((IIntervalUpdatePolicy) UpdatePolicyToolkit.getUpdatePolicy(m_connectionHandle, info.getMRI()))
						.getIntervalTime());
		assertMin("Description shorter than expected.", 10, info.getDescription().length()); //$NON-NLS-1$
		assertMin("Display name shorter than expected.", 4, MRIMetadataToolkit.getDisplayName(m_connectionHandle, //$NON-NLS-1$
				info.getMRI()).length());
		assertTrue(info.getMRI().getParentMRIs().length == 0);
	}

	@Test
	public void testCompositeIsChild() {
		MRI descriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage/committed"); //$NON-NLS-1$ //$NON-NLS-2$
		IMRIMetadata info = m_manager.getMetadata(descriptor);
		assertNotNull(info);
		MRI parent = new MRI(Type.ATTRIBUTE, "java.lang:type=Memory", "HeapMemoryUsage"); //$NON-NLS-1$ //$NON-NLS-2$
		IMRIMetadata parentInfo = m_manager.getMetadata(parent);
		assertNotNull(parentInfo);
		assertTrue("Info not child to parent!", parent.isChild(descriptor)); //$NON-NLS-1$
	}

	@Before
	public void setUp() throws Exception {
		m_manager = m_connectionHandle.getServiceOrThrow(IMRIMetadataService.class);
	}

	@After
	public void tearDown() throws Exception {
		m_manager = null;
	}
}
