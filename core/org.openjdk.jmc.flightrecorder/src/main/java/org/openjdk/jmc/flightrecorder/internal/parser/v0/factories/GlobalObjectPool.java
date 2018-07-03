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
package org.openjdk.jmc.flightrecorder.internal.parser.v0.factories;

import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.LoaderContext;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ContentTypeDescriptor;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ValueDescriptor;
import org.openjdk.jmc.flightrecorder.internal.util.CanonicalConstantMap;

public final class GlobalObjectPool {

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static IPoolFactory<?> getFactory(ContentTypeDescriptor contentType, LoaderContext context)
			throws InvalidJfrFileException {
		String name = contentType.getName();
		ValueDescriptor[] dataStructure = contentType.getDataStructure();
		if ("Method".equals(name)) { //$NON-NLS-1$
			return new MethodFactory(dataStructure, (CanonicalConstantMap) context.getConstantPool(name));
		}
		if ("StackTrace".equals(name)) { //$NON-NLS-1$
			return new StackTraceFactory(dataStructure, (CanonicalConstantMap) context.getConstantPool(name));
		}
		if ("JavaThread".equals(name)) { //$NON-NLS-1$
			return new JavaThreadFactory(dataStructure);
		}
		if ("Thread".equals(name)) { //$NON-NLS-1$
			return new OSThreadFactory((CanonicalConstantMap) context.getConstantPool(name));
		}
		if ("Class".equals(name)) { //$NON-NLS-1$
			return new TypeFactory(dataStructure, (CanonicalConstantMap) context.getConstantPool(name));
		}
		if ("ThreadGroup".equals(name)) { //$NON-NLS-1$
			return new ThreadGroupFactory(dataStructure);
		}
		if ("OldObject".equals(name)) { //$NON-NLS-1$
			return new OldObjectFactory(dataStructure);
		}
		if ("JavaObject".equals(name)) { //$NON-NLS-1$
			return new OldObjectFactory(dataStructure);
		}
		return null;
	}
}
