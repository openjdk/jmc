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
package org.openjdk.jmc.console.ui.mbeanbrowser.tree;

import javax.management.MBeanServerConnection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.console.ui.mbeanbrowser.MBeanBrowserPlugin;
import org.openjdk.jmc.console.ui.mbeanbrowser.messages.internal.Messages;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

/**
 * An action to query user about details for MBean creation.
 */
public class AddMBeanAction extends Action {

	private final MBeanServerConnection mbeanServerConnection;
	private final String guid;

	public AddMBeanAction(MBeanServerConnection connection, String guid) {
		super("", IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
		mbeanServerConnection = connection;
		this.guid = guid;
		setText(Messages.ADD_MBEAN_LABEL);
		setToolTipText(Messages.ADD_MBEAN_DESCRIPTION);
		setImageDescriptor(MBeanBrowserPlugin.getDefault().getMCImageDescriptor(MBeanBrowserPlugin.ICON_ADD));
		setId("addMBean"); //$NON-NLS-1$
	}

	@Override
	public void run() {
		AddMBeanWizardPage wizardPage = new AddMBeanWizardPage(mbeanServerConnection, guid);
		OnePageWizardDialog dialog = new OnePageWizardDialog(Display.getCurrent().getActiveShell(), wizardPage);
		dialog.open();
	}
}
