/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.common.util;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.io.IOToolkit;

public class IOToolkitTest {
	private static final int MAGIC_ZIP[] = new int[] {80, 75, 3, 4};
	private static final int MAGIC_GZ[] = new int[] {31, 139};

	private static final String UNCOMPRESSED = "test.txt";
	private static final String GZ = "test.txt.gz";
	private static final String LZ4 = "test.txt.lz4";
	private static final String ZIP = "test.txt.zip";

	private static final String GURKA = "Gurka";

	@Test
	public void testGetMagics() {
		Assert.assertArrayEquals(MAGIC_ZIP, IOToolkit.getZipMagic());
		Assert.assertArrayEquals(MAGIC_GZ, IOToolkit.getGzipMagic());
	}

	@Test
	public void testUncompressUncompressed() throws IOException {
		InputStream uncompressedStream = IOToolkit.openUncompressedStream(getStream(UNCOMPRESSED));
		String string = readFromStream(uncompressedStream);
		Assert.assertEquals("String should be " + GURKA, GURKA, string);
	}

	@Test
	public void testUncompressZipped() throws IOException {
		InputStream uncompressedStream = IOToolkit.openUncompressedStream(getStream(ZIP));
		String string = readFromStream(uncompressedStream);
		Assert.assertEquals("String should be " + GURKA, GURKA, string);
	}

	@Test
	public void testUncompressGZipped() throws IOException {
		InputStream uncompressedStream = IOToolkit.openUncompressedStream(getStream(GZ));
		String string = readFromStream(uncompressedStream);
		Assert.assertEquals("String should be " + GURKA, GURKA, string);
	}

	@Test
	public void testUncompressLZ4() throws IOException {
		InputStream uncompressedStream = IOToolkit.openUncompressedStream(getStream(LZ4));
		String string = readFromStream(uncompressedStream);
		Assert.assertEquals("String should be " + GURKA, GURKA, string);
	}

	public InputStream getStream(String resourceName) throws IOException {
		InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName);
		if (stream == null) {
			throw new IOException("Could not find the resource " + resourceName);
		}
		return stream;
	}

	public String readFromStream(InputStream stream) throws IOException {
		StringBuilder builder = new StringBuilder();
		int c = 0;
		while ((c = stream.read()) != -1) {
			builder.append((char) c);
		}
		return builder.toString();
	}

}
