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
package org.openjdk.jmc.rjmx.ui.internal;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.openjdk.jmc.rjmx.services.IAttribute;
import org.openjdk.jmc.rjmx.services.IIndexedAttributeChild;
import org.openjdk.jmc.rjmx.services.IReadOnlyAttribute;
import org.openjdk.jmc.rjmx.ui.celleditors.TypedEditingSupport;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnManager.IColumnState;
import org.openjdk.jmc.ui.column.IColumn;

public abstract class AbstractArrayElementMenuAction extends Action {

	private final ActionContributionItem m_contribution;
	private final ColumnManager m_columnsManager;
	private final IColumn m_column;
	private IIndexedAttributeChild m_element;

	protected AbstractArrayElementMenuAction(String name, final IMenuManager mm, ColumnManager columnsManager,
			IColumn column) {
		super(name);
		m_columnsManager = columnsManager;
		m_column = column;
		m_columnsManager.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				List<?> selectedRows = selection.toList();
				boolean visible = false;
				boolean enabled = false;
				if (isColumnVisible() && isActionApplicable(selectedRows)) {
					visible = true;
					m_element = getSelectedElement(selectedRows);
					enabled = isActionValid(m_element);
				} else {
					m_element = null;
				}
				if (m_contribution.isVisible() != visible || isEnabled() != enabled) {
					m_contribution.setVisible(visible);
					setEnabled(enabled);
					mm.update(true);
				}
			}

		});
		m_contribution = new ActionContributionItem(this);
		m_contribution.setVisible(false);
		setEnabled(false);
	}

	protected ActionContributionItem getActionContribution() {
		return m_contribution;
	}

	protected IIndexedAttributeChild getSelectedElement() {
		return m_element;
	}

	private boolean isColumnVisible() {
		return m_columnsManager.getColumnStates().filter(c -> c.getColumn().equals(m_column)).findAny()
				.map(IColumnState::isVisible).orElse(false);
	}

	private boolean isActionApplicable(List<?> selectedRows) {
		return selectedRows.size() == 1 && selectedRows.get(0) instanceof IIndexedAttributeChild;
	}

	private IIndexedAttributeChild getSelectedElement(List<?> selectedRows) {
		return (IIndexedAttributeChild) selectedRows.get(0);
	}

	private boolean isActionValid(IIndexedAttributeChild selectedElement) {
		IReadOnlyAttribute parent = selectedElement.getParent();
		return parent instanceof IAttribute && m_column.getEditingSupport() != null
				&& ((TypedEditingSupport<?>) m_column.getEditingSupport()).canEdit(parent);
	}

	@Override
	public void run() {
		if (m_element != null) {
			run(m_element);
			m_columnsManager.getViewer().refresh();
		}
	}

	protected abstract void run(IIndexedAttributeChild selectedElement);

}
