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
package org.openjdk.jmc.browser.wizards;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.browser.wizards.messages"; //$NON-NLS-1$

	public static String ConnectionExportWizard_EXPORT_CONNECTIONS_TITLE;
	public static String ConnectionExportWizard_EXPORT_CONNECTIONS_WIZARD_PAGE_NAME;
	public static String ConnectionExportWizard_EXPORT_FILE_ERROR_TITLE;
	public static String ConnectionExportWizard_FILE_ERROR_EXPORT_TEXT;
	public static String ConnectionExportWizard_SELECTIONCONNECTIONS_TO_EXPORT;
	public static String ConnectionExportWizard_WIZARD_EXPORT_CONNECTION_TITLE;
	public static String ConnectionImportWizard_ERROR_IMPORTING_CONNECTIONS_TITLE;
	public static String ConnectionImportWizard_ERROR_IMPORTING_FROM_FILE_X_TEXT;
	public static String ConnectionImportWizard_IMPORTCONNECTIONS_TITLE;
	public static String ConnectionImportWizard_IMPORT_CONNECTION_WIZARD_NAME;
	public static String ConnectionImportWizard_SELECT_CONNECTION_FILE_FOR_IMPORT_TEXT;
	public static String ConnectionWizardPage_BUTTON_CUSTOM_JMX_SERVICE_URL_TEXT;
	public static String ConnectionWizardPage_CAPTION_JAVA_COMMAND;
	public static String ConnectionWizardPage_CAPTION_JVMARGS;
	public static String ConnectionWizardPage_CAPTION_PID;
	public static String ConnectionWizardPage_CONNECTION_NAME_CAPTION;
	public static String ConnectionWizardPage_CONNECTION_NAME_TOOLTIP;
	public static String ConnectionWizardPage_COULD_NOT_CONNECT_DISABLE_NEXT;
	public static String ConnectionWizardPage_ERROR_INVALID_PORT;
	public static String ConnectionWizardPage_ERROR_MESSAGE_NOT_VALID_SERVICE_URL;
	public static String ConnectionWizardPage_ERROR_MESSAGE_PORT_MUST_BE_INTEGER;
	public static String ConnectionWizardPage_HOST_CAPTION;
	public static String ConnectionWizardPage_HOST_TOOLTIP;
	public static String ConnectionWizardPage_INFO_MESSAGE_NAME_ALREADY_EXIST;
	public static String ConnectionWizardPage_INFO_MESSAGE_SAME_HOST_PORT;
	public static String ConnectionWizardPage_INFO_MESSAGE_SAME_URL;
	public static String ConnectionWizardPage_MESSAGE_CONNECTED;
	public static String ConnectionWizardPage_MESSAGE_ENTER_CONNECTION_DETAILS;
	public static String ConnectionWizardPage_MESSAGE_LOCAL_CONNECTION;
	public static String ConnectionWizardPage_PAGE_NAME;
	public static String ConnectionWizardPage_PAGE_TITLE;
	public static String ConnectionWizardPage_PASSWORD_CAPTION;
	public static String ConnectionWizardPage_PASSWORD_TOOLTIP;
	public static String ConnectionWizardPage_PORT_CAPTION;
	public static String ConnectionWizardPage_PORT_TOOLTIP;
	public static String ConnectionWizardPage_SERVICE_URL_CAPTION;
	public static String ConnectionWizardPage_SERVICE_URL_TOOLTIP;
	public static String ConnectionWizardPage_STATUS_CAPTION;
	public static String ConnectionWizardPage_STATUS_IS_CONNECTED;
	public static String ConnectionWizardPage_STATUS_IS_NOT_CONNECTED;
	public static String ConnectionWizardPage_STATUS_IS_UNTESTED;
	public static String ConnectionWizardPage_STATUS_TOOLTIP;
	public static String ConnectionWizardPage_STORE_CAPTION;
	public static String ConnectionWizardPage_TESTING_CONNECTION_CAPTION;
	public static String ConnectionWizardPage_TEST_CONNECTION;
	public static String ConnectionWizardPage_TEXT_UNKNOWN;
	public static String ConnectionWizardPage_TOOLTIP_DISCOVERED_INFO;
	public static String ConnectionWizardPage_USER_CAPTION;
	public static String ConnectionWizardPage_USER_TOOLTIP;
	public static String ConnectionWizard_EXCEPTION_COULD_NOT_STORE_CONNECTION;
	public static String ConnectionWizard_TITLE_CONNECT;
	public static String ConnectionWizard_TITLE_CONNECTION_PROPERTIES;
	public static String ConnectionWizard_TITLE_NEW_CONNECTION;
	public static String ServerConnectWizardPage_SERVER_COMPONENT_ERROR;
	public static String ServerConnectWizardPage_SERVER_SELECT_DESCRIPTION;
	public static String ServerConnectWizardPage_TOOL_SELECT_DESCRIPTION;
	public static String ServerSelectionWizardPage_NEW_CONNECTION;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
