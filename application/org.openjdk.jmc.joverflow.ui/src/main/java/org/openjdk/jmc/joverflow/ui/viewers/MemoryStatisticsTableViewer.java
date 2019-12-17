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
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import org.openjdk.jmc.joverflow.ui.model.MemoryStatisticsItem;

class MemoryStatisticsTableViewer extends TableViewer {

	private long mHeapSize = 1;
	private final TableViewerColumn mPrimaryColumn;
	private final MemoryStatisticsContentProvider mContentProvider;
	private MemoryStatisticsItem mHighlightedItem = null;
	private PieChartViewer mPieChartViewer;

	static class MemoryStatisticsContentProvider extends ArrayContentProvider implements ILazyContentProvider {
		private Comparator<MemoryStatisticsItem> mComparator = Comparator
				.comparingLong(MemoryStatisticsItem::getMemory);
		private int mDirection = -1;

		private TableViewer mTableViewer;
		private Object[] mItems = new MemoryStatisticsItem[0];

		MemoryStatisticsContentProvider(TableViewer tableViewer) {
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
			items = Arrays.stream(items).filter(item -> ((MemoryStatisticsItem) item).getSize() > 0).toArray();
			mItems = Arrays.copyOf(items, items.length, MemoryStatisticsItem[].class);
			sort(mComparator, mDirection);
			mTableViewer.setItemCount(mItems.length);

			int index = Arrays.asList(mItems).indexOf(selected);
			if (index == -1) {
				mTableViewer.getTable().deselectAll();
				return;
			}

			mTableViewer.getTable().setSelection(index);
		}

		void sort(Comparator<MemoryStatisticsItem> comparator, int direction) {
			mComparator = comparator;
			mDirection = direction;
			if (mComparator != null) {
				Arrays.sort(mItems, (o1, o2) -> comparator.compare((MemoryStatisticsItem) o1, (MemoryStatisticsItem) o2)
						* direction);
			}
			mTableViewer.setInput(mItems);
		}

		Comparator<MemoryStatisticsItem> getSortingComparator() {
			return mComparator;
		}

		int getSortingDirection() {
			return mDirection;
		}

	}

	MemoryStatisticsTableViewer(Composite parent, int style) {
		super(parent, style | SWT.VIRTUAL | SWT.FULL_SELECTION);

		mContentProvider = new MemoryStatisticsContentProvider(this);
		setContentProvider(mContentProvider);

		mPrimaryColumn = createTableColumnViewer("Name", //
				MemoryStatisticsItem::getName, //
				null, //
				(item) -> mPieChartViewer != null ? mPieChartViewer.getArcAttributeProvider().getColor(item) : null, //
				Comparator.comparing(MemoryStatisticsItem::getName));

		TableViewerColumn sortingColumn = createTableColumnViewer("Memory KB", //
				model -> String.format("%,.2f (%d%%)", //
						(double) model.getMemory() / 1024f, //
						Math.round((double) model.getMemory() * 100f / (double) mHeapSize)), //
				model -> String.format("%,d Bytes", model.getMemory()), //
				null, //
				mContentProvider.getSortingComparator());

		createTableColumnViewer("Overhead KB", //
				model -> String.format("%,.2f (%d%%)", //
						(double) model.getOverhead() / 1024f, //
						Math.round((double) model.getOverhead() * 100f / (double) mHeapSize)), //
				model -> String.format("%,d Bytes", model.getMemory()), //
				null, //
				Comparator.comparingLong(MemoryStatisticsItem::getMemory));

		createTableColumnViewer("Objects", //
				model -> String.format("%,d", model.getSize()), //
				null, //
				null, //
				Comparator.comparingInt(MemoryStatisticsItem::getSize));

		getTable().setSortColumn(sortingColumn.getColumn());
		getTable().setSortDirection(SWT.DOWN);
		getTable().setLinesVisible(true);
		getTable().setHeaderVisible(true);
		ColumnViewerToolTipSupport.enableFor(this);

		this.getTable().addMouseMoveListener(e -> {
			ViewerRow row = MemoryStatisticsTableViewer.this.getViewerRow(new Point(e.x, e.y));
			if (row == null) {
				setHighlightedItem(null);
			} else {
				setHighlightedItem((MemoryStatisticsItem) row.getElement());
			}
		});

		this.getTable().addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(MouseEvent e) {
				setHighlightedItem(null);
			}

			public void mouseEnter(MouseEvent e) {
				setHighlightedItem(null);
			}
		});
	}

	public void setPieChartViewer(PieChartViewer pieChartViewer) {
		mPieChartViewer = pieChartViewer;
	}

	public void setHighlightedItem(MemoryStatisticsItem item) {
		if (mHighlightedItem != item) {
			MemoryStatisticsItem oldItem = mHighlightedItem;
			mHighlightedItem = item;

			if (oldItem != null) {
				mPrimaryColumn.getViewer().update(oldItem, null);
			}

			if (mHighlightedItem != null) {
				mPrimaryColumn.getViewer().update(mHighlightedItem, null);
			}
		}

		if (mPieChartViewer != null) {
			mPieChartViewer.setHighlightedItem(item);
		}
	}

	private TableViewerColumn createTableColumnViewer(
		String label, Function<MemoryStatisticsItem, String> labelProvider,
		Function<MemoryStatisticsItem, String> toolTipProvider, Function<MemoryStatisticsItem, Color> colorProvider,
		Comparator<MemoryStatisticsItem> comparator) {
		TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
		column.getColumn().setWidth(200);
		column.getColumn().setText(label);
		column.getColumn().setMoveable(true);

		column.setLabelProvider(new OwnerDrawLabelProvider() {
			@Override
			protected void paint(Event event, Object element) {
				Widget item = event.item;

				event.gc.setAntialias(SWT.ON);

				Rectangle bounds = ((TableItem) item).getBounds(event.index);
				Color bg = event.gc.getBackground();
				Color fg = event.gc.getForeground();

				Point p = event.gc.stringExtent(labelProvider.apply((MemoryStatisticsItem) element));

				int margin = (bounds.height - p.y) / 2;
				int dx = bounds.x + margin;

				if (colorProvider != null) {
					Color color = colorProvider.apply((MemoryStatisticsItem) element);
					if (color != null) {
						event.gc.setBackground(color);
						if (mHighlightedItem == element) {
							event.gc.fillArc(dx - margin / 2, bounds.y + margin * 2, p.y - margin, p.y - margin, 0,
									360);
						} else {
							event.gc.fillArc(dx, bounds.y + margin * 2, p.y - margin * 2, p.y - margin * 2, 0, 360);
						}
						dx += p.y + margin;
					}
				}

				event.gc.drawString(labelProvider.apply((MemoryStatisticsItem) element), dx, bounds.y + margin, true);

				event.gc.setBackground(bg);
				event.gc.setForeground(fg);
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
				return toolTipProvider.apply((MemoryStatisticsItem) element);
			}
		});

		column.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Comparator<MemoryStatisticsItem> newComparator = mContentProvider.getSortingComparator();
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

	void setPrimaryColumnText(String text) {
		mPrimaryColumn.getColumn().setText(text);
	}
}
