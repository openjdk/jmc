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
package org.openjdk.jmc.flightrecorder.rules.messages.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.flightrecorder.rules.messages.internal.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static final String ItemTreeToolkit_BREAKDOWN_HEADER_LAYERS = "ItemTreeToolkit_BREAKDOWN_HEADER_LAYERS"; //$NON-NLS-1$
	public static final String ItemTreeToolkit_BREAKDOWN_HEADER_MAX_DURATION_EVENT_CHAIN = "ItemTreeToolkit_BREAKDOWN_HEADER_MAX_DURATION_EVENT_CHAIN"; //$NON-NLS-1$
	public static final String ItemTreeToolkit_BREAKDOWN_LAYER_CAPTION = "ItemTreeToolkit_BREAKDOWN_LAYER_CAPTION"; //$NON-NLS-1$
	public static final String Result_SHORT_RECORDING = "Result_SHORT_RECORDING";; //$NON-NLS-1$
	public static final String RulesToolkit_ATTRIBUTE_NOT_FOUND = "RulesToolkit_ATTRIBUTE_NOT_FOUND"; //$NON-NLS-1$
	public static final String RulesToolkit_ATTRIBUTE_NOT_FOUND_LONG = "RulesToolkit_ATTRIBUTE_NOT_FOUND_LONG"; //$NON-NLS-1$
	public static final String RulesToolkit_EVERY_CHUNK = "RulesToolkit_EVERY_CHUNK"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_RECOMMENDS_EVENTS = "RulesToolkit_RULE_RECOMMENDS_EVENTS"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_EVENTS = "RulesToolkit_RULE_REQUIRES_EVENTS"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_EVENTS_LONG = "RulesToolkit_RULE_REQUIRES_EVENTS_LONG"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_EVENT_TYPE = "RulesToolkit_RULE_REQUIRES_EVENT_TYPE"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_EVENT_TYPE_LONG = "RulesToolkit_RULE_REQUIRES_EVENT_TYPE_LONG"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_EVENT_TYPE_NOT_AVAILABLE = "RulesToolkit_RULE_REQUIRES_EVENT_TYPE_NOT_AVAILABLE"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_EVENT_TYPE_NOT_AVAILABLE_LONG = "RulesToolkit_RULE_REQUIRES_EVENT_TYPE_NOT_AVAILABLE_LONG"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_SOME_EVENTS = "RulesToolkit_RULE_REQUIRES_SOME_EVENTS"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_UNAVAILABLE_EVENT_TYPE = "RulesToolkit_RULE_REQUIRES_UNAVAILABLE_EVENT_TYPE"; //$NON-NLS-1$
	public static final String RulesToolkit_RULE_REQUIRES_UNAVAILABLE_EVENT_TYPE_LONG = "RulesToolkit_RULE_REQUIRES_UNAVAILABLE_EVENT_TYPE_LONG"; //$NON-NLS-1$
	public static final String RulesToolkit_TOO_FEW_EVENTS = "RulesToolkit_TOO_FEW_EVENTS"; //$NON-NLS-1$
	public static final String Severity_INFORMATION = "Severity_INFORMATION"; //$NON-NLS-1$
	public static final String Severity_NOT_APPLICABLE = "Severity_NOT_APPLICABLE"; //$NON-NLS-1$
	public static final String Severity_OK = "Severity_OK"; //$NON-NLS-1$
	public static final String Severity_WARNING = "Severity_WARNING"; //$NON-NLS-1$

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
