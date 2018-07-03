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
package org.openjdk.jmc.common.item;

import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDescribable;

/**
 * A type for objects of class T. They may have properties that can be accessed by using
 * {@link IMemberAccessor member accessors}. If so, then items of the same type are expected to
 * share the same behavior when accessed by accessors.
 * <p>
 * Analogous to {@link java.lang.Class} in the Java type system.
 *
 * @param <T>
 *            class of the objects that the type is used for
 */
public interface IType<T> extends IDescribable {

	// FIXME: Lists of attributes should not be fetched directly from the type
	@Deprecated
	List<IAttribute<?>> getAttributes();

	/**
	 * Get keys for the accessors that this type knows of. Note that the returned accessors does not
	 * necessarily cover all possible data from the items of this type, and that it is always
	 * possible to define additional accessors that get or calculate values from the items in
	 * non-standard ways.
	 * <p>
	 * Should only be used for low level type inspection. Iterators etc. should use a collection of
	 * predefined {@link IAttribute attributes}.
	 *
	 * @return keys for the accessors defined for this type
	 */
	// FIXME: Can we find a more appropriate return type?
	Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys();

	/**
	 * Tell if {@link ICanonicalAccessorFactory attribute} can return an {@link IMemberAccessor
	 * accessor} for this {@link IType type}. This method is semantically equivalent to
	 * <code>attribute.{@link ICanonicalAccessorFactory#getAccessor(IType) getAccessor(type)} != null</code>,
	 * but may be cheaper.
	 *
	 * @param attribute
	 *            attribute to check
	 * @return {@code true} if the attribute can return an accessor for this type, {@code false} if
	 *         not
	 */
	boolean hasAttribute(ICanonicalAccessorFactory<?> attribute);

	/**
	 * Internal low-level mechanism for retrieving a member accessor for a type, or null if not
	 * available.
	 * <p>
	 * This is only intended to be used by implementors of {@link IAccessorFactory}. All other usage
	 * should be replaced with {@link IAttribute#getAccessor(IType)} call to pre-defined accessors.
	 *
	 * @param <M>
	 *            accessor value type
	 * @param attribute
	 *            the identifier for the field
	 * @return a member accessor
	 */
	<M> IMemberAccessor<M, T> getAccessor(IAccessorKey<M> attribute);

	/**
	 * String identifying the type. It must never be localized and it should only contain characters
	 * that are safe to use in various configuration files, e.g. in XML.
	 *
	 * @return type identifier
	 */
	String getIdentifier();
}
