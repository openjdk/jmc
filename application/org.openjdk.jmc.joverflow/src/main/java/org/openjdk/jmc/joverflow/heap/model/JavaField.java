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
package org.openjdk.jmc.joverflow.heap.model;

/**
 * Data field descriptor. Provides the field name, but does not distinguish between different object
 * types - a deficiency that comes from HPROF heap dump format.
 */
public class JavaField {
	private final String name;
	private final char typeId; // 'I', 'B' etc. for primitive types, 'L' for classes and '[' for arrays
	private final byte sizeInInstance;

	private static final JavaField[] STATIC_QUAZI_FIELDS = new JavaField[3];
	static {
		STATIC_QUAZI_FIELDS[0] = newInstance("<signers>", '[', 0);
		STATIC_QUAZI_FIELDS[1] = newInstance("<protection domain>", 'L', 0);
	}

	public static JavaField newInstance(String name, char typeId, int pointerSize) {
		byte sizeInInstance = 0;
		if (typeId == 'L' || typeId == '[') {
			sizeInInstance = (byte) pointerSize;
		} else {
			switch (typeId) {
			case 'I':
			case 'F':
				sizeInInstance = 4;
				break;
			case 'Z':
			case 'B':
				sizeInInstance = 1;
				break;
			case 'S':
			case 'C':
				sizeInInstance = 2;
				break;
			case 'J':
			case 'D':
				sizeInInstance = 8;
				break;
			}
		}

		return new JavaField(name, typeId, sizeInInstance);
	}

	private JavaField(String name, char typeId, byte sizeInInstance) {
		this.name = name;
		this.typeId = typeId;
		this.sizeInInstance = sizeInInstance;
	}

	public boolean isReference() {
		return (typeId == '[' || typeId == 'L');
	}

	public String getName() {
		return name;
	}

	public char getTypeId() {
		return typeId;
	}

	public int getSizeInInstance() {
		return sizeInInstance;
	}

	/**
	 * Adds two "quazi-fields" for data that exists in any Class object in addition to the normal
	 * static fields. This is done to enable our heap scanner to follow these references.
	 */
	public static void addStaticQuaziFields(JavaField[] staticFields) {
		System.arraycopy(STATIC_QUAZI_FIELDS, 0, staticFields, staticFields.length - 2, 2);
	}

	// Debugging

	@Override
	public String toString() {
		return Character.toString(typeId) + ' ' + name + " sizeInInstance = " + sizeInInstance;
	}
}
