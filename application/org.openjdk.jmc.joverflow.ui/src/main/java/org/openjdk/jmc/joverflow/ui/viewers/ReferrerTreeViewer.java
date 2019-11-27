/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.joverflow.ui.viewers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import org.openjdk.jmc.joverflow.ui.model.ReferrerItem;

// ReferrerTreeViewer is actually a TableViewer with its tree-like content
class ReferrerTreeViewer extends TableViewer {

	private long mHeapSize = 1;

	private final ReferrerTreeContentProvider mContentProvider;

	static class ReferrerTreeContentProvider extends ArrayContentProvider implements ILazyContentProvider {
		private Comparator<ReferrerItem> mComparator = Comparator.comparingLong(ReferrerItem::getMemory);
		private int mDirection = -1;

		private TableViewer mTableViewer;
		private Object[] mItems = new ReferrerItem[0];

		ReferrerTreeContentProvider(TableViewer tableViewer) {
			mTableViewer = tableViewer;
		}

		@Override
		public void updateElement(int index) {
			if (index >= mItems.length) {
				return;
			}
			mTableViewer.replace(mItems[index], index);
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			mItems = (Object[]) newInput;
		}

		void setInput(Object input) {
			Object selected = null;
			if (mTableViewer.getTable().getSelection().length > 0) {
				selected = mTableViewer.getTable().getSelection()[0].getData();
			}

			Object[] items = getElements(input);
			items = Arrays.stream(items).filter(item -> ((ReferrerItem) item).getSize() > 0).toArray();
			mItems = Arrays.copyOf(items, items.length, ReferrerItem[].class);
			sort(mComparator, mDirection);
			mTableViewer.setItemCount(mItems.length);

			int index = Arrays.asList(mItems).indexOf(selected);
			if (index == -1) {
				mTableViewer.getTable().deselectAll();
				return;
			}

			mTableViewer.getTable().setSelection(index);
		}

		void sort(Comparator<ReferrerItem> comparator, int direction) {
			mComparator = comparator;
			mDirection = direction;
			if (mComparator != null) {
				Arrays.sort(mItems, (o1, o2) -> {
					if (((ReferrerItem) o1).getLevel() == ((ReferrerItem) o2).getLevel()) {
						return direction * comparator.compare((ReferrerItem) o1, (ReferrerItem) o2);
					} else {
						return ((ReferrerItem) o1).getLevel() - ((ReferrerItem) o2).getLevel();
					}
				});
			}

			mTableViewer.setInput(mItems);
		}

		Comparator<ReferrerItem> getSortingComparator() {
			return mComparator;
		}

		int getSortingDirection() {
			return mDirection;
		}
	}

	ReferrerTreeViewer(Composite parent, int style) {
		super(parent, style | SWT.VIRTUAL | SWT.FULL_SELECTION);

		mContentProvider = new ReferrerTreeContentProvider(this);
		setContentProvider(mContentProvider);

		createTreeViewerColumn("Referrer", //
				ReferrerItem::getName, //
				null, //
				Comparator.comparing(ReferrerItem::getName), //
				true);

		TableViewerColumn sortingColumn = createTreeViewerColumn("Memory KiB", //
				model -> String.format("%,.2f (%d%%)", //
						(double) model.getMemory() / 1024f, //
						Math.round((double) model.getMemory() * 100f / (double) mHeapSize)), //
				model -> String.format("%,d Bytes", model.getMemory()), //
				mContentProvider.getSortingComparator(), //
				false);

		createTreeViewerColumn("Overhead KiB", //
				model -> String.format("%,.2f (%d%%)", //
						(double) model.getOvhd() / 1024f, //
						Math.round((double) model.getOvhd() * 100f / (double) mHeapSize)), //
				model -> String.format("%,d Bytes", model.getOvhd()), //
				Comparator.comparingLong(ReferrerItem::getOvhd), false);

		createTreeViewerColumn("Objects", //
				model -> String.format("%,d", model.getSize()), //
				null, //
				Comparator.comparingInt(ReferrerItem::getSize), //
				false);

		getTable().setSortColumn(sortingColumn.getColumn());
		getTable().setSortDirection(SWT.DOWN);
		getTable().setLinesVisible(true);
		getTable().setHeaderVisible(true);
		ColumnViewerToolTipSupport.enableFor(this);
	}

	private TableViewerColumn createTreeViewerColumn(
		String label, Function<ReferrerItem, String> labelProvider, Function<ReferrerItem, String> toolTipProvider,
		Comparator<ReferrerItem> comparator, boolean intent) {
		TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
		column.getColumn().setWidth(200);
		column.getColumn().setText(label);
		column.getColumn().setMoveable(true);

		column.setLabelProvider(new OwnerDrawLabelProvider() {
			Color referrerIconColor = new Color(Display.getCurrent(), 116, 184, 250);

			@Override
			protected void paint(Event event, Object element) {
				Widget item = event.item;

				event.gc.setAntialias(SWT.ON);

				Rectangle bounds = ((TableItem) item).getBounds(event.index);
				Point p = event.gc.stringExtent(labelProvider.apply((ReferrerItem) element));

				int margin = (bounds.height - p.y) / 2;
				int dx = bounds.x + margin;
				int dy = bounds.y + margin * 2;

				if (intent) {
					dx += 10 * ((ReferrerItem) element).getLevel();

					Color fg = event.gc.getForeground();
					event.gc.setForeground(referrerIconColor);
					event.gc.drawPolygon(new int[] {3 + dx, dy, //
							6 + dx, 7 + dy, //
							4 + dx, 7 + dy, //
							4 + dx, 9 + dy, //
							8 + dx, 9 + dy, //
							8 + dx, 11 + dy, //
							2 + dx, 11 + dy, //
							2 + dx, 7 + dy, //
							dx, 7 + dy});
					event.gc.setForeground(fg);
					dx += 11 + margin;
				}

				event.gc.drawString(labelProvider.apply((ReferrerItem) element), dx, bounds.y + margin, true);
			}

			@Override
			public void dispose() {
				referrerIconColor.dispose();

				super.dispose();
			}

			@Override
			protected void measure(Event event, Object element) {
				// no op
			}

			@Override
			protected void erase(Event event, Object element) {
				// no op
			}

			@Override
			public String getToolTipText(Object element) {
				if (toolTipProvider == null) {
					return super.getToolTipText(element);
				}
				return toolTipProvider.apply((ReferrerItem) element);
			}
		});

		column.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Comparator<ReferrerItem> newComparator = mContentProvider.getSortingComparator();
				int newDirection = mContentProvider.getSortingDirection();
				if (mContentProvider.getSortingComparator() == comparator) {
					newDirection *= -1;
				} else {
					newComparator = comparator;
					newDirection = -1;
				}

				getTable().setSortColumn(column.getColumn());
				getTable().setSortDirection(newDirection == 1 ? SWT.UP : SWT.DOWN);
				mContentProvider.sort(newComparator, newDirection);
			}
		});

		return column;
	}

	void setHeapSize(long size) {
		mHeapSize = size;
	}
}
