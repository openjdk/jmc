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
package org.openjdk.jmc.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.Form;

import org.openjdk.jmc.ui.misc.MCActionContributionItem;

public class OrientationAction extends Action {
	private final SashForm m_sashForm;
	private final int m_style;
	private Runnable layoutUpdater;

	private OrientationAction(IContributionManager contributionManager, SashForm sashForm, Runnable layoutUpdater,
			int style, String name) {
		super(name, IAction.AS_RADIO_BUTTON);
		m_sashForm = sashForm;
		this.layoutUpdater = layoutUpdater;
		m_style = style;
	}

	@Override
	public void run() {
		if (m_sashForm != null && !m_sashForm.isDisposed()) {
			m_sashForm.setOrientation(m_style);
		}
		layoutUpdater.run();
	}

	public static void installActions(Form form, SashForm sashForm) {
		installActions(form.getToolBarManager(), sashForm, () -> {
		});
		form.updateToolBar();
	}

	public static void installActions(IManagedForm managedForm, SashForm sashForm) {
		installActions(managedForm.getForm().getToolBarManager(), sashForm, () -> {
			if (managedForm != null && managedForm.getForm() != null && !managedForm.getForm().isDisposed()) {
				managedForm.getForm().reflow(true);
			}
		});
		managedForm.getForm().updateToolBar();
	}

	public static void installActions(
		IContributionManager contributionManager, SashForm sashForm, Runnable layoutUpdater) {
		boolean vertical = sashForm.getOrientation() == SWT.VERTICAL;

		OrientationAction h = new OrientationAction(contributionManager, sashForm, layoutUpdater, SWT.HORIZONTAL,
				Messages.MCMasterDetails_HORIZONTAL_LAYOUT);
		h.setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_HORIZONTAL_LAYOUT));
		h.setChecked(!vertical);
		h.setToolTipText(Messages.MCMasterDetails_HORIZONTAL_LAYOUT);
		h.setId("horizontal"); //$NON-NLS-1$

		OrientationAction v = new OrientationAction(contributionManager, sashForm, layoutUpdater, SWT.VERTICAL,
				Messages.MCMasterDetails_VERTICAL_LAYOUT);
		v.setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_VERTICAL_LAYOUT));
		v.setChecked(vertical);
		v.setToolTipText(Messages.MCMasterDetails_VERTICAL_LAYOUT);
		v.setId("vertical"); //$NON-NLS-1$

		contributionManager.add(new MCActionContributionItem(h));
		contributionManager.add(new MCActionContributionItem(v));
	}
}
