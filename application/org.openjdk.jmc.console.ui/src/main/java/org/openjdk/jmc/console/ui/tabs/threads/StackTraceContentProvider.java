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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.openjdk.jmc.console.ui.ConsolePlugin;
import org.openjdk.jmc.console.ui.messages.internal.Messages;

public class StackTraceContentProvider implements ITreeContentProvider {

	final private IThreadsModel m_threadsModel;

	public StackTraceContentProvider(IThreadsModel threadsModel) {
		m_threadsModel = threadsModel;
	}

	@Override
	public void dispose() {
		// ignore
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// ignore
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IStructuredSelection) {
			long[] threadIDArray = extractThreadIDsFromSelection((IStructuredSelection) inputElement);
			try {
				return m_threadsModel.getThreadInfo(threadIDArray, Integer.valueOf(Integer.MAX_VALUE));
			} catch (ThreadModelException e) {
				ConsolePlugin.getDefault().getLogger().log(Level.WARNING,
						Messages.ThreadsModel_EXCEPTION_NO_THREAD_INFO_MESSAGE, e);
			}
		}
		return new Object[0];
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ThreadInfoCompositeSupport) {
			return ((ThreadInfoCompositeSupport) parentElement).getStackTrace();
		}

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public static long[] extractThreadIDsFromSelection(IStructuredSelection selection) {
		ArrayList<Long> threadIDs = new ArrayList<>();
		@SuppressWarnings("rawtypes")
		Iterator iterator = selection.iterator();

		while (iterator.hasNext()) {
			ThreadInfoCompositeSupport tics = (ThreadInfoCompositeSupport) iterator.next();
			Long id = tics.getThreadId();
			if (id != null) {
				threadIDs.add(id);
			}
		}
		long[] threadIDArray = new long[threadIDs.size()];
		for (int n = 0; n < threadIDArray.length; n++) {
			threadIDArray[n] = threadIDs.get(n).longValue();
		}

		return threadIDArray;
	}
}
