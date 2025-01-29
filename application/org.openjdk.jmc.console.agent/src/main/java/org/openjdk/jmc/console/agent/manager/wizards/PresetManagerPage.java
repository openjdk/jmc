/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.console.agent.manager.wizards;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.openjdk.jmc.console.agent.AgentPlugin;
import org.openjdk.jmc.console.agent.manager.model.IPreset;
import org.openjdk.jmc.console.agent.manager.model.PresetRepository;
import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.openjdk.jmc.console.agent.wizards.BaseWizardPage;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

public class PresetManagerPage extends BaseWizardPage {

	private static final String ID_PRESET = "preset"; // $NON-NLS-1$
	private static final String PRESET_XML_EXTENSION = "*.xml"; // $NON-NLS-1$

	private final PresetRepository repository;

	private TableInspector tableInspector;

	public PresetManagerPage(PresetRepository repository) {
		super(Messages.PresetManagerPage_PAGE_NAME);

		this.repository = repository;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		setTitle(Messages.PresetManagerPage_MESSAGE_PRESET_MANAGER_PAGE_TITLE);
		setDescription(Messages.PresetManagerPage_MESSAGE_PRESET_MANAGER_PAGE_DESCRIPTION);

		ScrolledComposite sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		Composite container = new Composite(sc, SWT.NONE);
		sc.setContent(container);

		container.setLayout(new FillLayout());

		createPresetTableContainer(container);

		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(sc);

		populateUi();
	}

	private Composite createPresetTableContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

		tableInspector = new TableInspector(container,
				TableInspector.MULTI | TableInspector.ADD_BUTTON | TableInspector.EDIT_BUTTON
						| TableInspector.DUPLICATE_BUTTON | TableInspector.REMOVE_BUTTON
						| TableInspector.IMPORT_FILES_BUTTON | TableInspector.EXPORT_FILE_BUTTON) {
			@Override
			protected void addColumns() {
				addColumn(ID_PRESET, new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (!(element instanceof IPreset)) {
							throw new IllegalArgumentException("element must be an IPreset"); // $NON-NLS-1$
						}

						IPreset preset = (IPreset) element;
						return preset.getFileName() + " - " + preset.getEvents().length + " "
								+ Messages.PresetManagerPage_MESSAGE_EVENTS;
					}

					@Override
					public Image getImage(Object element) {
						return AgentPlugin.getDefault().getImage(AgentPlugin.ICON_AGENT);
					}
				});
			}

			@Override
			protected void onAddButtonSelected(IStructuredSelection selection) {
				IPreset preset = repository.createPreset();
				while (displayWizard(preset)) {
					try {
						repository.addPreset(preset);
					} catch (IllegalArgumentException | IOException e) {
						if (DialogToolkit.openConfirmOnUiThread(
								Messages.PresetManagerPage_MESSAGE_PRESET_MANAGER_UNABLE_TO_SAVE_THE_PRESET,
								e.getLocalizedMessage())) {
							continue;
						}
					}

					break;
				}

				tableInspector.getViewer().refresh();
			}

			@Override
			protected void onEditButtonSelected(IStructuredSelection selection) {
				IPreset original = (IPreset) selection.getFirstElement();
				IPreset workingCopy = original.createWorkingCopy();
				while (displayWizard(workingCopy)) {
					try {
						repository.updatePreset(original, workingCopy);
					} catch (IllegalArgumentException | IOException e) {
						if (DialogToolkit.openConfirmOnUiThread(
								Messages.PresetManagerPage_MESSAGE_PRESET_MANAGER_UNABLE_TO_SAVE_THE_PRESET,
								e.getLocalizedMessage())) {
							continue;
						}
					}

					break;
				}

				tableInspector.getViewer().refresh();
			}

			@Override
			protected void onDuplicateButtonSelected(IStructuredSelection selection) {
				IPreset original = (IPreset) selection.getFirstElement();
				IPreset duplicate = original.createDuplicate();

				try {
					repository.addPreset(duplicate);
				} catch (IllegalArgumentException | IOException e) {
					DialogToolkit.openConfirmOnUiThread(
							Messages.PresetManagerPage_MESSAGE_PRESET_MANAGER_UNABLE_TO_SAVE_THE_PRESET,
							e.getLocalizedMessage());
				}

				tableInspector.getViewer().refresh();
			}

			@Override
			protected void onRemoveButtonSelected(IStructuredSelection selection) {
				for (Object preset : selection) {
					repository.removePreset((IPreset) preset);
				}

				tableInspector.getViewer().refresh();
			}

			@Override
			protected void onImportFilesButtonSelected(IStructuredSelection selection) {
				String[] files = openFileDialog(Messages.PresetManagerPage_MESSAGE_IMPORT_EXTERNAL_PRESET_FILES,
						new String[] {PRESET_XML_EXTENSION}, SWT.OPEN | SWT.MULTI);
				if (files.length != 0) {
					for (String path : files) {
						File file = new File(path);
						try {
							repository.importPreset(file);
						} catch (IOException | SAXException e) {
							DialogToolkit.openConfirmOnUiThread(
									Messages.PresetManagerPage_MESSAGE_PRESET_MANAGER_UNABLE_TO_IMPORT_THE_PRESET,
									e.getLocalizedMessage());
						}
					}
				}

				tableInspector.getViewer().refresh();
				setFocusOnTableInspector();
			}

			@Override
			protected void onExportFileButtonSelected(IStructuredSelection selection) {
				String[] files = openFileDialog(Messages.PresetManagerPage_MESSAGE_EXPORT_PRESET_TO_A_FILE,
						new String[] {PRESET_XML_EXTENSION}, SWT.SAVE | SWT.SINGLE);
				if (files.length == 0) {
					setFocusOnTableInspector();
					return;
				}

				File file = new File(files[0]);
				try {
					repository.exportPreset((IPreset) selection.getFirstElement(), file);
				} catch (IOException e) {
					DialogToolkit.openConfirmOnUiThread(
							Messages.PresetManagerPage_MESSAGE_PRESET_MANAGER_UNABLE_TO_EXPORT_THE_PRESET,
							e.getLocalizedMessage());
				}
			}
		};
		tableInspector.setContentProvider(new PresetTableContentProvider());
		setFocusOnTableInspector();

		return container;
	}

	private void populateUi() {
		tableInspector.setInput(repository);
	}

	private boolean displayWizard(IPreset preset) {
		PresetEditingWizard wizard = new PresetEditingWizard(preset);
		wizard.setHelpAvailable(false);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
		return dialog.open() == Window.OK;
	}

	private static class PresetTableContentProvider extends AbstractStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (!(inputElement instanceof PresetRepository)) {
				throw new IllegalArgumentException("input element must be a PresetRepository"); // $NON-NLS-1$
			}

			PresetRepository repository = (PresetRepository) inputElement;
			return repository.listPresets();
		}
	}

	// 8204: This set focus is required in order to set the focus back to wizard instead of main screen
	private void setFocusOnTableInspector() {
		tableInspector.setFocus();
	}
}
