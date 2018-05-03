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
 * A simple, low-memory-overhead hash map that maps an int number to an object. If "linked"
 * parameter is set to true at creation time, the objects get linked internally, so that the order
 * of iteration over values becomes the same as the order in which elements have been added to the
 * table.
 */
public class IntToObjectMap<V> extends NumberToObjectMap<V> {
	private int[] keys;

	public IntToObjectMap(int initialCapacity, boolean linked) {
		super(initialCapacity, linked);
	}

	@Override
	public void put(long key, V value) {
		int intKey = (int) key;
		int idx = hash(intKey) % capacity;
		while (values[idx] != null) {
			if (keys[idx] == intKey) {
				values[idx] = value;
				return;
			}
			idx = (idx + 1) % capacity;
		}

		keys[idx] = intKey;
		values[idx] = value;

		finishPut(idx);
	}

	@Override
	public V get(long key) {
		int intKey = (int) key;
		int idx = hash(intKey) % capacity;
		while (values[idx] != null) {
			if (keys[idx] == intKey) {
				return values[idx];
			}
			idx = (idx + 1) % capacity;
		}
		return null;
	}

	private int hash(int v) {
		return v & 0x7FFFFFFF;
	}

	@Override
	protected void rehash() {
		long time = System.currentTimeMillis();
		int[] oldKeys = keys;
		V[] oldValues = values;
		int[] oldNextElement = nextElement;

		capacity = (capacity * 3 / 2) | 1;
		createTable();

		if (linked) {
			int idx = firstElementIdx;
			while (idx != -1) {
				put(oldKeys[idx], oldValues[idx]);
				idx = oldNextElement[idx];
			}
		} else {
			for (int i = 0; i < oldKeys.length; i++) {
				if (oldValues[i] != null) {
					put(oldKeys[i], oldValues[i]);
				}
			}
		}
		rehashTime += System.currentTimeMillis() - time;
		numRehashes++;
	}

	@Override
	@SuppressWarnings("unchecked") // Just for the (V[]) cast
	protected void createTable() {
		threshold = capacity / 4 * 3;
		size = 0;
		keys = new int[capacity];
		values = (V[]) (new Object[capacity]);
		if (linked) {
			nextElement = new int[capacity];
			prevAddedElementIdx = -1;
		}
	}

}
