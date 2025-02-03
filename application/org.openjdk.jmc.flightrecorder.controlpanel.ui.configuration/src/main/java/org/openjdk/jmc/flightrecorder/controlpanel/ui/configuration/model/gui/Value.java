/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.gui;

final class Value {
	public final static Value NULL = new Value(""); //$NON-NLS-1$
	public final static Value TRUE = new Value(Boolean.TRUE.toString());
	public final static Value FALSE = new Value(Boolean.FALSE.toString());

	private final String m_value;

	private Value(String value) {
		if (value == null) {
			throw new IllegalArgumentException("Value can't be null. Use empty string!"); //$NON-NLS-1$
		}
		m_value = value;
	}

	public boolean isTrue() {
		return TRUE.equals(this);
	}

	public boolean isFalse() {
		return !isTrue();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Value) {
			return m_value.equalsIgnoreCase(((Value) o).m_value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return m_value.hashCode();
	}

	public static Value valueOf(String value) {
		if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
			return TRUE;
		}
		if (Boolean.FALSE.toString().equalsIgnoreCase(value)) {
			return FALSE;
		}

		return new Value(value);
	}

	@Override
	public String toString() {
		return m_value;
	}

	public boolean isNull() {
		return "".equals(m_value); //$NON-NLS-1$
	}

	public static Value valueOf(boolean value) {
		return value ? Value.TRUE : Value.FALSE;
	}
}
