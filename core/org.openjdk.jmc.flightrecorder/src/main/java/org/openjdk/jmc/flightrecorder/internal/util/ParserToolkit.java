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
package org.openjdk.jmc.flightrecorder.internal.util;

import java.util.Objects;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;

public final class ParserToolkit {
	private ParserToolkit() {
		throw new Error("Don't"); //$NON-NLS-1$
	}

	public static <T> T get(T[] elements, int index) throws InvalidJfrFileException {
		if (index < 0 || index >= elements.length) {
			throw new InvalidJfrFileException();
		}
		return elements[index];
	}

	public static void assertValue(long value, long accepted) throws InvalidJfrFileException {
		if (value != accepted) {
			throw new InvalidJfrFileException(value + " is not the expected value " + accepted); //$NON-NLS-1$
		}
	}

	public static void assertValue(Object value, Object accepted) throws InvalidJfrFileException {
		if (!Objects.equals(value, accepted)) {
			throw new InvalidJfrFileException(value + " is not the expected value " + accepted); //$NON-NLS-1$
		}
	}

	public static void assertValue(Object value, Object ... accepted) throws InvalidJfrFileException {
		for (Object a : accepted) {
			if (Objects.equals(a, value)) {
				return;
			}
		}
		throw new InvalidJfrFileException(value + " is not among the expected values"); //$NON-NLS-1$
	}

	public static IMCFrame.Type parseFrameType(String type) {
		if ("Interpreted".equals(type)) { //$NON-NLS-1$
			return IMCFrame.Type.INTERPRETED;
		}
		if ("JIT compiled".equals(type)) { //$NON-NLS-1$
			return IMCFrame.Type.JIT_COMPILED;
		}
		if ("Inlined".equals(type)) { //$NON-NLS-1$
			return IMCFrame.Type.INLINED;
		}
		return IMCFrame.Type.UNKNOWN;
	}
}
