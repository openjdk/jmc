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
package org.openjdk.jmc.joverflow.heap.parser;

import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * The implementation of ReadBuffer that supports files of size larger than Integer.MAX_VALUE, i.e.
 * larger than a single MappedByteBuffer can hold. It is done by opening multiple MappedByteBuffers
 * for the file, and choosing the right one every time we need to read something from the file.
 */
class MappedReadMultiBuffer extends ReadBuffer {
	private final MappedByteBuffer bufs[];
	private final long mappedBBEndOfs[];
	private final int maxBufSize;

	MappedReadMultiBuffer(MappedByteBuffer bufs[], long mappedBBEndOfs[], int maxBufSize) {
		this.bufs = bufs;
		this.mappedBBEndOfs = mappedBBEndOfs;
		this.maxBufSize = maxBufSize;
	}

	private MappedByteBuffer seek(long pos) throws IOException {
		int bufIdx = (int) (pos / maxBufSize);
		while (pos > mappedBBEndOfs[bufIdx]) {
			bufIdx++;
		}
		MappedByteBuffer buf = bufs[bufIdx];
		if (bufIdx > 0) {
			buf.position((int) (pos - mappedBBEndOfs[bufIdx - 1] - 1));
		} else {
			buf.position((int) pos);
		}
		return buf;
	}

	@Override
	public void get(long pos, byte[] res) throws IOException {
		MappedByteBuffer buf = seek(pos);
		buf.get(res);
	}

	@Override
	public void get(long pos, byte[] res, int num) throws IOException {
		MappedByteBuffer buf = seek(pos);
		buf.get(res, 0, num);
	}

	@Override
	public int getInt(long pos) throws IOException {
		MappedByteBuffer buf = seek(pos);
		return buf.getInt();
	}

	@Override
	public long getLong(long pos) throws IOException {
		MappedByteBuffer buf = seek(pos);
		return buf.getLong();
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
