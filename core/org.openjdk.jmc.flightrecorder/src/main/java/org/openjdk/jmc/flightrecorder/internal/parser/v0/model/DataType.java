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
package org.openjdk.jmc.flightrecorder.internal.parser.v0.model;

/**
 * DataType representing the available data types in a flight recording.
 */
public enum DataType {
	/** Boolean 1-byte */
	BOOLEAN(1, false, false),

	/** Signed 1-byte */
	BYTE(1, true, false),

	/** Unsigned 1-byte */
	U1(1, true, false),

	/** Signed 2-byte */
	SHORT(2, true, false),

	/** Unsigned 2-byte */
	U2(2, true, false),

	/** Signed 4-byte */
	INTEGER(4, true, false),

	/** Unsigned 4-byte */
	U4(4, true, false),

	/** Signed 8-byte */
	LONG(8, true, false),

	/** Unsigned 8-byte */
	U8(8, true, false),

	/** 32-bit floating point */
	FLOAT(4, true, false),

	/** 64-bit floating point */
	DOUBLE(8, true, false),

	/** Character data in UTF-8 format, */
	UTF8(0, false, true),

	/** character data in 16-bit Unicode */
	STRING(0, false, true),

	/** Array of primitives */
	ARRAY(0, false, false),

	/** Complex type */
	STRUCT(0, false, false),

	/** Array of complex types */
	STRUCTARRAY(0, false, false);

	private final int m_size;
	private final boolean m_textual;
	private final boolean m_numeric;

	private DataType(int size, boolean numeric, boolean textual) {
		m_size = size;
		m_textual = textual;
		m_numeric = numeric;
	}

	public int getSize() {
		return m_size;
	}

	public boolean isPrimitive() {
		return ordinal() < ARRAY.ordinal();
	}

	public boolean isTextual() {
		return m_textual;
	}

	public boolean isNumeric() {
		return m_numeric;
	}
}
