/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.rjmx.common.messages.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.rjmx.common.messages.internal.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static final String ConnectionException_ATTACH_NOT_SUPPORTED = "ConnectionException_ATTACH_NOT_SUPPORTED"; //$NON-NLS-1$
	public static final String ConnectionException_COULD_NOT_CONNECT_MSG = "ConnectionException_COULD_NOT_CONNECT_MSG"; //$NON-NLS-1$
	public static final String ConnectionException_COULD_NOT_DETERMINE_IP_MSG = "ConnectionException_COULD_NOT_DETERMINE_IP_MSG"; //$NON-NLS-1$
	public static final String ConnectionException_MALFORMED_URL_MSG = "ConnectionException_MALFORMED_URL_MSG"; //$NON-NLS-1$
	public static final String ConnectionException_MSARMI_CHECK_PASSWORD = "ConnectionException_MSARMI_CHECK_PASSWORD"; //$NON-NLS-1$
	public static final String ConnectionException_NAME_NOT_FOUND_MSG = "ConnectionException_NAME_NOT_FOUND_MSG"; //$NON-NLS-1$
	public static final String ConnectionException_UNABLE_TO_CREATE_INITIAL_CONTEXT = "ConnectionException_UNABLE_TO_CREATE_INITIAL_CONTEXT"; //$NON-NLS-1$
	public static final String ConnectionException_UNABLE_TO_RESOLVE_CREDENTIALS = "ConnectionException_UNABLE_TO_RESOLVE_CREDENTIALS"; //$NON-NLS-1$
	public static final String ConnectionException_UNRESOLVED = "ConnectionException_UNRESOLVED"; //$NON-NLS-1$
	public static final String LABEL_NOT_AVAILABLE = "LABEL_NOT_AVAILABLE"; //$NON-NLS-1$
	public static final String MBeanOperationsWrapper_DESCRIPTOR = "MBeanOperationsWrapper_DESCRIPTOR"; //$NON-NLS-1$

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static String getString(String key, String def) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return def;
		}
	}

	public static boolean hasString(String key) {
		return RESOURCE_BUNDLE.containsKey(key);
	}
}
