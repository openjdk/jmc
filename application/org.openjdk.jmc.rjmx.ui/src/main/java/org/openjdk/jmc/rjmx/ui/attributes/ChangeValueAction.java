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

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.openjdk.jmc.rjmx.ui.celleditors.TypedEditingSupport;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.IColumn;

/**
 * Action to induce cell editing through context menu.
 */
public class ChangeValueAction extends Action {

	private final ColumnManager m_columnManager;
	private final IColumn m_column;
	private final ColumnViewer m_viewer;
	private Object m_element;
	private int m_columnIndex = -1;

	public ChangeValueAction(ColumnViewer viewer, ColumnManager columnManager, IColumn column) {
		this(Messages.ChangeValueAction_CHANGE_VALUE_ACTION_NAME, viewer, columnManager, column);
	}

	public ChangeValueAction(String name, ColumnViewer viewer, ColumnManager columnManager, IColumn column) {
		super(name);
		m_viewer = viewer;
		m_columnManager = columnManager;
		m_column = column;
		m_viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				List<?> selectedRows = selection.toList();
				m_columnIndex = m_columnManager.getVisibilityIndex(m_column);
				setEnabled(selectedRows.size() == 1 && canEditElementValue(m_element = selectedRows.get(0))
						&& m_columnIndex >= 0);
			}
		});
		setEnabled(false);
	}

	@Override
	public void run() {
		m_viewer.editElement(m_element, m_columnIndex);
	}

	private boolean canEditElementValue(Object element) {
		return m_column.getEditingSupport() != null
				&& ((TypedEditingSupport<?>) m_column.getEditingSupport()).canEdit(element);
	}
}
