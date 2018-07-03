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

/**
 * Simple constrained map that allows constraints to be added, and an optional fallback constraint.
 *
 * @param <K>
 *            the type of the keys in the map
 */
public class SimpleConstrainedMap<K> extends MutableConstrainedMap<K> {
	protected final Map<K, IConstraint<?>> constraints;
	protected final IConstraint<?> fallback;

	public SimpleConstrainedMap() {
		this(null);
	}

	public SimpleConstrainedMap(IConstraint<?> fallback) {
		this.constraints = new HashMap<>();
		this.fallback = fallback;
	}

	public SimpleConstrainedMap(Map<K, IConstraint<?>> constraints, IConstraint<?> fallback) {
		this.constraints = new HashMap<>(constraints);
		this.fallback = fallback;
	}

	protected SimpleConstrainedMap(Map<K, IConstraint<?>> constraints, IConstraint<?> fallback, Map<K, Object> values) {
		this(constraints, fallback);
		// FIXME: We should validate values, but validation is currently only accessible to subclasses.
		this.values.putAll(values);
	}

	@Override
	public IConstraint<?> getConstraint(K key) {
		return constraints.get(key);
	}

	@Override
	protected IConstraint<?> getSuggestedConstraint(K key) {
		return fallback;
	}

	@Override
	protected void addConstraint(K key, IConstraint<?> constraint) {
		constraints.put(key, constraint);
	}

	@Override
	public IMutableConstrainedMap<K> emptyWithSameConstraints() {
		return new SimpleConstrainedMap<>(constraints, fallback);
	}

	@Override
	public IMutableConstrainedMap<K> mutableCopy() {
		return new SimpleConstrainedMap<>(constraints, fallback, values);
	}
}
