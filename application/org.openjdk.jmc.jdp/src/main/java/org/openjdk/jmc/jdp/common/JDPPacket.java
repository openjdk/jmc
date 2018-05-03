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
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.jdp.client.Discoverable;

/**
 * Class for delivering and retrieving the content.
 */
public final class JDPPacket implements Discoverable {
	static final String KEY_DISCOVERABLE_ID = "DISCOVERABLE_SESSION_UUID"; //$NON-NLS-1$
	// Protocol version will be read as unsigned short
	private static final int PROTOCOL_VERSION = 1;
	private static final byte[] MAGIC = {(byte) 0xC0, (byte) 0xFF, (byte) 0xEE, (byte) 0x42};
	private final Map<String, String> decoded;
	private final byte[] encoded;
	private final String sessionId;

	public JDPPacket(Map<String, String> discoveryData) {
		decoded = discoveryData;
		encoded = encode(discoveryData);
		sessionId = decoded.get(KEY_DISCOVERABLE_ID);
	}

	public JDPPacket(byte[] data) throws CodingException {
		decoded = decode(data);
		encoded = data;
		sessionId = decoded.get(KEY_DISCOVERABLE_ID);
	}

	@Override
	public Map<String, String> getPayload() {
		return decoded;
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	public byte[] getDiscoveryDataAsByteArray() {
		return encoded;
	}

	private static byte[] encode(Map<String, String> discoveryData) throws CodingException {
		// First generate the content
		ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.write(MAGIC);
			dos.writeShort(PROTOCOL_VERSION);

			for (Entry<String, String> entry : discoveryData.entrySet()) {
				if (entry.getValue() != null) {
					dos.writeUTF(entry.getKey());
					dos.writeUTF(entry.getValue());
				}
			}
		} catch (IOException e) {
			throw new CodingException("Problem encoding JDP packet!", e); //$NON-NLS-1$
		}
		return baos.toByteArray();
	}

	private static Map<String, String> decode(byte[] data) throws CodingException {
		if (data.length < 6) {
			throw new CodingException("Corrupt packet! Length was " + data.length); //$NON-NLS-1$
		}
		if (checkMagic(data)) {
			try {
				return decodeHotSpot(data);
			} catch (IOException e) {
				throw new CodingException("Problem decoding JDP packet!", e); //$NON-NLS-1$
			}
		} else if (JRockitJDPPacketDecoder.checkJRockitJDP(data)) {
			return JRockitJDPPacketDecoder.decodeJRockitJDP(data);
		} else {
			throw new CodingException("Packet does not start with JDP magic!"); //$NON-NLS-1$
		}
	}

	private static Map<String, String> decodeHotSpot(byte[] data) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);
		Map<String, String> dataMap = new HashMap<>();
		// Read past magic - magic already verified when dispatching.
		dis.readInt();
		checkVersion(dis.readUnsignedShort());

		// Check for empty packet
		if (dis.available() == 0) {
			return dataMap;
		}

		String value = null;
		try {
			while (true) {
				String key = decodeString(dis);
				value = decodeString(dis);
				dataMap.put(key, value);
			}
		} catch (EOFException e) {
			if (value == null) {
				throw new IOException("Problem decoding JDP packet!", e); //$NON-NLS-1$
			}
		}

		return dataMap;
	}

	/**
	 * Reads a string from the byte buffer.
	 *
	 * @param data
	 *            the byte buffer to read from.
	 * @param size
	 *            the size to read.
	 * @param offset
	 *            the position to read from.
	 * @return the resulting String.
	 * @throws IOException
	 */
	private static String decodeString(DataInputStream dis) throws IOException {
		int length;
		length = dis.readUnsignedShort();
		if (length > dis.available()) {
			throw new CodingException("Discovered corrupt JDP packet!"); //$NON-NLS-1$
		}
		byte[] buf = new byte[length];
		if (dis.read(buf) != length) {
			throw new IOException("Problem decoding string!"); //$NON-NLS-1$
		}
		return new String(buf, "UTF-8"); //$NON-NLS-1$

	}

	/**
	 * Checks if the first bytes in the byte buffer are the JDP magic bytes.
	 *
	 * @param data
	 *            the byte buffer to check.
	 * @return true if the first bytes match, false otherwise.
	 */
	private static boolean checkMagic(byte[] data) {
		for (int i = 0; i < MAGIC.length; i++) {
			if (MAGIC[i] != data[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if this version bytes in the byte buffer are of a version that this class can decode.
	 */
	private static void checkVersion(int version) throws CodingException {
		if (PROTOCOL_VERSION != version) {
			throw new CodingException(
					String.format("Found JDP packet with unsupported version. Version found was %d.", version)); //$NON-NLS-1$
		}
	}

	@Override
	public int hashCode() {
		return sessionId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JDPPacket other = (JDPPacket) obj;
		if (!sessionId.equals(other.sessionId)) {
			return false;
		}
		if (!decoded.equals(other.decoded)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("JDPPacket(%s): (%s)", sessionId, decoded); //$NON-NLS-1$
	}

}
