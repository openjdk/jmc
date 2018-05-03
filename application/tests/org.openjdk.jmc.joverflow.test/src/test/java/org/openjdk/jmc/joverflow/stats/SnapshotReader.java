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
package org.openjdk.jmc.joverflow.stats;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.HeapDumpReader;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

/**
 * A utility class for reading .hprof files that are used in functional tests. The files should be
 * located in some directory on the classpath. This code finds them using
 * ClassLoader.getResourceAsStream() method, then reads them into memory and keeps them there.
 */
public class SnapshotReader {

	public static Snapshot readAndResolveHeapDump(String shortFileName)
			throws DumpCorruptedException, IOException, HprofParsingCancelledException {
		InputStream resStream = SnapshotReader.class.getClassLoader().getResourceAsStream(shortFileName);
		try (BufferedInputStream in = new BufferedInputStream(resStream)) {
			byte[] buffer = new byte[3000000];

			int totalBytes = 0;
			while (true) {
				int bytesRead = in.read(buffer, totalBytes, buffer.length - totalBytes);
				if (bytesRead == -1) {
					break;
				}
				totalBytes += bytesRead;
				if (totalBytes == buffer.length) {
					byte[] oldBuffer = buffer;
					buffer = new byte[oldBuffer.length * 3 / 2];
					System.arraycopy(oldBuffer, 0, buffer, 0, oldBuffer.length);
				}
			}

			byte[] oldBuffer = buffer;
			buffer = new byte[totalBytes];
			System.arraycopy(oldBuffer, 0, buffer, 0, totalBytes);

			VerboseOutputCollector vc = new VerboseOutputCollector();
			HeapDumpReader reader = HeapDumpReader.createReader(new ReadBuffer.ByteArrayBufferFactory(buffer), 0, vc);
			return reader.read();
		}
	}
}
