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
package org.openjdk.jmc.rjmx.test.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.ICommercialFeaturesService;
import org.openjdk.jmc.rjmx.test.ServerHandleTestCase;

public class CommercialFeaturesServiceTest extends ServerHandleTestCase {
	@Test
	public void testGetService() throws ConnectionException {
	}

	@Test
	public void testGetCommercialFeaturesService() throws ConnectionException {
		getCommercialFeaturesService();
	}

	@Test
	public void testReadCommercialFeaturesState() throws ConnectionException {
		ICommercialFeaturesService service = getCommercialFeaturesService();
		// Check state. Any state is okay, but we want to catch exceptions.
		service.isCommercialFeaturesEnabled();
	}

	@Test
	public void testSetCommercialFeaturesState() throws Exception {
		ICommercialFeaturesService service = getCommercialFeaturesService();
		// Check state. Any state is okay, but we want to catch exceptions.
		if (!service.isCommercialFeaturesEnabled()) {
			service.enableCommercialFeatures();
		}
		assertTrue("Commercial features should now be enabled!", service.isCommercialFeaturesEnabled());
	}

	private ICommercialFeaturesService getCommercialFeaturesService() throws ConnectionException {
		IConnectionHandle handle = getConnectionHandle();

		// LocalRJMXTestToolkit.createDefaultConnectionHandle(getConnectionManager());
		assumeHotSpot7u4OrLater(handle);

		ICommercialFeaturesService service = handle.getServiceOrNull(ICommercialFeaturesService.class);

		assertNotNull(
				"Could not retrieve the commercial features service. Please make sure that you are connecting to a Java 7u4 or later JVM.",
				service);
		return service;
	}
}
