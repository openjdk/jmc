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
package org.openjdk.jmc.ide.jdt;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.openjdk.jmc.ui.common.util.StatusFactory;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Job for looking for a Java source code element.
 */
public abstract class JumpToSourceJob extends Job {
	protected final String m_className;
	protected final String m_nestedTypes;
	protected final Integer m_lineNumber;

	public JumpToSourceJob(String className, String nestedTypes, Integer lineNumber) {
		super(Messages.JumpToSourceJob_LOOKING_FOR_JAVA_SOURCE_CODE_JOB_TITLE);
		m_className = className;
		m_nestedTypes = nestedTypes;
		m_lineNumber = lineNumber;
	}

	private void selectSourceLine(IEditorPart editorPart) {
		if (editorPart instanceof ITextEditor && m_lineNumber != null && m_lineNumber.intValue() > 0) {
			IEditorInput editorInput = null;
			IDocumentProvider provider = null;
			try {
				ITextEditor textEditor = (ITextEditor) editorPart;
				provider = textEditor.getDocumentProvider();
				editorInput = textEditor.getEditorInput();
				provider.connect(editorInput);
				IDocument document = provider.getDocument(editorInput);
				IRegion line = document.getLineInformation(m_lineNumber.intValue() - 1);
				textEditor.selectAndReveal(line.getOffset(), line.getLength());
			} catch (CoreException e1) {
				// It's ok to ignore this. The start line of the method will be selected instead.
			} catch (BadLocationException e) {
				// It's ok to ignore this. The start line of the method will be selected instead.
			} finally {
				if (editorInput != null && provider != null) {
					provider.disconnect(editorInput);
				}
			}
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			monitor.beginTask(Messages.JumpToSourceJob_TASK_LOOKING_FOR_SOURCE, IProgressMonitor.UNKNOWN);
			SubMonitor subMonitor = SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN);
			Map<IType, IMember> typeMap = createTypeToJavaElementMap(subMonitor);
			if (typeMap.isEmpty()) {
				DialogToolkit.showWarningDialogAsync(Display.getDefault(),
						Messages.JumpToSourceJob_SOURCE_CODE_NOT_FOUND_TITLE,
						NLS.bind(Messages.JumpToSourceJob_SOURCE_CODE_NOT_FOUND_TEXT, m_className));
			} else if (typeMap.size() == 1) {
				DisplayToolkit.safeAsyncExec(() -> jumpToElement(typeMap.values().iterator().next()));
			} else {
				IJavaSearchScope scope = SearchEngine
						.createJavaSearchScope(typeMap.keySet().stream().toArray(IType[]::new));
				DisplayToolkit.safeAsyncExec(() -> jumpToElement(scope, typeMap));
			}
			return Status.OK_STATUS;
		} catch (Exception e) {
			return StatusFactory.createErr(
					Messages.JumpToSourceJob_ERROR_WHEN_LOOKING_FOR_JAVA_SOURCE_STATUS_DESCRIPTION, e, false);
		}
	}

	private void jumpToElement(IJavaSearchScope scope, Map<IType, IMember> typeMap) {
		Shell shell = Display.getCurrent().getActiveShell();
		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell), scope,
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false, ""); //$NON-NLS-1$
			dialog.setTitle(getTitle());
			dialog.setMessage(Messages.OpenTypeRunnable_DIALOG_MESSAGE_MATCHING_ITEMS);
			if (dialog.open() == IDialogConstants.OK_ID) {
				Object[] results = dialog.getResult();
				if (results.length > 0 && results[0] instanceof IType) {
					jumpToElement(typeMap.get((results[0])));
				}
			}
		} catch (Exception e) {
			DialogToolkit.showError(shell, Messages.JumpToSourceJob_FAILED_TO_CHOOSE_TYPE, e.getLocalizedMessage());
		}
	}

	private void jumpToElement(IMember element) {
		try {
			selectSourceLine(JavaUI.openInEditor(element, true, OpenStrategy.activateOnOpen()));
		} catch (Exception e) {
			DialogToolkit.showError(Display.getCurrent().getActiveShell(),
					Messages.JumpToSourceJob_FAILED_TO_OPEN_ELEMENT, e.getLocalizedMessage());
		}
	}

	protected abstract Map<IType, IMember> createTypeToJavaElementMap(IProgressMonitor monitor) throws CoreException;

	protected abstract String getTitle();
}
