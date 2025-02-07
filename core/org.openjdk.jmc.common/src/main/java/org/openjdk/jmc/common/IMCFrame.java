/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openjdk.jmc.common.messages.internal.Messages;

/**
 * A stack trace frame.
 * <p>
 * It's recommended, but by design not a requirement, that classes that implements IMCFrame also
 * implement {@link IMCMethod}. This allows classes in higher layers to treat a frame just like a
 * method, for instance when using Eclipse object contribution mechanism.
 * <p>
 * This can be implemented simply by letting {@link IMCFrame#getMethod()} return {@code this}.
 */
// FIXME: Move IMC* classes to a subpackage
public interface IMCFrame {

	/**
	 * Frame compilation types.
	 */
	final class Type {
		/**
		 * The frame was executed as native code compiled by the Java JIT compiler.
		 */
		public static final Type JIT_COMPILED = new Type("JIT_COMPILED"); //$NON-NLS-1$

		/**
		 * The frame was executed as interpreted Java byte code.
		 */
		public static final Type INTERPRETED = new Type("INTERPRETED"); //$NON-NLS-1$

		/**
		 * The frame was executed as code that was inlined by the Java JIT compiler.
		 */
		public static final Type INLINED = new Type("INLINED"); //$NON-NLS-1$

		/**
		 * The frame was executed as native code, most probably a C function
		 */
		public static final Type NATIVE = new Type("NATIVE"); //$NON-NLS-1$

		/**
		 * The frame was executed as native code compiled from C++
		 */
		public static final Type CPP = new Type("CPP"); //$NON-NLS-1$

		/**
		 * The frame was executed as kernel native code
		 */
		public static final Type KERNEL = new Type("KERNEL"); //$NON-NLS-1$

		/**
		 * The frame compilation type is unknown.
		 */
		public static final Type UNKNOWN = new Type("UNKNONW"); //$NON-NLS-1$

		private static final String MSG_PREFIX = "IMCFrame_Type_";

		/*
		 * Maximum number of items the TYPE_CACHE will hold. The assumption is that the number of
		 * 'dynamic' frame types will be small, typically in ones rather than tens and as such 100
		 * items in the type cache should cover the vast majority of use cases.
		 */
		private static final int TYPE_CACHE_MAX_SIZE = 100;

		/*
		 * A helper cache for the unrecognized frame types to reduce the amount of allocated
		 * instances. The expectation is that the number of unrecognized frame types will be very
		 * small, usually zero, so the memory overhead of the cache stays negligible.
		 */
		private static final Map<String, Type> TYPE_CACHE = Collections
				.synchronizedMap(new LinkedHashMap<String, Type>() {
					private static final long serialVersionUID = 6330800425284157773L;

					@Override
					protected boolean removeEldestEntry(Map.Entry<String, Type> eldest) {
						return size() > TYPE_CACHE_MAX_SIZE;
					}
				});

		private final String id;
		private final String name;
		private final boolean isUnknown;

		private Type(String id) {
			this.id = id.toUpperCase();

			String key = MSG_PREFIX + this.id;
			if (Messages.hasString(key)) {
				name = Messages.getString(key);
				isUnknown = false;
			} else {
				name = this.id;
				isUnknown = true;
			}
		}

		public static Type cachedType(String type) {
			return TYPE_CACHE.computeIfAbsent(type, IMCFrame.Type::new);
		}

		public String getName() {
			return name;
		}

		public boolean isUnknown() {
			return isUnknown;
		}

		@Override
		public String toString() {
			return id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Type type = (Type) o;
			return id.equals(type.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
	}

	/**
	 * Returns the line number for the frame, or {@code null} if not available.
	 *
	 * @return the line number
	 */
	Integer getFrameLineNumber();

	/**
	 * Returns the byte code index in Java class file, or {@code null} if not available.
	 *
	 * @return the byte code index
	 */
	Integer getBCI();

	/**
	 * The method for the frame. See {@link IMCMethod}
	 *
	 * @return the method
	 */
	IMCMethod getMethod();

	/**
	 * The compilation type of the frame.
	 *
	 * @return the compilation type
	 */
	Type getType();
}
