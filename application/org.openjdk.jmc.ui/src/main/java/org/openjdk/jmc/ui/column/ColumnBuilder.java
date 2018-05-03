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

import java.util.Comparator;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Listener;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.ui.misc.DelegatingLabelProvider;
import org.openjdk.jmc.ui.misc.OptimisticComparator;

public class ColumnBuilder {

	private static final ColumnLabelProvider DEFAULT_LP = new ColumnLabelProvider() {
		@Override
		public String getText(Object element) {
			/*
			 * FIXME: Could consider displaying null, in italics, instead of an empty string.
			 * 
			 * But in that case we need to make sure our aggregate values are not null, so we don't
			 * confuse the user.
			 */
			return element == null ? "" : TypeHandling.getValueString(element); //$NON-NLS-1$
		};

		@Override
		public String getToolTipText(Object element) {
			return element == null ? "null" : TypeHandling.getVerboseString(element); //$NON-NLS-1$
		}

		@Override
		public Font getToolTipFont(Object element) {
			return element == null ? JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT)
					: super.getToolTipFont(element);
		};

		@Override
		public Font getFont(Object element) {
			// Always return a font, otherwise SWT will use the first columns font for all columns
			return JFaceResources.getDefaultFont();
		};
	};

	private static class Column implements IColumn {

		private ColumnLabelProvider labelProvider;
		private EditingSupport editingSupport;
		private IMemberAccessor<?, Object> cellAccessor;
		private final String name;
		private String description;
		private final String id;
		private int style = SWT.NONE;
		private Comparator<Object> comparator;
		private Listener columnDrawer;

		public Column(String name, String id) {
			this.name = name;
			this.id = id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public int getStyle() {
			return style;
		}

		@Override
		public ColumnLabelProvider getLabelProvider() {
			return labelProvider;
		}

		@Override
		public EditingSupport getEditingSupport() {
			return editingSupport;
		}

		@Override
		public Comparator<Object> getComparator() {
			return comparator;
		}

		@Override
		public IMemberAccessor<?, Object> getCellAccessor() {
			return cellAccessor;
		}

		@Override
		public Listener getColumnDrawer() {
			return columnDrawer;
		}

	}

	private Column column;

	public ColumnBuilder(String name, String id, ColumnLabelProvider labelProvider) {
		column = new Column(name, id);
		column.labelProvider = labelProvider;
	}

	/**
	 * Users of this method must ensure that all elements in the tree/table is of type T
	 */

	@SuppressWarnings("unchecked")
	public <T> ColumnBuilder(String name, String id, IMemberAccessor<?, T> cellAccessor) {
		column = new Column(name, id);
		column.cellAccessor = (IMemberAccessor<?, Object>) cellAccessor;
		column.labelProvider = new DelegatingLabelProvider(DEFAULT_LP, column.cellAccessor);
		column.comparator = new OptimisticComparator(column.cellAccessor, column.labelProvider);
	}

	private Column getColumn() {
		if (column == null) {
			throw new IllegalStateException("Column already built. ColumnBuilder cannot be reused."); //$NON-NLS-1$
		}
		return column;
	}

	public ColumnBuilder description(String description) {
		getColumn().description = description;
		return this;
	}

	public ColumnBuilder style(int style) {
		getColumn().style = style;
		return this;
	}

	/**
	 * Be aware that setting a label provider it will get the whole table rows as input, not only
	 * the column value through the accessor.
	 */
	public ColumnBuilder labelProvider(ColumnLabelProvider labelProvider) {
		getColumn().labelProvider = labelProvider;
		return this;
	}

	public ColumnBuilder comparator(Comparator<Object> comparator) {
		getColumn().comparator = comparator;
		return this;
	}

	public ColumnBuilder editingSupport(EditingSupport editingSupport) {
		getColumn().editingSupport = editingSupport;
		return this;
	}

	public ColumnBuilder columnDrawer(Listener drawer) {
		getColumn().columnDrawer = drawer;
		return this;
	}

	public IColumn build() {
		IColumn col = column;
		column = null;
		return col;
	}

}
