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
 * A Set of objects. The primary reason to use this is to avoid memory overhead of
 * java.util.HashSet. The implementation is simple, identity-based and doesn't support object
 * removal.
 */
public class SimpleIdentitySet<V> {
	private V[] table;
	private int size, threshold;
	private long rehashTime;

	public SimpleIdentitySet(int expectedObjNum) {
		int capacity = (expectedObjNum * 4 / 3 + 3) | 1;
		createTable(capacity);
	}

	/**
	 * Adds an instance to this set. If the instance is already there, does nothing and returns
	 * false; otherwise returns true.
	 */
	public boolean add(V v) {
		int idx = hash(v.hashCode()) % table.length;
		V vt;
		while ((vt = table[idx]) != null) {
			if (vt == v) {
				return false;
			}
			idx = (idx + 1) % table.length;
		}

		table[idx] = v;
		size++;
		if (size > threshold) {
			rehash();
		}
		return true;
	}

	public boolean contains(V v) {
		int idx = hash(v.hashCode()) % table.length;
		V vt;
		while ((vt = table[idx]) != null) {
			if (vt == v) {
				return true;
			}
			idx = (idx + 1) % table.length;
		}
		return false;
	}

	public int size() {
		return size;
	}

	public long getRehashTimeMillis() {
		return rehashTime;
	}

	private static int hash(int h) {
		return h & Integer.MAX_VALUE;
	}

	private void rehash() {
		long time = System.currentTimeMillis();
		V[] oldTable = table;

		int capacity = (table.length * 3 / 2) | 1;
		createTable(capacity);

		for (V v : oldTable) {
			if (v != null) {
				add(v);
			}
		}
		rehashTime += System.currentTimeMillis() - time;
	}

	@SuppressWarnings("unchecked") // Just for the (V[]) cast below
	private void createTable(int capacity) {
		threshold = capacity / 4 * 3;
		size = 0;
		table = (V[]) (new Object[capacity]);
	}
}
