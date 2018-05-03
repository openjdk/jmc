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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ImageConstants;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;

/**
 * Wizard page for configuring event options for a recording.
 */
public final class RecordingEventOptionsWizardPage extends WizardPage implements Observer {

	public static final String PAGE_NAME = "recordingEventOptionsWizard"; //$NON-NLS-1$
	private static final int WIZARD_STAGE = 1;

	private final RecordingWizardModel m_model;

	private RecordingTemplateControlView m_controlView;

	public RecordingEventOptionsWizardPage(RecordingWizardModel model) {
		super(PAGE_NAME);
		m_model = model;
		setImageDescriptor(ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.IMAGE_WIZARD_START_RECORDING));
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		setDescription(Messages.START_RECORDING_EVENT_OPTIONS_WIZARD_PAGE_DESCRIPTION);

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));

		m_controlView = new RecordingTemplateControlView(container, this::setError);

		setControl(container);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.RECORDING_WIZARD);
	}

	private void setError(String errorMessage) {
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(IHelpContextIds.RECORDING_WIZARD);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			IEventConfiguration ourConfig = m_model.getCurrentConfigurationAt(WIZARD_STAGE);

			setTitle(NLS.bind(Messages.START_RECORDING_EVENT_OPTIONS_WIZARD_PAGE_TITLE,
					m_model.getCurrentConfigurationAt(WIZARD_STAGE).getName()));

			XMLModel ourModel = ((EventConfiguration) ourConfig).getXMLModel();
			IEventConfiguration activeConfig = m_model.getActiveConfiguration();
			if (activeConfig != ourConfig) {
				if (!activeConfig.equalSettings(ourConfig)) {
					setMessage(Messages.START_RECORDING_WIZARD_LATER_STAGE_CHANGE_WARNING, WARNING);
				} else {
					setMessage(null);
				}
				ourModel.setDirty(false);
				// FIXME: This Observer is not deleted unless it is notified. Maybe it should be.
				ourModel.addObserver(this);
			} else {
				setMessage(null);
			}
			// FIXME: This should not modify the XML model, but currently it might (due to qualifying controls to URIs).
			m_controlView.cleanCreate(ourModel);
		}
		super.setVisible(visible);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof XMLModel) {
			m_model.flushConfigurationsBeyond(WIZARD_STAGE);
			setMessage(null);
			o.deleteObserver(this);
		}
	}
}
