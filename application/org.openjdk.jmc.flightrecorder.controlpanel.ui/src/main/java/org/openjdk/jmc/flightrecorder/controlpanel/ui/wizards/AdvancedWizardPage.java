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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ImageConstants;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;

/**
 * Wizard Page that shows advanced setting for a recording template
 */
final class AdvancedWizardPage extends WizardPage implements IPerformFinishable, Observer {
	public static final String PAGE_NAME = "advancedOptionWizard"; //$NON-NLS-1$
	private static final int WIZARD_STAGE = 2;

	private EventConfigurationPart m_recordingTemplateViewer;
	private final RecordingWizardModel m_model;
	private EventConfigurationModel m_eventConfigurationModel;

	protected AdvancedWizardPage(RecordingWizardModel model) {
		super(PAGE_NAME);
		m_model = model;
		setImageDescriptor(ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.IMAGE_WIZARD_START_RECORDING));
	}

	@Override
	public void createControl(Composite parent) {
		setDescription(Messages.ADVANCED_WIZARD_PAGE_DESCRIPTION);
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		m_eventConfigurationModel = createEditableModel();
		m_recordingTemplateViewer = new EventConfigurationPart(this, m_eventConfigurationModel, false);
		Control settingsTree = m_recordingTemplateViewer.createControl(container);
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		settingsTree.setLayoutData(gd1);

		setControl(container);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.RECORDING_WIZARD);
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(IHelpContextIds.RECORDING_WIZARD);
	}

	@Override
	public boolean performFinish() {
		m_eventConfigurationModel.pushServerMetadataToLocalConfiguration(false);
		return true;
	}

	private EventConfigurationModel createEditableModel() {
		IEventConfiguration configuration = m_model.getCurrentConfigurationAt(WIZARD_STAGE);
		if (configuration == null) {
			// Rare corner case that we need to handle.
			return null;
		}

		// Make sure to remove any control elements if modified at this stage, for the "last started" template.
		if (configuration.hasControlElements()) {
			XMLModel ourModel = ((EventConfiguration) configuration).getXMLModel();
			ourModel.setDirty(false);
			// FIXME: This Observer is not deleted unless it is notified. Maybe it should be.
			ourModel.addObserver(this);
		}

		// FIXME: Unexpected and quite ugly to update the title in here.
		setTitle(NLS.bind(Messages.ADVANCED_WIZARD_PAGE_TITLE, configuration.getName()));

		return EventConfigurationModel.create(configuration, m_model);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			if (m_recordingTemplateViewer != null) {
				m_recordingTemplateViewer.setInput(createEditableModel());
			}
		}
		super.setVisible(visible);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof XMLModel) {
			IEventConfiguration configuration = m_model.getCurrentConfigurationAt(WIZARD_STAGE);
			configuration.removeControlElements();
			o.deleteObserver(this);
		}
	}
}
