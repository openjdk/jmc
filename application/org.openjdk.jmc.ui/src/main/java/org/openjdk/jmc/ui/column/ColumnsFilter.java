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
package org.openjdk.jmc.ui.column;

import java.util.stream.Collectors;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.ui.column.ColumnManager.IColumnState;
import org.openjdk.jmc.ui.common.util.FilterMatcher;
import org.openjdk.jmc.ui.common.util.FilterMatcher.Where;

public class ColumnsFilter extends ViewerFilter {

	private String filterString;
	private Iterable<? extends IColumn> columns;

	public static Text addFilterControl(
		Composite filterComposite, FormToolkit toolkit, final ColumnManager columnManager) {
		final Text filterText = toolkit.createText(filterComposite, "", SWT.SEARCH); //$NON-NLS-1$
		filterText.setMessage(Messages.SEARCH_COLUMNS_TEXT);
		filterText.setToolTipText(org.openjdk.jmc.ui.Messages.SEARCH_KLEENE_OR_REGEXP_TOOLTIP);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final ColumnsFilter filter = new ColumnsFilter();
		filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String text = filterText.getText();
				filter.filterString = "".equals(text) ? null //$NON-NLS-1$
						: FilterMatcher.autoAddKleene(text, Where.BEFORE_AND_AFTER);
				filter.columns = columnManager.getColumnStates().filter(IColumnState::isVisible)
						.map(IColumnState::getColumn).collect(Collectors.toList());
				columnManager.getViewer().refresh();
			}
		});
		columnManager.getViewer().addFilter(filter);
		return filterText;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (filterString == null || columns == null) {
			return true;
		}
		for (IColumn c : columns) {
			String label = c.getLabelProvider().getText(element);
			if (FilterMatcher.getInstance().match(label, filterString, true)) {
				return true;
			}
		}
		return false;
	}
}
