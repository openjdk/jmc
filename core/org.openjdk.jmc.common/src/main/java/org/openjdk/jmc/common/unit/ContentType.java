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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.messages.internal.Messages;

/**
 * A content type describes what kind of data a value is. It's not specific unit, like
 * seconds/minutes or bytes/MB, but it could be time or memory.
 * <p>
 * Data values of the same content type should be able share a single axis in a graph.
 *
 * @param <T>
 *            the type of values that the content type is used for
 */
public class ContentType<T> implements IType<T> {
	private final List<DisplayFormatter<T>> m_formatters = new ArrayList<>();

	protected final String m_identifier;
	private final String m_localizedName;

	ContentType(String identifier) {
		this(identifier, lookupNameFor(identifier));
	}

	public ContentType(String identifier, String localizedName) {
		m_identifier = identifier;
		m_localizedName = localizedName;
	}

	protected void addFormatter(DisplayFormatter<T> formatter) {
		// FIXME: Disallow formatters with the same identifier.
		m_formatters.add(formatter);
	}

	public List<DisplayFormatter<T>> getFormatters() {
		return Collections.unmodifiableList(m_formatters);
	}

	public IFormatter<T> getDefaultFormatter() {
		return m_formatters.get(0);
	}

	@Override
	public String getName() {
		return m_localizedName;
	}

	/*
	 * FIXME: This is a clash between IType and the legacy ContentType identifier (used in the
	 * Console when specifying units for MRIs, but shouldn't be used elsewhere).
	 */
	@Override
	public String getIdentifier() {
		return m_identifier;
	}

	public IPersister<T> getPersister() {
		return null;
	}

	// FIXME: Replace formatterIdentifier with some specifier.
	public IFormatter<T> getFormatter(String formatterIdentifier) {
		for (DisplayFormatter<T> formatter : m_formatters) {
			if (formatterIdentifier.equals(formatter.getIdentifier())) {
				return formatter;
			}
		}
		return m_formatters.get(0);
	}

	@Override
	public String getDescription() {
		// TODO: Add description?
		return ""; //$NON-NLS-1$
	}

	@Override
	public List<IAttribute<?>> getAttributes() {
		return Collections.emptyList();
	}

	@Override
	public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
		return Collections.emptyMap();
	}

	@Override
	public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
		return attribute.getAccessor(this) != null;
	}

	@Override
	public <M> IMemberAccessor<M, T> getAccessor(IAccessorKey<M> attribute) {
		return null;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString() {
		return "Type(" + m_identifier + ')';
	}

	protected static String lookupNameFor(String typeIdentifier) {
		return Messages.getString("ContentType_" + typeIdentifier); //$NON-NLS-1$
	}
}
