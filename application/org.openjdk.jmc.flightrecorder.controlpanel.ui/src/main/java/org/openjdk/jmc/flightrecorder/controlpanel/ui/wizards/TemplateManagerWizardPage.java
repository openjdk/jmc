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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ImageConstants;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationRepository;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.PrivateStorageDelegate;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

/**
 * Dialog controller to manage templates for flight recording configurations.
 */
public class TemplateManagerWizardPage extends WizardPage {
	private static final String JFC_FILE_EXTENSION = '*' + IEventConfiguration.JFC_FILE_EXTENSION;
	private static final String ALL_FILE_EXTENSION = "*"; //$NON-NLS-1$

	private static final String[] FILE_EXTENSIONS = new String[] {JFC_FILE_EXTENSION, ALL_FILE_EXTENSION};

	private static final String[] FILE_DESCRIPTIONS = new String[] {
			NLS.bind(Messages.FILE_RECORDING_DESCRIPTION, JFC_FILE_EXTENSION), Messages.FILE_ALL_DESCRIPTION};

	private static final int MAX_REMOVE_TEMPLATES_TO_DISPLAY = 5;

	private final EventConfigurationRepository repository;
	/**
	 * Settings, or null if offline.
	 */
	private final IDescribedMap<EventOptionID> eventDefaults;
	private final Map<? extends IEventTypeID, ? extends IEventTypeInfo> eventTypeInfos;
	private final SchemaVersion version;
	private Observer repositoryObserver;
	private TableViewer tableViewer;
	private Button editButton;
	private Button exportButton;
	private Button deleteButton;
	private Button newButton;
	private Button duplicateButton;
	private Button refreshButton;

	/**
	 * Create a Template Manager Dialog, without opening it.
	 *
	 * @param shell
	 *            a shell
	 * @param repository
	 *            the repository to edit
	 * @param currentEventTypeSettings
	 *            current settings on server or null for offline mode
	 * @param schemaVersion
	 * @return a {@link Dialog} that should be opened by the caller.
	 */
	public static Dialog createDialogFor(
		Shell shell, EventConfigurationRepository repository, IDescribedMap<EventOptionID> currentEventTypeSettings,
		Map<? extends IEventTypeID, ? extends IEventTypeInfo> eventTypeInfos, SchemaVersion version) {
		OnePageWizardDialog dialog = new OnePageWizardDialog(shell,
				new TemplateManagerWizardPage(repository, currentEventTypeSettings, eventTypeInfos, version));
		// FIXME: Change to "Close"-button for dialog.
		dialog.setHideCancelButton(true);
		dialog.setWidthConstraint(600, 800);
		dialog.setHeightConstraint(600, 800);
		return dialog;
	}

	/**
	 * Create the Template Manager wizard page.
	 *
	 * @param repository
	 *            the repository to edit
	 * @param currentEventTypeSettings
	 *            current settings on server or null for offline mode
	 * @param version
	 */
	public TemplateManagerWizardPage(EventConfigurationRepository repository,
			IDescribedMap<EventOptionID> currentEventTypeSettings,
			Map<? extends IEventTypeID, ? extends IEventTypeInfo> eventTypeInfos, SchemaVersion version) {
		// FIXME: Include version in the title
		super("templateManager", //$NON-NLS-1$
				currentEventTypeSettings == null ? Messages.TEMPLATE_MANAGER_DIALOG_OFFLINE_TITLE
						: Messages.TEMPLATE_MANAGER_DIALOG_TITLE,
				ControlPanel.getDefault()
						.getImageDescriptor(ImageConstants.ICON_FLIGHT_RECORDING_CONFIGURATION_TEMPLATE));
		this.repository = repository;
		eventDefaults = currentEventTypeSettings;
		this.version = version;
		if (eventTypeInfos != null) {
			this.eventTypeInfos = eventTypeInfos;
		} else {
			this.eventTypeInfos = Collections.emptyMap();
		}
		if (eventDefaults == null) {
			// FIXME: Include version in the description
			setDescription(Messages.TEMPLATE_MANAGER_OFFLINE_DESCRIPTION);
		} else {
			setDescription(NLS.bind(Messages.TEMPLATE_MANAGER_DESCRIPTION, version.getDescription()));
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		tableViewer = new TableViewer(container, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI);
		tableViewer.setContentProvider(new TemplateProvider(null));
		tableViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new TemplateLabelProvider(true)));
		tableViewer.setInput(repository);
		tableViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableViewer.getTable().setData("name", "templateTable"); //$NON-NLS-1$ //$NON-NLS-2$

		Composite buttons = new Composite(container, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, true));
		layout = new GridLayout(1, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttons.setLayout(layout);

		createButtons(buttons);

		setControl(container);

		// Keep up to date
		setUpListeners();
	}

	protected void setUpListeners() {
		updateButtonsAccordingTo(tableViewer.getSelection());
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonsAccordingTo(event.getSelection());
			}
		});

		updateButtonsForRepository();
		repositoryObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if (tableViewer != null) {
					tableViewer.refresh();
				}
				updateButtonsForRepository();
			}
		};
		repository.addObserver(repositoryObserver);
	}

	private void updateButtonsAccordingTo(ISelection selection) {
		boolean oneSelected = false;
		boolean someSelected = !selection.isEmpty();
		boolean allDeletable = someSelected;
		boolean allCloneable = someSelected;
		boolean allRefreshable = someSelected && (version == SchemaVersion.V2);
		boolean oneEditable = false;
		boolean oneExportable = false;

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			oneSelected = (sel.size() == 1);
			oneEditable = oneSelected && ((IEventConfiguration) sel.getFirstElement()).isSaveable();
			oneExportable = oneSelected && ((IEventConfiguration) sel.getFirstElement()).isExportable();
			@SuppressWarnings("unchecked")
			Iterator<IEventConfiguration> templates = sel.iterator();

			while ((allDeletable || allCloneable || allRefreshable) && templates.hasNext()) {
				IEventConfiguration next = templates.next();
				allDeletable &= next.isDeletable();
				allCloneable &= next.isCloneable();
				allRefreshable &= next.isSaveable() && next.getVersion() == SchemaVersion.V2;
			}
		}

		editButton.setEnabled(oneEditable);
		exportButton.setEnabled(oneExportable);
		deleteButton.setEnabled(allDeletable);
		duplicateButton.setEnabled(allCloneable);
		refreshButton.setEnabled(allRefreshable);
	}

	private void updateButtonsForRepository() {
		if (newButton != null) {
			newButton.setEnabled(repository.canCreateTemplates());
		}
	}

	protected void createButtons(Composite buttons) {
		editButton = createButton(buttons, Messages.BUTTON_EDIT_TEXT);
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editSelected();
			}
		});

		createButton(buttons, Messages.BUTTON_IMPORT_FILE_TEXT).addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importUsingFileDialog();
			}
		});

		// Export to file system, not IDE workspace.
		exportButton = createButton(buttons, Messages.BUTTON_EXPORT_FILE_TEXT);
		exportButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportUsingFileDialog();
			}
		});

		deleteButton = createButton(buttons, Messages.BUTTON_REMOVE_TEXT);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteSelected();
			}
		});

		newButton = createButton(buttons, Messages.BUTTON_NEW_TEXT);
		newButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createNewTemplate();
			}
		});

		duplicateButton = createButton(buttons, Messages.BUTTON_DUPLICATE_TEXT);
		duplicateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				duplicateSelected();
			}
		});

		refreshButton = createButton(buttons, Messages.BUTTON_REFRESH_TEXT);
		refreshButton.setToolTipText(Messages.BUTTON_REFRESH_TOOLTIP);
		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshSelected();
			}
		});
	}

	private Button createButton(Composite container, String text) {
		Button button = new Button(container, SWT.NONE);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return button;
	}

	private void editSelected() {
		IEventConfiguration template = getSingleSelectedTemplate();
		if (template != null) {
			IEventConfiguration workingCopy = template.createWorkingCopy();
			// FIXME: Can't consider it online in the JFR IDE launch case.
			boolean online = (workingCopy.getVersion() == version);
			EventConfigurationModel model = EventConfigurationModel.create(workingCopy,
					online ? eventDefaults : ConfigurationToolkit.getEventOptions(workingCopy.getVersion()),
					online ? eventTypeInfos : Collections.emptyMap());
			Dialog dialog = TemplateEditPage.createDialogFor(getShell(), model, repository);
			if (dialog.open() == Window.OK) {
				// Contents of workingCopy (or a clone thereof) have already replaced the contents of template.
				// Only need to notify observers here (so we don't do that from a modal dialog).
				repository.notifyObservers();
			}
		}
	}

	private void importUsingFileDialog() {
		FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		fileDialog.setFilterExtensions(FILE_EXTENSIONS);
		fileDialog.setFilterNames(FILE_DESCRIPTIONS);
		if (fileDialog.open() == null) {
			// Dialog was cancelled. Bail out early to avoid handling that case later. Premature?
			return;
		}

		List<File> files = new ArrayList<>(fileDialog.getFileNames().length);
		File directory = new File(fileDialog.getFilterPath());
		for (String fileName : fileDialog.getFileNames()) {
			files.add(new File(directory, fileName));
		}

		List<IEventConfiguration> imported = TemplateToolkit.importFilesTo(repository, files);

		if (!imported.isEmpty()) {
			// Update all other observers, keeping their selections, if applicable.
			repository.notifyObservers();
			// Change *our* selection to only contain the newly imported templates.
			tableViewer.setSelection(new StructuredSelection(imported), true);
		}
	}

	private void exportUsingFileDialog() {
		IEventConfiguration template = getSingleSelectedTemplate();
		if (template == null) {
			return;
		}

		FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
		fileDialog.setFilterExtensions(FILE_EXTENSIONS);
		fileDialog.setFilterNames(FILE_DESCRIPTIONS);
		fileDialog.setOverwrite(true);
		// FIXME: Transform to valid filename? Remember directory?
		fileDialog.setFileName(template.getName());

		String fileName = fileDialog.open();
		if (fileName == null) {
			return;
		}
		try {
			template.exportToFile(new File(fileName));
		} catch (IOException ioe) {
			DialogToolkit.showExceptionDialogAsync(getControl().getDisplay(),
					Messages.IMPORT_EXPORT_TOOLKIT_COULD_NOT_EXPORT_DIALOG_TITLE, ioe);
		}
	}

	private IEventConfiguration getSingleSelectedTemplate() {
		ISelection selection = tableViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			return (IEventConfiguration) ((IStructuredSelection) selection).getFirstElement();
		}
		return null;
	}

	private Iterable<IEventConfiguration> getSelectedTemplates() {
		ISelection selection = tableViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			@SuppressWarnings("unchecked")
			List<IEventConfiguration> list = ((IStructuredSelection) selection).toList();
			return list;
		}
		return Collections.emptyList();
	}

	private void deleteSelected() {
		int selectedCount = 0;
		StringBuilder namesBuilder = new StringBuilder();
		for (IEventConfiguration template : getSelectedTemplates()) {
			if (selectedCount < MAX_REMOVE_TEMPLATES_TO_DISPLAY) {
				namesBuilder.append("\n"); //$NON-NLS-1$
				namesBuilder.append(template.getName());
			} else if (selectedCount == MAX_REMOVE_TEMPLATES_TO_DISPLAY) {
				namesBuilder.append("\n..."); //$NON-NLS-1$
			}
			selectedCount++;
		}
		if (selectedCount <= 0) {
			return;
		}

		boolean askConfirm = FlightRecorderUI.getDefault().getConfirmRemoveTemplate();
		boolean doDelete = false;
		if (askConfirm) {
			String messageText = NLS.bind(Messages.CONFIRM_REMOVE_TEMPLATE_DIALOG_MESSAGE_SINGULAR,
					namesBuilder.toString());
			if (selectedCount > 1) {
				messageText = NLS.bind(Messages.CONFIRM_REMOVE_TEMPLATE_DIALOG_MESSAGE_PLURAL, selectedCount,
						namesBuilder.toString());
			}
			MessageDialogWithToggle confirmDialog = MessageDialogWithToggle.openOkCancelConfirm(getShell(),
					Messages.CONFIRM_REMOVE_TEMPLATE_DIALOG_TITLE, messageText,
					Messages.CONFIRM_REMOVE_TEMPLATE_DIALOG_CHECKBOX_ASK_CONFIRMATION, true, null, null);
			if (confirmDialog.getReturnCode() == IDialogConstants.OK_ID) {
				doDelete = true;
				if (!confirmDialog.getToggleState()) {
					FlightRecorderUI.getDefault().setConfirmRemoveTemplate(false);
				}
			}
		} else {
			doDelete = true;
		}
		if (doDelete) {
			for (IEventConfiguration template : getSelectedTemplates()) {
				if (template.delete()) {
					repository.remove(template);
				}
			}
			repository.notifyObservers();
		}
	}

	private void duplicateSelected() {
		List<IEventConfiguration> added = new ArrayList<>();
		for (IEventConfiguration template : getSelectedTemplates()) {
			try {
				IEventConfiguration newTemplate = template.createCloneWithStorage(PrivateStorageDelegate.getDelegate());
				if (repository.addAsUnique(newTemplate)) {
					added.add(newTemplate);
				}
			} catch (IOException e) {
				ControlPanel.getDefault().getLogger().log(Level.WARNING,
						"Got exception when duplicating template " + template.getName(), e); //$NON-NLS-1$
			}
		}
		if (!added.isEmpty()) {
			repository.notifyObservers();
			// Change *our* selection to only contain the newly duplicated templates.
			tableViewer.setSelection(new StructuredSelection(added), true);
		}
	}

	private void refreshSelected() {
		for (IEventConfiguration template : getSelectedTemplates()) {
			pushServerMetadataToConfiguration(template);
		}
	}

	private void pushServerMetadataToConfiguration(IEventConfiguration configuration) {
		EventConfigurationModel.pushServerMetadataToLocalConfiguration(configuration, eventDefaults, eventTypeInfos,
				false);
	}

	private void createNewTemplate() {
		try {
			IEventConfiguration newTemplate = repository.createTemplate();
			if ((newTemplate != null) && repository.addAsUnique(newTemplate)) {
				repository.notifyObservers();
				// Change *our* selection to only contain the newly created template.
				tableViewer.setSelection(new StructuredSelection(newTemplate), true);
			}
		} catch (IOException ioe) {
			DialogToolkit.showExceptionDialogAsync(getControl().getDisplay(),
					Messages.TEMPLATE_MANAGER_COULD_NOT_CREATE_TEMPLATE_ERROR_DIALOG_TITLE, ioe);
		}
	}

	// FIXME: Verify that this is actually called, or we might leak.
	@Override
	public void dispose() {
		repository.deleteObserver(repositoryObserver);
		super.dispose();
	}
}
