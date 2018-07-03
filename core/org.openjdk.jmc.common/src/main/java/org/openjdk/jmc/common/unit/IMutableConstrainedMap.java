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

/**
 * A mutable extension to {@link IConstrainedMap}. Note that it is the values that can be mutated.
 * Existing constraints cannot be modified. However, some implementations may allow adding
 * constraints for keys that currently doesn't have a constraint, and thus no current value.
 *
 * @param <K>
 *            the type of the keys in the map
 */
public interface IMutableConstrainedMap<K> extends IConstrainedMap<K> {

	/**
	 * Map {@code key} to {@code value}, if allowed according to either an existing
	 * {@link IConstraint constraint} or an implicit default constraint, if available.
	 *
	 * @throws ClassCastException
	 *             if the actual type of {@code value} prevents it from being accepted
	 * @throws IllegalArgumentException
	 *             if some restriction built into this map prevents values to be added for
	 *             {@code key}, or possibly if some type aspect of {@code value} prevents it from
	 *             being accepted
	 * @throws QuantityConversionException
	 *             if {@code value} is rejected by the constraint in some other way
	 */
	void put(K key, Object value) throws QuantityConversionException;

	/**
	 * Map {@code key} to the value obtained by parsing {@code persisted}, if allowed according to
	 * either an existing {@link IConstraint constraint} or an implicit default constraint, if
	 * available.
	 *
	 * @throws IllegalArgumentException
	 *             if some restriction built into this map prevents values to be added for
	 *             {@code key}
	 * @throws QuantityConversionException
	 *             if {@code persisted} is rejected by the constraint in some way
	 */
	void putPersistedString(K key, String persisted) throws QuantityConversionException;

	/**
	 * Map {@code key} to {@code value}, if allowed according to {@code constraint} and additional
	 * restrictions on this map such as a conflicting {@link IConstraint constraint} being in effect
	 * for this {@code key}.
	 *
	 * @throws IllegalArgumentException
	 *             if some restriction built into this map prevents values to be added for
	 *             {@code key}, or a different {@link IConstraint constraint} being in effect for
	 *             {@code key}, or possibly if some type aspect of {@code value} prevents it from
	 *             being accepted
	 * @throws QuantityConversionException
	 *             if {@code value} is rejected by the constraint in some other way
	 */
	<T> void put(K key, IConstraint<T> constraint, T value) throws QuantityConversionException;

	/**
	 * Map {@code key} to the value obtained by parsing {@code persisted}, if allowed according to
	 * {@code constraint} and additional restrictions on this map such as a conflicting
	 * {@link IConstraint constraint} being in effect for this {@code key}.
	 *
	 * @throws IllegalArgumentException
	 *             if some restriction built into this map prevents values to be added for
	 *             {@code key}, or a different {@link IConstraint constraint} being in effect for
	 *             {@code key}
	 * @throws QuantityConversionException
	 *             if {@code persisted} is rejected by the constraint in some other way
	 */
	<T> void putPersistedString(K key, IConstraint<T> constraint, String persisted) throws QuantityConversionException;
}
