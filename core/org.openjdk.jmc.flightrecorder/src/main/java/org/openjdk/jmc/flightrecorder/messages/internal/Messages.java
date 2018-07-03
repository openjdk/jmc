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
package org.openjdk.jmc.flightrecorder.messages.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.flightrecorder.messages.internal.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static final String ATTR_CENTER_TIME = "ATTR_CENTER_TIME"; //$NON-NLS-1$
	public static final String ATTR_DURATION = "ATTR_DURATION"; //$NON-NLS-1$
	public static final String ATTR_END_TIME = "ATTR_END_TIME"; //$NON-NLS-1$
	public static final String ATTR_EVENT_STACKTRACE = "ATTR_EVENT_STACKTRACE"; //$NON-NLS-1$
	public static final String ATTR_EVENT_THREAD = "ATTR_EVENT_THREAD"; //$NON-NLS-1$
	public static final String ATTR_EVENT_THREAD_DESC = "ATTR_EVENT_THREAD_DESC"; //$NON-NLS-1$
	public static final String ATTR_EVENT_TIMESTAMP = "ATTR_EVENT_TIMESTAMP"; //$NON-NLS-1$
	public static final String ATTR_EVENT_TIMESTAMP_DESC = "ATTR_EVENT_TIMESTAMP_DESC"; //$NON-NLS-1$
	public static final String ATTR_EVENT_TYPE = "ATTR_EVENT_TYPE"; //$NON-NLS-1$
	public static final String ATTR_EVENT_TYPE_DESC = "ATTR_EVENT_TYPE_DESC"; //$NON-NLS-1$
	public static final String ATTR_FLR_DATA_LOST = "ATTR_FLR_DATA_LOST"; //$NON-NLS-1$
	public static final String ATTR_FLR_DATA_LOST_DESC = "ATTR_FLR_DATA_LOST_DESC"; //$NON-NLS-1$
	public static final String ATTR_LIFETIME = "ATTR_LIFETIME"; //$NON-NLS-1$
	public static final String ATTR_START_TIME = "ATTR_START_TIME"; //$NON-NLS-1$
	public static final String EventParserManager_TYPE_BUFFER_LOST = "EventParserManager_TYPE_BUFFER_LOST"; //$NON-NLS-1$
	public static final String EventParserManager_TYPE_BUFFER_LOST_DESC = "EventParserManager_TYPE_BUFFER_LOST_DESC"; //$NON-NLS-1$
	public static final String JfrThread_UNKNOWN_THREAD_GROUP = "JfrThread_UNKNOWN_THREAD_GROUP"; //$NON-NLS-1$
	public static final String JfrThread_UNKNOWN_THREAD_NAME = "JfrThread_UNKNOWN_THREAD_NAME"; //$NON-NLS-1$
	public static final String TypeManager_EXPERIMENTAL_TYPE = "TypeManager_EXPERIMENTAL_TYPE"; //$NON-NLS-1$

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
