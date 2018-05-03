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

import static org.openjdk.jmc.rjmx.ui.attributes.Messages.AttributeInspector_VALUE_COLUMN_HEADER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.services.IAttribute;
import org.openjdk.jmc.rjmx.services.IAttributeInfo;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.services.IReadOnlyAttribute;
import org.openjdk.jmc.rjmx.services.IllegalOperandException;
import org.openjdk.jmc.rjmx.ui.attributes.AttributeTreeBuilder;
import org.openjdk.jmc.rjmx.ui.attributes.ChangeValueAction;
import org.openjdk.jmc.rjmx.ui.attributes.ValueColumnLabelProvider;
import org.openjdk.jmc.rjmx.ui.celleditors.AttributeEditingSupport;
import org.openjdk.jmc.rjmx.ui.internal.InsertArrayElementMenuAction;
import org.openjdk.jmc.rjmx.ui.internal.RemoveArrayElementMenuAction;
import org.openjdk.jmc.rjmx.util.internal.DefaultAttribute;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.MementoToolkit;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

public class InvocatorBuilderForm {

	public interface InvocatorUpdateListener {
		void onInvocatorUpdated(IOperation operation, Callable<?> invocator);
	}

	private List<IAttribute> attributes;
	private final TableViewer operationsPart;
	private final InvocatorUpdateListener invocatorUpdatedListener;
	private Set<IAttributeInfo> invalidValues = Collections.emptySet();
	private final ColumnManager paramsColumnManager;

	private class EditingSupport extends AttributeEditingSupport<IAttribute> {

		public EditingSupport(ColumnViewer viewer) {
			super(viewer, IAttribute.class);
		}

		@Override
		protected void setValue(Object element, Object value) {
			super.setValue(element, value);
			evaluateOperation(getSelectedOperation());
			getViewer().refresh(element);
		}
	};

	private class ValueLabelProvider extends ValueColumnLabelProvider {

		@Override
		protected Color getForegroundTyped(IReadOnlyAttribute value) {
			if (invalidValues.contains(value.getInfo())) {
				return JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR);
			}
			return super.getForegroundTyped(value);
		}

		@Override
		protected String getToolTipTextTyped(IReadOnlyAttribute attribute) {
			if (invalidValues.contains(attribute.getInfo())) {
				return NLS.bind(Messages.InvocatorBuilderForm_ILLEGAL_OPERAND,
						TypeHandling.simplifyType(attribute.getInfo().getType()));
			}
			return super.getToolTipTextTyped(attribute);
		}

		@Override
		protected Color getBackgroundTyped(IReadOnlyAttribute element) {
			return Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		}
	}

	private class ArgumentAttribute extends DefaultAttribute {

		private final IOperation operation;

		public ArgumentAttribute(IOperation operation, IAttributeInfo info) {
			super(info);
			this.operation = operation;
		}

		@Override
		public void setValue(Object value) {
			super.setValue(value);
			evaluateOperation(operation);
		}
	}

	public InvocatorBuilderForm(SashForm parent, FormToolkit formToolkit, boolean showOperationReturnType,
			TableSettings tableSettings, InvocatorUpdateListener invocatorUpdatedListener) {
		this.invocatorUpdatedListener = invocatorUpdatedListener;
		parent.setBackground(formToolkit.getColors().getBackground());
		operationsPart = createIOperationList(parent, showOperationReturnType);
		operationsPart.getTable().setHeaderVisible(true);

		Tree paramsTree = new Tree(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		final TreeViewer parametersViewer = new TreeViewer(paramsTree);
		parametersViewer.setContentProvider(new TreeStructureContentProvider());
		ColumnViewerToolTipSupport.enableFor(parametersViewer);

		IColumn valueColumn = new ColumnBuilder(AttributeInspector_VALUE_COLUMN_HEADER, "value", //$NON-NLS-1$
				AttributeTreeBuilder.VALUE_CELL_ACCESSOR).labelProvider(new ValueLabelProvider())
						.editingSupport(new EditingSupport(parametersViewer)).build();
		List<IColumn> paramColumns = Arrays.asList(AttributeTreeBuilder.NAME, valueColumn, AttributeTreeBuilder.TYPE,
				AttributeTreeBuilder.DESCRIPTION);
		paramsColumnManager = ColumnManager.build(parametersViewer, paramColumns, tableSettings);

		MCContextMenuManager paramsMenu = MCContextMenuManager.create(paramsTree, null);
		ColumnMenusFactory.addDefaultMenus(paramsColumnManager, paramsMenu);

		paramsMenu.add(new ChangeValueAction(parametersViewer, paramsColumnManager, valueColumn));
		paramsMenu.add(InsertArrayElementMenuAction.createInsertArrayElementMenuActionContribution(paramsMenu,
				paramsColumnManager, valueColumn, false));
		paramsMenu.add(InsertArrayElementMenuAction.createInsertArrayElementMenuActionContribution(paramsMenu,
				paramsColumnManager, valueColumn, true));
		paramsMenu.add(RemoveArrayElementMenuAction.createRemoveArrayElementMenuActionContribution(paramsMenu,
				paramsColumnManager, valueColumn));
		IAction delAction = ActionToolkit.forListSelection(parametersViewer, null, true, 1,
				(List<IAttribute> selection) -> {
					selection.stream().forEach(a -> a.setValue(null));
					evaluateOperation(getSelectedOperation());
				});
		ActionToolkit.convertToCommandAction(delAction, IWorkbenchCommandConstants.EDIT_DELETE);
		paramsMenu.appendToGroup(MCContextMenuManager.GROUP_EDIT, delAction);
		InFocusHandlerActivator.install(paramsTree, delAction);

		parent.setWeights(new int[] {1, 2});
		operationsPart.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IOperation operation = getSelectedOperation();
				attributes = new ArrayList<>();
				if (operation != null) {
					for (IAttributeInfo valueInfo : operation.getSignature()) {
						attributes.add(new ArgumentAttribute(operation, valueInfo));
					}
				}
				evaluateOperation(operation);
				parametersViewer.setInput(attributes.toArray());
			}
		});

	}

	private static TableViewer createIOperationList(Composite parent, boolean showReturns) {
		TableViewer tableViewer = new TableViewer(new Composite(parent, SWT.NONE),
				SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				if (element instanceof IOperation) {
					return ((IOperation) element).getImpact().ordinal();
				}
				return 0;
			}
		});
		TableColumn tvc = new TableColumn(tableViewer.getTable(), SWT.NONE);
		tvc.setText(Messages.OperationsSectionPart_TITLE);
		TableColumnLayout l = new TableColumnLayout();
		tableViewer.getTable().getParent().setLayout(l);
		l.setColumnData(tvc, new ColumnWeightData(1));
		tableViewer.setLabelProvider(new OperationsLabelProvider(showReturns));
		ColumnViewerToolTipSupport.enableFor(tableViewer);
		return tableViewer;
	}

	public void setOperations(Collection<? extends IOperation> input) {
		operationsPart.setInput(input);
	}

	private void evaluateOperation(IOperation operation) {
		try {
			Callable<?> invocator = operation == null ? null : operation.getInvocator(getValues());
			invocatorUpdatedListener.onInvocatorUpdated(operation, invocator);
			invalidValues = Collections.emptySet();
		} catch (IllegalOperandException e) {
			invalidValues = e.getInvalidValues();
			invocatorUpdatedListener.onInvocatorUpdated(operation, null);
		}
	}

	private Object[] getValues() {
		Object[] values = new Object[attributes.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = attributes.get(i).getValue();
		}
		return values;
	}

	public IOperation getSelectedOperation() {
		ISelection selection = operationsPart.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection str = (IStructuredSelection) selection;
			if (str.getFirstElement() instanceof IOperation) {
				return (IOperation) str.getFirstElement();
			}
		}
		return null;
	}

	public void saveState(IMemento state) {
		paramsColumnManager.getSettings().saveState(MementoToolkit.asWritableState(state));
	}
}
