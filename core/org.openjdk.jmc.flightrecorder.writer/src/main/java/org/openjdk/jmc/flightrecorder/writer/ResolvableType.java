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
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

public final class ResolvableType implements TypeImpl {
	private final String typeName;
	private final MetadataImpl metadata;

	private volatile TypeImpl delegate;

	ResolvableType(String typeName, MetadataImpl metadata) {
		this.typeName = typeName;
		this.metadata = metadata;
		// self-register in metadata as 'unresolved'
		this.metadata.addUnresolved(this);
	}

	@Override
	public boolean isResolved() {
		return delegate != null;
	}

	private void checkResolved() {
		if (delegate == null) {
			throw new IllegalStateException();
		}
	}

	@Override
	public long getId() {
		checkResolved();
		return delegate.getId();
	}

	@Override
	public boolean hasConstantPool() {
		checkResolved();
		return delegate.hasConstantPool();
	}

	@Override
	public TypedValueImpl asValue(String value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(byte value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(char value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(short value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(int value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(long value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(float value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(double value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(boolean value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl asValue(Consumer<TypedValueBuilder> builderCallback) {
		checkResolved();
		return delegate.asValue(builderCallback);
	}

	@Override
	public TypedValueImpl asValue(Object value) {
		checkResolved();
		return delegate.asValue(value);
	}

	@Override
	public TypedValueImpl nullValue() {
		checkResolved();
		return delegate.nullValue();
	}

	@Override
	public boolean isBuiltin() {
		checkResolved();
		return delegate.isBuiltin();
	}

	@Override
	public boolean isSimple() {
		checkResolved();
		return delegate.isSimple();
	}

	@Override
	public String getSupertype() {
		checkResolved();
		return delegate.getSupertype();
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		checkResolved();
		return delegate.getFields();
	}

	@Override
	public TypedFieldImpl getField(String name) {
		checkResolved();
		return delegate.getField(name);
	}

	@Override
	public List<Annotation> getAnnotations() {
		checkResolved();
		return delegate.getAnnotations();
	}

	@Override
	public boolean canAccept(Object value) {
		checkResolved();
		return delegate.canAccept(value);
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public boolean isSame(NamedType other) {
		checkResolved();
		return delegate.isSame(other);
	}

	@Override
	public boolean isUsedBy(Type other) {
		checkResolved();
		return delegate.isUsedBy(other);
	}

	@Override
	public TypesImpl getTypes() {
		return null;
	}

	@Override
	public TypedValueBuilderImpl valueBuilder() {
		checkResolved();
		return delegate.valueBuilder();
	}

	boolean resolve() {
		TypeImpl resolved = metadata.getType(typeName, false);
		if (resolved instanceof BaseType) {
			delegate = resolved;
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ResolvableType that = (ResolvableType) o;
		return typeName.equals(that.typeName);
	}

	@Override
	public int hashCode() {
		return NonZeroHashCode.hash(typeName);
	}

	@Override
	public String toString() {
		return "ResolvableType [typeName=" + typeName + ", metadata=" + metadata + ", delegate=" + delegate + "]";
	}
}
