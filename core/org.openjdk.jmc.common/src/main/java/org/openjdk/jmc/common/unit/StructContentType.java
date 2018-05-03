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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IMemberAccessor;

public class StructContentType<T> extends ContentType<T> {

	private static final class AccessorEntry<T> implements IDescribable {
		final IMemberAccessor<?, T> accessor;
		final String name;
		final String description;

		AccessorEntry(IMemberAccessor<?, T> accessor, String name, String description) {
			this.accessor = accessor;
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

	private final List<IAttribute<?>> m_attributes = new ArrayList<>();
	private final Map<IAccessorKey<?>, AccessorEntry<T>> m_accessors = new LinkedHashMap<>();

	private final String description;

	public StructContentType(String identifier, String name, String description) {
		super(identifier, name == null ? lookupNameFor(identifier) : name);
		this.description = ((description != null) ? description + " [" : "[") + identifier + ']'; //$NON-NLS-1$ //$NON-NLS-2$
		addFormatter(new DisplayFormatter<>(this, IDisplayable.AUTO, "Value")); //$NON-NLS-1$
	}

	protected StructContentType(String identifier, String name) {
		super(identifier, name == null ? lookupNameFor(identifier) : name);
		// Mimic implementation in ContentType, although doubtful choice and reason for implementing IDescribable is unknown.
		this.description = ""; //$NON-NLS-1$
	}

	public <M> void addField(
		String identifier, ContentType<M> contentType, String name, String desc, IMemberAccessor<M, T> accessor) {
		// FIXME: Attribute should not be created here
		IAttribute<M> attr = Attribute.attr(identifier, name, desc, contentType);
		m_attributes.add(attr);
		m_accessors.put(attr.getKey(), new AccessorEntry<>(accessor, name, desc));
	}

	// IType.getAttributes is deprecated
	@Deprecated
	public <M> void addExtraAttribute(int atIndex, IAttribute<M> attribute) {
		m_attributes.add(atIndex, attribute);
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<IAttribute<?>> getAttributes() {
		return m_attributes;
	}

	@Override
	public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
		return m_accessors;
	}

	@Override
	public <M> IMemberAccessor<M, T> getAccessor(IAccessorKey<M> attribute) {
		AccessorEntry<T> accessorEntry = m_accessors.get(attribute);
		if (accessorEntry != null) {
			// Cast is safe since type is checked on addField call
			@SuppressWarnings("unchecked")
			IMemberAccessor<M, T> typedAccessor = (IMemberAccessor<M, T>) accessorEntry.accessor;
			return typedAccessor;
		}
		return null;
	}

}
