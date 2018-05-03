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

/**
 * Implementation of ReadBuffer that uses a simple byte[] backing array.
 */
public class ByteArrayReadBuffer extends ReadBuffer {
	private byte[] ar;

	ByteArrayReadBuffer(byte[] array) {
		this.ar = array;
	}

	@Override
	public void get(long longPos, byte[] buf) {
		System.arraycopy(ar, (int) longPos, buf, 0, buf.length);
	}

	@Override
	public void get(long longPos, byte[] buf, int num) {
		System.arraycopy(ar, (int) longPos, buf, 0, num);
	}

	@Override
	public int getInt(long longPos) {
		int pos = (int) longPos;
		return getInt(pos);
	}

	private int getInt(int pos) {
		return ((ar[pos] & 0xFF) << 24) | ((ar[pos + 1] & 0xFF) << 16) | ((ar[pos + 2] & 0xFF) << 8)
				| (ar[pos + 3] & 0xFF);
	}

	@Override
	public long getLong(long longPos) throws IOException {
		int pos = (int) longPos;
		return (((long) getInt(pos)) << 32) | ((getInt(pos + 4)) & 0xFFFFFFFFL);
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
