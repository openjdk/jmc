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
package org.openjdk.jmc.common.version;

/**
 * Java versions that have support for various features.
 * <p>
 * Some of the constants are not used anymore, but keeping them at least for a while as pure
 * information.
 */
public class JavaVersionSupport {

	public static final JavaVersion JDK_6 = new JavaVersion(6, 0);
	public static final JavaVersion EARLIEST_JDK_SUPPORTED = JDK_6;
	public static final JavaVersion JDK_7 = new JavaVersion(7, 0);
	public static final JavaVersion JDK_7_U_4 = new JavaVersion(7, 0, 4);
	public static final JavaVersion JFR_ENGINE_SUPPORTED = JDK_7_U_4;
	// 1.7.0_12 was later renamed 14, and then released as 40
	public static final JavaVersion JDK_7_U_40 = new JavaVersion(7, 0, 12);
	public static final JavaVersion DIAGNOSTIC_COMMANDS_SUPPORTED = JDK_7_U_40;
	public static final JavaVersion JFR_FULLY_SUPPORTED = JDK_7_U_40;
	public static final JavaVersion JDK_8 = new JavaVersion(8, 0);
	public static final JavaVersion JDK_8_U_20 = new JavaVersion(8, 0, 20);
	public static final JavaVersion DUMP_ON_EXIT_WITHOUT_DEFAULTRECORDING_SUPPORTED = JDK_8_U_20;
	public static final JavaVersion STRING_DEDUPLICATION_SUPPORTED = JDK_8_U_20;
	public static final JavaVersion JDK_8_U_40 = new JavaVersion(8, 0, 40);
	public static final JavaVersion DYNAMIC_JFR_SUPPORTED = JDK_8_U_40;
	public static final JavaVersion JDK_9 = new JavaVersion(9);
	// FIXME: Update this if JDK-8036749 is ever backported to 8uX.
	public static final JavaVersion DEBUG_NON_SAFEPOINTS_IMPLICITLY_ENABLED = JDK_9;
	// FIXME: Update this if JDK-8054307 is ever backported to 8uX.
	public static final JavaVersion STRING_IS_BYTE_ARRAY = JDK_9;
	public static final JavaVersion JDK_11_EA = new JavaVersion(true, 11);
	public static final JavaVersion JFR_NOT_COMMERCIAL = JDK_11_EA;

}
