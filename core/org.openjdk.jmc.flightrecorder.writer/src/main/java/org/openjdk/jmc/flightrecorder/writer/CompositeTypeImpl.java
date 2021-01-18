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

import org.openjdk.jmc.flightrecorder.writer.api.Annotation;
import org.openjdk.jmc.flightrecorder.writer.util.NonZeroHashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A composite JFR type */
final class CompositeTypeImpl extends BaseType {
	private int hashCode = 0;

	private final Map<String, TypedFieldImpl> fieldMap;
	private final List<TypedFieldImpl> fields;
	private final List<Annotation> annotations;

	CompositeTypeImpl(long id, String name, String supertype, TypeStructureImpl typeStructure,
			ConstantPools constantPools, TypesImpl types) {
		super(id, name, supertype, constantPools, types);
		this.fields = collectFields(typeStructure);
		this.annotations = typeStructure == null ? Collections.emptyList()
				: Collections.unmodifiableList(typeStructure.getAnnotations());
		this.fieldMap = fields.stream().collect(Collectors.toMap(TypedFieldImpl::getName, f -> f));
	}

	private List<TypedFieldImpl> collectFields(TypeStructureImpl typeStructure) {
		if (typeStructure == null) {
			return Collections.emptyList();
		}
		List<TypedFieldImpl> fields = new ArrayList<>();
		for (TypedFieldImpl field : typeStructure.fields) {
			if (field.getType() == SelfType.INSTANCE) {
				fields.add(new TypedFieldImpl(this, field.getName(), field.isArray()));
			} else {
				fields.add(field);
			}
		}
		return fields;
	}

	@Override
	public boolean isBuiltin() {
		return false;
	}

	@Override
	public List<TypedFieldImpl> getFields() {
		return fields;
	}

	@Override
	public TypedFieldImpl getField(String name) {
		return fieldMap.get(name);
	}

	@Override
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public boolean canAccept(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof TypedValueImpl) {
			return ((TypedValueImpl) value).getType().equals(this);
		}
		return value instanceof Map;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		CompositeTypeImpl that = (CompositeTypeImpl) o;
		return fields.equals(that.fields) && annotations.equals(that.annotations);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			List<TypedFieldImpl> nonRecursiveFields = new ArrayList<>(fields.size());
			for (TypedFieldImpl typedField : fields) {
				if (typedField.getType() != this) {
					nonRecursiveFields.add(typedField);
				}
			}
			hashCode = NonZeroHashCode.hash(super.hashCode(), nonRecursiveFields, annotations);
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "CompositeType(" + getTypeName() + ")";
	}
}
