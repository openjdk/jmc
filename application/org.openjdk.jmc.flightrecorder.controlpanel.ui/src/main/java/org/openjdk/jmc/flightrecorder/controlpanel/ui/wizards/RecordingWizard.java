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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;

public abstract class RecordingWizard extends Wizard {

	private final RecordingWizardModel m_model;

	public RecordingWizard(RecordingWizardModel model) {
		m_model = model;
		setWindowTitle(model.isEditing() ? Messages.EDIT_RECORDING_WIZARD_PAGE_TITLE
				: Messages.START_RECORDING_WIZARD_PAGE_TITLE);
	}

	@Override
	public boolean performFinish() {
		for (IWizardPage page : getPages()) {
			if (page instanceof IPerformFinishable) {
				boolean subFinish = ((IPerformFinishable) page).performFinish();
				if (!subFinish) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void addPages() {
		addPage(new RecordingWizardPage(m_model));
		addPage(new RecordingEventOptionsWizardPage(m_model));
		addPage(new AdvancedWizardPage(m_model));
	}

	protected RecordingWizardModel getModel() {
		return m_model;
	}
}
