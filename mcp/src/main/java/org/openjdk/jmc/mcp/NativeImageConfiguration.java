/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.mcp;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Native image reflection registrations.
 * <p>
 * The JFR parser materializes the structured values in a recording - threads, classes, methods,
 * stack frames - with {@code Class.newInstance()} followed by {@code Field.set()} (see
 * {@code ValueReaders.ReflectiveReader} in org.openjdk.jmc.flightrecorder). Native image has no way
 * to see those types are needed, so without this registration parsing any recording fails with
 * {@code InstantiationException}. Both the no-arg constructors and the declared fields have to be
 * reachable, hence fields = true.
 */
@RegisterForReflection(fields = true, classNames = {
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrThread",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrThreadGroup",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrJavaPackage",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrJavaModule",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrJavaClassLoader",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrJavaClass",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrOldObjectGcRoot",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrOldObject",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrOldObjectArray",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrOldObjectField",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrMethod",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrFrame",
		"org.openjdk.jmc.flightrecorder.internal.parser.v1.StructTypes$JfrStackTrace"})
public final class NativeImageConfiguration {

	private NativeImageConfiguration() {
	}
}
