/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

/**
 * A toolkit for strings.
 */
public final class StringToolkit {

	private StringToolkit() {
		throw new AssertionError("This is not the constructor you are looking for!"); //$NON-NLS-1$
	}

	/**
	 * Reads the contents of a stream to a string, assuming UTF-8 encoding.
	 *
	 * @param in
	 *            the stream to read from
	 * @return a string with the contents available from the stream
	 * @throws IOException
	 *             if something went wrong
	 */
	public static String readString(InputStream in) throws IOException {
		return readString(in, StandardCharsets.UTF_8);
	}

	/**
	 * Reads the contents of a stream with specified character encoding to a string.
	 *
	 * @param in
	 *            the stream to read from
	 * @param charset
	 *            the {@link java.nio.charset.Charset charset}
	 * @return a string with the contents available from the stream
	 * @throws IOException
	 *             if something went wrong
	 */
	public static String readString(InputStream in, Charset charset) throws IOException {
		ByteArrayOutputStream buf = read(in);
		return buf.toString(charset);
	}

	/**
	 * Reads the contents of a stream with specified character encoding to a string.
	 *
	 * @param in
	 *            the stream to read from
	 * @param charsetName
	 *            the name of a supported {@link java.nio.charset.Charset charset}
	 * @return a string with the contents available from the stream
	 * @throws IOException
	 *             if something went wrong
	 */
	public static String readString(InputStream in, String charsetName) throws IOException {
		ByteArrayOutputStream buf = read(in);
		return buf.toString(charsetName);
	}

	private static ByteArrayOutputStream read(InputStream in) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while (result != -1) {
			buf.write((byte) result);
			result = bis.read();
		}
		return buf;
	}

	/**
	 * Encode a string so that it becomes safe to use as a file name.
	 *
	 * @param string
	 *            string to encode
	 * @return a string usable as a file name
	 */
	public static String encodeFilename(String string) {
		return URLEncoder.encode(string, StandardCharsets.UTF_8);
	}

	/**
	 * Joins items in collection to one string with delimiter between each item.
	 *
	 * @param s
	 *            collection of items
	 * @param delimiter
	 *            string to put between collection items
	 * @return a string joining all items in the collection with delimiter
	 */
	public static String join(Collection<?> s, String delimiter) {
		StringBuilder builder = new StringBuilder();
		Iterator<?> iter = s.iterator();
		while (iter.hasNext()) {
			builder.append(iter.next());
			if (!iter.hasNext()) {
				break;
			}
			builder.append(delimiter);
		}
		return builder.toString();
	}

	/**
	 * Joins items in an array to one string with delimiter between each item.
	 *
	 * @param a
	 *            array of items
	 * @param delimiter
	 *            string to put between collection items
	 * @return a string joining all items in the array with delimiter
	 */
	public static String join(Object[] a, String delimiter) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < a.length - 1; i++) {
			builder.append(String.valueOf(a[i]));
			builder.append(delimiter);
		}
		builder.append(a[a.length - 1]);
		return builder.toString();
	}

}
