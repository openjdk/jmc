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
package org.openjdk.jmc.flightrecorder.internal.parser.v0;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openjdk.jmc.flightrecorder.internal.parser.v0.factories.IPoolFactory;

/**
 * A list of objects with an associated timestamp. The list is optimized for the case of only a
 * single value.
 */
class ConstantEntryList {

	private class ConstantEntry {

		Object value;
		long timestamp;
		byte resolveState;

		ConstantEntry(Object value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}

		Object getResolved(long objectIdOfThisEntry) {
			switch (resolveState) {
			case 1:
				// Cyclic reference not supported
				return null;
			case 0:
				resolveState = 1;
				value = ConstantMap.resolve(value, timestamp);
				if (factory != null) {
					value = factory.createObject(objectIdOfThisEntry, value);
				}
				resolveState = 2;
			default:
				return value;
			}
		}
	}

	private static final Comparator<ConstantEntry> CHRONOLOGICAL = new Comparator<ConstantEntry>() {

		@Override
		public int compare(ConstantEntry o1, ConstantEntry o2) {
			return o1.timestamp < o2.timestamp ? -1 : (o1.timestamp == o2.timestamp) ? 0 : 1;
		}
	};

	private final IPoolFactory<?> factory;
	private final ConstantEntry firstEntry;
	private List<ConstantEntry> list;

	ConstantEntryList(Object firstValue, long timestamp, IPoolFactory<?> factory) {
		firstEntry = new ConstantEntry(firstValue, timestamp);
		this.factory = factory;
	}

	void add(Object value, long timestamp) {
		if (list == null) {
			list = new ArrayList<>(5);
			list.add(firstEntry);
		}
		list.add(new ConstantEntry(value, timestamp));
	}

	void sort() {
		if (list != null) {
			Collections.sort(list, CHRONOLOGICAL);
		}
	}

	void touchAll(long objectIdOfThisEntry) {
		if (list == null) {
			firstEntry.getResolved(objectIdOfThisEntry);
		} else {
			for (ConstantEntry entry : list) {
				entry.getResolved(objectIdOfThisEntry);
			}
		}
	}

	/**
	 * Returns the value associated with the smallest timestamp larger than or equals to
	 * {@code timestamp}.
	 *
	 * @param objectIdOfThisEntry
	 *            The object id of all objects in this entry list
	 * @param timestamp
	 * @return
	 */
	Object getFirstObjectAfter(long objectIdOfThisEntry, long timestamp) {
		if (list == null) {
			return firstEntry.timestamp >= timestamp ? firstEntry.getResolved(objectIdOfThisEntry) : null;
		} else {
			for (ConstantEntry e : list) {
				if (e.timestamp >= timestamp) {
					return e.getResolved(objectIdOfThisEntry);
				}
			}
			return null;
		}
	}
}
