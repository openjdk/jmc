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
package org.openjdk.jmc.common.util;

import org.openjdk.jmc.common.unit.IPersister;

/**
 * A preference key with a default value. Note that the configured value is not stored in the
 * preference. Instead an {@link IPreferenceValueProvider} is used to get values.
 *
 * @param <T>
 *            the type of the value
 */
public class TypedPreference<T> {

	private final String identifier;
	private final String name;
	private final String description;
	private final IPersister<T> persister;
	private final T defaultValue;

	/**
	 * @param identifier
	 *            preference identifier
	 * @param name
	 *            preference name
	 * @param description
	 *            a longer description of the preference
	 * @param persister
	 *            a persister that can parse and format values
	 * @param defaultValue
	 *            if a value for this preference has not been set, then use this default value
	 */
	public TypedPreference(String identifier, String name, String description, IPersister<T> persister,
			T defaultValue) {
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.persister = persister;
		this.defaultValue = defaultValue;
	}

	/**
	 * @return preference identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return preference name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return preference description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get a persister that can be used to convert between the preference value type and strings.
	 *
	 * @return value persister
	 */
	public IPersister<T> getPersister() {
		return persister;
	}

	/**
	 * @return the default value for this preference
	 */
	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public int hashCode() {
		return identifier.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TypedPreference<?>) {
			return ((TypedPreference<?>) o).identifier.equals(this.identifier);
		}
		return false;
	}

	@Override
	public String toString() {
		return "[" + identifier + ", " + description + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
