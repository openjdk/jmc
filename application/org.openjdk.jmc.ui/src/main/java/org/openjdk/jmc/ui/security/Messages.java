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
package org.openjdk.jmc.ui.security;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.ui.security.messages"; //$NON-NLS-1$

	public static String MASTER_PASSWORD_WIZARD_PAGE;
	public static String MasterPasswordWizardPage_CAPTION_CONFIRM_PASSWORD;
	public static String MasterPasswordWizardPage_CAPTION_ENTER_PASSWORD;
	public static String MasterPasswordWizardPage_CAPTION_NEW_PASSWORD;
	public static String MasterPasswordWizardPage_ERROR_MESSAGE_PASSWORDS_DO_NOT_MATCH_TEXT;
	public static String MasterPasswordWizardPage_ERROR_MESSAGE_PASSWORD_SHORTER_THAN_X_CHARACTERS_TEXT;
	public static String MasterPasswordWizardPage_ERROR_PASSWORD_EMPTY_TEXT;
	public static String MasterPasswordWizardPage_SET_MASTER_PASSWORD_DESCRIPTION_TEXT;
	public static String MasterPasswordWizardPage_SET_MASTER_PASSWORD_TITLE;
	public static String MasterPasswordWizardPage_TOOLTIP_CONFIRM_PASSWORD;
	public static String MasterPasswordWizardPage_TOOLTIP_ENTER_PASSWORD;
	public static String MasterPasswordWizardPage_VERIFY_MASTER_PASSWORD_DESCRIPTION_TEXT;
	public static String MasterPasswordWizardPage_VERIFY_MASTER_PASSWORD_TITLE;
	public static String MasterPasswordWizardPage_WARN_DATA_CLEAR_TEXT;
	public static String SecurityDialogs_CIPHER_NOT_AVAILABLE_TEXT;
	public static String SecurityDialogs_CIPHER_NOT_AVAILABLE_TITLE;
	public static String SecurityDialogs_FAILED_TO_SAVE;
	public static String SecurityDialogs_NO_CIPHER_AVAILABLE_TEXT;
	public static String SecurityDialogs_SECURE_STORE_WILL_NOT_BE_ABLE_TO_SAVE;
	public static String SecurityDialogs_UNABLE_TO_CONTINUE;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
