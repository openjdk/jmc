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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.JVMSupportToolkit;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.internal.ServerHandle;
import org.openjdk.jmc.rjmx.messages.internal.Messages;
import org.openjdk.jmc.ui.common.jvm.JVMArch;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.jvm.JVMType;

@SuppressWarnings("nls")
public class JVMSupportToolkitTest {

	// FIXME: Add tests for the methods that take IConnectionHandle as a parameter.

	private static final String SUPPORTED_MESSAGE = null;

	@Test
	public void testJfrNoInfoSupported() {
		ServerHandle server = new ServerHandle(new ServerDescriptor(null, null, null),
				new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(), null);
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, false);
		assertEquals(SUPPORTED_MESSAGE, errorMessage);
	}

	@Test
	public void testJfr17U40HotSpotSupported() {
		ServerHandle server = new ServerHandle(
				new ServerDescriptor(null, null,
						new JVMDescriptor("1.7.0_40", JVMType.HOTSPOT, JVMArch.UNKNOWN, null, null, null, false, null)),
				new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(), null);
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, false);
		assertEquals(SUPPORTED_MESSAGE, errorMessage);
	}

	@Test
	public void testJfr17U4HotSpotNotFullySupported() {
		ServerHandle server = new ServerHandle(
				new ServerDescriptor(null, null,
						new JVMDescriptor("1.7.0_04", JVMType.HOTSPOT, JVMArch.UNKNOWN, null, null, null, false, null)),
				new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(), null);
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, false);
		assertEquals(Messages.JVMSupport_FLIGHT_RECORDER_NOT_FULLY_SUPPORTED_OLD_HOTSPOT, errorMessage);
	}

	@Test
	public void testJfr17HotSpotNotSupported() {
		ServerHandle server = new ServerHandle(
				new ServerDescriptor(null, null,
						new JVMDescriptor("1.7.0", JVMType.HOTSPOT, JVMArch.UNKNOWN, null, null, null, false, null)),
				new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(), null);
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, false);
		assertEquals(Messages.JVMSupport_FLIGHT_RECORDER_NOT_SUPPORTED_OLD_HOTSPOT, errorMessage);
	}

	@Test
	public void testJfrJRockitNotSupported() {
		ServerHandle server = new ServerHandle(
				new ServerDescriptor(null, null,
						new JVMDescriptor("1.6", JVMType.JROCKIT, JVMArch.UNKNOWN, null, null, null, false, null)),
				new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(), null);
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, false);
		assertEquals(Messages.JVMSupport_JROCKIT_NOT_LONGER_SUPPORTED, errorMessage);
	}

	@Test
	public void testJfrOldHotSpotNotSupported() {
		ServerHandle server = new ServerHandle(
				new ServerDescriptor(null, null,
						new JVMDescriptor("1.6", JVMType.HOTSPOT, JVMArch.UNKNOWN, null, null, null, false, null)),
				new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(), null);
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, false);
		assertEquals(Messages.JVMSupport_FLIGHT_RECORDER_NOT_SUPPORTED_OLD_HOTSPOT, errorMessage);
	}

	@Test
	public void testJfrNonHotSpotNotSupported() {
		ServerHandle server = new ServerHandle(
				new ServerDescriptor(null, null,
						new JVMDescriptor("1.7", JVMType.OTHER, JVMArch.UNKNOWN, null, null, null, false, null)),
				new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(), null);
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, false);
		assertEquals(Messages.JVMSupport_FLIGHT_RECORDER_NOT_SUPPORTED_NOT_HOTSPOT, errorMessage);
	}

	@Test
	public void testJfrUnknownNoWarning() {
		ServerHandle server = new ServerHandle(
				new ServerDescriptor(null, null,
						new JVMDescriptor("1.7", JVMType.UNKNOWN, JVMArch.UNKNOWN, null, null, null, false, null)),
				new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(), null);
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, false);
		assertEquals(SUPPORTED_MESSAGE, errorMessage);
	}

}
