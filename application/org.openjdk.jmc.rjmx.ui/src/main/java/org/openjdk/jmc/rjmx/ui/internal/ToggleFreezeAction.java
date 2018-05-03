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
package org.openjdk.jmc.rjmx.ui.internal;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.MCToolBarManager;

/**
 * Used to freeze or resume updates of values in attribute viewers.
 */
public class ToggleFreezeAction extends Action {

	private final FreezeModel m_freezeModel;

	// private MCToolBarManager m_toolBarManager;

	/**
	 * Creates a new toggle freeze action that will inform a section part on state changes.
	 *
	 * @param toolBarManager
	 *            the tool bar manager that this action belongs to
	 * @param freezeModel
	 *            the freeze model determining whether some component is running in freezed mode or
	 *            not
	 */
	public ToggleFreezeAction(final MCToolBarManager toolBarManager, FreezeModel freezeModel) {
		super(Messages.UpdatesAction_ACTION_NAME, IAction.AS_CHECK_BOX);
		// m_toolBarManager = toolBarManager;
		m_freezeModel = freezeModel;
		m_freezeModel.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				boolean freezeUpdates = m_freezeModel.isFreezed();
				if (freezeUpdates != isChecked()) {
					setChecked(freezeUpdates);
					toolBarManager.update();
				}
			}
		});
		setChecked(false);
		setDisabledImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_REFRESH));
		setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_REFRESH_GRAY));
		setToolTipText(Messages.UpdatesAction_TOOLTIP_TEXT);
		setId("toggle.freeze"); //$NON-NLS-1$
	}

	@Override
	public void run() {
		m_freezeModel.setFreezed(isChecked());
	}

}
