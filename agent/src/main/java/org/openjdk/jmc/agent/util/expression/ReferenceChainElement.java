/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.util.expression;

import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.util.AccessUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public interface ReferenceChainElement {
	// class/interface which the reference is from
	Class<?> getMemberingClass();

	// class/interface which the reference is to
	Class<?> getReferencedClass();

	// the type of the class/interface which the reference is from 
	Type getMemberingType();

	// the type of the class/interface which the reference is to
	Type getReferencedType();

	// if this reference is static
	boolean isStatic();

	class FieldReference implements ReferenceChainElement {
		private final Class<?> memberingClass;
		private final Field field;

		public FieldReference(Class<?> memberingClass, Field field) {
			this.memberingClass = memberingClass;
			this.field = field;

			try {
				AccessUtils.getFieldOnHierarchy(memberingClass, field.getName());
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException(
						String.format("'%s' is not a field of '%s'", field.getName(), memberingClass.getName()));
			}
		}

		@Override
		public Class<?> getMemberingClass() {
			return memberingClass;
		}

		@Override
		public Class<?> getReferencedClass() {
			return field.getType();
		}

		@Override
		public Type getMemberingType() {
			return Type.getType(getMemberingClass());
		}

		@Override
		public Type getReferencedType() {
			return Type.getType(getReferencedClass());
		}

		@Override
		public boolean isStatic() {
			return Modifier.isStatic(field.getModifiers());
		}

		@Override
		public String toString() {
			return String.format("%s.%s:%s", getMemberingClass().getName(), getName(), getReferencedClass().getName());
		}

		public Field getField() {
			return field;
		}

		public String getName() {
			return getField().getName();
		}
	}

	class ThisReference implements ReferenceChainElement {
		private final Class<?> clazz;

		public ThisReference(Class<?> clazz) {
			this.clazz = clazz;

			Objects.requireNonNull(clazz, "Class is not nullable");
		}

		@Override
		public Class<?> getMemberingClass() {
			return clazz;
		}

		@Override
		public Class<?> getReferencedClass() {
			return clazz;
		}

		@Override
		public Type getMemberingType() {
			return Type.getType(getMemberingClass());
		}

		@Override
		public Type getReferencedType() {
			return Type.getType(getReferencedClass());
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public String toString() {
			return "this";
		}
	}

	class QualifiedThisReference implements ReferenceChainElement {
		private final Class<?> innerClass;
		private final Class<?> enclosingClass;
		private final int depth;

		public QualifiedThisReference(Class<?> innerClass, Class<?> enclosingClass) {
			this.innerClass = innerClass;
			this.enclosingClass = enclosingClass;

			Class<?> c = innerClass;
			int d = 0; // depth of inner class nesting, used for this$i reference to enclosing classes
			while (!enclosingClass.equals(c.getEnclosingClass())) {
				Class<?> enclosing = c.getEnclosingClass();
				if (enclosing == null) {
					throw new IllegalArgumentException(String.format("%s is not an enclosing class of %s",
							enclosingClass.getName(), innerClass.getName()));
				}

				d++;
				c = enclosing;
			}

			this.depth = d;
		}

		@Override
		public Class<?> getMemberingClass() {
			return innerClass;
		}

		@Override
		public Class<?> getReferencedClass() {
			return enclosingClass;
		}

		@Override
		public Type getMemberingType() {
			return Type.getType(getMemberingClass());
		}

		@Override
		public Type getReferencedType() {
			return Type.getType(getReferencedClass());
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public String toString() {
			return String.format("%s.this", getReferencedClass().getName());
		}

		public int getDepth() {
			return depth;
		}
	}
}
