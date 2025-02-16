/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.flamegraph;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.flightrecorder.flamegraph.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static final String FLAMEVIEW_FLAME_GRAPH = "FLAMEVIEW_FLAME_GRAPH"; //$NON-NLS-1$
	public static final String FLAMEVIEW_ICICLE_GRAPH = "FLAMEVIEW_ICICLE_GRAPH"; //$NON-NLS-1$
	public static final String FLAMEVIEW_SAVE_AS = "FLAMEVIEW_SAVE_AS"; //$NON-NLS-1$
	public static final String FLAMEVIEW_PRINT = "FLAMEVIEW_PRINT"; //$NON-NLS-1$
	public static final String FLAMEVIEW_SAVE_FLAME_GRAPH_AS = "FLAMEVIEW_SAVE_FLAME_GRAPH_AS"; //$NON-NLS-1$
	public static final String FLAMEVIEW_JPEG_IMAGE = "FLAMEVIEW_JPEG_IMAGE"; //$NON-NLS-1$
	public static final String FLAMEVIEW_PNG_IMAGE = "FLAMEVIEW_PNG_IMAGE"; //$NON-NLS-1$
	public static final String FLAMEVIEW_TOGGLE_MINIMAP = "FLAMEVIEW_TOGGLE_MINIMAP"; //$NON-NLS-1$
	public static final String FLAMEVIEW_RESET_ZOOM = "FLAMEVIEW_RESET_ZOOM"; //$NON-NLS-1$

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
