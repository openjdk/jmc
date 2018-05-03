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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationRepository;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.EventConfigurationPart;

/**
 * Dialog controller to edit a template (for flight recording options) which doesn't have a control
 * section.
 */
public class TemplateEditAdvancedPage extends TemplateEditPage {
	EventConfigurationPart detailPart;

	public TemplateEditAdvancedPage(EventConfigurationModel model, EventConfigurationRepository repository) {
		super(model, repository, "templateEditAdvanced", Messages.TEMPLATE_EDIT_ADVANCED_DIALOG_TITLE); //$NON-NLS-1$
	}

	@Override
	protected void createSpecificArea(Composite parent) {
		StringBuilder message = new StringBuilder();
		int messageType = 0;
		if (model.getConfiguration().removeControlElements()) {
			message.append(Messages.TEMPLATE_EDIT_ADVANCED_LOSING_CONTROL_ELEMENT_WARNING);
			messageType = WARNING;
		}
		if (model.isOffline()) {
			message.append(System.getProperty("line.separator")); //$NON-NLS-1$
			message.append(Messages.TEMPLATE_EDIT_ADVANCED_OFFLINE_WARNING);
			messageType = Math.max(messageType, WARNING);
		}
		if (message.length() > 0) {
			setMessage(message.toString(), messageType);
		}
		detailPart = new EventConfigurationPart(this, model, true);
		Control control = detailPart.createControl(parent);
		control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
}
