/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2025, Red Hat Inc. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.ui.model.JavaThingItem;

public class JavaThingTreeViewer<T extends JavaThingItem> extends TreeViewer {
	private static final String ELLIPSIS = "..."; //$NON-NLS-1$
	private static final String NAME_COLUMN = "Name"; //$NON-NLS-1$
	private static final String SIZE_COLUMN = "Size"; //$NON-NLS-1$
	private static final String VALUE_COLUMN = "Value"; //$NON-NLS-1$

	public JavaThingTreeViewer(Composite parent, int style) {
		super(parent, style);

		setContentProvider(new JavaThingItemContentProvider());

		createTreeViewerColumn(NAME_COLUMN, T::getName);
		createTreeViewerColumn(VALUE_COLUMN, T::getValue);
		createTreeViewerColumn(SIZE_COLUMN, T::getSize);

		getTree().setLinesVisible(true);
		getTree().setHeaderVisible(true);

		setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				JavaThingItem item1 = (JavaThingItem) e1;
				JavaThingItem item2 = (JavaThingItem) e2;
				// keep the overflow ellipsis row at the bottom of the view
				if (item1.getName().equals(ELLIPSIS)) {
					return SWT.MAX;
				} else if (item2.getName().equals(ELLIPSIS)) {
					return -SWT.MAX;
				}
				int result = 0;
				int sortDirection = getTree().getSortDirection() == SWT.UP ? 1
						: getTree().getSortDirection() == SWT.DOWN ? -1 : SWT.NONE;
				if (getTree().getSortColumn() != null) {
					if (getTree().getSortColumn().getText().equals(NAME_COLUMN)) {
						result = item1.getName().compareTo(item2.getName());
					} else if (getTree().getSortColumn().getText().equals(VALUE_COLUMN)) {
						result = item1.getValue().compareTo(item2.getValue());
					} else if (getTree().getSortColumn().getText().equals(SIZE_COLUMN)) {
						result = Integer.parseInt(item1.getSize()) - Integer.parseInt(item2.getSize());
					}
				}
				return result * sortDirection;
			}
		});
	}

	private void createTreeViewerColumn(String label, Function<T, String> labelProvider) {
		TreeViewerColumn column = new TreeViewerColumn(this, SWT.NONE);
		column.getColumn().setWidth(300);
		column.getColumn().setText(label);
		column.getColumn().setMoveable(true);

		column.setLabelProvider(new ColumnLabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public String getText(Object element) {
				return labelProvider.apply((T) element);
			}
		});

		column.getColumn().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int newSortDirection = SWT.DOWN;
				if (getTree().getSortColumn() != null
						&& getTree().getSortColumn().getText().equals(column.getColumn().getText())) {
					newSortDirection = getTree().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
				}
				getTree().setSortColumn(column.getColumn());
				getTree().setSortDirection(newSortDirection);
				refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	private class JavaThingItemContentProvider implements ITreeContentProvider {

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement == null) {
				return new Object[0];
			}

			List<JavaThingItem> items = (List<JavaThingItem>) inputElement;
			return items.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			JavaThingItem item = (JavaThingItem) parentElement;
			Iterable<JavaThingItem> childItems = item.getChildItems();
			if (childItems == null) {
				ArrayList<JavaThingItem> items = new ArrayList<>();
				JavaThing thing = item.getContent();
				if (thing instanceof JavaObject) {
					JavaObject o = (JavaObject) thing;
					JavaField[] fields = o.getClazz().getFieldsForInstance();
					JavaThing[] values = o.getFields();
					for (int i = 0; i < fields.length; i++) {
						items.add(new JavaThingItem(item.getLevel() + 1, fields[i].getName(), values[i]));
					}
				} else if (thing instanceof JavaObjectArray) {
					JavaObjectArray o = (JavaObjectArray) thing;
					int i = 0;
					for (JavaThing th : o.getElements()) {
						items.add(new JavaThingItem(item.getLevel() + 1, "[" + (i++) + "]", th)); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} else if (thing instanceof JavaValueArray) {
					JavaValueArray o = (JavaValueArray) thing;
					int i = 0;
					for (String value : o.getValuesAsStrings()) {
						items.add(new JavaThingItem(item.getLevel() + 1, "[" + (i++) + "]", value, o.getElementSize(),
								null)); //$NON-NLS-1$ //$NON-NLS-2$
					}

				}
				item.setChildItems(items);
				return items.toArray();
			}

			return StreamSupport.stream(childItems.spliterator(), false).toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			JavaThingItem item = (JavaThingItem) element;
			JavaThing thing = item.getContent();
			return thing instanceof JavaObject && ((JavaObject) thing).getClazz().getFieldsForInstance().length > 0
					|| thing instanceof JavaObjectArray && ((JavaObjectArray) thing).getLength() > 0
					|| thing instanceof JavaValueArray && ((JavaValueArray) thing).getLength() > 0;
		}
	}
}
