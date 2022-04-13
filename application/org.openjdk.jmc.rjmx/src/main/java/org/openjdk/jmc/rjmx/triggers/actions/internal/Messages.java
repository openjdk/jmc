/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.rjmx.triggers.actions.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.rjmx.triggers.actions.internal.messages"; //$NON-NLS-1$

	public static String TriggerActionDiagnosticCommand_APPEND_ACTION_TEXT;
	public static String TriggerActionDiagnosticCommand_WRITE_ACTION_TEXT;
	public static String TriggerActionLogToFile_JOBNAME;
	public static String TriggerActionMail_MAIL_HEADER_MAILER;
	public static String TriggerActionMail_SUBJECT_INVOKED;
	public static String TriggerActionMail_SUBJECT_RECOVERED;
	public static String TriggerActionMail_SUBJECT_TRIGGERED;
	public static String TriggerActionSystemOut_FOOTER;
	public static String TriggerActionSystemOut_HEADER;
	public static String TriggerActionTwitterSendUpdateStatus_ErrorMessage;
	public static String TriggerActionTwitterSendDirectMessage_ErrorMessage;
	public static String TriggerActionTwitterVerifyTweeter_ErrorMessage;
	public static String TriggerActionTwitterUnauthorizedUser_Title;
	public static String TriggerActionTwitterUnauthorizedUser_ErrorMessage;
	public static String TriggerActionTwitterInvalidUser_Title;
	public static String TriggerActionTwitterInvalidUser_ErrorMessage;
	public static String TriggerActionTwitterAuthorization_Exception;
	public static String TriggerActionTwitterInvalidToken_Exception;
	public static String TriggerActionTwitterAuthentication_Exception;
	public static String TriggerActionTwitterRequestToken_Exception;
	public static String TriggerActionTwitterURIParsing_Exception;
	public static String TriggerActionTwitterEncryption_Exception;
	public static String TriggerActionTwitterEncoding_Exception;
	public static String TriggerActionTwitterPreferenceStorage_Exception;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
