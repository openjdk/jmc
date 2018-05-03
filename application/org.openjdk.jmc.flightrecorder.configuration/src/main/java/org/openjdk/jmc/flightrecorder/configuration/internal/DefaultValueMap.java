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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.QuantityConversionException;

/**
 * Describable map with defaults values for all known keys, and acceptable optional keys, if any.
 *
 * @param <K>
 */
public class DefaultValueMap<K> implements IDescribedMap<K> {
	// NOTE: This should not be modifiable.
	private final Map<K, ? extends IOptionDescriptor<?>> knownOptions;
	// NOTE: This should ideally be a (JDK 8) Function. May be null.
	private final IMapper<K, IOptionDescriptor<?>> fallbacks;

	public DefaultValueMap(Map<K, ? extends IOptionDescriptor<?>> knownOptions) {
		this(knownOptions, null);
	}

	@SuppressWarnings("unchecked")
	public DefaultValueMap(IMapper<K, IOptionDescriptor<?>> fallbacks) {
		// NOTE: Cast is just to circumvent issue in Eclipse 4.5.2 with JDK 7 compiler/libs.
		this((Map<K, ? extends IOptionDescriptor<?>>) Collections.emptyMap(), fallbacks);
	}

	public DefaultValueMap(Map<K, ? extends IOptionDescriptor<?>> knownOptions,
			IMapper<K, IOptionDescriptor<?>> fallbacks) {
		this.knownOptions = knownOptions;
		this.fallbacks = fallbacks;
	}

	@Override
	public IMutableConstrainedMap<K> emptyWithSameConstraints() {
		return new FixedDescriptorMap<>(makeCombinedMapper());
	}

	@Override
	public IMutableConstrainedMap<K> mutableCopy() {
		FixedDescriptorMap<K> copy = new FixedDescriptorMap<>(makeCombinedMapper());
		try {
			for (Entry<K, ? extends IOptionDescriptor<?>> entry : knownOptions.entrySet()) {
				Object value = entry.getValue().getDefault();
				if (value != null) {
					copy.put(entry.getKey(), value);
				}
			}
		} catch (QuantityConversionException e) {
			throw new RuntimeException("Implementation error", e); //$NON-NLS-1$
		}
		return copy;
	}

	protected IMapper<K, ? extends IOptionDescriptor<?>> makeCombinedMapper() {
		if (knownOptions.isEmpty()) {
			// FIXME: fallbacks may be null here, but it is unlikely in practice. Should be handled.
			return fallbacks;
		} else if (fallbacks == null) {
			return new IMapper.MapMapper<>(knownOptions);
		} else {
			return new IMapper<K, IOptionDescriptor<?>>() {
				@Override
				public IOptionDescriptor<?> get(K key) {
					IOptionDescriptor<?> desc = knownOptions.get(key);
					return (desc != null) ? desc : fallbacks.get(key);
				};
			};
		}
	}

	protected IOptionDescriptor<?> getDescriptor(K key) {
		IOptionDescriptor<?> desc = knownOptions.get(key);
		return ((desc == null) && (fallbacks != null)) ? fallbacks.get(key) : desc;
	}

	@Override
	public IConstraint<?> getConstraint(K key) {
		IOptionDescriptor<?> desc = getDescriptor(key);
		return (desc != null) ? desc.getConstraint() : null;
	}

	@Override
	public IOptionDescriptor<?> getDescribable(K key) {
		return getDescriptor(key);
	}

	@Override
	public Set<K> keySet() {
		return knownOptions.keySet();
	}

	@Override
	public Object get(K key) {
		// NOTE: Only return non-null values for the known keys, for the valueKeySet() contract to hold.
		IOptionDescriptor<?> desc = knownOptions.get(key);
		return (desc != null) ? desc.getDefault() : null;
	}

	@Override
	public String getPersistableString(K key) {
		return getPersistableDefault(getDescriptor(key));
	}

	private <V> String getPersistableDefault(IOptionDescriptor<V> desc) {
		if (desc != null) {
			try {
				V value = desc.getDefault();
				return (value == null) ? null : desc.getConstraint().persistableString(value);
			} catch (QuantityConversionException e) {
				Messages.LOGGER.log(Level.WARNING, "Problem parsing option default", e); //$NON-NLS-1$
			}
		}
		return null;
	}
}
