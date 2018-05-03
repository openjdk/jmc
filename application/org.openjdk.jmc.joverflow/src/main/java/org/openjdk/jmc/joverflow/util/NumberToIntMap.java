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
package org.openjdk.jmc.joverflow.util;

/**
 * Superclass for IntToIntMap and LongToIntMap, with their common functionality.
 * <p>
 * Note that I've tried to replace the simple implementation where the size of the map can be just
 * any odd number, with a supposedly faster one, similar to java.util.HashMap, where the size is a
 * power of two. However, that resulted in no noticeable performance improvement, but rather
 * significant memory growth. E.g. for CRMSoa.hprof used memory with power-of-two map was 731M vs
 * 623M for the simple table. It might be that in the power-of-two we indeed have fewer hash
 * collisions - but it's very likely that for tables of this size increased memory usage causes
 * slowdown that well offsets any improvements. And anyway, smaller memory usage is a higher
 * priority for us than a few per cent speedup.
 */
public abstract class NumberToIntMap {

	protected int[] values;

	protected int size, capacity, threshold;

	protected long rehashTime; // Debugging

	protected NumberToIntMap(int expectedMaxSize) {
		if (expectedMaxSize < 11) {
			expectedMaxSize = 11; // Protect ourselves from stupidly small capacity
		}
		capacity = (4 * expectedMaxSize) / 3;
		createTable();
	}

	public abstract void put(long key, int value);

	public abstract int get(long key);

	public int size() {
		return size;
	}

	public int capacity() {
		return capacity;
	}

	/**
	 * This method may be called once it's known that no more elements are going to be added to the
	 * table. It checks whether the current capacity is appropriate compared to size (no more than
	 * 10% larger or smaller than size * 4 / 3. If not, capacity is adjusted and table is rehashed.
	 */
	public void adjustCapacityIfNeeded() {
		int optimalCapacity = (4 * size / 3 + 10) | 1;
		int avgCapacityValue = (optimalCapacity + capacity) / 2;
		if (((double) Math.abs(optimalCapacity - capacity)) / avgCapacityValue > 0.1) {
			rehash(optimalCapacity);
		}
	}

	protected void finishPut() {
		size++;
		if (size > threshold) {
			rehash((capacity * 3 / 2) | 1);
		}
	}

	protected final int nextKeyIndex(int idx) {
		// It looks like avoiding '%' operation here, using 'if' instead,
		// improves performance noticeably.
		int nextIdx = idx + 1;
		return (nextIdx < capacity ? nextIdx : 0);
	}

	protected abstract void rehash(int newCapacity);

	protected abstract void createTable();

	public long getRehashTimeMillis() {
		return rehashTime;
	}
}
