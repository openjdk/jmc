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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import org.junit.Assert;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;

/**
 * The Jemmy base wrapper for the flight recorder template manager
 */
public class MCTemplateManager extends MCJemmyBase {
	private static final String TEMPLATE_MANAGER_DIALOG_TITLE = org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages.TEMPLATE_MANAGER_DIALOG_TITLE;
	private MCDialog templateDialog;
	public MCTable templates;
	public MCButton editButton;
	public MCButton importButton;
	public MCButton exportButton;
	public MCButton removeButton;
	public MCButton newButton;
	public MCButton duplicateButton;
	public MCButton okButton;

	/**
	 * Instantiate and set up the template manager
	 */
	public MCTemplateManager() {
		templateDialog = new MCDialog(TEMPLATE_MANAGER_DIALOG_TITLE);
		templates = MCTable.getByName(templateDialog, "templateTable");
		editButton = MCButton.getByLabel(templateDialog.getDialogShell(), "Edit", false);
		importButton = MCButton.getByLabel(templateDialog.getDialogShell(), "Import Files...", false);
		exportButton = MCButton.getByLabel(templateDialog.getDialogShell(), "Export File...", false);
		removeButton = MCButton.getByLabel(templateDialog.getDialogShell(), "Remove", false);
		newButton = MCButton.getByLabel(templateDialog.getDialogShell(), "New", false);
		duplicateButton = MCButton.getByLabel(templateDialog.getDialogShell(), "Duplicate", false);
		okButton = MCButton.getByLabel(templateDialog.getDialogShell(), MCButton.Labels.OK, false);
	}

	/**
	 * Removes the selected template
	 */
	public void removeSelected() {
		Assert.assertTrue("Remove button isn't active for the currently selected template", removeButton.isEnabled());
		removeButton.click();
		MCDialog confirmation = new MCDialog("Confirm remove");
		confirmation.closeWithButton(MCButton.Labels.OK);
	}

	/**
	 * Removes the template
	 * 
	 * @param name
	 *            the name of the template to remove
	 */
	public void removeTemplate(String name) {
		templates.select(name);
		removeSelected();
	}

	/**
	 * Opens a template editor for the currently selected template
	 * 
	 * @return a {@link MCDialog} for the template editor
	 */
	public MCDialog editSelected() {
		Assert.assertTrue("Edit button isn't active for the currently selected template", editButton.isEnabled());
		editButton.click();
		return MCDialog.getByAnyDialogTitle("Template Event Details", "Template Options");
	}

	/**
	 * Opens a template editor
	 * 
	 * @param name
	 *            the name of the template to edit
	 * @return a {@link MCDialog} for the template editor
	 */
	public MCDialog editTemplate(String name) {
		templates.select(name);
		return editSelected();
	}

	/**
	 * Closes the template editor
	 */
	public void close() {
		templateDialog.closeWithButton(MCButton.Labels.OK);
	}
}
