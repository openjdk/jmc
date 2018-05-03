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
package org.openjdk.jmc.flightrecorder.ui;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.preferences.PreferenceKeys;

public class ResultPageBookView extends PageBookView {

	private static final String HELP_CONTEXT_ID = FlightRecorderUI.PLUGIN_ID + ".ResultView"; //$NON-NLS-1$
	private IPropertyChangeListener analysisEnabledPropertyListener;

	@Override
	protected IPage createDefaultPage(PageBook book) {
		MessagePage page = new MessagePage();
		initPage(page);
		page.createControl(book);
		analysisEnabledPropertyListener = e -> {
			if (e.getProperty().equals(PreferenceKeys.PROPERTY_ENABLE_RECORDING_ANALYSIS)) {
				setDefaultMessage(page, (Boolean) e.getNewValue());
			}
		};
		FlightRecorderUI.getDefault().getPreferenceStore().addPropertyChangeListener(analysisEnabledPropertyListener);
		setDefaultMessage(page, FlightRecorderUI.getDefault().isAnalysisEnabled());
		return page;
	}

	private void setDefaultMessage(MessagePage page, Boolean analysisEnabled) {
		page.setMessage(
				analysisEnabled ? Messages.RESULT_VIEW_NO_EDITOR_SELECTED : Messages.RESULT_VIEW_ANALYSIS_DISABLED);
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		if (isImportant(part)) {
			ResultPage p = ((JfrEditor) part).createResultPage();
			initPage(p);
			p.createControl(getPageBook());
			PlatformUI.getWorkbench().getHelpSystem().setHelp(p.getControl(), HELP_CONTEXT_ID);
			return new PageRec(part, p);
		}
		return new PageRec(part, getDefaultPage());
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		FlightRecorderUI.getDefault().getPreferenceStore()
				.removePropertyChangeListener(analysisEnabledPropertyListener);
		pageRecord.page.dispose();
		pageRecord.dispose();
	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		IWorkbenchPage page = getSite().getPage();
		if (page != null) {
			return page.getActiveEditor();
		}
		return null;
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return (part instanceof JfrEditor) && FlightRecorderUI.getDefault().isAnalysisEnabled();
	}

}
