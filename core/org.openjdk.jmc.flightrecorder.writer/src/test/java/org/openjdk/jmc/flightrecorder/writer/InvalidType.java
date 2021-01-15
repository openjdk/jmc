/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.writer;

import java.util.List;
import java.util.function.Consumer;

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.api.NamedType;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValueBuilder;

final class InvalidType implements TypeImpl {
	@Override
	public long getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isBuiltin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSimple() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isResolved() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasConstantPool() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSupertype() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedFieldImpl getField(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Annotation> getAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canAccept(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(byte value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(char value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(short value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(long value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(float value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(boolean value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(Consumer<TypedValueBuilder> builderCallback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl asValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypedValueImpl nullValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isUsedBy(Type other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTypeName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSame(NamedType other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypesImpl getTypes() {
		return null;
	}
}
