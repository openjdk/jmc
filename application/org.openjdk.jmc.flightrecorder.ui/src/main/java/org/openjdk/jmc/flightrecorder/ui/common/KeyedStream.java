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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public class KeyedStream<K, V> {

	private final Builder<V> builder = Stream.builder();
	private final K key;
	private Stream<V> collected;

	private KeyedStream(K key) {
		this.key = key;
	}

	public K getKey() {
		return key;
	}

	public Stream<V> getStream() {
		Stream<V> stream = builder.build();
		return collected == null ? stream : Stream.concat(collected, stream);
	}

	private void collect(Stream<V> c) {
		collected = collected == null ? c : Stream.concat(collected, c);
	}

	private static class MapBuilder<K, V> {
		final Map<K, KeyedStream<K, V>> map = new HashMap<>();
		final Function<V, K> mappingFunction;

		MapBuilder(Function<V, K> mappingFunction) {
			this.mappingFunction = mappingFunction;
		}

		void add(V value) {
			map.computeIfAbsent(mappingFunction.apply(value), key -> new KeyedStream<>(key)).builder.accept(value);
		}

		Stream<KeyedStream<K, V>> build() {
			return map.values().stream();
		}
	}

	private static <K, V> MapBuilder<K, V> combine(MapBuilder<K, V> a, MapBuilder<K, V> b) {
		for (Entry<K, KeyedStream<K, V>> e : b.map.entrySet()) {
			a.map.merge(e.getKey(), e.getValue(), (v1, v2) -> {
				v1.collect(v2.getStream());
				return v1;
			});
		}
		return a;
	}

	private static <K, V> Supplier<MapBuilder<K, V>> supplier(Function<V, K> mappingFunction) {
		return () -> new MapBuilder<>(mappingFunction);
	}

	public static <K, T> Collector<T, ?, Stream<KeyedStream<K, T>>> collector(Function<T, K> mappingFunction) {
		Function<MapBuilder<K, T>, Stream<KeyedStream<K, T>>> f = MapBuilder::build;
		return Collector.of(supplier(mappingFunction), MapBuilder::add, KeyedStream::combine, f,
				Characteristics.UNORDERED);
	}
}
