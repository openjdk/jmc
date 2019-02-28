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
package org.openjdk.jmc.rjmx.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Test;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;

/**
 * Sanity test for the default services available from the default implementation of
 * {@link IConnectionHandle} for Mission Control.
 */
public class DefaultServicesTest extends ServerHandleTestCase {

	@Test
	public void testMBeanServerConnection() throws Exception {
		IConnectionHandle handle = getDefaultServer().connect("Test");
		MBeanServerConnection connection = handle.getServiceOrThrow(MBeanServerConnection.class);

		String[] domains = connection.getDomains();
		assertNotNull(connection.getDomains());
		// At least java.lang, no matter what, or we're breaking J2SE compliance...
		for (String domain : domains) {
			if (domain.equals("java.lang")) {
				return;
			}
		}
		fail("Could not find java.lang.management among the domains!");
	}

	@Test
	public void xtestMBeanHelperService() throws Exception {
		IConnectionHandle handle = getDefaultServer().connect("Test");
		IMBeanHelperService helper = handle.getServiceOrThrow(IMBeanHelperService.class);

		// FIXME: JMC-4270 - Server time approximation is not reliable. Disabling until a solution is found.
//		long time = System.currentTimeMillis();
//
//		// The server time calculations should not be this much off.
//		long diff = time - helper.getApproximateServerTime(time);
//		assertLessThan("Server time approximation off by more than five seconds", 5000L, Math.abs(diff));
//		System.out.println("DefaultServicesTest.testMBeanHelperService(): Server time approximation difference = "
//				+ Math.abs(diff) + " ms");

		// Should at least contain the java.lang mbeans. Just testing for the Threading one.
		assertTrue("Could not find the Threading MBean!", helper.getMBeanNames().contains(
				new ObjectName("java.lang:type=Threading")));
	}
}
