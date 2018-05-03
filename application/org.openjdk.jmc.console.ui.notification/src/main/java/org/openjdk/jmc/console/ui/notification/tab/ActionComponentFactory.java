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
package org.openjdk.jmc.console.ui.notification.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.console.ui.notification.widget.ActionChooser;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.actions.internal.TriggerActionDiagnosticCommand;
import org.openjdk.jmc.rjmx.triggers.fields.internal.Field;
import org.openjdk.jmc.rjmx.ui.operations.InvocatorBuilderForm;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;

// FIXME: This is a workaround to enable custom action UI. Should be moved to an extension point, but not worth the effort until the Trigger framework is rewritten.
public class ActionComponentFactory implements ActionChooser.ComponentFactory {
	private final IConnectionHandle connection;
	private final FormToolkit toolkit;

	public ActionComponentFactory(FormToolkit toolkit, IConnectionHandle connection) {
		this.toolkit = toolkit;
		this.connection = connection;
	}

	@Override
	public Composite createComponent(Composite parent, ITriggerAction action) {
		if (action instanceof TriggerActionDiagnosticCommand) {
			try {
				IDiagnosticCommandService diagCommandService = connection
						.getServiceOrThrow(IDiagnosticCommandService.class);
				TriggerActionDiagnosticCommand diagnosticCommandAction = (TriggerActionDiagnosticCommand) action;
				final Field f = (Field) diagnosticCommandAction.getSetting("command"); //$NON-NLS-1$

				// FIXME: State should be stored with the tab
				List<ColumnSettings> columnSettings = new ArrayList<>();
				columnSettings.add(new ColumnSettings("name", true, 150, null)); //$NON-NLS-1$
				columnSettings.add(new ColumnSettings("description", false, 200, null)); //$NON-NLS-1$
				columnSettings.add(new ColumnSettings("value", false, 200, null)); //$NON-NLS-1$
				columnSettings.add(new ColumnSettings("type", true, 100, null)); //$NON-NLS-1$
				TableSettings tableSettings = new TableSettings(null, columnSettings);

				SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
				InvocatorBuilderForm invocatorForm = new InvocatorBuilderForm(sash, toolkit, false, tableSettings,
						new InvocatorBuilderForm.InvocatorUpdateListener() {

							@Override
							public void onInvocatorUpdated(IOperation operation, Callable<?> invocator) {
								f.setValue(invocator == null ? "" : invocator.toString()); //$NON-NLS-1$
							}
						});

				invocatorForm.setOperations(diagCommandService.getOperations());
				return sash;
			} catch (Exception e) {
				NotificationPlugin.getDefault().getLogger().log(Level.FINE,
						"Could not create diagnostic command ui for trigger action", e); //$NON-NLS-1$
			}
		}
		return null;
	}
}
