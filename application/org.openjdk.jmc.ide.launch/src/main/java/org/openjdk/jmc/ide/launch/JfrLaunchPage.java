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
package org.openjdk.jmc.ide.launch;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.RecordingWizardPage;
import org.openjdk.jmc.ide.launch.model.JfrLaunchModel;

public class JfrLaunchPage extends RecordingWizardPage implements Observer {

	private Button enabledCheckbox;
	private Button autoOpenCheckbox;
	private JfrLaunchModel model;

	public JfrLaunchPage(JfrLaunchModel model) {
		super(model, false, false, false, false, false, true, true, true);
		this.model = model;
		model.addObserver(this);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		int cols = 2;
		comp.setLayout(new GridLayout(cols, false));

		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(gd1);

		createEnabled(comp, cols);
		createOpenAutomatically(comp, cols);
		// TODO: Add info text and help text

		createSeparator(comp, cols);

		super.createControl(comp);
		GridData gd3 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd3.horizontalSpan = cols;
		super.getControl().setLayoutData(gd3);
		setControl(comp);
	}

	private void createSeparator(Composite comp, int cols) {
		Label sep = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd2.horizontalSpan = cols;
		sep.setLayoutData(gd2);
	}

	private void createEnabled(Composite parent, int cols) {
		enabledCheckbox = new Button(parent, SWT.CHECK);
		enabledCheckbox.setText(Messages.JfrLaunch_ENABLE_JFR);
		enabledCheckbox.setToolTipText(Messages.JfrLaunch_ENABLE_JFR_TOOLTIP);
		enabledCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.setJfrEnabled(enabledCheckbox.getSelection());
			}
		});
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.horizontalSpan = 1;
		enabledCheckbox.setLayoutData(gd);
	}

	private void createOpenAutomatically(Composite parent, int cols) {
		autoOpenCheckbox = new Button(parent, SWT.CHECK);
		autoOpenCheckbox.setText(Messages.JfrLaunch_AUTO_OPEN);
		autoOpenCheckbox.setToolTipText(Messages.JfrLaunch_AUTO_OPEN_TOOLTIP);
		autoOpenCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.setAutoOpen(autoOpenCheckbox.getSelection());
			}
		});
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.horizontalSpan = cols - 1;
		autoOpenCheckbox.setLayoutData(gd);
	}

	public void setJfrEnabled(boolean jfrEnabled) {
		enabledCheckbox.setSelection(jfrEnabled);
	}

	public boolean isJfrEnabled() {
		return enabledCheckbox.getSelection();
	}

	public void setAutoOpen(boolean autoOpen) {
		autoOpenCheckbox.setSelection(autoOpen);
	}

	public boolean getAutoOpen() {
		return autoOpenCheckbox.getSelection();
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg != null && arg.equals(JfrLaunchModel.JRE_SUPPORTS_DUMPONEXIT_CHANGED)) {
			Boolean jreSupportsDumpOnExitWithoutDefaultRecording = model
					.isJreSupportsDumpOnExitWithoutDefaultRecording();
			setBehaviorForContinuous(!jreSupportsDumpOnExitWithoutDefaultRecording,
					!jreSupportsDumpOnExitWithoutDefaultRecording);
		}
	}
}
