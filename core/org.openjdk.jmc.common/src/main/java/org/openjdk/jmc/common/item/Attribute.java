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

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.common.unit.ContentType;

public class Attribute<T> extends CanonicalAccessorFactory<T> implements IAttribute<T> {
	private static Map<IAccessorKey<?>, IAttribute<?>> CANONICAL_MAP = new HashMap<>();

	/**
	 * Obtain an attribute.
	 *
	 * @param <T>
	 *            attribute value type
	 * @param identifier
	 *            attribute id
	 * @param name
	 *            attribute name
	 * @param description
	 *            attribute description
	 * @param contentType
	 *            content type of the attribute values
	 * @return an attribute
	 */
	public static final <T> IAttribute<T> attr(
		String identifier, String name, String description, ContentType<T> contentType) {
		return getCanonical(new Attribute<>(identifier, name, description, contentType));
	}

	/**
	 * Obtain an attribute.
	 *
	 * @param <T>
	 *            attribute value type
	 * @param identifier
	 *            attribute id
	 * @param name
	 *            attribute name
	 * @param contentType
	 *            content type of the attribute values
	 * @return an attribute
	 */
	public static final <T> IAttribute<T> attr(String identifier, String name, ContentType<T> contentType) {
		return attr(identifier, name, null, contentType);
	}

	public static final <T> ICanonicalAccessorFactory<T> attr(String identifier, ContentType<T> contentType) {
		return getCanonical(new CanonicalAccessorFactory<>(identifier, contentType));
	}

	/**
	 * If the attribute {@code key} has a canonical equivalent, get that. Otherwise, use
	 * {@code key}.
	 *
	 * @param key
	 * @return {@code key} or a canonical equivalent
	 */
	private static <T> ICanonicalAccessorFactory<T> getCanonical(CanonicalAccessorFactory<T> key) {
		@SuppressWarnings("unchecked")
		ICanonicalAccessorFactory<T> canonical = (ICanonicalAccessorFactory<T>) CANONICAL_MAP.get(key);
		return (canonical != null) ? canonical : key;
	}

	/**
	 * If the attribute {@code key} has a canonical equivalent, get that. Otherwise, use
	 * {@code key}.
	 *
	 * @param key
	 * @return {@code key} or a canonical equivalent
	 */
	private static <T> IAttribute<T> getCanonical(Attribute<T> key) {
		@SuppressWarnings("unchecked")
		IAttribute<T> canonical = (IAttribute<T>) CANONICAL_MAP.get(key);
		return (canonical != null) ? canonical : key;
	}

	/**
	 * Set {@code key} as the canonical (and only allowed) attribute for its equivalence class.
	 *
	 * @param <T>
	 *            attribute value type
	 * @param key
	 *            attribute to canonicalize
	 * @return canonicalized attribute
	 * @throws IllegalStateException
	 *             if an equivalent attribute has already been canonicalized
	 */
	@SuppressWarnings("nls")
	public static <T> IAttribute<T> canonicalize(Attribute<T> key) {
		IAttribute<?> old = CANONICAL_MAP.put(key, key);
		if (old != null) {
			throw new IllegalStateException("Canonical attribute " + old + " already existed when adding " + key);
		}
		return key;
	}

	private final String name;
	private final String description;

	/**
	 * Protected constructor. Use one of the parameterized factory methods instead.
	 *
	 * @param identifier
	 *            attribute id
	 * @param name
	 *            attribute name
	 * @param description
	 *            attribute description
	 * @param contentType
	 *            content type of the attribute values
	 * @see #attr(String, String, String, ContentType)
	 * @see #attr(String, String, ContentType)
	 */
	protected Attribute(String identifier, String name, String description, ContentType<T> contentType) {
		super(identifier, contentType);
		this.name = name;
		this.description = description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}
}
