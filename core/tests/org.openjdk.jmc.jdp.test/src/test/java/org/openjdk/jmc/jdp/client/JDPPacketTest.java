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
package org.openjdk.jmc.jdp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.openjdk.jmc.jdp.common.JDPPacket;

@SuppressWarnings("nls")
public class JDPPacketTest {
	private final static String KEY_NAME = "Name";
	private final static String KEY_SKILLS = "Skills";
	private final static String VALUE_NAME = "Duke";
	private final static String VALUE_SKILLS = "Barrista, Programmer";

	@Test
	public void testCreatePacket() throws Exception {
		new JDPPacket(createDefaultProperties());
	}

	@Test
	public void testEncodeDecode() throws Exception {
		JDPPacket packet = new JDPPacket(createDefaultProperties());
		byte[] bytes = packet.getDiscoveryDataAsByteArray();
		JDPPacket packet2 = new JDPPacket(bytes);
		Map<String, String> props = packet2.getPayload();
		assertEquals(props.get(KEY_NAME), VALUE_NAME);
		assertEquals(props.get(KEY_SKILLS), VALUE_SKILLS);
	}

	static Map<String, String> createDefaultProperties() {
		Map<String, String> props = new HashMap<>();
		props.put(KEY_NAME, VALUE_NAME);
		props.put(KEY_SKILLS, VALUE_SKILLS);
		return props;
	}

	@Test
	public void testEncodeDecodeEmpty() throws Exception {
		JDPPacket packet = new JDPPacket(new HashMap<String, String>());
		byte[] bytes = packet.getDiscoveryDataAsByteArray();
		JDPPacket packet2 = new JDPPacket(bytes);
		assertTrue(packet2.getPayload().size() == 0);
	}

	@Test
	public void testTestToolkit() {
		assertEquals("JMX_SERVICE_URL", TestToolkit
				.parseCommaSeparatedByteString("74, 77, 88, 95, 83, 69, 82, 86, 73, 67, 69, 95, 85, 82, 76"));
	}
}
