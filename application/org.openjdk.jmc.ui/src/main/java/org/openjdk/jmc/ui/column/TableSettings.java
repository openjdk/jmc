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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.ui.UIPlugin;

public class TableSettings {

	private static final String ELEMENT_COLUMN = "column"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String ATTRIBUTE_HIDDEN = "hidden"; //$NON-NLS-1$
	private static final String ATTRIBUTE_WIDTH = "width"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SORTCOLUMN = "sortColumn"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SORTASCENDING = "sortAscending"; //$NON-NLS-1$

	public static class ColumnSettings {
		private final String id;
		private final Boolean hidden;
		private final Integer width;
		private final Boolean sortAscending;

		public ColumnSettings(String id, Boolean hidden, Integer width, Boolean sortAscending) {
			this.id = id;
			this.hidden = hidden;
			this.width = width;
			this.sortAscending = sortAscending;
		}

		public Boolean isHidden() {
			return hidden;
		}

		public Integer getWidth() {
			return width;
		}

		public Boolean isSortAscending() {
			return sortAscending;
		}

		public String getId() {
			return id;
		}
	}

	private final String orderBy;
	private final List<ColumnSettings> columns;

	public TableSettings(IState state) {
		columns = new ArrayList<>();
		for (IState a : state.getChildren(ELEMENT_COLUMN)) {
			Boolean hidden = StateToolkit.readBoolean(a, ATTRIBUTE_HIDDEN, null);
			Integer width = StateToolkit.readInt(a, ATTRIBUTE_WIDTH, null);
			Boolean sort = StateToolkit.readBoolean(a, ATTRIBUTE_SORTASCENDING, null);
			columns.add(new ColumnSettings(a.getAttribute(ATTRIBUTE_ID), hidden, width, sort));
		}
		orderBy = state.getAttribute(ATTRIBUTE_SORTCOLUMN);
	}

	public TableSettings(String orderBy, List<ColumnSettings> columns) {
		this.orderBy = orderBy;
		this.columns = columns;
	}

	public List<ColumnSettings> getColumns() {
		return columns;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void saveState(IWritableState state) {
		for (ColumnSettings c : columns) {
			IWritableState col = state.createChild(ELEMENT_COLUMN);
			col.putString(ATTRIBUTE_ID, c.id);
			StateToolkit.writeBoolean(col, ATTRIBUTE_HIDDEN, c.hidden);
			StateToolkit.writeInt(col, ATTRIBUTE_WIDTH, c.width);
			StateToolkit.writeBoolean(col, ATTRIBUTE_SORTASCENDING, c.sortAscending);
		}
		state.putString(ATTRIBUTE_SORTCOLUMN, orderBy);
	}

	public static TableSettings forState(IState state) {
		return state == null ? null : new TableSettings(state);
	}

	public static TableSettings forStateAndColumns(
		IState state, Collection<String> allColumns, Collection<String> visibleColumns) {

		TableSettings tableSettings = null;
		if (state != null) {
			try {
				tableSettings = new TableSettings(state);
			} catch (RuntimeException e) {
				UIPlugin.getDefault().getLogger().log(Level.WARNING, "Broken settings", e); //$NON-NLS-1$
			}
		}
		if (tableSettings == null) {
			List<ColumnSettings> defaultListCols = new ArrayList<>();
			for (String columnId : allColumns) {
				defaultListCols.add(new ColumnSettings(columnId, !visibleColumns.contains(columnId), null, null));
			}
			tableSettings = new TableSettings(null, defaultListCols);
		}
		return tableSettings;
	}
}
