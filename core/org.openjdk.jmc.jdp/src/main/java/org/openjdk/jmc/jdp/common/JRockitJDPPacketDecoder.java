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
package org.openjdk.jmc.jdp.common;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.remote.JMXServiceURL;

/**
 * This class provides compatibility with the old style JRockit JDP packets.
 */
final class JRockitJDPPacketDecoder {
	public static final String KEY_VERSION = "version"; //$NON-NLS-1$
	private static final char DELIMITER = '!';

	// FIXME: These keys need to be shared/standardized
	private static final String KEY_SERVICE_URL = "serviceURL"; //$NON-NLS-1$
	private static final String KEY_NAME = "name"; //$NON-NLS-1$

	private JRockitJDPPacketDecoder() {
		throw new AssertionError("Not to be instantiated!"); //$NON-NLS-1$
	}

	public static boolean checkJRockitJDP(byte[] data) {
		if (data[4] == '!') {
			return true;
		}
		return false;
	}

	public static Map<String, String> decodeJRockitJDP(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data, 2, data.length - 2);
		DataInputStream dis = new DataInputStream(bis);
		Map<String, String> result = new HashMap<>();

		String address = null;
		int port = 0;
		boolean supportsJMXRMI = true;

		String payload = null;
		try {
			payload = dis.readUTF();

			StringTokenizer strTok = new StringTokenizer(payload, String.valueOf(DELIMITER), false);

			int totalTokens = strTok.countTokens();
			if (totalTokens >= 4) {
				// *** New protocol with version and jmx information***
				decodeExtendedInfo(result, strTok.nextToken());
				supportsJMXRMI = useJMXMAPI(strTok.nextToken());
				address = strTok.nextToken();
				port = Integer.parseInt(strTok.nextToken());
				if (totalTokens >= 5) {
					// Contains additional user defined name
					result.put(KEY_NAME, strTok.nextToken());
				}
			} else {
				// *** Old protocol ***
				address = strTok.nextToken();
				try {
					port = Integer.parseInt(strTok.nextToken());
				} catch (NumberFormatException nfe) {
				}
				supportsJMXRMI = false;
			}
		} catch (Exception e) {
		}

		try {
			result.put(KEY_SERVICE_URL, createServiceURL(address, port, supportsJMXRMI).toString());
		} catch (MalformedURLException e) {
			// This should not happen - if it does, we simply will have no valid
			// URL in the packet.
		}
		// FIXME: auto-resolve hostname?
		result.put(JDPPacket.KEY_DISCOVERABLE_ID, address + ":" + port); //$NON-NLS-1$
		return result;
	}

	private static boolean useJMXMAPI(String token) {
		if ("1".equals(token)) { //$NON-NLS-1$
			return true;
		}
		return false;
	}

	/**
	 * Creates a jmx over rmi or "jmx over rmp" service URL.
	 *
	 * @param host
	 *            the host name.
	 * @param port
	 *            port or {@link JMXDescriptorBuilder#DEFAULT_PORT} for the default port for the
	 *            selected protocol
	 * @param useJMXRMI
	 *            true if JMX over RMI should be used, false to use JMX over RMP
	 * @return the {@link JMXServiceURL}.
	 * @throws MalformedURLException
	 *             if the URL could not be created with the provided data.
	 */
	private static JMXServiceURL createServiceURL(String host, int port, boolean useJMXRMI)
			throws MalformedURLException {
		if (useJMXRMI) {
			return new JMXServiceURL("rmi", "", 0, "/jndi/rmi://" + host + ":" + port + "/jmxrmi"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		} else {
			return new JMXServiceURL("rmp", host, port); //$NON-NLS-1$
		}
	}

	/**
	 * Decodes the extendedInfo into the hash map.
	 *
	 * @param result
	 * @param extendedInfo
	 */
	private static void decodeExtendedInfo(Map<String, String> result, String extendedInfo) {
		String[] info = extendedInfo.split(","); //$NON-NLS-1$
		if (info == null) {
			return;
		}
		// For backwards compatibility with the very oldest version of JDP...
		if (info.length == 1 && extendedInfo.indexOf('=') < 0) {
			result.put(KEY_VERSION, extendedInfo);
			return;
		}

		for (String element : info) {
			String[] keyValue = element.split("="); //$NON-NLS-1$
			if (keyValue != null && keyValue.length == 2) {
				result.put(keyValue[0], keyValue[1]);
			}
		}
	}
}
