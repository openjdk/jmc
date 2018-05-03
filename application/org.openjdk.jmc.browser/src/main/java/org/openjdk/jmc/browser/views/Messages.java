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
package org.openjdk.jmc.browser.views;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.browser.views.messages"; //$NON-NLS-1$

	public static String JVMBrowserView_ACTION_DISCONNECT_HEADER;
	public static String JVMBrowserView_ACTION_DISCONNECT_TEXT;
	public static String JVMBrowserView_ACTION_DISCONNECT_TOOLTIP;
	public static String JVMBrowserView_ACTION_EDIT_TEXT;
	public static String JVMBrowserView_ACTION_NEW_CONNECTION_TEXT;
	public static String JVMBrowserView_ACTION_NEW_CONNECTION_TOOLTIP;
	public static String JVMBrowserView_ACTION_NEW_FOLDER_DISABLED_TOOLTIP;
	public static String JVMBrowserView_ACTION_NEW_FOLDER_TEXT;
	public static String JVMBrowserView_ACTION_NEW_FOLDER_TOOLTIP;
	public static String JVMBrowserView_ACTION_REMOVE_TEXT;
	public static String JVMBrowserView_ACTION_TREE_LAYOUT_TEXT;
	public static String JVMBrowserView_ACTION_TREE_LAYOUT_TOOLTIP;
	public static String JVMBrowserView_COMMAND_LINE;
	public static String JVMBrowserView_CONNECTION_STATE;
	public static String JVMBrowserView_CONNECTION_STATE_CONNECTED;
	public static String JVMBrowserView_CONNECTION_STATE_NOT_CONNECTED;
	public static String JVMBrowserView_CONNECTION_STATE_UNCONNECTABLE;
	public static String JVMBrowserView_DIALOG_MESSAGE_TITLE;
	public static String JVMBrowserView_DIALOG_NEW_FOLDER_DEFAULT_VALUE;
	public static String JVMBrowserView_DIALOG_NEW_FOLDER_ERROR_MESSAGE_VALIDATION;
	public static String JVMBrowserView_DIALOG_NEW_FOLDER_TEXT;
	public static String JVMBrowserView_DIALOG_NEW_FOLDER_TITLE;
	public static String JVMBrowserView_DIALOG_REMOVE_MULTIPLE_TEXT;
	public static String JVMBrowserView_DIALOG_REMOVE_TEXT;
	public static String JVMBrowserView_DIALOG_REMOVE_TITLE;
	public static String JVMBrowserView_DIALOG_TITLE_PROBLEM_CONNECT;
	public static String JVMBrowserView_FOLDER_NAME_TEXT;
	public static String JVMBrowserView_FOLDER_PROPERTIES_TITLE_TEXT;
	public static String JVMBrowserView_JAVA_VERSION;
	public static String JVMBrowserView_NO_LOCAL_JVMS_MESSAGE;
	public static String JVMBrowserView_NO_LOCAL_JVMS_TITLE;
	public static String JVMBrowserView_NO_LOCAL_JVMS_WARN_CAUSE;
	public static String JVMBrowserView_NO_LOCAL_JVMS_WARN_PREFERENCE;
	public static String JVMBrowserView_TOOLTIP_PID;
	public static String JVMBrowserView_UNKNOWN;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
