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
package org.openjdk.jmc.console.ui.tabs.threads;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;

public class StackTraceLabelProvider extends ColumnLabelProvider {
	private static final String MESSAGE_PART_NON_NATIVE_METHOD = ""; //$NON-NLS-1$
	private static final String MESSAGE_PART_UNKNOWN_THREAD_ID = "?"; //$NON-NLS-1$
	private static final String UNKNOWN_COMPOSITE_SUPPORT_CLASS = ""; //$NON-NLS-1$
	private static final String MESSAGE_PART_UNKNOWN_THREAD_STATE = "?"; //$NON-NLS-1$
	// # 0 = Thread name
	// # 1 = Thread ID
	// # 2 = Thread state
	private static final String THREAD_FORMAT_STRING = "{0} [{1}] ({2})"; //$NON-NLS-1$

	@Override
	public String getText(Object element) {
		if (element instanceof IMCFrame) {
			return formatStackTraceElement((IMCFrame) element);
		}
		if (element instanceof ThreadInfoCompositeSupport) {
			ThreadInfoCompositeSupport tip = (ThreadInfoCompositeSupport) element;
			String[] messages = new String[3];
			messages[0] = tip.getThreadName();
			messages[1] = String.valueOf(tip.getThreadId());
			messages[2] = String.valueOf(tip.getThreadState());

			if (messages[0] == null) {
				messages[0] = Messages.StackTraceLabelProvider_MESSAGE_PART_NAME_UNKNOWN_THREAD_NAME;
			}
			if (messages[1] == null) {
				messages[1] = MESSAGE_PART_UNKNOWN_THREAD_ID;
			}
			if (messages[2] == null) {
				messages[2] = MESSAGE_PART_UNKNOWN_THREAD_STATE;
			}
			return NLS.bind(THREAD_FORMAT_STRING, messages);
		}

		return UNKNOWN_COMPOSITE_SUPPORT_CLASS;
	}

	private static String formatStackTraceElement(IMCFrame element) {
		String[] messages = new String[4];
		messages[0] = element.getMethod().getType().getFullName();
		messages[1] = element.getMethod().getMethodName();

		Integer lineNumber = element.getFrameLineNumber();
		if (lineNumber != null && lineNumber >= 0) {
			messages[2] = String.valueOf(lineNumber);
		} else {
			messages[2] = Messages.StackTraceLabelProvider_MESSAGE_PART_LINE_NUMBER_NOT_AVAILABLE;
		}
		Boolean nativeMethod = element.getMethod().isNative();
		if (Boolean.TRUE.equals(nativeMethod)) {
			messages[3] = Messages.StackTraceLabelProvider_MESSAGE_PART_NATIVE_METHOD;
		} else {
			messages[3] = MESSAGE_PART_NON_NATIVE_METHOD;
		}

		return NLS.bind(Messages.StackTraceLabelProvider_STACK_TRACE_FORMAT_STRING, messages);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof IMCFrame) {
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_STACKTRACE_ELEMENT);
		} else if (element instanceof ThreadInfoCompositeSupport) {
			return getThreadImage((ThreadInfoCompositeSupport) element);
		}
		return null;
	};

	static Image getThreadImage(ThreadInfoCompositeSupport th) {
		if (th.isDeadlocked()) {
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_THREAD_DEADLOCKED);
		}
		switch (th.getThreadState()) {
		case BLOCKED:
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_THREAD_BLOCKED);
		case WAITING:
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_THREAD_WAITING);
		case TIMED_WAITING:
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_THREAD_TIMEWAITING);
		case SUSPENDED:
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_THREAD_SUSPENDED);
		case TERMINATED:
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_THREAD_TERMINATED);
		default:
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_THREAD_RUNNING);
		}
	}

}
