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

import java.util.Set;

/**
 * Read only interface for a map where the values are constrained by an {@link IConstraint} per key.
 * Note that as this is read only, its methods shouldn't throw any exceptions. This means that all
 * implementations must ensure upon creation and insertion that their contents are valid.
 *
 * @param <K>
 *            the type of the keys in the map
 */
public interface IConstrainedMap<K> {

	/**
	 * @return A {@link Set set} of keys which are known to be valid. That is, those that currently
	 *         are known to have a {@link IConstraint constraint}. This includes all keys which
	 *         currently have a value, but additional keys may be included. In other words,
	 *         {@link #get(Object)} may return {@code null} for some keys included in this set.
	 */
	Set<K> keySet();

	/**
	 * Get the mapped value for {@code key}, or null if no value is currently mapped. If this method
	 * ever returns a non-null value, {@link #getConstraint(Object)} for the same {@code key} will
	 * from that point forward return the same matching non-null constraint.
	 *
	 * @return the mapped value or {@code null}
	 */
	Object get(K key);

	/**
	 * Get a {@link IConstraint constraint} for mapped values of {@code key}, if a constraint has
	 * been imposed for {@code key}.
	 *
	 * @return a constraint or {@code null}
	 */
	IConstraint<?> getConstraint(K key);

	/**
	 * Get the persistable string of the mapped value for {@code key}, or null if no value is
	 * currently mapped. If this method ever returns a non-null value,
	 * {@link #getConstraint(Object)} for the same {@code key} will from that point forward return
	 * the same matching non-null constraint.
	 *
	 * @return a persistable string or {@code null}
	 */
	String getPersistableString(K key);

	/**
	 * Create an empty {@link IMutableConstrainedMap mutable} map, with the same initial constraints
	 * as this {@link IConstrainedMap map}. It might be possible to add {@link IConstraint
	 * constraints} to the created map, depending on the restrictions built into this map.
	 */
	IMutableConstrainedMap<K> emptyWithSameConstraints();

	/**
	 * Create a {@link IMutableConstrainedMap mutable} copy of this {@link IConstrainedMap map},
	 * containing the same initial values as this map. It might be possible to add
	 * {@link IConstraint constraints} to the copy, depending on the restrictions built into this
	 * map.
	 */
	IMutableConstrainedMap<K> mutableCopy();
}
