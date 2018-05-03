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
package org.openjdk.jmc.rjmx.messages.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.rjmx.messages.internal.messages"; //$NON-NLS-1$

	public static String ConnectionException_ATTACH_NOT_SUPPORTED;
	public static String ConnectionException_COULD_NOT_CONNECT_MSG;
	public static String ConnectionException_COULD_NOT_DETERMINE_IP_MSG;
	public static String ConnectionException_MALFORMED_URL_MSG;
	public static String ConnectionException_MSARMI_CHECK_PASSWORD;
	public static String ConnectionException_NAME_NOT_FOUND_MSG;
	public static String ConnectionException_UNABLE_TO_CREATE_INITIAL_CONTEXT;
	public static String ConnectionException_UNABLE_TO_RESOLVE_CREDENTIALS;
	public static String ConnectionException_UNRESOLVED;
	public static String JVMSupport_FLIGHT_RECORDER_DISABLED;
	public static String JVMSupport_FLIGHT_RECORDER_DISABLED_SHORT;
	public static String JVMSupport_FLIGHT_RECORDER_NOT_ENABLED;
	public static String JVMSupport_FLIGHT_RECORDER_NOT_ENABLED_SHORT;
	public static String JVMSupport_FLIGHT_RECORDER_NOT_FULLY_SUPPORTED_OLD_HOTSPOT;
	public static String JVMSupport_FLIGHT_RECORDER_NOT_FULLY_SUPPORTED_OLD_HOTSPOT_SHORT;
	public static String JVMSupport_FLIGHT_RECORDER_NOT_SUPPORTED_NOT_HOTSPOT;
	public static String JVMSupport_FLIGHT_RECORDER_NOT_SUPPORTED_NOT_HOTSPOT_SHORT;
	public static String JVMSupport_FLIGHT_RECORDER_NOT_SUPPORTED_OLD_HOTSPOT;
	public static String JVMSupport_FLIGHT_RECORDER_NOT_SUPPORTED_OLD_HOTSPOT_SHORT;
	public static String JVMSupport_JROCKIT_NOT_LONGER_SUPPORTED;
	public static String JVMSupport_JROCKIT_NOT_LONGER_SUPPORTED_SHORT;
	public static String JVMSupport_MESSAGE_JROCKIT_NOT_SUPPORTED;
	public static String JVMSupport_MESSAGE_NOT_A_KNOWN_JVM;
	public static String JVMSupport_MESSAGE_TOO_OLD_JVM_CONSOLE;
	public static String JVMSupport_TITLE_JROCKIT_NOT_SUPPORTED;
	public static String JVMSupport_TITLE_NOT_A_KNOWN_JVM;
	public static String JVMSupport_TITLE_TOO_OLD_JVM_CONSOLE;
	public static String LABEL_NOT_AVAILABLE;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
