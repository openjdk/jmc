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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.logging.Level;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.jdp.common.Configuration;
import org.openjdk.jmc.jdp.server.JDPServer;
import org.openjdk.jmc.jdp.server.jmx.JMXJDPServer;

@SuppressWarnings("nls")
public final class TestToolkit {
	private static final String HEXES = "0123456789ABCDEF";
	private final static SecureRandom RND = new SecureRandom();
	public static final int TEST_MULTICAST_PORT = 7711;
	private static final String TEST_MULTICAST_ADDRESS_STRING = "224.0.23.177";
	public static final InetAddress TEST_MULTICAST_ADDRESS;

	static {
		InetAddress tmp = null;
		try {
			tmp = InetAddress.getByName(TEST_MULTICAST_ADDRESS_STRING);
		} catch (UnknownHostException e) {
			// Multicast address by IP, should never happen!
			JDPClientTest.LOGGER.log(Level.SEVERE, "Could not create test multicast address!", e);
		}
		TEST_MULTICAST_ADDRESS = tmp;
	}

	private TestToolkit() {
		throw new AssertionError("Nope!");
	}

	public static String toHexString(byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}

	public static long nextLong() {
		return RND.nextLong();
	}

	public static String generateNewID(String prefix) {
		return String.format("%s %X", prefix, RND.nextLong());
	}

	public static void printServerSettings(JDPServer server) {
		System.out.println(
				String.format("JDP Server created at %s:%d", server.getConfiguration().getMulticastAddress().toString(),
						server.getConfiguration().getMulticastPort()));
	}

	public static JDPServer createDefaultJMXJDPServer(String discoverableID) throws MalformedURLException {
		return new JMXJDPServer(discoverableID, createConfiguration(), createServiceURL("localhost", 7091), null);
	}

	public static JMXServiceURL createServiceURL(String host, int port) throws MalformedURLException {
		return new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port));
	}

	public static String parseCommaSeparatedByteString(String str) {
		String[] tmp = str.split(", ");
		byte[] bytes = toBytes(tmp);
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static Configuration createConfiguration() {
		return new Configuration() {

			@Override
			public short getTTL() {
				return 1;
			}

			@Override
			public int getMulticastPort() {
				return TEST_MULTICAST_PORT;
			}

			@Override
			public InetAddress getMulticastAddress() {
				return TEST_MULTICAST_ADDRESS;
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

	private static byte[] toBytes(String[] tmp) {
		byte[] bytes = new byte[tmp.length];
		for (int i = 0; i < tmp.length; i++) {
			bytes[i] = Byte.parseByte(tmp[i]);
		}
		return bytes;
	}
}
