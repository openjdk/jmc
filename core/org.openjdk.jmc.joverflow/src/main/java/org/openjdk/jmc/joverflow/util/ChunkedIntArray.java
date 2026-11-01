/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.joverflow.util;

import java.util.Arrays;

/**
 * Growable int array backed by fixed-size chunks; growth does not copy existing elements.
 */
public class ChunkedIntArray {
	private static final int CHUNK_MAGNITUDE = 20; // 1M ints, i.e. 4MB per chunk
	private static final int CHUNK_SIZE = 1 << CHUNK_MAGNITUDE;
	private static final int POS_IN_CHUNK_MASK = CHUNK_SIZE - 1;
	private static final int INITIAL_NUM_CHUNKS = 8;

	private int[][] chunks = new int[INITIAL_NUM_CHUNKS][];
	private int size;

	public void add(int value) {
		if (size == Integer.MAX_VALUE) {
			throw new IllegalStateException("ChunkedIntArray cannot hold more than " + Integer.MAX_VALUE + " elements");
		}
		int chunkIdx = size >>> CHUNK_MAGNITUDE;
		if (chunkIdx == chunks.length) {
			chunks = Arrays.copyOf(chunks, chunks.length * 2);
		}
		int[] chunk = chunks[chunkIdx];
		if (chunk == null) {
			chunk = new int[CHUNK_SIZE];
			chunks[chunkIdx] = chunk;
		}
		chunk[size & POS_IN_CHUNK_MASK] = value;
		size++;
	}

	public int get(int idx) {
		checkIndex(idx);
		return chunks[idx >>> CHUNK_MAGNITUDE][idx & POS_IN_CHUNK_MASK];
	}

	public void set(int idx, int value) {
		checkIndex(idx);
		chunks[idx >>> CHUNK_MAGNITUDE][idx & POS_IN_CHUNK_MASK] = value;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Clears the array, retaining chunks for reuse.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * Clears the array and releases its chunks.
	 */
	public void release() {
		chunks = new int[INITIAL_NUM_CHUNKS][];
		size = 0;
	}

	/**
	 * Returns an exact-length copy.
	 */
	public int[] toArray() {
		int[] result = new int[size];
		int pos = 0;
		for (int chunkIdx = 0; pos < size; chunkIdx++) {
			int len = Math.min(CHUNK_SIZE, size - pos);
			System.arraycopy(chunks[chunkIdx], 0, result, pos, len);
			pos += len;
		}
		return result;
	}

	private void checkIndex(int idx) {
		if (idx < 0 || idx >= size) {
			throw new IndexOutOfBoundsException("Index " + idx + ", size " + size);
		}
	}
}
