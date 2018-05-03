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

import java.lang.reflect.Modifier;

/**
 * A method representation.
 * <p>
 * There are many different ways methods are represented in Mission Control and in Java:
 * MethodIdentfier, java.lang.String, stack trace locations, profiled methods, JFR methods etc.
 * <p>
 * Some IMCMethod implementations may support a wide range of method information while others might
 * only be able provide the basic method name. It's up to the user of this interface to examine what
 * is available. If information is missing {@code null} is returned.
 */
// FIXME: Move IMC* classes to a subpackage
public interface IMCMethod {
	/**
	 * Returns the class this method is declared in.
	 *
	 * @return the class declaring this method
	 */
	IMCType getType();

	/**
	 * Returns the method name not including parameters.
	 * <p>
	 * An example is "mymethod". If the method is native the format is undefined.
	 *
	 * @return the name of this method, or {@code null} if unavailable
	 */
	String getMethodName();

	/**
	 * Returns the formal descriptor.
	 * <p>
	 * For example, the method descriptor for the method
	 * {@code Object mymethod(int i, double d, Thread t)} is
	 * {@code (IDLjava/lang/Thread;)Ljava/lang/Object;}
	 *
	 * @return the formal method descriptor, or {@code null} if unavailable
	 */
	String getFormalDescriptor();

	/**
	 * Returns the modifier used in the Java class file.
	 * <p>
	 * Examples of modifiers are "protected", "public", etc.
	 * <p>
	 * See {@link Modifier} for more information about the bit pattern and for methods that can be
	 * used to decode it.
	 *
	 * @return the modifier used in the class file, or {@code null} if not available
	 */
	Integer getModifier();

	/**
	 * Whether this method is native.
	 *
	 * @return {@code Boolean.TRUE} if the method is native, {@code Boolean.FALSE} if not, or
	 *         {@code null} if the information is not available
	 */
	Boolean isNative();
}
