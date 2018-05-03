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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.common.collection.SimpleArray;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.util.SortedHead;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.ItemIterableToolkit;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnManager.ColumnComparator;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;

public class ItemList {

	public static class ItemListBuilder {

		private final List<IColumn> columns = new ArrayList<>();

		public void addColumn(IAttribute<?> a) {
			IMemberAccessor<?, IItem> accessor = ItemToolkit.accessor(a);
			// FIXME: Calculate column id, e.g. using getColumnId.
			// Otherwise there will be problem if adding multiple attributes with the same id.
			// Requires update of all column id references, e.g. in the pages xml.
			addColumn(a.getIdentifier(), a.getName(), a.getDescription(),
					a.getContentType() instanceof LinearKindOfQuantity, accessor);
		}

		public void addColumn(
			String columnId, String name, String description, boolean right, IMemberAccessor<?, IItem> accessor) {
			columns.add(new ColumnBuilder(name, columnId, accessor).description(description)
					.style(right ? SWT.RIGHT : SWT.NONE).build());
		}

		public ItemList buildWithoutBorder(Composite container, TableSettings tableSettings) {
			return new ItemList(container, columns, tableSettings, SWT.NONE);
		}

		public ItemList build(Composite container, TableSettings tableSettings) {
			return new ItemList(container, columns, tableSettings, SWT.BORDER);
		}
	}

	private final int maxSize = (int) FlightRecorderUI.getDefault().getItemListSize().longValue();
	private final IItem[] maxSizeArray = new IItem[maxSize + 1];
	private final ColumnManager columnManager;
	// FIXME: JMC-5127 - Don't initialize to 1000 elements
	private final SimpleArray<IItem> tail = new SimpleArray<>(new IItem[1000]);

	private ExtraRowTableViewer tableViewer;

	private ItemList(Composite container, List<IColumn> columns, TableSettings tableSettings, int style) {
		tableViewer = new ExtraRowTableViewer(container,
				SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | style);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		ColumnViewerToolTipSupport.enableFor(tableViewer);
		Consumer<ColumnComparator> onSortChange = comparator -> {
			Object input = tableViewer.getInput();
			if (input instanceof IItem[] && comparator != null) {
				IItem[] head = (IItem[]) input;
				if (tail.size() > 0) {
					Iterator<IItem> oldTailIterator = tail.iterator();
					tail.clear();
					// addSorted will write to the array in tail, but not faster than it is read
					SortedHead.addSorted(oldTailIterator, head, tail, comparator);
				} else {
					Arrays.sort(head, comparator);
				}
				tableViewer.refresh();
				tableViewer.setSelection(StructuredSelection.EMPTY);
			}
		};
		if (UIPlugin.getDefault().getAccessibilityMode()) {
			FocusTracker.enableFocusTracking(tableViewer.getTable());
		}

		columnManager = ColumnManager.build(tableViewer, columns, tableSettings, onSortChange);
	}

	public ColumnManager getManager() {
		return columnManager;
	}

	@SuppressWarnings("unchecked")
	public Supplier<Stream<? extends IItem>> getSelection() {
		List<? extends IItem> list = ((IStructuredSelection) columnManager.getViewer().getSelection()).toList();
		return () -> list.stream();
	}

	public void show(IItemCollection items) {
		show(ItemCollectionToolkit.stream(items).flatMap(ItemIterableToolkit::stream).iterator());
	}

	public void show(Iterator<? extends IItem> it) {
		boolean showEllipsisMessage = false;
		int count = 0;

		while (it.hasNext() && count < maxSizeArray.length - 1) {
			maxSizeArray[count++] = it.next();
		}
		IItem[] head;
		if (count < maxSizeArray.length) {
			head = Arrays.copyOf(maxSizeArray, count);
		} else {
			head = maxSizeArray;
		}

		ColumnComparator columnComparator = columnManager.getColumnComparator();
		tail.clear();
		if (it.hasNext()) {
			showEllipsisMessage = true;
			if (columnComparator != null) {
				SortedHead.addSorted(it, head, tail, columnComparator);
			} else {
				while (it.hasNext()) {
					tail.add(it.next());
				}
			}
		} else if (columnComparator != null) {
			Arrays.sort(head, columnComparator);
		}
		if (showEllipsisMessage) {
			setEllipsisMessage();
		} else {
			clearEllipsisMessage();
		}

		// FIXME: Remove and make table handle model updates with preserved selection.
		// If selection is not cleared the viewer tries to preserve selection but selects the wrong rows.
		((TableViewer) columnManager.getViewer()).getTable().deselectAll();
		columnManager.getViewer().setInput(head);
	}

	private void setEllipsisMessage() {
		// FIXME: Would like for this item to be displayed with a different font...
		tableViewer.setExtraMessage(NLS.bind(Messages.ITEM_LIST_ELLIPSIS_TEXT, maxSize, maxSize + tail.size()));
	}

	private void clearEllipsisMessage() {
		tableViewer.setExtraMessage(null);
	}

	/**
	 * Construct an identifier that can be used when persisting column state.
	 *
	 * @param a
	 * @return An identifier based on attribute id and type
	 */
	public static String getColumnId(IAttribute<?> a) {
		return a.getIdentifier() + ':' + a.getContentType().getIdentifier();
	}

	/*
	 * @Override public void accept(Supplier<Stream<ItemStream>> items) {
	 * show(items.get().flatMap(ItemStream::items).iterator()); }
	 *
	 * @Override public Control getControl() { return getManager().getViewer().getControl(); }
	 */
}
