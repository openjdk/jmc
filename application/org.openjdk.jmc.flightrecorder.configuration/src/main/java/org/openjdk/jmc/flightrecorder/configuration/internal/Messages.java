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
package org.openjdk.jmc.flightrecorder.configuration.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Messages {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.flightrecorder.configuration.internal.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static final String CommonConstraints_BEGINNING_OF_EVERY_CHUNK = "CommonConstraints_BEGINNING_OF_EVERY_CHUNK"; //$NON-NLS-1$
	public static final String CommonConstraints_END_OF_EVERY_CHUNK = "CommonConstraints_END_OF_EVERY_CHUNK"; //$NON-NLS-1$
	public static final String CommonConstraints_ONCE_EVERY_CHUNK = "CommonConstraints_ONCE_EVERY_CHUNK"; //$NON-NLS-1$
	public static final String OptionInfo_DISALLOWED_OPTION = "OptionInfo_DISALLOWED_OPTION"; //$NON-NLS-1$
	public static final String RecordingOption_DEFAULT_RECORDING_NAME = "RecordingOption_DEFAULT_RECORDING_NAME"; //$NON-NLS-1$
	public static final String SchemaVersion_JDK_7_OR_8 = "SchemaVersion_JDK_7_OR_8"; //$NON-NLS-1$
	public static final String SchemaVersion_JDK_9_AND_ABOVE = "SchemaVersion_JDK_9_AND_ABOVE"; //$NON-NLS-1$

	// Not the most logical place for this, but at least it has something to do with messages.
	static final Logger LOGGER = Logger.getLogger("org.openjdk.jmc.flightrecorder.configuration"); //$NON-NLS-1$

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			LOGGER.log(Level.WARNING, "Missing option translation!", e); //$NON-NLS-1$
			return '!' + key + '!';
		}
	}
}
