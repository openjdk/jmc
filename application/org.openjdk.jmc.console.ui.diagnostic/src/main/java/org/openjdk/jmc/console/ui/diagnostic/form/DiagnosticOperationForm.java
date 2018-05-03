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
package org.openjdk.jmc.console.ui.diagnostic.form;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.console.ui.diagnostic.DiagnosticPlugin;
import org.openjdk.jmc.console.ui.diagnostic.preferences.DiagnosticPage;
import org.openjdk.jmc.console.ui.diagnostic.preferences.PreferenceConstants;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.services.IOperation.OperationImpact;
import org.openjdk.jmc.rjmx.ui.operations.ExecuteOperationForm;
import org.openjdk.jmc.rjmx.ui.operations.OperationsLabelProvider;
import org.openjdk.jmc.ui.UIPlugin;

public class DiagnosticOperationForm extends ExecuteOperationForm {

	private final static String HELP_CMD = "help"; //$NON-NLS-1$

	public DiagnosticOperationForm(SashForm parent, FormToolkit formToolkit, IMemento state,
			Collection<? extends IOperation> operations) {
		super(parent, formToolkit, false, state);
		setOperations(operations);
		for (IOperation operation : operations) {
			if (operation.getName().equals(HELP_CMD)) {
				addHelpButton(operation);
				return;
			}
		}
	}

	private void addHelpButton(final IOperation helpOperation) {
		SelectionAdapter helpProvider = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String operationName = getSelectedOperation().getName();
				invokeAsync(new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						return helpOperation.getInvocator(null, operationName).call();
					}

					@Override
					public String toString() {
						return HELP_CMD + " " + operationName; //$NON-NLS-1$
					}
				});
			}
		};
		Button helpButton = createButton(Messages.DiagnosticTab_HELP_LABEL);
		helpButton.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_HELP));
		helpButton.addSelectionListener(helpProvider);
	}

	@Override
	protected void executeOperation() {
		IOperation selectedOperation = getSelectedOperation();
		if (selectedOperation != null) {
			OperationImpact impact = selectedOperation.getImpact();
			String promptPrefKey = PreferenceConstants.getPromptKey(impact);
			IPreferenceStore preferenceStore = DiagnosticPlugin.getDefault().getPreferenceStore();
			if (preferenceStore.getBoolean(promptPrefKey)) {
				String promptQuestion = DiagnosticPage.getPromptQuestion(impact);
				String impactName = OperationsLabelProvider.impactAsString(impact);
				String title = NLS.bind(Messages.DiagnosticTab_WARNING_FOR_ADVANCED_USER_TITLE, impactName);
				MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(
						Display.getCurrent().getActiveShell(), title,
						Messages.DiagnosticTab_WARNING_FOR_ADVANCED_USER_MESSAGE, promptQuestion, true, null, null);
				if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
					if (!dialog.getToggleState()) {
						preferenceStore.setValue(promptPrefKey, false);
					}
				} else {
					return;
				}
			}
			super.executeOperation();
		}
	}
}
