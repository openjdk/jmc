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
import java.util.Collections;

import javax.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import org.openjdk.jmc.console.ui.editor.IConsolePageContainer;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.ui.operations.ExecuteOperationForm;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;

/**
 * Tab for Diagnostic Commands
 */
public class DiagnosticTab implements IConsolePageStateHandler {

	private ExecuteOperationForm operationsForm;

	@Inject
	protected void createPageContent(
		IConsolePageContainer page, IManagedForm managedForm, IConnectionHandle connection) {
		ScrolledForm form = managedForm.getForm();
		form.getBody().setLayout(MCLayoutFactory.createFormPageLayout());
		IMemento state = page.loadConfig();
		SashForm sash = new SashForm(form.getBody(), SWT.VERTICAL);
		operationsForm = new DiagnosticOperationForm(sash, managedForm.getToolkit(), state,
				loadOperations(page, connection));
		sash.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
	}

	private Collection<? extends IOperation> loadOperations(IConsolePageContainer page, IConnectionHandle connection) {
		IDiagnosticCommandService diagCommandService = connection.getServiceOrNull(IDiagnosticCommandService.class);
		if (diagCommandService != null) {
			try {
				return diagCommandService.getOperations();
			} catch (Exception e) {
				page.presentError(Messages.DiagnosticTab_MESSAGE_DIAGNOSTIC_COMMANDS_NOT_SUPPORTED);
				return Collections.emptyList();
			}
		} else {
			page.presentError(Messages.DiagnosticTab_MESSAGE_DIAGNOSTIC_COMMANDS_NOT_SUPPORTED);
			return Collections.emptyList();
		}
	}

	@Override
	public boolean saveState(IMemento state) {
		if (operationsForm != null) {
			operationsForm.saveState(state);
		}
		return true;
	}

	@Override
	public void dispose() {
	}
}
