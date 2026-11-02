/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.joverflow.ui.viewers;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.joverflow.dominator.DominatorTree;
import org.openjdk.jmc.joverflow.heap.model.HeapObjectReader;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.ui.Messages;
import org.openjdk.jmc.joverflow.ui.model.DominatorTreeItem;

public class DominatorTreeViewer extends TreeViewer {
	private static final int OBJECT_COLUMN_WIDTH = 450;
	private static final int SIZE_COLUMN_WIDTH = 120;
	private static final int PERCENTAGE_COLUMN_WIDTH = 90;

	private final ReadBuffer.Factory readBufferFactory;
	private HeapObjectReader reader;
	private DominatorTreeItem rootItem;
	private Comparator<DominatorTreeItem> sortComparator;

	/**
	 * @param readBufferFactory
	 *            opens independent buffers for the input heap dump
	 */
	public DominatorTreeViewer(Composite parent, int style, ReadBuffer.Factory readBufferFactory) {
		super(parent, style);
		this.readBufferFactory = Objects.requireNonNull(readBufferFactory);
		setUseHashlookup(true);
		setContentProvider(new ContentProvider());
		getControl().addDisposeListener(event -> closeReader());

		TreeViewerColumn objectColumn = createColumn(Messages.DominatorTreeViewer_COLUMN_OBJECT, OBJECT_COLUMN_WIDTH,
				SWT.LEFT, DominatorTreeItem::getLabel, null, Comparator.comparing(DominatorTreeItem::getLabel));
		styleObjectColumn(objectColumn);
		createColumn(Messages.DominatorTreeViewer_COLUMN_SHALLOW_SIZE, SIZE_COLUMN_WIDTH, SWT.RIGHT,
				item -> formatSize(item.getShallowSize()), item -> formatBytes(item.getShallowSize()),
				Comparator.comparingLong(DominatorTreeItem::getShallowSize));
		createColumn(Messages.DominatorTreeViewer_COLUMN_RETAINED_SIZE, SIZE_COLUMN_WIDTH, SWT.RIGHT,
				item -> formatSize(item.getRetainedSize()), item -> formatBytes(item.getRetainedSize()),
				Comparator.comparingLong(DominatorTreeItem::getRetainedSize));
		createColumn(Messages.DominatorTreeViewer_COLUMN_RETAINED_PERCENTAGE, PERCENTAGE_COLUMN_WIDTH, SWT.RIGHT,
				item -> item.isPlaceholder() ? "" : String.format("%.2f %%", item.getRetainedPercentage()), null, //$NON-NLS-1$ //$NON-NLS-2$
				Comparator.comparingLong(DominatorTreeItem::getRetainedSize));

		getTree().setHeaderVisible(true);
		getTree().setLinesVisible(true);
		ColumnViewerToolTipSupport.enableFor(this);

		// Keep retained-size order until a column is selected, and placeholders last.
		setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				DominatorTreeItem item1 = (DominatorTreeItem) e1;
				DominatorTreeItem item2 = (DominatorTreeItem) e2;
				if (item1.isPlaceholder() != item2.isPlaceholder()) {
					return item1.isPlaceholder() ? 1 : -1;
				}
				if (sortComparator == null || item1.isPlaceholder()) {
					return 0;
				}
				int result = sortComparator.compare(item1, item2);
				return getTree().getSortDirection() == SWT.UP ? result : -result;
			}
		});

		addDoubleClickListener(event -> {
			Object element = ((IStructuredSelection) event.getSelection()).getFirstElement();
			if (element != null) {
				setExpandedState(element, !getExpandedState(element));
			}
		});
	}

	/**
	 * Sets the tree to show, or null to clear the viewer.
	 */
	public void setDominatorTree(DominatorTree tree) {
		setInput(tree);
	}

	private void closeReader() {
		if (reader != null) {
			reader.close();
			reader = null;
		}
	}

	private void styleObjectColumn(TreeViewerColumn column) {
		LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), getControl());
		Font boldFont = resources.createFont(FontDescriptor.createFrom(getTree().getFont()).setStyle(SWT.BOLD));
		StyledString.Styler emphasis = new StyledString.Styler() {
			@Override
			public void applyStyles(TextStyle textStyle) {
				textStyle.font = boldFont;
			}
		};
		column.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				DominatorTreeItem item = (DominatorTreeItem) cell.getElement();
				String text = item.getLabel();
				StyledString styled = new StyledString(text);
				if (item.isPlaceholder()) {
					styled.setStyle(0, text.length(), StyledString.QUALIFIER_STYLER);
				} else {
					int nameLength = item.getNameLength();
					int prefixLength = item.getReferencePrefixLength();
					styled.setStyle(0, prefixLength > 0 ? prefixLength : nameLength, emphasis);
					styled.setStyle(nameLength, text.length() - nameLength, StyledString.QUALIFIER_STYLER);
				}
				cell.setText(text);
				cell.setStyleRanges(styled.getStyleRanges());
				super.update(cell);
			}
		});
	}

	private TreeViewerColumn createColumn(
		String title, int width, int alignment, Function<DominatorTreeItem, String> text,
		Function<DominatorTreeItem, String> toolTip, Comparator<DominatorTreeItem> comparator) {
		TreeViewerColumn viewerColumn = new TreeViewerColumn(this, alignment);
		TreeColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(width);
		column.setMoveable(true);
		column.setResizable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return text.apply((DominatorTreeItem) element);
			}

			@Override
			public String getToolTipText(Object element) {
				return toolTip == null ? null : toolTip.apply((DominatorTreeItem) element);
			}
		});
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int direction = SWT.DOWN;
				if (getTree().getSortColumn() == column) {
					direction = getTree().getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN;
				}
				getTree().setSortColumn(column);
				getTree().setSortDirection(direction);
				sortComparator = comparator;
				refresh();
			}
		});
		return viewerColumn;
	}

	private static String formatSize(long bytes) {
		return bytes < 0 ? "" : UnitLookup.BYTE.quantity(bytes).displayUsing(IDisplayable.AUTO); //$NON-NLS-1$
	}

	private static String formatBytes(long bytes) {
		return bytes < 0 ? null : NLS.bind(Messages.DominatorTreeViewer_BYTES_TOOLTIP, String.format("%,d", bytes)); //$NON-NLS-1$
	}

	private class ContentProvider implements ITreeContentProvider {
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			closeReader();
			rootItem = null;
			if (newInput instanceof DominatorTree) {
				DominatorTree tree = (DominatorTree) newInput;
				reader = new HeapObjectReader(tree.getSnapshot(), readBufferFactory);
				rootItem = new DominatorTreeItem(tree, DominatorTree.ROOT_NODE, reader);
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return rootItem == null ? new Object[0] : rootItem.getChildren().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return ((DominatorTreeItem) parentElement).getChildren().toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return ((DominatorTreeItem) element).hasChildren();
		}
	}
}
