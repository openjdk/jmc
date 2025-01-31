/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.testutils.parser;

import java.io.IOException;

/** A chunk header data object */
public final class ChunkHeader {
	public static final byte[] MAGIC = new byte[] {'F', 'L', 'R', '\0'};
	public final short major;
	public final short minor;
	public final long size;
	public final long cpOffset;
	public final long metaOffset;
	public final long startNanos;
	public final long duration;
	public final long startTicks;
	public final long frequency;
	public final boolean compressed;

	ChunkHeader(RecordingStream recording) throws IOException {
		byte[] buffer = new byte[MAGIC.length];
		recording.read(buffer, 0, MAGIC.length);
		for (int i = 0; i < MAGIC.length; i++) {
			if (buffer[i] != MAGIC[i]) {
				throw new IOException("Invalid JFR Magic Number: " + bytesToString(buffer, 0, MAGIC.length));
			}
		}
		major = recording.readShort();
		minor = recording.readShort();
		size = recording.readLong();
		cpOffset = recording.readLong();
		metaOffset = recording.readLong();
		startNanos = recording.readLong();
		duration = recording.readLong();
		startTicks = recording.readLong();
		frequency = recording.readLong();
		compressed = recording.readInt() != 0;
	}

	@Override
	public String toString() {
		return "ChunkHeader{" + "major=" + major + ", minor=" + minor + ", size=" + size + ", cpOffset=" + cpOffset
				+ ", metaOffset=" + metaOffset + ", startNanos=" + startNanos + ", duration=" + duration
				+ ", startTicks=" + startTicks + ", frequency=" + frequency + ", compressed=" + compressed + '}';
	}

	private static String bytesToString(byte[] array, int offset, int len) {
		StringBuilder sb = new StringBuilder("[");
		boolean comma = false;
		for (int i = 0; i < len; i++) {
			if (comma) {
				sb.append(", ");
			} else {
				comma = true;
			}
			sb.append(array[i + offset]);
		}
		sb.append(']');
		return sb.toString();
	}
}
