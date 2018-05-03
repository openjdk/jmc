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
package org.openjdk.jmc.rjmx.ui.operations;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.util.ExceptionToolkit;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.attributes.AttributeTreeBuilder;
import org.openjdk.jmc.rjmx.ui.internal.IconConstants;
import org.openjdk.jmc.rjmx.util.internal.DefaultAttribute;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MementoToolkit;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

public class ExecuteOperationForm {

	private final FormToolkit formToolkit;
	private final Text commandText;
	private final CTabFolder results;
	private final Button invokeButton;
	private final InvocatorBuilderForm invocatorForm;
	private Callable<?> invocator;
	private final Composite buttonContainer;
	public static final String RESULT_TAB_NAME = "operation.result"; //$NON-NLS-1$
	public static final String RESULT_TREE_NAME = "operation.result.tree"; //$NON-NLS-1$

	public ExecuteOperationForm(SashForm parent, FormToolkit formToolkit, boolean showOperationReturnType,
			IMemento state) {
		this.formToolkit = formToolkit;
		parent.setBackground(formToolkit.getColors().getBackground());
		Composite upperContainer = formToolkit.createComposite(parent);
		upperContainer.setLayout(MCLayoutFactory.createMarginFreeFormPageLayout());
		SashForm invocatorFormSash = new SashForm(upperContainer, SWT.HORIZONTAL);
		invocatorForm = new InvocatorBuilderForm(invocatorFormSash, formToolkit, showOperationReturnType,
				TableSettings.forState(MementoToolkit.asState(state)),
				new InvocatorBuilderForm.InvocatorUpdateListener() {
					@Override
					public void onInvocatorUpdated(IOperation operation, Callable<?> invocator) {
						ExecuteOperationForm.this.invocator = invocator;
						buttonContainer.setVisible(operation != null);
						invokeButton.setEnabled(invocator != null);
						commandText.setText(invocator == null ? "" : invocator.toString()); //$NON-NLS-1$
						invokeButton.setImage(OperationsLabelProvider.getOperationIcon(operation));
						buttonContainer.layout(true);
					}
				});
		invocatorFormSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		buttonContainer = formToolkit.createComposite(upperContainer);
		RowLayout buttonLayout = new RowLayout();
		buttonLayout.center = true;
		buttonLayout.wrap = false;
		buttonContainer.setLayout(buttonLayout);
		buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		buttonContainer.setVisible(false);

		invokeButton = formToolkit.createButton(buttonContainer, Messages.ExecuteOperationForm_EXECUTE_BUTTON_TEXT,
				SWT.PUSH);
		commandText = new Text(buttonContainer, SWT.READ_ONLY | SWT.WRAP);
		commandText.setBackground(formToolkit.getColors().getBackground());

		results = new CTabFolder(parent, SWT.CLOSE | SWT.NO_BACKGROUND);
		results.setBackground(formToolkit.getColors().getBackground());
		results.setVisible(false);
		results.setData("name", RESULT_TAB_NAME); //$NON-NLS-1$
		results.addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void close(CTabFolderEvent event) {
				if (results.getItemCount() <= 1) {
					results.setVisible(false);
					results.getParent().layout();
				}
			}
		});
		addResultsContextMenu();
		parent.setWeights(new int[] {2, 1});
		invokeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				executeOperation();
			}
		});
	}

	protected void executeOperation() {
		invokeAsync(invocator);
	}

	public void saveState(IMemento state) {
		invocatorForm.saveState(state);
	}

	protected Button createButton(String label) {
		Button button = formToolkit.createButton(buttonContainer, label, SWT.PUSH);
		button.moveAbove(invokeButton);
		return button;
	}

	protected void invokeAsync(final Callable<?> job) {
		new Job(job.toString()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Object result;
				String resultTitle;
				Image resultImage = null;
				try {
					result = job.call();
					resultTitle = NLS.bind(Messages.ExecuteOperationForm_RESULT_MSG, getCurrentTimeFormatted(), job);
				} catch (final Exception e) {
					resultTitle = NLS.bind(Messages.ExecuteOperationForm_FAILED_EXECUTION_MSG,
							getCurrentTimeFormatted(), job);
					resultImage = RJMXUIPlugin.getDefault().getImage(IconConstants.ICON_ERROR);
					result = NLS.bind(Messages.ExecuteOperationForm_FAILED_TO_EXECUTE_MSG, job) + "\n\n" //$NON-NLS-1$
							+ ExceptionToolkit.toString(e);
				}
				showAsyncInvocationResult(monitor, resultTitle, resultImage, result);
				return Status.OK_STATUS;
			}

			private String getCurrentTimeFormatted() {
				return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date());
			}

			private void showAsyncInvocationResult(
				IProgressMonitor monitor, final String resultTitle, final Image resultImage, final Object result) {
				if (!monitor.isCanceled()) {
					DisplayToolkit.safeAsyncExec(new Runnable() {
						@Override
						public void run() {
							if (!results.isDisposed()) {
								createInvocationResult(resultTitle, resultImage, result);
							}
						}
					});
				}
			}
		}.schedule();
	}

	private void createInvocationResult(String resultTitle, Image resultImage, Object result) {
		final CTabItem tab = new CTabItem(results, SWT.NONE);
		tab.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				Control c = tab.getControl();
				if (c != null && !c.isDisposed()) {
					c.dispose();
				}
			}
		});
		tab.setText(resultTitle);
		if (resultImage != null) {
			tab.setImage(resultImage);
		}
		Object[] val = new DefaultAttribute(null, result).getChildren().toArray();
		if (val.length > 0) {
			Tree tree = new Tree(results, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			tree.setData("name", RESULT_TREE_NAME); //$NON-NLS-1$
			tab.setControl(tree);

			TreeViewer viewer = new TreeViewer(tree);
			viewer.setContentProvider(new TreeStructureContentProvider());
			ColumnViewerToolTipSupport.enableFor(viewer);

			List<ColumnSettings> columnSettings = new ArrayList<>();
			columnSettings.add(new ColumnSettings(AttributeTreeBuilder.NAME.getId(), false, 300, null));
			columnSettings.add(new ColumnSettings(AttributeTreeBuilder.VALUE.getId(), false, 300, null));
			columnSettings.add(new ColumnSettings(AttributeTreeBuilder.TYPE.getId(), true, 150, null));
			TableSettings tableSettings = new TableSettings(AttributeTreeBuilder.NAME.getId(), columnSettings);
			ColumnManager.build(viewer,
					Arrays.asList(AttributeTreeBuilder.NAME, AttributeTreeBuilder.VALUE, AttributeTreeBuilder.TYPE),
					tableSettings);

			viewer.setInput(val);
		} else {
			String resultValue = ""; //$NON-NLS-1$
			if (result != null) {
				resultValue = (result.getClass().isArray()) ? Arrays.toString((Object[]) result) : result.toString();
			}
			Text textResult = formToolkit.createText(results, resultValue, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
			tab.setControl(textResult);
		}
		results.setVisible(true);
		results.setSelection(tab);
		results.getParent().layout();
	}

	private void addResultsContextMenu() {
		MenuManager mm = new MenuManager();
		mm.add(new Action(Messages.ExecuteOperationForm_CLOSE_ALL_LABEL) {
			@Override
			public void runWithEvent(Event event) {
				while (results.getItemCount() != 0) {
					results.getItem(0).dispose();
				}
				results.setVisible(false);
				results.getParent().layout();
			}
		});
		Menu menu = mm.createContextMenu(results);
		results.setMenu(menu);
	}

	public void setOperations(Collection<? extends IOperation> input) {
		invocatorForm.setOperations(input);
	}

	protected IOperation getSelectedOperation() {
		return invocatorForm.getSelectedOperation();
	}
}
