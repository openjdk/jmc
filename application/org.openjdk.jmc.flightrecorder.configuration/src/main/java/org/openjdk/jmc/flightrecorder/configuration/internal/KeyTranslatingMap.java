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
package org.openjdk.jmc.flightrecorder.configuration.internal;

import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.QuantityConversionException;

/**
 * Abstract base class for {@link IConstrainedMap constrained map} wrappers that can re-map a fixed
 * number of keys into other keys before delegating to the underlying map. All other keys are passed
 * through.
 *
 * @param <K>
 * @param <M>
 */
public abstract class KeyTranslatingMap<K, M extends IConstrainedMap<K>> implements IConstrainedMap<K> {
	protected final M delegate;
	private final Map<K, K> translations;

	public KeyTranslatingMap(M delegate, Map<K, K> translations) {
		this.delegate = delegate;
		this.translations = translations;
	}

	protected final K translate(K key) {
		K translation = translations.get(key);
		return (translation != null) ? translation : key;
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public Object get(K key) {
		return delegate.get(translate(key));
	}

	@Override
	public IConstraint<?> getConstraint(K key) {
		return delegate.getConstraint(translate(key));
	}

	@Override
	public String getPersistableString(K key) {
		return delegate.getPersistableString(translate(key));
	}

	@Override
	public IMutableConstrainedMap<K> emptyWithSameConstraints() {
		return new Mutable<>(delegate.emptyWithSameConstraints(), translations);
	}

	@Override
	public IMutableConstrainedMap<K> mutableCopy() {
		return new Mutable<>(delegate.mutableCopy(), translations);
	}

	public static class Constrained<K> extends KeyTranslatingMap<K, IConstrainedMap<K>> {
		public Constrained(IConstrainedMap<K> delegate, Map<K, K> translations) {
			super(delegate, translations);
		}
	}

	public static class Described<K> extends KeyTranslatingMap<K, IDescribedMap<K>> implements IDescribedMap<K> {
		public Described(IDescribedMap<K> delegate, Map<K, K> translations) {
			super(delegate, translations);
		}

		@Override
		public IDescribable getDescribable(K key) {
			return delegate.getDescribable(translate(key));
		}
	}

	public static class Mutable<K> extends KeyTranslatingMap<K, IMutableConstrainedMap<K>>
			implements IMutableConstrainedMap<K> {
		public Mutable(IMutableConstrainedMap<K> delegate, Map<K, K> translations) {
			super(delegate, translations);
		}

		@Override
		public void put(K key, Object value) throws QuantityConversionException {
			delegate.put(translate(key), value);
		}

		@Override
		public void putPersistedString(K key, String persisted) throws QuantityConversionException {
			delegate.putPersistedString(translate(key), persisted);
		}

		@Override
		public <T> void put(K key, IConstraint<T> constraint, T value) throws QuantityConversionException {
			delegate.put(translate(key), value);
		}

		@Override
		public <T> void putPersistedString(K key, IConstraint<T> constraint, String persisted)
				throws QuantityConversionException {
			delegate.putPersistedString(translate(key), constraint, persisted);
		}
	}
}
