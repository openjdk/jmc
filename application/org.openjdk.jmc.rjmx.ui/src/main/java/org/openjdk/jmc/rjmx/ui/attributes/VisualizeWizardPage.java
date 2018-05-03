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
package org.openjdk.jmc.rjmx.ui.attributes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.SectionPart;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.internal.CombinedChartSectionPart;
import org.openjdk.jmc.rjmx.ui.internal.IconConstants;
import org.openjdk.jmc.rjmx.ui.internal.NewChartAction;
import org.openjdk.jmc.rjmx.ui.internal.SectionPartManager;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;

/**
 * Wizard page that creates a chart.
 * <p>
 * In the future this wizard page can be part of a wizard where the user can go through series of
 * steps to visualize an attribute
 * <ol>
 * <li>Create a new FormPage</li>
 * <li>Create a new section</><li>Set the type visualizer, dial, graph etc.</li>
 * <li>Set the formatting, units, multipliers.</li>
 * </ol>
 */
public final class VisualizeWizardPage extends WizardPage implements IPerformFinishable {
	static class SectionPartLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			return ((SectionPart) element).getSection().getText();
		}
	}

	static class SectionPartContentProvider extends AbstractStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			ArrayList<IFormPart> list = new ArrayList<>();
			SectionPartManager manager = (SectionPartManager) inputElement;
			for (IFormPart visualizer : manager.getParts()) {
				if (visualizer instanceof CombinedChartSectionPart) {
					list.add(visualizer);
				}
			}
			return list.toArray();
		}
	}

	final private SectionPartManager m_sectionPartManager;
	private ListViewer m_listViewer;
	private final List<MRI> m_attributes;
	private final IConnectionHandle m_connection;

	public VisualizeWizardPage(SectionPartManager sectionPartManager, List<MRI> attributes, IConnectionHandle ch) {
		super(Messages.VisualizeWizardPage_CREATE_CHART_TITLE_TEXT);
		setImageDescriptor(RJMXUIPlugin.getDefault().getMCImageDescriptor(IconConstants.IMG_TOOLBAR_OVERVIEW));
		setDescription(Messages.VisualizeWizardPage_SELECT_CHART_TEXT);
		setTitle(Messages.VisualizeWizardPage_CREATE_CHART_TITLE_TEXT);

		// We only support the overview chart. In the future we could show a
		// tree of tabs and section parts.
		m_sectionPartManager = sectionPartManager;
		m_attributes = attributes;
		m_connection = ch;
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);

		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, true);
		m_listViewer = createViewer(container);
		m_listViewer.getControl().setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		Control buttons = createButtonContainer(container);
		buttons.setLayoutData(gd2);

		container.setLayout(new GridLayout(2, false));
		updatePageComplete(true);
		setControl(container);
	}

	private void updatePageComplete(boolean firstTime) {
		boolean hasCharts = !getCharts().isEmpty();
		setPageComplete(hasCharts);
		if (!hasCharts && !firstTime) {
			setErrorMessage(Messages.VisualizeWizardPage_ERROR_TEXT_ONE_CHART_MUST_BE_SELECTED_TEXT);
		} else {
			setErrorMessage(null);
		}
	}

	private Control createButtonContainer(Composite parent) {
		Button addButton = new Button(parent, SWT.NONE);
		addButton.setText(Messages.VisualizeWizardPage_CREATE_CHART_BUTTON_TEXT);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String newChartName = requestChartName();
				if (newChartName == null) {
					return;
				}
				Action action = new NewChartAction(m_sectionPartManager, m_connection, newChartName);
				action.run();
				m_listViewer.refresh();
				selectLastListItem();
				updatePageComplete(false);
			}

			private String requestChartName() {
				String suggestion = Messages.VisualizeWizardPage_DEFAULT_CHART_NAME;
				for (MRI mri : m_attributes) {
					suggestion = mri.getDataPath();
					break;
				}
				InputDialog id = new InputDialog(getShell(), Messages.VisualizeWizardPage_TITLE_NEW_CHART,
						Messages.VisualizeWizardPage_DESCRIPTION_NEW_CHART, suggestion, new IInputValidator() {
							@Override
							public String isValid(String newText) {
								if (newText == null || newText.length() <= 0) {
									return Messages.VisualizeWizardPage_INVALID_CHART_NAME;
								}
								return null;
							}
						});
				id.open();
				if (id.getReturnCode() == SWT.CANCEL) {
					return null;
				}
				return id.getValue();
			}

			/**
			 * Tries to get all available items from the content provider and selects the last one.
			 * This will of course look strange if there is an sorter present...
			 */
			private void selectLastListItem() {
				IStructuredContentProvider listContentProvider = (IStructuredContentProvider) m_listViewer
						.getContentProvider();
				Object[] elements = listContentProvider.getElements(m_sectionPartManager);
				m_listViewer.setSelection(new StructuredSelection(elements[elements.length - 1]));
			}
		});
		return addButton;
	}

	private ListViewer createViewer(Composite container) {
		ListViewer sectionPartViewer = new ListViewer(container);
		sectionPartViewer.setContentProvider(new SectionPartContentProvider());
		sectionPartViewer.setLabelProvider(new SectionPartLabelProvider());
		sectionPartViewer.setInput(m_sectionPartManager);
		sectionPartViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updatePageComplete(false);
			}
		});
		return sectionPartViewer;
	}

	@Override
	public boolean performFinish() {
		for (CombinedChartSectionPart part : getCharts()) {
			for (MRI mri : m_attributes) {
				part.add(mri);
			}
		}
		return true;
	}

	private List<CombinedChartSectionPart> getCharts() {
		ArrayList<CombinedChartSectionPart> list = new ArrayList<>();
		IStructuredSelection selection = (IStructuredSelection) m_listViewer.getSelection();
		for (Iterator<?> it = selection.iterator(); it.hasNext();) {
			Object visualizer = it.next();
			if (visualizer instanceof CombinedChartSectionPart) {
				list.add((CombinedChartSectionPart) visualizer);
			}
		}
		return list;
	}
}
