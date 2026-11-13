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
package org.openjdk.jmc.agent.collections.bootstrap;

import java.lang.invoke.MethodHandle;

/**
 * The bootstrap-visible bridge between the instrumented JDK collections and the agent's emitter.
 * <p>
 * Depends only on {@code java.base} types ({@link MethodHandle}). It is defined into the bootstrap
 * class loader (via {@code Unsafe.defineClass} with a {@code null} loader) so bootstrap-loaded
 * classes like {@link java.util.HashMap} can call it, and must not reference {@code jdk.jfr} or any
 * agent class (not visible from bootstrap). It forwards to {@code CollectionResizeEmitter} through
 * the {@link MethodHandle} set by {@link #install}.
 */
public final class CollectionEventBridge {

	private static volatile MethodHandle resizeHandle;
	// Threshold mirrored from the emitter (see setMinSize) so the dominant below-threshold case
	// exits on a simple field compare instead of crossing the mutable - hence not inlinable -
	// MethodHandle. Effectively disabled until the emitter pushes the configured value.
	private static volatile long minSize = Long.MAX_VALUE;

	private CollectionEventBridge() {
	}

	/**
	 * Installs the emitter handle (called reflectively by the agent at init).
	 *
	 * @param resize
	 *            handle to the resize emitter, of type {@code (Object,long,Object,Object)void}.
	 */
	public static void install(MethodHandle resize) {
		resizeHandle = resize;
	}

	/**
	 * Sets the fast-filter threshold (called reflectively by the emitter at init and on retunes).
	 *
	 * @param newMinSize
	 *            the minimum collection size (entry count) before a resize is forwarded.
	 */
	public static void setMinSize(long newMinSize) {
		minSize = newMinSize;
	}

	/**
	 * Invoked by the instrumented resize methods. Arrays are passed as {@link Object} so no
	 * capacity computation is woven into the hot class; the emitter derives the capacities.
	 */
	public static void onResize(Object collection, long size, Object oldArray, Object newArray) {
		if (size < minSize) {
			return;
		}
		MethodHandle h = resizeHandle;
		if (h == null) {
			return;
		}
		try {
			h.invokeExact(collection, size, oldArray, newArray);
		} catch (Throwable t) {
			// A failure here must not break the instrumented collection.
		}
	}
}
