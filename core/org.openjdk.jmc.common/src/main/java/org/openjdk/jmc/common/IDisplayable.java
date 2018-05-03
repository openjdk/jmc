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
package org.openjdk.jmc.common;

import org.openjdk.jmc.common.unit.IQuantity;

/**
 * Interface for objects that can be formatted for human display purposes, using a formatting hint.
 */
// FIXME: Replace String format hint constants with semi-enum.
public interface IDisplayable {
	/**
	 * Identifier for formatters suitable when many items are typically shown, such as tables,
	 * overviews, etc. Relatively compact, but not overly so. Using reasonable precision for
	 * at-a-glance display. Such a formatter is expected to exist for all {@link IDisplayable}
	 * implementations.
	 */
	static final String AUTO = "auto"; //$NON-NLS-1$

	/**
	 * Identifier for formatters which shows the value with maximum known precision, but still only
	 * as one value. For quantities, this will often be similar to
	 * {@link IQuantity#interactiveFormat()}, but tweaked for display, such as using non-breaking
	 * space between number and unit, and in applicable locales, between number groups. Also, not
	 * always parsable constructs like custom units may be used.
	 */
	static final String EXACT = "exact"; //$NON-NLS-1$

	/**
	 * Identifier for formatters suitable for tool tips. For quantities, in addition to showing the
	 * exact value, like {@link #EXACT}, the value may additionally be displayed in different units.
	 */
	static final String VERBOSE = "verbose"; //$NON-NLS-1$

	/**
	 * Format this object for display purposes, preferably using the formatter hinted by
	 * {@code formatHint}. If no such formatter is defined for this type, the default formatter will
	 * be used.
	 *
	 * @param formatHint
	 *            the format hint
	 * @return a formatted string according to the hinted (or default) formatter
	 */
	String displayUsing(String formatHint);
}
