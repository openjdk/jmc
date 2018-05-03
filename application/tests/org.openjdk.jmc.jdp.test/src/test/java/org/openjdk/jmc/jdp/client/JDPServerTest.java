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

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;

import org.junit.Assume;
import org.junit.Test;

import org.openjdk.jmc.jdp.common.Configuration;
import org.openjdk.jmc.jdp.server.JDPServer;

@SuppressWarnings("nls")
public class JDPServerTest {
	@Test
	public void testStartStopServer() throws Exception {
		skip("This test should not be run unless we start using the JDP server for the JMC client.");

		JDPServer server = new JDPServer(getNewId(), createCustomConfiguration());
		TestToolkit.printServerSettings(server);

		server.setDiscoveryData(JDPPacketTest.createDefaultProperties());
		server.start();
		assertTrue(server.isAlive());
		Thread.sleep(10000);
		server.stop();
		assertTrue(!server.isAlive());
	}

	@Test
	public void testRepeatedStartStopServer() throws Exception {
		skip("This test should not be run unless we start using the JDP server for the JMC client.");

		JDPServer server = new JDPServer(getNewId(), TestToolkit.createConfiguration());
		TestToolkit.printServerSettings(server);

		server.setDiscoveryData(JDPPacketTest.createDefaultProperties());
		for (int i = 0; i < 200; i++) {
			server.start();
			assertTrue("Server should be alive!", server.isAlive());
			server.stop();
			assertTrue("Server should be dead!", !server.isAlive());
			Thread.yield();
		}
	}

	@Test
	public void testStartEmptyPacket() throws Exception {
		JDPServer server = new JDPServer(getNewId(), TestToolkit.createConfiguration());
		server.start();
		assertTrue(server.isAlive());
		Thread.sleep(4000);
		server.stop();
	}

	private static String getNewId() {
		return TestToolkit.generateNewID("JDPServerTest");
	}

	private static Configuration createCustomConfiguration() {
		return new Configuration() {
			@Override
			public short getTTL() {
				return Configuration.DEFAULT_TTL;
			}

			@Override
			public int getMulticastPort() {
				return TestToolkit.TEST_MULTICAST_PORT;
			}

			@Override
			public InetAddress getMulticastAddress() {
				return TestToolkit.TEST_MULTICAST_ADDRESS;
			}

			@Override
			public int getBroadcastPeriod() {
				return 1000;
			}

			@Override
			public int getMaxHeartBeatTimeout() {
				return Configuration.DEFAULT_MAX_HEART_BEAT_TIMEOUT;
			}
		};
	}

	/**
	 * @see Assume
	 */
	private final void skip(String message) {
		Assume.assumeTrue(message, false);
	}
}
