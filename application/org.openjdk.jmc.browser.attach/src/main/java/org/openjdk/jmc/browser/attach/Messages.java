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
package org.openjdk.jmc.browser.attach;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.browser.attach.messages"; //$NON-NLS-1$

	public static String LocalConnectionDescriptor_ERROR_AUTO_START_SWITCHED_OFF;
	public static String LocalConnectionDescriptor_ERROR_MESSAGE_ATTACH_NOT_SUPPORTED;
	public static String LocalDescriptorProvider_PROVIDER_DESCRIPTION;
	public static String LocalDescriptorProvider_PROVIDER_NAME;
	public static String RemoteJMXStarterAction_CONTROL_REMOTE_JMX_AGENT;
	public static String RemoteJMXStarterAction_CONTROL_REMOTE_JMX_AGENT_DESCRIPTION;
	public static String RemoteJMXStarterAction_REMOTE_JMX_AGENT_PROBLEM_DESCRIPTION;
	public static String RemoteJMXStarterAction_REMOTE_JMX_AGENT_PROBLEM_TITLE;
	public static String RemoteJMXStarterAction_REMOTE_JMX_AGENT_RESULT_DESCRIPTION;
	public static String RemoteJMXStarterAction_REMOTE_JMX_AGENT_RESULT_TITLE;
	public static String RemoteJMXStarterAction_START_REMOTE_JMX_AGENT_TITLE;
	public static String RemoteJMXStarterAction_START_REMOTE_JMX_AGENT_TITLE_START;
	public static String RemoteJMXStarterAction_START_REMOTE_JMX_AGENT_TITLE_STOP;
	public static String RemoteJMXStarterWizardPage_AGENT_STATUS;
	public static String RemoteJMXStarterWizardPage_AGENT_STATUS_DISABLED;
	public static String RemoteJMXStarterWizardPage_AGENT_STATUS_ENABLED;
	public static String RemoteJMXStarterWizardPage_AGENT_STATUS_UNKNOWN;
	public static String RemoteJMXStarterWizardPage_AUTHENTICATE_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_AUTHENTICATE_LABEL;
	public static String RemoteJMXStarterWizardPage_AUTODISCOVERY_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_AUTODISCOVERY_LABEL;
	public static String RemoteJMXStarterWizardPage_CANNOT_PARSE_INT;
	public static String RemoteJMXStarterWizardPage_COMMAND;
	public static String RemoteJMXStarterWizardPage_CURRENT_SETTINGS;
	public static String RemoteJMXStarterWizardPage_INTERVAL_ERROR;
	public static String RemoteJMXStarterWizardPage_INVALID_JDP_NAME;
	public static String RemoteJMXStarterWizardPage_JDP_ADDRESS_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_JDP_ADDRESS_LABEL;
	public static String RemoteJMXStarterWizardPage_JDP_NAME_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_JDP_NAME_LABEL;
	public static String RemoteJMXStarterWizardPage_JDP_PAUSE_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_JDP_PAUSE_LABEL;
	public static String RemoteJMXStarterWizardPage_JDP_PORT_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_JDP_PORT_LABEL;
	public static String RemoteJMXStarterWizardPage_JDP_SOURCE_ADDRESS_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_JDP_SOURCE_ADDRESS_LABEL;
	public static String RemoteJMXStarterWizardPage_JDP_TTL_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_JDP_TTL_LABEL;
	public static String RemoteJMXStarterWizardPage_NEW_SETTINGS;
	public static String RemoteJMXStarterWizardPage_PORT_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_PORT_LABEL;
	public static String RemoteJMXStarterWizardPage_REGISTRY_SSL_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_REGISTRY_SSL_LABEL;
	public static String RemoteJMXStarterWizardPage_RMI_PORT_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_RMI_PORT_LABEL;
	public static String RemoteJMXStarterWizardPage_SSL_DESCRIPTION;
	public static String RemoteJMXStarterWizardPage_SSL_LABEL;
	public static String RemoteJMXStarterWizardPage_TOO_SMALL_ERROR;
	public static String START_REMOTE_JMX_AGENT;
	public static String START_REMOTE_JMX_AGENT_DESCRIPTION;
	public static String STOP_REMOTE_JMX_AGENT;
	public static String STOP_REMOTE_JMX_AGENT_DESCRIPTION;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
