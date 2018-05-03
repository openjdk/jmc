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

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.MutableConstrainedMap;
import org.openjdk.jmc.common.unit.UnitLookup;

/**
 * General mutable {@link IOptionDescriptor} based {@link IDescribedMap} with possibility to add
 * constraints.
 *
 * @param <K>
 */
// FIXME: Rename to something more accurate.
public class FixedDescriptorMap<K> extends MutableConstrainedMap<K> implements IDescribedMap<K> {
	private final IMapper<K, ? extends IOptionDescriptor<?>> mapper;
	protected final Map<K, IConstraint<?>> constraints;

	public FixedDescriptorMap(IMapper<K, ? extends IOptionDescriptor<?>> mapper) {
		this(mapper, new HashMap<K, Object>());
	}

	protected FixedDescriptorMap(IMapper<K, ? extends IOptionDescriptor<?>> mapper, HashMap<K, Object> values) {
		this(mapper, values, new HashMap<K, IConstraint<?>>());
	}

	protected FixedDescriptorMap(IMapper<K, ? extends IOptionDescriptor<?>> mapper, HashMap<K, Object> values,
			HashMap<K, IConstraint<?>> constraints) {
		super(values);
		this.mapper = mapper;
		this.constraints = constraints;
	}

	@Override
	public IMutableConstrainedMap<K> emptyWithSameConstraints() {
		return new FixedDescriptorMap<>(mapper, new HashMap<K, Object>(), new HashMap<>(constraints));
	}

	@Override
	public IMutableConstrainedMap<K> mutableCopy() {
		return new FixedDescriptorMap<>(mapper, new HashMap<>(values), new HashMap<>(constraints));
	}

	@Override
	public IConstraint<?> getConstraint(K key) {
		IOptionDescriptor<?> desc = mapper.get(key);
		return (desc != null) ? desc.getConstraint() : constraints.get(key);
	}

	@Override
	protected IConstraint<?> getSuggestedConstraint(K key) {
		IOptionDescriptor<?> desc = mapper.get(key);
		return (desc != null) ? null : UnitLookup.PLAIN_TEXT.getPersister();
	}

	@Override
	public IOptionDescriptor<?> getDescribable(K key) {
		return mapper.get(key);
	}

	@Override
	protected void addConstraint(K key, IConstraint<?> constraint) {
		IOptionDescriptor<?> desc = mapper.get(key);
		if (desc != null) {
			throw new IllegalArgumentException("Key '" + key + "' is expressly prohibited in this map."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		constraints.put(key, constraint);
	}
}
