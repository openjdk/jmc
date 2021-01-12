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

import org.openjdk.jmc.flightrecorder.writer.util.TypeByUsageComparator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A per-type map of {@linkplain ConstantPool} instances */
public final class ConstantPools implements Iterable<ConstantPool> {
	private final Map<TypeImpl, ConstantPool> constantPoolMap = new ConcurrentHashMap<>();

	/**
	 * Get the {@linkplain ConstantPool} instance associated with the given type
	 *
	 * @param type
	 *            the type to get the constant pool for
	 * @return the associated {@linkplain ConstantPool} instance
	 * @throws IllegalArgumentException
	 *             if the type does not support constant pools
	 */
	public ConstantPool forType(TypeImpl type) {
		if (!type.hasConstantPool()) {
			throw new IllegalArgumentException();
		}
		return constantPoolMap.computeIfAbsent(type, this::newConstantPool);
	}

	public int size() {
		return constantPoolMap.size();
	}

	@Override
	public Iterator<ConstantPool> iterator() {
		return getOrderedPools().iterator();
	}

	@Override
	public void forEach(Consumer<? super ConstantPool> action) {
		getOrderedPoolsStream().forEach(action);
	}

	@Override
	public Spliterator<ConstantPool> spliterator() {
		return getOrderedPools().spliterator();
	}

	/**
	 * The pool instances need to be sorted in a way that if a value from pool P1 is using value(s)
	 * from pool P2 then P2 must come before P1.
	 *
	 * @return sorted pool instances
	 */
	private Stream<ConstantPool> getOrderedPoolsStream() {
		return constantPoolMap.entrySet().stream()
				.sorted((e1, e2) -> TypeByUsageComparator.INSTANCE.compare(e1.getKey(), e2.getKey()))
				.map(Map.Entry::getValue);
	}

	private List<ConstantPool> getOrderedPools() {
		return getOrderedPoolsStream().collect(Collectors.toList());
	}

	private ConstantPool newConstantPool(TypeImpl type) {
		return new ConstantPool(type);
	}

	@Override
	public String toString() {
		return "ConstantPools [constantPoolMap=" + constantPoolMap + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constantPoolMap == null) ? 0 : constantPoolMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConstantPools other = (ConstantPools) obj;
		if (constantPoolMap == null) {
			if (other.constantPoolMap != null)
				return false;
		} else if (!constantPoolMap.equals(other.constantPoolMap))
			return false;
		return true;
	}
}
