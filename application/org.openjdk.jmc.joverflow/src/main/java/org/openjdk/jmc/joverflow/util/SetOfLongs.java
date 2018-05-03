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
 * A Set containing elements of type long. The implementation is simple and doesn't support element
 * removal.
 */
public class SetOfLongs {
	private long[] table;
	private boolean[] occupied;
	private int size, capacity, threshold;

	public SetOfLongs(int initialCapacity) {
		capacity = initialCapacity | 1;
		createTable();
	}

	/**
	 * Adds a value to this set. If the value is already there, does nothing and returns false;
	 * otherwise returns true.
	 */
	public boolean add(long v) {
		int idx = hash(v) % capacity;
		while (occupied[idx]) {
			if (table[idx] == v) {
				return false;
			}
			idx = (idx + 1) % capacity;
		}

		table[idx] = v;
		occupied[idx] = true;
		size++;
		if (size > threshold) {
			rehash();
		}
		return true;
	}

	public boolean contains(long v) {
		int idx = hash(v) % capacity;
		while (occupied[idx]) {
			if (table[idx] == v) {
				return true;
			}
			idx = (idx + 1) % capacity;
		}
		return false;
	}

	public int size() {
		return size;
	}

	private int hash(long v) {
		return ((int) (v ^ (v >>> 32))) & 0x7FFFFFFF;
	}

	private void rehash() {
		long[] oldTable = table;
		boolean[] oldOccupied = occupied;

		capacity = (capacity * 3 / 2) | 1;
		createTable();

		for (int i = 0; i < oldTable.length; i++) {
			if (oldOccupied[i]) {
				add(oldTable[i]);
			}
		}
	}

	private void createTable() {
		threshold = capacity / 4 * 3;
		size = 0;
		table = new long[capacity];
		occupied = new boolean[capacity];
	}
}
