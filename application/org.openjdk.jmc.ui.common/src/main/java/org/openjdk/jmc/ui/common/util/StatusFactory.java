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
package org.openjdk.jmc.ui.common.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.openjdk.jmc.common.util.ExceptionToolkit;
import org.openjdk.jmc.ui.common.CorePlugin;

/**
 * Utility class to create status objects. Slightly influenced by the Eclipse internal StatusUtil.
 */
public class StatusFactory {
	/**
	 * Create an error IStatus suitable for showing in JobErrorDialog.
	 *
	 * @param message
	 *            the localized status message
	 * @param cause
	 *            an exception or null
	 * @param showStackTrace
	 *            expand stack trace into one child per stack frame
	 * @return
	 */
	public static IStatus createErr(String message, Throwable cause, boolean showStackTrace) {
		if (cause == null) {
			return createErr(message);
		}

		IStatus status;
		if (showStackTrace) {
			status = expandStackTrace(cause);
		} else {
			String subMsg = cause.getLocalizedMessage();
			// FIXME: If subMsg == null, return plain status instead.
			status = new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, 0,
					(subMsg != null) ? subMsg : cause.getClass().getName(), null);
		}
		return new MultiStatus(CorePlugin.PLUGIN_ID, 0, new IStatus[] {status}, message, null);
	}

	/**
	 * Create an error IStatus suitable for showing in JobErrorDialog.
	 *
	 * @param message
	 *            the localized status message
	 * @return
	 */
	public static IStatus createErr(String message) {
		return new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, 0, message, null);
	}

	/**
	 * Create an OK IStatus.
	 *
	 * @param message
	 *            the localized status message
	 * @return
	 */
	public static IStatus createOk(String message) {
		return new Status(IStatus.OK, CorePlugin.PLUGIN_ID, 0, message, null);
	}

	/**
	 * Create a MultiStatus with an IStatus child for each line in the stack trace for the given
	 * exception.
	 *
	 * @param e
	 *            a non-null exception
	 * @return
	 */
	private static IStatus expandStackTrace(Throwable e) {
		// Skip empty lines and trailing line terminators
		String[] lines = ExceptionToolkit.toString(e).split("[\r\n]+"); //$NON-NLS-1$
		IStatus[] statuses = new IStatus[lines.length];
		for (int i = 0; i < statuses.length; i++) {
			statuses[i] = new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, 0, lines[i], null);
		}
		return new MultiStatus(CorePlugin.PLUGIN_ID, 0, statuses,
				e.getClass().getName() + ": " + e.getLocalizedMessage(), null); //$NON-NLS-1$
	}
}
