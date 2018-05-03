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
package org.openjdk.jmc.common.test.mock.item;

import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;

@SuppressWarnings("nls")
public class MockAttributes {
	public static final String DOUBLE_VALUE_ID = "mock/doubletype"; //$NON-NLS-1$
	public static final String LONG_INDEX_ID = "mock/index"; //$NON-NLS-1$
	/*
	 * NOTE: Need to be the same as the actual attribute used in Flight Recorder. Maybe use constant
	 * from somewhere else? If stacktrace mocking is moved to flightrecorder.test then we could
	 * perhaps use accessor from there.
	 */
	public static final String STACKTRACE_ID = "stackTrace"; //$NON-NLS-1$

	public static final IAttribute<IQuantity> DOUBLE_VALUE = Attribute.attr(DOUBLE_VALUE_ID, "A double value",
			UnitLookup.NUMBER);
	public static final IAttribute<IQuantity> INDEX_VALUE = Attribute.attr(LONG_INDEX_ID, "The index for an item",
			UnitLookup.NUMBER);
	public static final IAttribute<IMCStackTrace> STACKTRACE_VALUE = Attribute.attr(STACKTRACE_ID, "A stack trace",
			UnitLookup.STACKTRACE);
}
