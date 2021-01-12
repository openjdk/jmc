/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldValue;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class SingleFieldMap implements Map<String, TypedFieldValue> {
	private int hashCode = 0;
	private final ImmutableMapEntry<String, TypedFieldValue> entry;

	SingleFieldMap(String name, TypedFieldValue value) {
		Objects.nonNull(name);
		Objects.nonNull(value);
		this.entry = new ImmutableMapEntry<>(name, value);
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean containsKey(Object o) {
		return entry.getKey().equals(o);
	}

	@Override
	public boolean containsValue(Object o) {
		return entry.getValue().equals(o);
	}

	@Override
	public TypedFieldValue get(Object o) {
		return entry.getKey().equals(o) ? entry.getValue() : null;
	}

	@Override
	public TypedFieldValue put(String s, TypedFieldValue typedFieldValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedFieldValue remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends TypedFieldValue> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		return Collections.singleton(entry.getKey());
	}

	@Override
	public Collection<TypedFieldValue> values() {
		return Collections.singleton(entry.getValue());
	}

	@Override
	public Set<Entry<String, TypedFieldValue>> entrySet() {
		return Collections.singleton(entry);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SingleFieldMap that = (SingleFieldMap) o;
		return entry.equals(that.entry);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = NonZeroHashCode.hash(entry);
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "SingleFieldMap{" + entry.getKey() + "=" + entry.getValue() + '}';
	}
}
