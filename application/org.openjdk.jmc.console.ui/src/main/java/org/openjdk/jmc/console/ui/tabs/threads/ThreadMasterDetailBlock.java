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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.ui.OrientationAction;

public class ThreadMasterDetailBlock {
	private static final String VERTICAL_SASH_ID = "verticalSash"; //$NON-NLS-1$
	private final ThreadTableSectionPart threadTableSectionPart;
	private final SashForm sashForm;

	public ThreadMasterDetailBlock(Composite parent, IManagedForm managedForm, IWorkbenchPartSite site,
			IThreadsModel model, IMemento state) {
		FormToolkit toolkit = managedForm.getToolkit();

		boolean vertical = (state == null ? true : !Boolean.FALSE.equals(state.getBoolean(VERTICAL_SASH_ID)));
		sashForm = new SashForm(parent, vertical ? SWT.VERTICAL : SWT.HORIZONTAL);
		toolkit.adapt(sashForm, false, false);

		threadTableSectionPart = new ThreadTableSectionPart(sashForm, toolkit, model, state);
		managedForm.addPart(threadTableSectionPart);
		model.getPollManager().addPollee(threadTableSectionPart);

		StackTraceSectionPart stackTracePart = new StackTraceSectionPart(sashForm, managedForm, site, model);
		managedForm.addPart(stackTracePart);
		model.getPollManager().addPollee(stackTracePart);
		threadTableSectionPart.addSelectionListener(stackTracePart);

		sashForm.setWeights(new int[] {2, 1});

		OrientationAction.installActions(managedForm, sashForm);
	}

	SashForm getSashForm() {
		return sashForm;
	}

	void saveState(IMemento memento) {
		memento.putBoolean(VERTICAL_SASH_ID, sashForm.getOrientation() == SWT.VERTICAL);
		threadTableSectionPart.saveState(memento);
	}
}
