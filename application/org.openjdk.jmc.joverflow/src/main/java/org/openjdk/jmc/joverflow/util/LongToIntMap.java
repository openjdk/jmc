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
 * A simple, low-memory-overhead hash map that maps a long number to an int number. Does not support
 * negative values, and returns -1 result to signal that the value for the given key does not exist.
 */
public class LongToIntMap extends NumberToIntMap {
	private long[] keys;

	public LongToIntMap(int expectedMaxSize) {
		super(expectedMaxSize);
	}

	@Override
	public void put(long key, int value) {
		int idx = hash(key);
		while (values[idx] != -1) {
			if (keys[idx] == key) {
				values[idx] = value;
				return;
			}
			idx = nextKeyIndex(idx);
		}

		keys[idx] = key;
		values[idx] = value;

		finishPut();
	}

	@Override
	public int get(long key) {
		int idx = hash(key);
		while (values[idx] != -1) {
			if (keys[idx] == key) {
				return values[idx];
			}
			idx = nextKeyIndex(idx);
		}
		return -1;
	}

	private int hash(long v) {
		int h = ((int) (v ^ (v >>> 32)));
		// Looks like it helps a bit to do some hash randomization similar to what's
		// done in java.util.HashMap.hash(). But apparently one has to be very
		// careful with this - doing this wrong may easily cause a negative effect.
		h ^= (h >>> 7) ^ (h >>> 4);
		h &= 0x7FFFFFFF;
		return h % capacity;
	}

	@Override
	protected void rehash(int newCapacity) {
		long time = System.currentTimeMillis();
		long[] oldKeys = keys;
		int[] oldValues = values;

		capacity = newCapacity;
		createTable();

		for (int i = 0; i < oldKeys.length; i++) {
			if (oldValues[i] != -1) {
				put(oldKeys[i], oldValues[i]);
			}
		}
		rehashTime += System.currentTimeMillis() - time;
	}

	@Override
	protected void createTable() {
		threshold = capacity / 4 * 3;
		size = 0;
		keys = new long[capacity];
		values = new int[capacity];
		for (int i = 0; i < capacity; i++) {
			values[i] = -1;
		}
	}

}
