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
package org.openjdk.jmc.common.unit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A mutable extension to {@link IConstrainedMap}. Note that it is the values that can be mutated.
 * Existing constraints cannot be modified. However, some implementations may allow adding
 * constraints for keys that currently doesn't have a constraint, and thus no current value.
 *
 * @param <K>
 *            the type of the keys in the map
 */
@SuppressWarnings("nls")
public abstract class MutableConstrainedMap<K> implements IMutableConstrainedMap<K> {
	protected final Map<K, Object> values;

	protected MutableConstrainedMap() {
		values = new HashMap<>();
	}

	protected MutableConstrainedMap(HashMap<K, Object> values) {
		this.values = values;
	}

	@Override
	public Set<K> keySet() {
		return values.keySet();
	}

	@Override
	public Object get(K key) {
		return values.get(key);
	}

	@Override
	public abstract IConstraint<?> getConstraint(K key);

	/**
	 * This method will only be called for keys for which {@link #getConstraint(Object)} currently
	 * returns null for. Thus, it doesn't matter, and is unspecified, what it would return for keys
	 * which {@link #getConstraint(Object)} return non-null for, which typically simplifies the
	 * implementations.
	 */
	protected IConstraint<?> getSuggestedConstraint(K key) {
		return null;
	}

	@Override
	public String getPersistableString(K key) {
		Object value = values.get(key);
		if (value != null) {
			try {
				return getPersistableString(getConstraint(key), value);
			} catch (QuantityConversionException e) {
				// Shouldn't happen.
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> String getPersistableString(IConstraint<T> constraint, Object value)
			throws QuantityConversionException {
		return constraint.persistableString((T) value);
	}

	@SuppressWarnings("unchecked")
	private <T> void validate(IConstraint<T> constraint, Object value) throws QuantityConversionException {
		constraint.validate((T) value);
	}

	@Override
	public void put(K key, Object value) throws QuantityConversionException {
		IConstraint<?> constraint = getConstraint(key);
		if (constraint == null) {
			constraint = getSuggestedConstraint(key);
			if (constraint == null) {
				throw new IllegalArgumentException(
						"Key '" + key + "' is not allowed in this map (without explicit constraint).");
			}
			addConstraint(key, constraint);
		}
		validate(constraint, value);
		values.put(key, value);
	}

	@Override
	public void putPersistedString(K key, String persisted) throws QuantityConversionException {
		IConstraint<?> constraint = getConstraint(key);
		if (constraint == null) {
			constraint = getSuggestedConstraint(key);
			if (constraint == null) {
				throw new IllegalArgumentException(
						"Key '" + key + "' is not allowed in this map (without explicit constraint).");
			}
			addConstraint(key, constraint);
		}
		Object value = constraint.parsePersisted(persisted);
		values.put(key, value);
	}

	@Override
	public <T> void put(K key, IConstraint<T> constraint, T value) throws QuantityConversionException {
		IConstraint<?> oldConstraint = getConstraint(key);
		if (oldConstraint == null) {
			addConstraint(key, constraint);
		} else if (!constraint.equals(oldConstraint)) {
			throw new IllegalArgumentException("Constraints cannot be changed. Attempted for key '" + key + "'.");
		}

		if (value != null) {
			constraint.validate(value);
		}
		values.put(key, value);
	}

	protected void addConstraint(K key, IConstraint<?> constraint) {
		throw new IllegalArgumentException("Constraints cannot be added to this map. Attempted for key '" + key + "'.");
	}

	@Override
	public <T> void putPersistedString(K key, IConstraint<T> constraint, String persisted)
			throws QuantityConversionException {
		IConstraint<?> oldConstraint = getConstraint(key);
		if (oldConstraint == null) {
			addConstraint(key, constraint);
		} else if (!constraint.equals(oldConstraint)) {
			throw new IllegalArgumentException("Constraints cannot be changed. Attempted for key '" + key + "'.");
		}

		T value = constraint.parsePersisted(persisted);
		values.put(key, value);
	}
}
