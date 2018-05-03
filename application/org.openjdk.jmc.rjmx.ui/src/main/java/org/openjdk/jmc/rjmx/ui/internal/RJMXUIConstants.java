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
package org.openjdk.jmc.rjmx.ui.internal;

public interface RJMXUIConstants {
	public final static String PROPERTY_ASK_USER_BEFORE_MBEAN_UNREGISTER = "console.ui.mbeanbrowser.ask_before_mbean_deregister"; //$NON-NLS-1$
	public static final String PROPERTY_MBEAN_PROPERTY_KEY_ORDER = "console.ui.mbeanbrowser.propertyKeyList"; //$NON-NLS-1$
	public static final String PROPERTY_MBEAN_SUFFIX_PROPERTY_KEY_ORDER = "console.ui.mbeanbrowser.suffixPropertyKeyList"; //$NON-NLS-1$
	public static final String PROPERTY_MBEAN_PROPERTIES_IN_ALPHABETIC_ORDER = "console.ui.mbeanbrowser.propertiesInAlphabeticOrder"; //$NON-NLS-1$
	public static final String PROPERTY_MBEAN_CASE_INSENSITIVE_PROPERTY_ORDER = "console.ui.mbeanbrowser.caseInsensitivePropertyOrder"; //$NON-NLS-1$
	public static final String PROPERTY_MBEAN_SHOW_COMPRESSED_PATHS = "console.ui.mbeanbrowser.showCompressedPaths"; //$NON-NLS-1$
	public static final boolean DEFAULT_ASK_USER_BEFORE_MBEAN_UNREGISTER = true;
	public static final String DEFAULT_MBEAN_PROPERTY_KEY_ORDER = "type,j2eeType"; //$NON-NLS-1$
	public static final String DEFAULT_MBEAN_SUFFIX_PROPERTY_KEY_ORDER = "name"; //$NON-NLS-1$
	public static final boolean DEFAULT_MBEAN_PROPERTIES_IN_ALPHABETIC_ORDER = false;
	public static final boolean DEFAULT_MBEAN_CASE_INSENSITIVE_PROPERTY_ORDER = false;
	public static final boolean DEFAULT_MBEAN_SHOW_COMPRESSED_PATHS = false;
}
