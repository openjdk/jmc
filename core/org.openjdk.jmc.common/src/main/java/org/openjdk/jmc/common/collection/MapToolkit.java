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
package org.openjdk.jmc.common.collection;

import org.openjdk.jmc.common.collection.EntryHashMap.Entry;

/**
 * Useful methods related to maps.
 */
public class MapToolkit {

	/**
	 * A map entry used to store integer values. Used by maps created with
	 * {@link MapToolkit#createIntMap(int, float)}.
	 * 
	 * @param <K>
	 *            key type
	 */
	public static class IntEntry<K> extends Entry<K> implements Comparable<IntEntry<K>> {
		private int value = 0;

		private IntEntry(K key) {
			super(key);
		}

		@Override
		public int compareTo(IntEntry<K> o) {
			return Integer.compare(value, o.value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof IntEntry) {
				IntEntry<?> o = (IntEntry<?>) obj;
				return getKey().equals(o.getKey()) && value == o.value;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return 17 * getKey().hashCode() + value;
		}

		/**
		 * Get the entry value.
		 * 
		 * @return the value corresponding to this entry
		 */
		public int getValue() {
			return value;
		}

		/**
		 * Set the entry value.
		 * 
		 * @param value
		 *            the value corresponding to this entry
		 */
		public void setValue(int value) {
			this.value = value;
		}
	}

	/**
	 * Create a map for storing integers based on a given key type. Typically this is used to access
	 * and update integer values computed from or for the keys.
	 * 
	 * @param initialCapacity
	 *            initial storage capacity
	 * @param loadFactor
	 *            load factor at which to increase the internal storage capacity
	 * @param <K>
	 *            the key type
	 * @return a map of integer entries
	 */
	public static <K> EntryHashMap<K, IntEntry<K>> createIntMap(int initialCapacity, float loadFactor) {
		return new EntryHashMap<K, IntEntry<K>>(initialCapacity, loadFactor) {

			@Override
			protected IntEntry<K> computeValue(K key) {
				return new IntEntry<>(key);
			}
		};
	}
}
