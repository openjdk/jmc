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
package org.openjdk.jmc.agent.test.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

import org.openjdk.jmc.agent.test.InstrumentMe;

public final class TestToolkit {
	public static final Random RND = new Random();

	private TestToolkit() {
		throw new UnsupportedOperationException("Not to be instantiated."); //$NON-NLS-1$
	}

	public static byte[] getByteCode(Class<?> c) throws IOException {
		InputStream is = c.getClassLoader().getResourceAsStream(c.getName().replace('.', '/') + ".class"); //$NON-NLS-1$
		return readFully(is, -1, true);
	}

	public static byte[] readFully(InputStream is, int length, boolean readAll) throws IOException {
		byte[] output = {};
		if (length == -1) {
			length = Integer.MAX_VALUE;
		}
		int pos = 0;
		while (pos < length) {
			int bytesToRead;
			if (pos >= output.length) { // Only expand when there's no room
				bytesToRead = Math.min(length - pos, output.length + 1024);
				if (output.length < pos + bytesToRead) {
					output = Arrays.copyOf(output, pos + bytesToRead);
				}
			} else {
				bytesToRead = output.length - pos;
			}
			int cc = is.read(output, pos, bytesToRead);
			if (cc < 0) {
				if (readAll && length != Integer.MAX_VALUE) {
					throw new EOFException("Detect premature EOF"); //$NON-NLS-1$
				} else {
					if (output.length != pos) {
						output = Arrays.copyOf(output, pos);
					}
					break;
				}
			}
			pos += cc;
		}
		return output;
	}

	public static long randomLong() {
		return RND.nextLong();
	}

	public static String randomString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < RND.nextInt(10) + 1; i++) {
			builder.append(Character.toString((char) (RND.nextInt(26) + 64)));
		}
		return builder.toString();
	}

	public static InputStream getProbesXML(String testName) {
		try {
			String s = readTemplate();
			s = s.replaceAll("%TEST_NAME%", testName); //$NON-NLS-1$
			return new ByteArrayInputStream(s.getBytes());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String readTemplate() throws IOException {
		InputStream inputStream = InstrumentMe.class.getResourceAsStream("jfrprobes_template.xml"); //$NON-NLS-1$
		String s = readString(inputStream);
		closeSilently(inputStream);
		return s;
	}

	public static String readString(InputStream in) throws IOException {
		return readString(new BufferedReader(new InputStreamReader(in), 8192));
	}

	public static String readString(InputStream in, String charsetName) throws IOException {
		return readString(new BufferedReader(new InputStreamReader(in, charsetName), 8192));
	}

	private static String readString(BufferedReader reader) throws IOException {
		String s;
		StringBuilder builder = new StringBuilder();
		try {
			while ((s = reader.readLine()) != null) {
				builder.append(s + "\r"); //$NON-NLS-1$
			}
			s = builder.toString();
		} finally {
			closeSilently(reader);
		}
		return s;
	}

	private static void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Throwable t) {

			}
		}
	}
}
