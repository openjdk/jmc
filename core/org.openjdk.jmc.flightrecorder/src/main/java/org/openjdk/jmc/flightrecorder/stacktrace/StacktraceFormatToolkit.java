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
package org.openjdk.jmc.flightrecorder.stacktrace;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.messages.internal.Messages;

/**
 * Toolkit for presenting stack traces and stack frames in textual form.
 */
public class StacktraceFormatToolkit {

	/**
	 * Return a text representation of a frame with only the information that is relevant according
	 * to the frame categorization.
	 */
	public static String formatFrame(IMCFrame frame, FrameSeparator frameSeparator) {
		return formatFrame(frame, frameSeparator, true, true, true, true, true, true);
	}

	/**
	 * Return a text representation of a frame with only the information that is relevant according
	 * to the frame categorization. All the boolean parameters are passed on to
	 * {@link FormatToolkit#getHumanReadable(org.openjdk.jmc.common.IMCMethod, boolean, boolean, boolean, boolean, boolean, boolean)}.
	 */
	public static String formatFrame(
		IMCFrame frame, FrameSeparator frameSeparator, boolean showReturnValue, boolean showReturnValuePackage,
		boolean showClassName, boolean showClassPackageName, boolean showArguments, boolean showArgumentsPackage) {
		// TODO: Better presentation of UNKNOWN_FRAME
		if (frame == StacktraceModel.UNKNOWN_FRAME) {
			return Messages.getString(Messages.STACKTRACE_UNCLASSIFIABLE_FRAME);
		}
		String formatted;
		String detailsText = ""; //$NON-NLS-1$
		switch (frameSeparator.getCategorization()) {
		case PACKAGE:
			formatted = FormatToolkit.getPackage(frame.getMethod().getType().getPackage());
			break;
		case CLASS:
			formatted = FormatToolkit.getType(frame.getMethod().getType(), showClassPackageName);
			break;
		case BCI:
			detailsText = " [BCI: " + asString(frame.getBCI()) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			// Fall through
		case LINE:
			detailsText = ":" + asString(frame.getFrameLineNumber()) + detailsText; //$NON-NLS-1$
			// Fall through
		default:
			formatted = FormatToolkit.getHumanReadable(frame.getMethod(), showReturnValue, showReturnValuePackage,
					showClassName, showClassPackageName, showArguments, showArgumentsPackage) + detailsText;
		}

		if (frameSeparator.isDistinguishFramesByOptimization()) {
			formatted = formatted + " (" + frame.getType().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return formatted;
	}

	private static String asString(Object element) {
		return element == null ? "?" : element.toString(); //$NON-NLS-1$
	}

}
