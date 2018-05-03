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

import java.util.Arrays;

/**
 * A set of objects that's expected to be small, perhaps 4 elements at most. The implementation is
 * optimized for size. Removal and null elements are not supported.
 */
public class SmallSet<V> {
	Object elements[];

	/** Creates a new set, with the default capacity of 2 */
	public SmallSet() {
		elements = new Object[2];
	}

	/** Creates a new set, copying into it all the elements of the provided set */
	public SmallSet(SmallSet<V> other) {
		elements = new Object[other.elements.length];
		System.arraycopy(other.elements, 0, elements, 0, other.elements.length);
	}

	/**
	 * Adds an element to the set, if it's not present. Returns true if there was no element with
	 * the same value previously in this set; false otherwise.
	 */
	public boolean add(V v) {
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] == null) {
				elements[i] = v;
				return true;
			} else if (elements[i].equals(v)) {
				return false;
			}
		}

		Object oldElements[] = elements;
		elements = new Object[oldElements.length + 2];
		System.arraycopy(oldElements, 0, elements, 0, oldElements.length);
		elements[oldElements.length] = v;
		return true;
	}

	/** Adds all the elements from the provided small set to this set. */
	@SuppressWarnings("unchecked") // Just for the (V) cast below
	public void addAll(SmallSet<V> other) {
		for (Object v : other.elements) {
			if (v == null) {
				break;
			}
			add((V) v);
		}
	}

	/** Returns an array containing elements of this set. */
	public <T> T[] getElements(Class<T[]> arrayClass) {
		int count = elements.length;
		while (count > 0 && elements[count - 1] == null) {
			count--;
		}

		return Arrays.copyOf(elements, count, arrayClass);
	}
}
