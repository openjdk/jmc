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
package org.openjdk.jmc.ui.wizards;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

/**
 */
abstract public class ExportTreeToFileWizardPage extends ExportToFileWizardPage {
	public static final String TREE_NAME = "wizards.importexport.tree.name"; //$NON-NLS-1$

	private ContainerCheckedTreeViewer ctw;

	public ExportTreeToFileWizardPage(String pageName, String fileExtension) {
		super(pageName, fileExtension);
	}

	/**
	 * initialize the TreeViewer this wizard uses
	 *
	 * @param viewer
	 */
	abstract protected void initializeViewer(TreeViewer viewer);

	/**
	 * Create the container with the buttons. That is a
	 * <li>Select all button
	 * <li>Deselect all button
	 *
	 * @param parent
	 *            the parent composite to use
	 * @return a composite with the buttons
	 */
	private Composite createButtonContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.makeColumnsEqualWidth = true;
		container.setLayout(layout);

		createButtonWithHandler(container, Messages.TreeContentProviderWizardPage_SELECT_ALL_TEXT,
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						// Checking roots, ContainerCheckedTreeViewer will do the rest
						for (TreeItem treeItem : ctw.getTree().getItems()) {
							ctw.setChecked(treeItem.getData(), true);
						}
						updatePageComplete();
					}
				});

		createButtonWithHandler(container, Messages.TreeContentProviderWizardPage_DESELECT_ALL_TEXT,
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						// Unchecking roots, ContainerCheckedTreeViewer will do the rest
						for (TreeItem treeItem : ctw.getTree().getItems()) {
							ctw.setChecked(treeItem.getData(), false);
						}
						updatePageComplete();
					}
				});

		return container;
	}

	private Control createButtonWithHandler(Composite parent, String text, SelectionListener s) {
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false);
		Button button = new Button(parent, SWT.NONE);
		button.setText(text);
		button.addSelectionListener(s);
		button.setLayoutData(gd1);
		return button;
	}

	@Override
	protected Composite createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		ctw = new ContainerCheckedTreeViewer(container);
		ctw.getTree().setData("name", TREE_NAME); //$NON-NLS-1$
		initializeViewer(ctw);

		setPageComplete(false);
		ctw.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				updatePageComplete();
			}
		});

		Composite buttonBar = createButtonContainer(container);

		container.setLayout(new GridLayout(2, false));
		ctw.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		return container;
	}

	@Override
	protected boolean isSelectionValid() {
		Collection<?> selected = getSelectedItems();
		return selected != null && selected.size() > 0;
	}

	/**
	 * Gets the items that were checked in the tree of this wizard.
	 *
	 * @return the checked items
	 */
	public Collection<?> getSelectedItems() {
		return Arrays.asList(ctw.getCheckedElements());
	}
}
