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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.openjdk.jmc.alert.AlertPlugin;
import org.openjdk.jmc.console.ui.actions.MBeanAutomaticRefreshAction;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.console.ui.notification.wizard.RuleExportWizard;
import org.openjdk.jmc.console.ui.notification.wizard.RuleImportWizard;
import org.openjdk.jmc.console.ui.notification.wizard.RuleWizardDialog;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.IRefreshable;
import org.openjdk.jmc.ui.misc.MCSectionPart;

/**
 * TriggerSectionPart
 */
public class TriggerSectionPart extends MCSectionPart {
	private Button m_addButton;
	private Button m_deleteButton;
	private CheckboxTreeViewer m_viewer;
	private Button m_exportButton;
	private Button m_importButton;
	private Button m_renameButton;
	private Button m_resetButton;

	private static final int MAX_REMOVE_TRIGGERS_TO_DISPLAY = 5;
	private final IConnectionHandle connection;

	private final IRefreshable refresher = new IRefreshable() {

		@Override
		public boolean refresh() {
			DisplayToolkit.safeAsyncExec(new Runnable() {
				@Override
				public void run() {
					if (!m_viewer.getControl().isDisposed()) {
						m_viewer.refresh();
					}
				}
			});
			return false;
		}

	};

	private final MBeanAutomaticRefreshAction refreshAction = new MBeanAutomaticRefreshAction(refresher);
	private final NotificationRegistry model;

	private static class ShowAlertsButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			AlertPlugin.getDefault().showDialog(true);
		}
	}

	public TriggerSectionPart(Composite parent, FormToolkit toolkit, NotificationRegistry model,
			IConnectionHandle connection) {
		super(parent, toolkit, MCSectionPart.DEFAULT_TITLE_DESCRIPTION_STYLE);
		this.model = model;
		this.connection = connection;
		getSection().setText(Messages.TriggerSectionPart_SECTION_TEXT);
		getSection().setDescription(Messages.TriggerSectionPart_SECTION_DESCRIPTION);
		createClient(toolkit);

		setupRefreshAction();
		setupExpansionStateHandling();
	}

	@Override
	public void dispose() {
		super.dispose();
		connection.getServiceOrDummy(IMBeanHelperService.class).removeMBeanServerChangeListener(refreshAction);
	}

	private Object getFirstSelected() {
		IStructuredSelection selection = (IStructuredSelection) m_viewer.getSelection();
		return selection.getFirstElement();
	}

	private void selectRule(TriggerRule rule) {
		IStructuredSelection s = new StructuredSelection(new Object[] {rule});
		m_viewer.setSelection(s, true);
	}

	private void createClient(FormToolkit toolkit) {
		Section section = getSection();
		Composite client = toolkit.createComposite(section, SWT.WRAP);
		Color bgColor = section.getBackground();

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 2;

		GridData gd = null;

		Tree tree = toolkit.createTree(client, SWT.CHECK);
		tree.setData("name", "triggers.RulesTree"); //$NON-NLS-1$ //$NON-NLS-2$
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		m_viewer = createViewer(toolkit, tree, client);

		tree.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		Composite buttons = createButtons(toolkit, client);
		buttons.setBackground(bgColor);
		buttons.setLayoutData(gd);

		client.setBackground(bgColor);
		client.setLayout(layout);
		bgColor.dispose();

		section.setClient(client);
	}

	private Composite createButtons(FormToolkit toolkit, Composite client) {
		Composite buttonContainer = toolkit.createComposite(client, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;

		GridData gd1 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		m_addButton = createAddButton(toolkit, buttonContainer);
		m_addButton.setLayoutData(gd1);

		GridData gd3 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		m_renameButton = createRenameButton(toolkit, buttonContainer);
		m_renameButton.setLayoutData(gd3);

		GridData gd4 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		m_deleteButton = createDeleteButton(toolkit, buttonContainer);
		m_deleteButton.setLayoutData(gd4);

		GridData gd6 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		gd6.verticalIndent = 10;
		m_importButton = createImport(toolkit, buttonContainer);
		m_importButton.setLayoutData(gd6);

		GridData gd7 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		m_exportButton = createExport(toolkit, buttonContainer);
		m_exportButton.setLayoutData(gd7);

		GridData gd8 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		m_resetButton = createReset(toolkit, buttonContainer);
		m_resetButton.setLayoutData(gd8);

		GridData gd9 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		gd9.verticalIndent = 10;
		Button showAlertButton = createShowAlertButton(toolkit, buttonContainer);
		showAlertButton.setLayoutData(gd9);

		buttonContainer.setLayout(layout);

		addSelectionListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selected = getFirstSelected();
				boolean enableButtons = selected instanceof TriggerRule || selected instanceof RuleGroup;
				m_deleteButton.setEnabled(enableButtons);
				m_renameButton.setEnabled(enableButtons);
			}
		});

		return buttonContainer;
	}

	private Button createReset(FormToolkit toolkit, final Composite buttonContainer) {
		final Button button = toolkit.createButton(buttonContainer,
				Messages.TriggerSectionPart_TRIGGERS_RESET_BUTTON_TEXT, SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (DialogToolkit.openQuestionOnUiThread(Messages.TriggerSectionPart_RESET_TITLE_TEXT,
						Messages.TriggerSectionPart_RESET_TO_DEFAULT_QUESTION_TEXT)) {
					IStatus status = TriggerToolkit.resetTriggers(model);
					if (status.getSeverity() != IStatus.OK) {
						ErrorDialog.openError(getSection().getShell(),
								Messages.TriggerSectionPart_ERROR_MESSAGE_RESETTING_TRIGGERS, null, status);
					}
					updateTree();
				}
			}
		});

		return button;
	}

	private void updateTree() {
		m_viewer.refresh();
		m_exportButton.setEnabled(m_viewer.getTree().getItemCount() > 0);
	}

	private void addNewRule() {
		Display display = Display.getCurrent();
		if (display.isDisposed()) {
			return;
		}
		Shell shell = display.getActiveShell();
		if (shell == null) {
			return;
		}

		IStructuredSelection selection = (IStructuredSelection) m_viewer.getSelection();
		String ruleGroupName = Messages.TriggerSectionPart_DEFAULT_RULES_GROUP_NAME_TEXT;
		if (!selection.isEmpty()) {
			Object object = selection.iterator().next();
			if (object instanceof RuleGroup) {
				ruleGroupName = ((RuleGroup) object).getName();
			}
			if (object instanceof TriggerRule) {
				ruleGroupName = ((TriggerRule) object).getRulePath();
			}
		}
		RuleWizardDialog rwd = new RuleWizardDialog(shell, null, connection, ruleGroupName, model);

		if (rwd.open() != Window.OK) {
			return;
		}

		TriggerRule rule = rwd.getNewRule();
		model.addNotificationRule(rule);

		updateTree();
		selectRule(rule);
	}

	private void rename(final RuleGroup group) {
		// OK, to rename to an existing name. They will merge.
		InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
				Messages.TriggerSectionPart_RENAME_RULE_GROUP_TITLE,
				Messages.TriggerSectionPart_ENTER_NEW_GROUP_NAME_TEXT, group.getName(), null);
		if (dialog.open() == Window.OK) {
			String newName = dialog.getValue();
			for (TriggerRule rule : group.getRules()) {
				rule.setRulePath(newName);
			}
		}
		updateTree();
	}

	private void rename(final TriggerRule rule) {
		InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
				Messages.TriggerSectionPart_DIALOG_RENAME_RULE_TITLE,
				Messages.TriggerSectionPart_DIALOG_RENAME_RULE_MESSAGE_TEXT, rule.getName(), new IInputValidator() {
					@Override
					public String isValid(String newText) {
						for (TriggerRule aRule : model.getAvailableRules()) {
							if (aRule.getName().equals(newText) && aRule != rule) {
								return Messages.TriggerSectionPart_DIALOG_RULE_EXISTS_MESSAGE_TEXT;
							}
						}
						return null;
					}
				});
		if (dialog.open() == Window.OK) {
			String newName = dialog.getValue();
			if (!rule.getName().equals(newName)) {
				rule.setName(newName);
			}
		}
		m_viewer.refresh();
	}

	private Button createShowAlertButton(FormToolkit toolkit, Composite buttonContainer) {
		Button button = toolkit.createButton(buttonContainer, Messages.TriggerSectionPart_BUTTON_SHOW_ALERTS_TEXT0,
				SWT.NONE);
		button.setImage(NotificationPlugin.getDefault().getImage(NotificationPlugin.IMG_ALERT_OBJ));
		button.addSelectionListener(new ShowAlertsButtonSelectionListener());
		return button;
	}

	private Button createAddButton(FormToolkit toolkit, Composite buttonContainer) {
		Button button = toolkit.createButton(buttonContainer, Messages.TriggerSectionPart_BUTTON_ADD_TEXT, SWT.PUSH);
		button.setToolTipText(Messages.TriggerSectionPart_BUTTON_ADD_TOOLTIP);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addNewRule();
			}
		});

		return button;
	}

	private Button createImport(FormToolkit toolkit, final Composite buttonContainer) {
		Button button = toolkit.createButton(buttonContainer, Messages.TriggerSectionPart_IMPORT_TRIGGERS_BUTTON_TEXT,
				SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showWizardPage(buttonContainer.getShell(), new RuleImportWizard());
				updateTree();
			}
		});

		return button;
	}

	private Button createExport(FormToolkit toolkit, final Composite buttonContainer) {
		Button button = toolkit.createButton(buttonContainer,
				Messages.TriggerSectionPart_EXPORT_TRIGGER_BUTTON_TEXT_NAME, SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showWizardPage(buttonContainer.getShell(), new RuleExportWizard());
			}
		});

		return button;
	}

	private void showWizardPage(Shell shell, IWorkbenchWizard wizard) {
		wizard.init(PlatformUI.getWorkbench(), (IStructuredSelection) m_viewer.getSelection());
		WizardDialog wd = new WizardDialog(shell, wizard);
		wd.open();
	}

	private Button createRenameButton(FormToolkit toolkit, Composite buttonContainer) {
		Button button = toolkit.createButton(buttonContainer, Messages.TriggerSectionPart_BUTTON_RENAME_TEXT, SWT.PUSH);
		button.setToolTipText(Messages.TriggerSectionPart_TOOLTIP_RENAME_RULE_TEXT);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object selected = getFirstSelected();
				if (selected instanceof TriggerRule) {
					rename((TriggerRule) selected);
				}
				if (selected instanceof RuleGroup) {
					rename((RuleGroup) selected);
				}
			}
		});

		return button;
	}

	private boolean askConfirmation(Shell shell, TriggerRule ... rules) {
		LinkedList<String> names = new LinkedList<>();
		for (TriggerRule rule : rules) {
			names.add(rule.getName());
		}
		Collections.sort(names);

		StringBuilder namesBuilder = new StringBuilder();
		int index = 0;
		for (String name : names) {
			if (index < MAX_REMOVE_TRIGGERS_TO_DISPLAY) {
				namesBuilder.append("\n"); //$NON-NLS-1$
				namesBuilder.append(name);
			} else if (index == MAX_REMOVE_TRIGGERS_TO_DISPLAY) {
				namesBuilder.append("\n..."); //$NON-NLS-1$
				break;
			}
			index++;
		}
		String messageText = NLS.bind(Messages.TriggerSectionPart_CONFIRM_REMOVE_TRIGGER_SINGULAR,
				namesBuilder.toString());
		if (names.size() > 1) {
			messageText = NLS.bind(Messages.TriggerSectionPart_CONFIRM_REMOVE_TRIGGER_PLURAL, names.size(),
					namesBuilder.toString());
		}
		return MessageDialog.openConfirm(shell, Messages.TriggerSectionPart_CONFIRM_REMOVE_TITLE, messageText);
	}

	private void askConfirmationAndDelete(Shell shell, TriggerRule ... rules) {
		if (rules.length > 0 && askConfirmation(shell, rules)) {
			for (TriggerRule rule : rules) {
				model.removeNotificationRule(rule);
			}
		}
	}

	private Button createDeleteButton(FormToolkit toolkit, final Composite buttonContainer) {
		Button button = toolkit.createButton(buttonContainer, Messages.TriggerSectionPart_BUTTON_DELETE_TEXT, SWT.PUSH);
		button.setToolTipText(Messages.TriggerSectionPart_BUTTON_DELETE_TOOLTIP);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object selected = getFirstSelected();
				if (selected instanceof TriggerRule) {
					askConfirmationAndDelete(buttonContainer.getShell(), (TriggerRule) selected);
				} else if (selected instanceof RuleGroup) {
					List<TriggerRule> rules = ((RuleGroup) (selected)).getRules();
					askConfirmationAndDelete(buttonContainer.getShell(), rules.toArray(new TriggerRule[rules.size()]));
				}
				updateTree();
			}
		});

		return button;
	}

	private void setupRefreshAction() {
		connection.getServiceOrDummy(IMBeanHelperService.class).addMBeanServerChangeListener(refreshAction);
		getMCToolBarManager().add(refreshAction);
	}

	private String getServerGuid() {
		return connection.getServerDescriptor().getGUID();
	}

	private void setupExpansionStateHandling() {
		TriggerToolkit.retrieveExpansionState(m_viewer, model);
		// needed so the preferences always are up to with state from the start
		TriggerToolkit.storeExpansionState(m_viewer);
		// There is no way to hook in so the expansion state can be stored just at the end.
		// We need to store it all the time
		m_viewer.getTree().getParent().addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				TriggerToolkit.storeExpansionState(m_viewer);
			}
		});
	}

	private CheckboxTreeViewer createViewer(FormToolkit toolkit, Tree tree, Composite client) {
		IMRIService mriService = connection.getServiceOrNull(IMRIService.class);
		RuleCheckedStateProvider ruleStateProvider = new RuleCheckedStateProvider(mriService, getServerGuid(), model);
		CheckboxTreeViewer viewer = new ContainerCheckedTreeViewer(tree);
		viewer.setContentProvider(new TriggerContentProvider(model));
		viewer.setComparator(new ViewerComparator());
		viewer.setLabelProvider(new TriggerLabelProvider(ruleStateProvider));
		viewer.setInput(model);
		viewer.setCheckStateProvider(ruleStateProvider);
		viewer.addCheckStateListener(ruleStateProvider);
		return viewer;
	}

	public void addSelectionListener(ISelectionChangedListener l) {
		m_viewer.addSelectionChangedListener(l);
	}

	public IRefreshable getRefresher() {
		return refresher;
	}

}
