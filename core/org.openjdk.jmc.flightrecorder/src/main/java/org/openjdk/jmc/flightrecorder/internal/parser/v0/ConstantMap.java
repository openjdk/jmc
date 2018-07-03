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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.factories.IPoolFactory;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.DataType;

/**
 * A map of objectId->object where each objectId can map to several values, each associated with a
 * timestamp.
 */
class ConstantMap {

	/**
	 * Same as java.lang.Long, but {@code value} can be modified so a lookup instance can be reused
	 * and object creation avoided
	 */
	private final static class Key {
		long value;

		Key(long value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			return (int) (value ^ (value >>> 32));
		}

		@Override
		public boolean equals(Object object) {
			if (object instanceof Key) {
				return ((Key) object).value == value;
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return Long.toString(value);
		}
	}

	private class Reference {
		private final long id;

		Reference(long id) {
			this.id = id;
		}

		Object resolve(long atTimestamp) {
			return get(id, atTimestamp);
		}
	}

	private final Map<Key, ConstantEntryList> map = new HashMap<>(17, 0.5f);
	private boolean allConstantsLoaded;
	private IPoolFactory<?> factory;
	private final Key lookupKey = new Key(0);
	private IValueReader valueReader;
	private DataType keyType;

	void init(IValueReader valueReader, DataType keyType, IPoolFactory<?> factory) {
		this.valueReader = valueReader;
		this.keyType = keyType;
		this.factory = factory;
	}

	void setLoadDone() throws InvalidJfrFileException {
		allConstantsLoaded = true;
		if (valueReader == null) {
			throw new InvalidJfrFileException(); // Constant type referenced but never defined
		}
		for (ConstantEntryList entries : map.values()) {
			entries.sort();
		}
	}

	void touchAll() {
		for (Entry<Key, ConstantEntryList> list : map.entrySet()) {
			list.getValue().touchAll(list.getKey().value);
		}
	}

	void readValue(byte[] data, Offset offset, long timestamp) throws InvalidJfrFileException {
		long key = NumberReaders.readKey(data, offset, keyType);
		put(key, valueReader.readValue(data, offset, timestamp), timestamp);
	}

	private void put(long valueId, Object value, long timestamp) {
		ConstantEntryList entries = getEntryList(valueId);
		if (entries == null) {
			entries = new ConstantEntryList(value, timestamp, factory);
			map.put(new Key(valueId), entries);
		} else {
			entries.add(value, timestamp);
		}
	}

	ContentType<?> getContentType() {
		return factory != null ? factory.getContentType() : valueReader.getValueType();
	}

	/**
	 * Returns one of the values identified by {@code valueId}. The value returned is the value
	 * associated with the smallest timestamp larger than or equals to {@code atTimestamp}. If the
	 * map doesn't contain a value identified by {@code valueId}, a dummy object may be returned.
	 */
	Object get(long valueId, long atTimestamp) {
		if (allConstantsLoaded) {
			ConstantEntryList entryList = getEntryList(valueId);
			if (entryList != null) {
				return entryList.getFirstObjectAfter(valueId, atTimestamp);
			} else if (factory != null) {
				// Lookup of missing key. Return dummy object.
				return factory.createObject(valueId, null);
			} else {
				return null;
			}
		} else {
			return new Reference(valueId);
		}
	}

	private ConstantEntryList getEntryList(long valueId) {
		lookupKey.value = valueId;
		return map.get(lookupKey);
	}

	static Object resolve(Object o, long atTimestamp) {
		if (o instanceof Reference) {
			return resolve(((Reference) o).resolve(atTimestamp), atTimestamp);
		} else if (o != null && o.getClass().isArray()) {
			Object[] array = (Object[]) o;
			for (int n = 0; n < array.length; n++) {
				array[n] = resolve(array[n], atTimestamp);
			}
			return array;
		}
		return o;
	}

}
