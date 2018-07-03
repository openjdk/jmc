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
package org.openjdk.jmc.flightrecorder.parser;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.unit.ContentType;

/**
 * A descriptor of a value field for use in
 * {@link IEventSinkFactory#create(String, String, String[], String, java.util.List)
 * IEventSinkFactory.create}.
 */
// FIXME: Investigate if this should be replaced by IAttribute
public final class ValueField {

	private final String identifier;
	private final String name;
	private final String description;
	private final ContentType<?> contentType;

	/**
	 * Create a value field matching an {@link IAttribute}.
	 *
	 * @param attribute
	 *            attribute to match
	 */
	public ValueField(IAttribute<?> attribute) {
		this(attribute.getIdentifier(), attribute.getName(), attribute.getDescription(), attribute.getContentType());
	}

	/**
	 * Create a value field.
	 *
	 * @param identifier
	 *            field ID
	 * @param name
	 *            human readable field name
	 * @param description
	 *            human readable field description
	 * @param contentType
	 *            content type of the field
	 */
	public ValueField(String identifier, String name, String description, ContentType<?> contentType) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.contentType = contentType;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public ContentType<?> getContentType() {
		return contentType;
	}

	/**
	 * Check if a value field matches an attribute. Note that only ID and content type is checked
	 * since human readable values may differ, especially due to translations.
	 *
	 * @param a
	 *            attribute to match
	 * @return {@code true} if the value field and the attribute match, {@code false} if not
	 */
	public boolean matches(IAttribute<?> a) {
		return identifier.equals(a.getIdentifier()) && contentType.equals(a.getContentType());
	}

	@Override
	public String toString() {
		// For debugging purposes
		return identifier + ":" + contentType; //$NON-NLS-1$
	}
}
