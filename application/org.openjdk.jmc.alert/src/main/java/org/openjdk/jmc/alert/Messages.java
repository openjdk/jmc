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
package org.openjdk.jmc.alert;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.alert.messages"; //$NON-NLS-1$

	public static String AlertDialog_CLEAR_ALERTS_TEXT0;
	public static String AlertDialog_COLUMN_HEADER_DATE;
	public static String AlertDialog_COLUMN_HEADER_RULE;
	public static String AlertDialog_COLUMN_HEADER_SOURCE;
	public static String AlertDialog_DIALOG_MESSAGE;
	public static String AlertDialog_DIALOG_TITLE;
	public static String AlertDialog_POP_UP_ON_ALERTS_TEXT;
	public static String AlertPlugin_MESSAGE_EXCEPTION_INVOKING_ACTION;
	public static String AlertPlugin_MESSAGE_EXCEPTION_INVOKING_ACTION_MESSAGE_CAPTION;
	public static String AlertPlugin_MESSAGE_EXCEPTION_INVOKING_ACTION_MESSAGE_MORE_INFORMATION;
	public static String AlertPlugin_RULE_X_Y_TEXT;
	public static String AlertPlugin_SOURCE_X_TEXT;
	public static String AlertPlugin_TIME_X_Y_TEXT;
	public static String AlertPlugin_TRIGGER_ALERT_TEXT;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_ACTUAL_TRIGGER_VALUE;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_CREATION_TIME;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_RECOVERED;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_RULE;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_SOURCE;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_NOTIFICATION_TRIGGERED;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_RULE_TRIGGER_CONDITION;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_TRIGGER_CONDITION_OPTIONAL_SUSTAIN;
	public static String NotificationUIToolkit_EVENT_TOOLKIT_TYPE_DESCRIPTION;
	public static String TriggerActionThreadStackDump_JOB_TITLE_WRITING_STACK_DUMP;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
