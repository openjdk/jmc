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
package org.openjdk.jmc.agent.collections;

/**
 * Immutable configuration for collection resize tracking. The only tunable is {@code minsize} - the
 * minimum collection size before a resize event is emitted; everything else is fixed and hidden.
 */
public final class CollectionTrackingSettings {

	/**
	 * Default {@code minsize}, used when none is specified. Suppresses noise from small collections
	 * resizing early in their life.
	 */
	public static final int DEFAULT_MIN_SIZE = 128;

	private final boolean enabled;
	private final int minSize;

	private CollectionTrackingSettings(boolean enabled, int minSize) {
		this.enabled = enabled;
		this.minSize = minSize;
	}

	/**
	 * @return settings representing a disabled collection tracking capability.
	 */
	public static CollectionTrackingSettings disabled() {
		return new CollectionTrackingSettings(false, DEFAULT_MIN_SIZE);
	}

	/**
	 * @param minSize
	 *            the minimum collection size (entry count) before a resize event is emitted.
	 * @return settings representing an enabled collection tracking capability.
	 */
	public static CollectionTrackingSettings enabled(int minSize) {
		return new CollectionTrackingSettings(true, minSize);
	}

	/**
	 * @return {@code true} if collection resize tracking is enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @return the minimum collection size (entry count) before a resize event is emitted.
	 */
	public int getMinSize() {
		return minSize;
	}

	@Override
	public String toString() {
		return "CollectionTrackingSettings [enabled=" + enabled + ", minSize=" + minSize + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
