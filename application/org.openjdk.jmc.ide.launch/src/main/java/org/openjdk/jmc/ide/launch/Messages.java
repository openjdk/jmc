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
package org.openjdk.jmc.ide.launch;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.ide.launch.messages"; //$NON-NLS-1$

	public static String JfrLaunch_AUTO_OPEN;
	public static String JfrLaunch_AUTO_OPEN_TOOLTIP;
	public static String JfrLaunch_CONFIG_EXCEPTION;
	public static String JfrLaunch_ENABLE_JFR;
	public static String JfrLaunch_ENABLE_JFR_TOOLTIP;
	public static String JfrLaunch_JFR;
	public static String JfrLaunch_JFR_FILE_DID_NOT_EXIST;
	public static String JfrLaunch_JFR_LAUNCH_PROBLEM_TITLE;
	public static String JfrLaunch_JFR_OPTIONS_PROBLEM;
	public static String JfrLaunch_RECORDING_WAITER_INTERRUPTED;
	public static String JfrLaunch_UNKNOWN_JRE_NAME;
	public static String JfrLaunch_UNKNOWN_JRE_VERSION;
	public static String JfrLaunch_ORACLE_JDK_LESS_THAN_11;
	public static String JfrLaunch_ORACLE_JDK_CECHKBOX_MESSAGE;
	public static String VOLATILE_CONFIGURATION_IN_JRE;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
