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
package org.openjdk.jmc.console.ui.mbeanbrowser.tab;

import javax.inject.Inject;
import javax.management.MBeanServerConnection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import org.openjdk.jmc.console.ui.editor.IConsolePageContainer;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.console.ui.editor.internal.ConsoleEditor;
import org.openjdk.jmc.console.ui.mbeanbrowser.tree.MBeanTreeSectionPart;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.ui.OrientationAction;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;

/**
 * Tab that hold the MBean tree and attribute values in a {@link IDetailsPage}.
 */
public class MBeanTab implements IConsolePageStateHandler {

	private SashForm sashForm;
	private FeatureSectionPart detailsPart;
	private static final String VERTICAL_SASH_ID = "verticalSash"; //$NON-NLS-1$

	@Inject
	protected void createPageContent(IConsolePageContainer page, IManagedForm managedForm, IConnectionHandle ch) {
		IMemento state = page.loadConfig();
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		boolean vertical = Boolean.TRUE.equals(state.getBoolean(VERTICAL_SASH_ID));

		sashForm = new SashForm(form.getBody(), vertical ? SWT.VERTICAL : SWT.HORIZONTAL);
		toolkit.adapt(sashForm, false, false);

		MBeanServerConnection mbeanServer = ch.getServiceOrDummy(MBeanServerConnection.class);
		String guid = ch.getServerDescriptor().getGUID();
		IMBeanHelperService mbeanService = ch.getServiceOrDummy(IMBeanHelperService.class);
		MBeanTreeSectionPart tree = new MBeanTreeSectionPart(sashForm, toolkit, mbeanServer, guid, mbeanService);
		managedForm.addPart(tree);
		detailsPart = new FeatureSectionPart(sashForm, toolkit, state, ch,
				((ConsoleEditor) page.getEditor()).getSectionPartManagers().get(0));
		managedForm.addPart(detailsPart);
		tree.addMBeanListener(detailsPart);
		tree.selectDefaultBean();

		sashForm.setWeights(new int[] {7, 20});
		form.getBody().setLayout(MCLayoutFactory.createFormPageLayout());
		sashForm.setLayoutData(MCLayoutFactory.createFormPageLayoutData());

		OrientationAction.installActions(managedForm, sashForm);
	}

	@Override
	public boolean saveState(IMemento state) {
		state.putBoolean(VERTICAL_SASH_ID, sashForm.getOrientation() == SWT.VERTICAL);
		detailsPart.saveState(state);
		return true;
	}

	@Override
	public void dispose() {
	}
}
