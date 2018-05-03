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
 * A simple, low-memory-overhead hash map whose main usage is mapping object IDs to int numbers. The
 * map does not support negative values, and returns -1 to signal that the value for the given key
 * does not exist.
 * <p>
 * Object IDs passed to this map as keys are long numbers that always have only 32 meaningful (i.e.
 * changing) bits. They may represent either real 32-bit pointers, or "narrow" pointers used by the
 * JVM in 64-bit mode. In the latter case, these bits may either only the lower 32-bit word of the
 * long number, or 29 upper bits in the low word and the lower 3 bits in the high word. Note that
 * more than 3 bits in the high word may be actually set, but only the 3 lower bits there may
 * change.
 * <p>
 * In the case of narrow pointers in 64-bit mode we have IDs that always end with 8, but may be
 * longer than 8 hexadecimal places, like 0x6e07b0808 and 0x7e07b07b8. To deal with this case
 * properly, we internally convert a long ID into an int by ORing its int words. Furthermore, we
 * take only the lower 3 bits from the high word.
 */
public class IntToIntMap extends NumberToIntMap {
	private int[] keys;

	/**
	 * If useOnlyLowWord is true, it means that we have case (3) described in the class-level
	 * javadoc. Otherwise, it's case (2). Case (1) is handled correctly regardless of the value of
	 * this parameter.
	 */
	public IntToIntMap(int expectedMaxSize) {
		super(expectedMaxSize);
	}

	@Override
	public void put(long key, int value) {
		int intKey = longKeyToIntKey(key);
		doPut(intKey, value);
	}

	private void doPut(int intKey, int value) {
		int idx = hash(intKey);
		while (values[idx] != -1) {
			if (keys[idx] == intKey) {
				throwCollisionException(intKey);
			}
			idx = nextKeyIndex(idx);
		}

		keys[idx] = intKey;
		values[idx] = value;

		finishPut();
	}

	@Override
	public int get(long key) {
		int intKey = longKeyToIntKey(key);
		int idx = hash(intKey);
		while (values[idx] != -1) {
			if (keys[idx] == intKey) {
				return values[idx];
			}
			idx = nextKeyIndex(idx);
		}
		return -1;
	}

	private int longKeyToIntKey(long key) {
		// Preserve all of the meaningful 32 bits in the long object id
		return ((int) key) | (((int) (key >> 32)) & 7);
	}

	private int hash(int h) {
		/*
		 * Looks like it helps a bit to do some hash randomization similar to what's done in
		 * java.util.HashMap.hash(). But apparently one has to be very careful with this - doing
		 * this wrong will very easily cause a negative effect.
		 */
//		h ^= (h >>> 7) ^ (h >>> 4);
		h &= 0x7FFFFFFF;
		return h % capacity;
	}

	@Override
	protected void rehash(int newCapacity) {
		long time = System.currentTimeMillis();
		int[] oldKeys = keys;
		int[] oldValues = values;

		capacity = newCapacity;
		createTable();

		for (int i = 0; i < oldKeys.length; i++) {
			if (oldValues[i] != -1) {
				doPut(oldKeys[i], oldValues[i]);
			}
		}
		rehashTime += System.currentTimeMillis() - time;
	}

	@Override
	protected void createTable() {
		threshold = capacity / 4 * 3;
		size = 0;
		keys = new int[capacity];
		values = new int[capacity];
		for (int i = 0; i < capacity; i++) {
			values[i] = -1;
		}
	}

	/*
	 * Looks like moving this code into a separate method improves performance a bit, maybe because
	 * inlining is made easier in the caller.
	 */
	private void throwCollisionException(int intKey) {
		throw new RuntimeException("Collision for intKey = " + Integer.toHexString(intKey)
				+ ". Verify that IDs have 32 meaningful bits and/or that useOnlyLowWord was " + "set correctly.");
	}
}
