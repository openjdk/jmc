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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.Page;

import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.overview.ResultReportUi;
import org.openjdk.jmc.flightrecorder.ui.preferences.PreferenceKeys;

public class ResultPage extends Page {

	private Browser browser;
	private ResultReportUi report;
	private Label errorMessage;
	private IPageContainer editor;
	private SashForm container;
	private IPropertyChangeListener analysisEnabledListener;
	private Set<String> topics;

	@Override
	public void createControl(Composite parent) {
		container = new SashForm(parent, SWT.HORIZONTAL);
		errorMessage = new Label(container, SWT.NONE);
		try {
			browser = new Browser(container, SWT.NONE);
			container.setMaximizedControl(browser);
			report = new ResultReportUi(true);
			report.setShowOk(true);
			if (editor != null) {
				DataPageDescriptor currentPage = ((JfrEditor) editor).getCurrentPage();
				if (currentPage != null) {
					setTopics(Arrays.asList(currentPage.getTopics()));
				}
			}
		} catch (SWTException | SWTError e) {
			browser = null;
			container.setMaximizedControl(errorMessage);
			errorMessage.setText(e.getMessage());
		}
		analysisEnabledListener = e -> {
			if (e.getProperty().equals(PreferenceKeys.PROPERTY_ENABLE_RECORDING_ANALYSIS)) {
				Boolean isEnabled = (Boolean) e.getNewValue();
				container.setMaximizedControl(isEnabled ? browser : errorMessage);
				errorMessage.setText(
						isEnabled ? Messages.RESULT_VIEW_NO_EDITOR_SELECTED : Messages.RESULT_VIEW_ANALYSIS_DISABLED);
			}
		};
		FlightRecorderUI.getDefault().getPreferenceStore().addPropertyChangeListener(analysisEnabledListener);
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public void dispose() {
		FlightRecorderUI.getDefault().getPreferenceStore().removePropertyChangeListener(analysisEnabledListener);
		super.dispose();
	}

	@Override
	public void setFocus() {
		if (browser != null && FlightRecorderUI.getDefault().isAnalysisEnabled()) {
			browser.setFocus();
		} else {
			errorMessage.setFocus();
		}
	}

	public void setEditor(IPageContainer editor) {
		this.editor = editor;
	}

	public void setTopics(Collection<String> topics) {
		this.topics = new HashSet<>(topics);
		report.setResults(editor.getRuleManager().getResults(topics));
		report.createHtmlOverview(browser, editor, null);
	}

	public void updateRule(Result result) {
		if (topics.contains(result.getRule().getTopic())) {
			report.updateRule(result.getRule());
		}
	}

}
