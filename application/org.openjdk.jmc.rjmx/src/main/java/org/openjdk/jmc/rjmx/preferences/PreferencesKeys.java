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
package org.openjdk.jmc.rjmx.preferences;

import java.io.File;

import org.openjdk.jmc.ui.common.CorePlugin;

/**
 * Preference keys and default values
 */
public class PreferencesKeys {
	/** Update interval for attribute subscriptions */
	public static final String PROPERTY_UPDATE_INTERVAL = "org.openjdk.jmc.console.preferences.updateinterval"; //$NON-NLS-1$
	public static final int DEFAULT_UPDATE_INTERVAL = 1000;

	/** Default value of retained event values for subscriptions */
	public static final String PROPERTY_RETAINED_EVENT_VALUES = "rjmx.events.retained"; //$NON-NLS-1$
	public static final int DEFAULT_RETAINED_EVENT_VALUES = 7 * 24 * 3600;

	public static final String PROPERTY_MAIL_SERVER = "rjmx.smtp.server"; //$NON-NLS-1$
	public static final String PROPERTY_MAIL_SERVER_PORT = "rjmx.smtp.server.port"; //$NON-NLS-1$
	public static final String PROPERTY_MAIL_SERVER_SECURE = "rjmx.smtp.server.secure"; //$NON-NLS-1$
	public static final String PROPERTY_MAIL_SERVER_CREDENTIALS = "rjmx.smtp.server.credentials"; //$NON-NLS-1$
	public static final String DEFAULT_MAIL_SERVER = "mail.example.org"; //$NON-NLS-1$
	public static final int DEFAULT_MAIL_SERVER_PORT = 25;
	public static final boolean DEFAULT_MAIL_SERVER_SECURE = false;
	public static final String DEFAULT_MAIL_SERVER_USER = ""; //$NON-NLS-1$
	public static final String DEFAULT_MAIL_SERVER_PASSWORD = ""; //$NON-NLS-1$
	public static final String DEFAULT_MAIL_SERVER_CREDENTIALS = ""; //$NON-NLS-1$

	// Persistence
	public static final String PROPERTY_PERSISTENCE_LOG_ROTATION_LIMIT_KB = "rjmx.services.persistence.log.rotation.limit"; //$NON-NLS-1$
	public static final long DEFAULT_PERSISTENCE_LOG_ROTATION_LIMIT_KB = 100;
	public static final String PROPERTY_PERSISTENCE_DIRECTORY = "rjmx.services.persistence.directory"; //$NON-NLS-1$
	public static final String DEFAULT_PERSISTENCE_DIRECTORY = CorePlugin.getDefault().getWorkspaceDirectory().getPath()
			+ File.separator + "persisted_jmx_data" + File.separator; //$NON-NLS-1$

	public static final String PROPERTY_LIST_AGGREGATE_SIZE = "rjmx.internal.listAggregateSize"; //$NON-NLS-1$
	public static final int DEFAULT_LIST_AGGREGATE_SIZE = 40;
}
