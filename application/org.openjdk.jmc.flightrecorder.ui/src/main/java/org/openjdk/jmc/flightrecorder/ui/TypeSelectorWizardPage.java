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
package org.openjdk.jmc.flightrecorder.ui;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.ui.EventTypeFolderNode.EventTypeNode;
import org.openjdk.jmc.flightrecorder.ui.common.TypeFilterBuilder;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

class TypeSelectorWizardPage extends WizardPage implements IPerformFinishable {

	private final EventTypeFolderNode root;
	private final Consumer<Set<IType<IItem>>> onTypesSelected;
	private TypeFilterBuilder typeSelector;

	TypeSelectorWizardPage(EventTypeFolderNode root, Consumer<Set<IType<IItem>>> onTypesSelected, String title,
			String description) {
		super("TypeSelectorWizardPage"); //$NON-NLS-1$
		this.root = root;
		this.onTypesSelected = onTypesSelected;
		setTitle(title);
		setDescription(description);
	}

	@Override
	public void createControl(Composite parent) {
		typeSelector = new TypeFilterBuilder(parent,
				() -> setPageComplete(typeSelector.getCheckedTypeIds().findAny().isPresent()));
		typeSelector.setInput(root);
		setControl(typeSelector.getControl());
	}

	@Override
	public boolean performFinish() {
		onTypesSelected.accept(typeSelector.getSelectedTypes().map(EventTypeNode::getType).collect(Collectors.toSet()));
		return true;
	}

	static void openDialog(
		EventTypeFolderNode root, Consumer<Set<IType<IItem>>> onTypesSelected, String title, String description) {
		OnePageWizardDialog.open(new TypeSelectorWizardPage(root, onTypesSelected, title, description), 500, 600);
	}
}
