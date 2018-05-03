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
package org.openjdk.jmc.console.ui.mbeanbrowser.messages.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.console.ui.mbeanbrowser.messages.internal.messages"; //$NON-NLS-1$

	public static String ADD_MBEAN_DESCRIPTION;
	public static String ADD_MBEAN_LABEL;
	public static String ADD_MBEAN_LONG_DESCRIPTION;
	public static String ADD_MBEAN_TITLE;
	public static String INVALID_EMPTY_CLASS_NAME;
	public static String INVALID_MBEAN_NAME_MESSAGE;
	public static String MBEAN_CLASS_NAME_LABEL;
	public static String MBEAN_OBJECT_NAME_LABEL;
	public static String MBeanTreeSectionPart_MBEAN_TREE_FILTER_TEXT;
	public static String MBeanTreeSectionPart_MBEAN_TREE_TITLE_TEXT;
	public static String UNABLE_TO_CREATE_MBEAN_MESSAGE;
	public static String UNREGISTER_MBEAN_ACTION_DESCRIPTION;
	public static String UNREGISTER_MBEAN_ACTION_LABEL;
	public static String UNREGISTER_MBEAN_LABEL;
	public static String UNREGISTER_MBEAN_TITLE;
	public static String UNREGISTER_MBEAN_TOGGLE_LABEL;
	public static String UNREGISTER_SYSTEM_MBEAN_LABEL;
	public static String UNREGISTER_SYSTEM_MBEAN_TITLE;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
