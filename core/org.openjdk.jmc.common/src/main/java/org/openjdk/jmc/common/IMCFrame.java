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
package org.openjdk.jmc.common;

import org.openjdk.jmc.common.messages.internal.Messages;

/**
 * A stack trace frame.
 * <p>
 * It's recommended, but by design not a requirement, that classes that implements IMCFrame also
 * implement {@link IMCMethod}. This allow classes in higher layers to treat a frame just like a
 * method, for instance when using Eclipse object contribution mechanism.
 * <p>
 * This can be implemented simply by letting {@link IMCFrame#getMethod()} return {@code this}.
 */
// FIXME: Move IMC* classes to a subpackage
public interface IMCFrame {

	/**
	 * Frame compilation types.
	 */
	public enum Type {
		/**
		 * The frame was executed as native code compiled by the Java JIT compiler.
		 */
		JIT_COMPILED,
		/**
		 * The frame was executed as interpreted Java byte code.
		 */
		INTERPRETED,
		/**
		 * The frame was executed as code that was inlined by the Java JIT compiler.
		 */
		INLINED,
		/**
		 * The frame compilation type is unknown.
		 */
		UNKNOWN;

		private final String name;

		private Type() {
			name = Messages.getString("IMCFrame_Type_" + toString()); //$NON-NLS-1$
		}

		public String getName() {
			return name;
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
	public Type getType();
}
