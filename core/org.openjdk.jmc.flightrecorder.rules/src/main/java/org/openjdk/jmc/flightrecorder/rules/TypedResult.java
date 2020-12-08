/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules;

import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.rules.messages.internal.Messages;

public class TypedResult<T> {

	/**
	 * A constant to be used while transitioning the rules api from 1.0 to 2.0 to keep the old score
	 * values.
	 */
	public static final TypedResult<IQuantity> SCORE = new TypedResult<>("score", //$NON-NLS-1$
			Messages.getString(Messages.TypedResult_SCORE_NAME),
			Messages.getString(Messages.TypedResult_SCORE_DESCRIPTION), UnitLookup.NUMBER, IQuantity.class);

	public static final ContentType<IItemQuery> QUERY = UnitLookup.createSyntheticContentType("itemQuery"); //$NON-NLS-1$

	public static final TypedResult<IItemQuery> ITEM_QUERY = new TypedResult<>("itemQuery", "Item Query", //$NON-NLS-1$
			"Relevant items used to evaluate this rule.", QUERY, IItemQuery.class);

	private final String identifier;
	private final String name;
	private final String description;
	private final ContentType<T> contentType;
	private final Class<T> clazz;

	/**
	 * Creates an object describing a singular typed result value with all needed information.
	 * 
	 * @param identifier
	 *            result identifier
	 * @param name
	 *            result name
	 * @param description
	 *            a longer description of the result
	 * @param contentType
	 *            a contentType that can parse and format values
	 * @param clazz
	 *            the class of the typed result
	 */
	public TypedResult(String identifier, String name, String description, ContentType<T> contentType, Class<T> clazz) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.contentType = contentType;
		this.clazz = clazz;
	}

	public TypedResult(String identifier, String name, String description, ContentType<T> contentType) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.contentType = contentType;
		this.clazz = null;
	}

	public TypedResult(String identifier, IAggregator<T, ?> aggregator, ContentType<T> contentType, Class<T> clazz) {
		this.identifier = identifier;
		name = aggregator.getName();
		description = aggregator.getDescription();
		this.contentType = contentType;
		this.clazz = clazz;
	}

	public Class<T> getResultClass() {
		return clazz;
	}

	/**
	 * @return result identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return result name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return result description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the {@link ContentType} for the result.
	 *
	 * @return value contentType
	 */
	public ContentType<T> getPersister() {
		return contentType;
	}

	@Override
	public int hashCode() {
		return identifier.hashCode();
	}

	public String format(Object result) {
		return contentType.getDefaultFormatter().format(clazz.cast(result));
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TypedResult<?>) {
			return ((TypedResult<?>) o).identifier.equals(this.identifier);
		}
		return false;
	}

	@Override
	public String toString() {
		return "[" + identifier + ", " + description + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
