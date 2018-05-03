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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;

public class ColumnManager {

	public interface IColumnState {
		IColumn getColumn();

		Integer getWidth();

		Boolean isHidden();

		Boolean isSortAscending();

		boolean isVisible();
	}

	public static class SelectionState {
		private int scrollIndex;
		private int[] selectionIndices;

		private SelectionState(int scrollIndex, int[] selectionIndices) {
			this.scrollIndex = scrollIndex;
			this.selectionIndices = selectionIndices;
		}

		private int[] getSelectionIndices() {
			return selectionIndices;
		}

		private int getScrollIndex() {
			return scrollIndex;
		}

	}

	private static final int DEFAULT_WIDTH = 150;
	private static final boolean DEFAULT_HIDDEN = false;
	private static final boolean DEFAULT_SORT_ASC = true;

	private final List<ColumnEntry> addedColumns;
	private final ColumnViewer viewer;
	private ColumnEntry sortColumn;

	private final Listener customDrawer = new Listener() {

		@Override
		public void handleEvent(Event event) {
			Item[] columnWidgets = getColumnWidgets();
			if (event.index < columnWidgets.length) {
				Widget colWidget = columnWidgets[event.index];
				for (ColumnEntry ce : addedColumns) {
					if (ce.ui != null && colWidget == getColumnWidget(ce.ui)) {
						Listener drawer;
						if ((drawer = ce.getColumn().getColumnDrawer()) != null) {
							drawer.handleEvent(event);
						}
					}
				}
			}
		}
	};
	private final Consumer<ColumnComparator> onSortChange;

	static class ColumnEntry implements ControlListener, IColumnState {
		private final IColumn impl;
		private Boolean hidden;
		private Integer width;
		private Boolean sortAscending;
		private ViewerColumn ui;

		public ColumnEntry(IColumn columnImpl, Boolean hidden, Integer width, Boolean sortAscending) {
			impl = columnImpl;
			this.hidden = hidden;
			this.width = width;
			this.sortAscending = sortAscending;
		}

		@Override
		public IColumn getColumn() {
			return impl;
		}

		@Override
		public Integer getWidth() {
			return width;
		}

		@Override
		public Boolean isHidden() {
			return hidden;
		}

		@Override
		public boolean isVisible() {
			return ui != null;
		}

		@Override
		public Boolean isSortAscending() {
			return sortAscending;
		}

		boolean isSortAscendingPreferred() {
			return sortAscending == null ? DEFAULT_SORT_ASC : sortAscending;
		}

		Item create(ColumnViewer viewer, int columnIndex) {
			Item columnWidget;
			int colWidth = width == null ? DEFAULT_WIDTH : width;
			if (viewer instanceof TableViewer) {
				TableViewerColumn vc = new TableViewerColumn((TableViewer) viewer, getColumn().getStyle(), columnIndex);
				ui = vc; // set before usage since viewerColumn is used to implement isVisible()
				vc.getColumn().setMoveable(true);
				vc.getColumn().addControlListener(this);
				vc.getColumn().setToolTipText(getColumn().getDescription());
				vc.getColumn().setWidth(colWidth);
				columnWidget = vc.getColumn();

			} else {
				TreeViewerColumn vc = new TreeViewerColumn(((TreeViewer) viewer), getColumn().getStyle(), columnIndex);
				ui = vc; // set before usage since viewerColumn is used to implement isVisible()
				vc.getColumn().setMoveable(true);
				vc.getColumn().addControlListener(this);
				vc.getColumn().setToolTipText(getColumn().getDescription());
				vc.getColumn().setWidth(colWidth);
				columnWidget = vc.getColumn();
			}
			ui.setEditingSupport(getColumn().getEditingSupport());
			ui.setLabelProvider(getColumn().getLabelProvider());
			columnWidget.setText(getColumn().getName());
			return columnWidget;
		}

		private void doHide() {
			ViewerColumn vc = ui;
			ui = null; // clear before dispose since viewerColumn is used to implement isVisible()
			if (vc instanceof TableViewerColumn) {
				TableViewerColumn tc = ((TableViewerColumn) vc);
				ColumnViewer viewer = vc.getViewer();
				try {
					// Workaround. disable redraw, or TableViewer will throw exception when removing the last column
					viewer.getControl().setRedraw(false);
					tc.getColumn().dispose();
				} finally {
					try {
						viewer.getControl().setRedraw(true);
					} catch (SWTException e) {
						// Workaround. for some reason the table sometimes complains about the table is
						// disposed even though it seem to be working perfectly.
					}
				}
			} else if (vc != null) {
				((TreeViewerColumn) vc).getColumn().dispose();
			}
		}

		@Override
		public void controlMoved(ControlEvent e) {

		}

		@Override
		public void controlResized(ControlEvent e) {
			width = getColumnWidth(ui);
			// FIXME: Workaround to avoid drawing dug on windows when having hooked EraseItem listener and the first tree column draws a custom background or is right aligned
			ui.getViewer().getControl().redraw();
		}

	}

	private static Boolean getNullForDefault(boolean hidden) {
		return hidden == DEFAULT_HIDDEN ? null : hidden;
	}

	public static ColumnManager build(TableViewer viewer, List<IColumn> columns, TableSettings ts) {
		return build((ColumnViewer) viewer, columns, ts, viewer::setComparator);
	}

	public static ColumnManager build(
		TableViewer viewer, List<IColumn> columns, TableSettings ts, Consumer<ColumnComparator> onSortChange) {
		return build((ColumnViewer) viewer, columns, ts, onSortChange);
	}

	public static ColumnManager build(TreeViewer viewer, List<IColumn> columns, TableSettings ts) {
		return build((ColumnViewer) viewer, columns, ts, viewer::setComparator);
	}

	public static ColumnManager build(
		TreeViewer viewer, List<IColumn> columns, TableSettings ts, Consumer<ColumnComparator> onSortChange) {
		return build((ColumnViewer) viewer, columns, ts, onSortChange);
	}

	private static ColumnManager build(
		ColumnViewer viewer, List<IColumn> columns, TableSettings ts, Consumer<ColumnComparator> onSortChange) {
		List<ColumnEntry> entries = new ArrayList<>();
		String sortColumn = null;
		if (ts != null) {
			Map<String, IColumn> columnsMap = new LinkedHashMap<>(); // preserve order for columns with no settings
			for (IColumn c : columns) {
				columnsMap.put(c.getId(), c);
			}
			if (ts.getOrderBy() != null && columnsMap.containsKey(ts.getOrderBy())) {
				sortColumn = ts.getOrderBy();
			}
			// Add settings in order
			for (ColumnSettings cc : ts.getColumns()) {
				IColumn c = columnsMap.remove(cc.getId());
				if (c != null) {
					entries.add(new ColumnEntry(c, cc.isHidden(), cc.getWidth(), cc.isSortAscending()));
				}
			}
			// Add columns with no settings as hidden
			for (IColumn c : columnsMap.values()) {
				entries.add(new ColumnEntry(c, getNullForDefault(true), null, null));
			}
		} else {
			// No settings, add all columns as visible
			for (IColumn c : columns) {
				entries.add(new ColumnEntry(c, getNullForDefault(false), null, null));
			}
		}
		return new ColumnManager(viewer, sortColumn, entries, onSortChange);
	}

	ColumnManager(ColumnViewer viewer, String sortColumnId, List<ColumnEntry> columns,
			Consumer<ColumnComparator> onSortChange) {
		this.viewer = viewer;
		addedColumns = columns;
		this.onSortChange = onSortChange;
		setLinesVisible(true);
		setHeaderVisible(true);
		viewer.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return Messages.ALL_COLUMNS_HIDDEN_LABEL;
			}

			@Override
			public Font getFont(Object element) {
				return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
			}

			@Override
			public Color getForeground(Object element) {
				return JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
			}
		});
		viewer.getControl().addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				updateColumnOrder();
			}
		});
		for (ColumnEntry ce : addedColumns) {
			if (ce.hidden == null ? !DEFAULT_HIDDEN : !ce.hidden) {
				createColumnUi(ce);
			}
		}
		updateEraseItemListener();
		if (sortColumnId != null) {
			setSortColumn(sortColumnId);
		}
	}

	private void updateEraseItemListener() {
		for (ColumnEntry c : addedColumns) {
			if (c.isVisible() && c.getColumn().getColumnDrawer() != null) {
				// Custom drawer found, ensure the the listener is added
				for (Listener l : viewer.getControl().getListeners(SWT.EraseItem)) {
					if (l == customDrawer) {
						return;
					}
				}
				viewer.getControl().addListener(SWT.EraseItem, customDrawer);
				return;
			}
		}

		// No custom drawer found, ensure the the listener is not added
		viewer.getControl().removeListener(SWT.EraseItem, customDrawer);
	}

	public ColumnViewer getViewer() {
		return viewer;
	}

	void setColumnHidden(String columnId, boolean hidden) {
		updateColumnOrder();
		ColumnEntry columnEntry = getColumnEntry(columnId);
		columnEntry.hidden = hidden;
		if (hidden) {
			columnEntry.doHide();
			updateEraseItemListener();
		} else {
			createColumnUi(columnEntry);
			updateEraseItemListener();
			getViewer().refresh(); // Need to populate the added column
		}
		getViewer().getControl().getParent().layout();
	}

	private void createColumnUi(ColumnEntry columnEntry) {
		Item columnWidget = columnEntry.create(viewer, countVisibleColumnsBefore(columnEntry));
		columnWidget.addListener(SWT.Selection, e -> changeOrFlipSortColumn(columnEntry));
	}

	private int countVisibleColumnsBefore(ColumnEntry columnEntry) {
		int i = 0;
		for (ColumnEntry c : addedColumns) {
			if (c == columnEntry) {
				return i;
			} else if (c.isVisible()) {
				i++;
			}
		}
		return i;
	}

	private void updateColumnOrder() {
		if (viewer instanceof TableViewer) {
			Table table = ((TableViewer) viewer).getTable();
			updateColumnOrder(table.getColumns(), table.getColumnOrder());
		} else {
			Tree tree = ((TreeViewer) viewer).getTree();
			updateColumnOrder(tree.getColumns(), tree.getColumnOrder());
		}
	}

	private void updateColumnOrder(Widget[] columns, int[] order) {
		int visibleIndex = 0;
		for (int i = 0; i < addedColumns.size(); i++) {
			ColumnEntry e = addedColumns.get(i);
			if (e.isVisible()) {
				Widget v1 = getColumnWidget(e.ui);
				Widget v2 = columns[order[visibleIndex++]];
				if (v2 != v1) {
					// swap position for v1 and v2;
					for (int j = i; j < addedColumns.size(); j++) {
						ColumnEntry e2 = addedColumns.get(j);
						if (e2.ui != null && v2 == getColumnWidget(e2.ui)) {
							addedColumns.set(i, e2);
							addedColumns.set(j, e);

						}
					}
				}
			}
		}
	}

	/**
	 * Change the current sort column. If this column is already the sort column, then switch sort
	 * order.
	 *
	 * @param entry
	 *            Column to sort on. May not be null.
	 */
	private void changeOrFlipSortColumn(ColumnEntry entry) {
		if (sortColumn == entry) {
			setSortColumn(entry, !entry.isSortAscendingPreferred());
		} else {
			setSortColumn(entry, entry.isSortAscendingPreferred());
		}
	}

	public void clearSortColumn() {
		sortColumn = null;
		setSortColumn(null, SWT.UP);
	}

	public void setSortColumn(String columnId) {
		ColumnEntry columnEntry = getColumnEntry(columnId);
		setSortColumn(columnEntry, columnEntry.isSortAscendingPreferred());
	}

	public void setSortColumn(String columnId, boolean sortAscending) {
		setSortColumn(getColumnEntry(columnId), sortAscending);
	}

	public ColumnComparator getColumnComparator() {
		return sortColumn == null ? null : new ColumnComparator(sortColumn.impl, sortColumn.isSortAscendingPreferred());
	}

	private void setSortColumn(ColumnEntry entry, boolean sortAscending) {
		sortColumn = entry;
		entry.sortAscending = sortAscending;
		setSortColumn(entry.ui, sortAscending ? SWT.UP : SWT.DOWN);
	}

	private ColumnEntry getColumnEntry(String columnId) {
		return addedColumns.stream().filter(ce -> ce.impl.getId().equals(columnId)).findAny().get();
	}

	private void setSortColumn(ViewerColumn vc, int direction) {
		if (viewer instanceof TableViewer) {
			Table table = ((TableViewer) viewer).getTable();
			table.setSortColumn(vc == null ? null : ((TableViewerColumn) vc).getColumn());
			table.setSortDirection(direction);
		} else {
			Tree tree = ((TreeViewer) viewer).getTree();
			tree.setSortColumn(vc == null ? null : ((TreeViewerColumn) vc).getColumn());
			tree.setSortDirection(direction);
		}
		onSortChange.accept(getColumnComparator());
	}

	private static Item getColumnWidget(ViewerColumn vc) {
		if (vc instanceof TableViewerColumn) {
			return ((TableViewerColumn) vc).getColumn();
		} else {
			return ((TreeViewerColumn) vc).getColumn();
		}
	}

	private Item[] getColumnWidgets() {
		if (viewer instanceof TableViewer) {
			return ((TableViewer) viewer).getTable().getColumns();
		} else {
			return ((TreeViewer) viewer).getTree().getColumns();
		}
	}

	private static int getColumnWidth(ViewerColumn vc) {
		if (vc instanceof TableViewerColumn) {
			return ((TableViewerColumn) vc).getColumn().getWidth();
		} else {
			return ((TreeViewerColumn) vc).getColumn().getWidth();
		}
	}

	private void setHeaderVisible(boolean visible) {
		if (viewer instanceof TableViewer) {
			((TableViewer) viewer).getTable().setHeaderVisible(visible);
		} else {
			((TreeViewer) viewer).getTree().setHeaderVisible(visible);
		}
	}

	private void setLinesVisible(boolean visible) {
		if (viewer instanceof TableViewer) {
			((TableViewer) viewer).getTable().setLinesVisible(visible);
		} else {
			((TreeViewer) viewer).getTree().setLinesVisible(visible);
		}
	}

	public Stream<? extends IColumnState> getColumnStates() {
		if (!viewer.getControl().isDisposed()) {
			updateColumnOrder();
		}
		return addedColumns.stream();
	}

	public TableSettings getSettings() {
		List<ColumnSettings> cols = getColumnStates().map(ColumnManager::buildColumnConfig)
				.collect(Collectors.toList());
		return new TableSettings(sortColumn == null ? null : sortColumn.getColumn().getId(), cols);
	}

	private static ColumnSettings buildColumnConfig(IColumnState state) {
		return new ColumnSettings(state.getColumn().getId(), state.isHidden(), state.getWidth(),
				state.isSortAscending());
	}

	public static class ColumnComparator extends ViewerComparator implements Comparator<Object> {

		private final IColumn column;
		private final boolean sortAscending;

		private ColumnComparator(IColumn column, boolean sortAscending) {
			this.column = column;
			this.sortAscending = sortAscending;
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			return compare(e1, e2);
		}

		@Override
		public int compare(Object row1, Object row2) {
			int compare = 0;
			Comparator<Object> comparator = column.getComparator();
			if (comparator != null) {
				compare = comparator.compare(row1, row2);
			} else {
				ColumnLabelProvider clp = column.getLabelProvider();
				String l1 = clp.getText(row1);
				String l2 = clp.getText(row2);
				compare = l1 == null ? (l2 == null ? 0 : -1) : (l2 == null ? 1 : l1.compareTo(l2));
			}
			return sortAscending ? compare : -compare;
		}

		public IColumn getColumn() {
			return column;
		}

		public boolean isSortAscending() {
			return sortAscending;
		}
	}

	public int getVisibilityIndex(IColumn column) {
		for (ColumnEntry c : addedColumns) {
			if (c.impl == column) {
				if (c.ui != null) {
					return Arrays.asList(getColumnWidgets()).indexOf(getColumnWidget(c.ui));
				}
				break;
			}
		}
		return -1;
	}

	public SelectionState getSelectionState() {
		Table table = (Table) getViewer().getControl();
		return new SelectionState(table.getTopIndex(), table.getSelectionIndices());
	}

	public void setSelectionState(SelectionState state) {
		if (state == null) {
			return;
		}
		Table table = (Table) getViewer().getControl();
		table.setSelection(state.getSelectionIndices());
		// Workaround to fire selection events for listeners of the TableViewer
		getViewer().setSelection(getViewer().getSelection());
		table.setTopIndex(state.getScrollIndex());
	}

}
