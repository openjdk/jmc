/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.MemoryStatisticsItem;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;
import org.openjdk.jmc.joverflow.ui.swt.ArcItem;
import org.openjdk.jmc.joverflow.ui.swt.FilterList;
import org.openjdk.jmc.joverflow.ui.util.ColorIndexedArcAttributeProvider;

public class AncestorViewer extends BaseViewer {
	private final SashForm mContainer;
	private final PieChartViewer mPieChart;
	private final FilterList<RefChainElement> mFilterList;
	private final Text mInput;
	private final MemoryStatisticsTableViewer mTableViewer;

	private String mPrefix = ""; //$NON-NLS-1$

	private RefChainElement lastRef;
	private MemoryStatisticsItem lastItem;
	private final Map<Object, MemoryStatisticsItem> items = new HashMap<>();

	private boolean mAllIncluded = false;

	public AncestorViewer(Composite parent, int style) {
		mContainer = new SashForm(parent, style);

		{
			Composite leftContainer = new Composite(mContainer, SWT.BORDER);
			leftContainer.setLayout(new FormLayout());

			Label title = new Label(leftContainer, SWT.NONE);
			title.setText("Ancestor Referrer");
			{
				FormData data = new FormData();
				data.top = new FormAttachment(0, 10);
				data.left = new FormAttachment(0, 10);
				title.setLayoutData(data);

			}

			{
				Button update = new Button(leftContainer, SWT.NONE);
				update.setText("Update");
				update.addListener(SWT.Selection, event -> updatePrefixFilter());
				{
					FormData data = new FormData();
					data.bottom = new FormAttachment(100, -10);
					data.right = new FormAttachment(100, -10);
					update.setLayoutData(data);
				}

				mInput = new Text(leftContainer, SWT.BORDER);
				mInput.setMessage("Ancestor prefix");
				mInput.addListener(SWT.Traverse, event -> {
					if (event.detail == SWT.TRAVERSE_RETURN) {
						updatePrefixFilter();
					}
				});
				{

					FormData fd_text = new FormData();
					fd_text.right = new FormAttachment(update, -10);
					fd_text.bottom = new FormAttachment(update, 0, SWT.CENTER);
					fd_text.left = new FormAttachment(0, 10);
					mInput.setLayoutData(fd_text);
				}

				SashForm container = new SashForm(leftContainer, SWT.VERTICAL);
				{
					FormData fd_sashForm = new FormData();
					fd_sashForm.bottom = new FormAttachment(update, -10);
					fd_sashForm.top = new FormAttachment(title, 10);
					fd_sashForm.right = new FormAttachment(100, -10);
					fd_sashForm.left = new FormAttachment(0, 10);
					container.setLayoutData(fd_sashForm);
				}

				mPieChart = new PieChartViewer(container, SWT.NONE);
				mPieChart.setContentProvider(ArrayContentProvider.getInstance());
				ColorIndexedArcAttributeProvider provider = new ColorIndexedArcAttributeProvider() {
					@Override
					public int getWeight(Object element) {
						return (int) ((MemoryStatisticsItem) element).getMemory();
					}
				};
				provider.setMinimumArcAngle(5);
				mPieChart.setArcAttributeProvider(provider);
				mPieChart.setMinimumArcAngle(5);
				mPieChart.getPieChart().setZoomRatio(1.2);
				mPieChart.setComparator(new ViewerComparator() {
					@Override
					public int compare(Viewer viewer, Object e1, Object e2) {
						return (int) (((MemoryStatisticsItem) e2).getMemory()
								- ((MemoryStatisticsItem) e1).getMemory());
					}
				});

				mFilterList = new FilterList<>(container, SWT.NONE);
				mFilterList.addFilterChangedListener(this::notifyFilterChangedListeners);

				container.setWeights(new int[] {3, 2});
			}

		}

		{
			Composite tableContainer = new Composite(mContainer, SWT.BORDER);
			tableContainer.setLayout(new FillLayout(SWT.HORIZONTAL));

			mTableViewer = new MemoryStatisticsTableViewer(tableContainer, SWT.NONE);

			BiConsumer<MemoryStatisticsItem, Boolean> addFilter = (item, exclusion) -> {
				if (item.getId() == null) {
					return;
				}

				mFilterList.addFilter(new Predicate<RefChainElement>() {
					final String ancestor = item.getId().toString();
					final boolean excluded = exclusion;

					@Override
					public boolean test(RefChainElement referrer) {
						while (referrer != null) {
							String refName = referrer.toString();
							if (ancestor.equals(refName)) {
								return !excluded;
							}
							referrer = referrer.getReferer();
						}
						return excluded;
					}

					@Override
					public String toString() {
						return "Ancestors" + (excluded ? " \u220C " : " \u220B ") + ancestor; //$NON-NLS-2$ //$NON-NLS-3$
					}

					@Override
					public int hashCode() {
						return ancestor.hashCode();
					}

					@Override
					public boolean equals(Object obj) {
						if (obj == null) {
							return false;
						}
						if (getClass() != obj.getClass()) {
							return false;
						}

						return hashCode() == obj.hashCode();
					}
				});

			};

			mPieChart.getPieChart().addMouseListener(new MouseListener() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// no op
				}

				@Override
				public void mouseDown(MouseEvent e) {
					// no op
				}

				@Override
				public void mouseUp(MouseEvent e) {
					ArcItem item = mPieChart.getPieChart().getHighlightedItem();
					if (item == null) {
						return;
					}

					if (item.getData() == null) {
						return;
					}

					addFilter.accept((MemoryStatisticsItem) item.getData(), e.button != 1);
				}
			});

			mTableViewer.getTable().addMouseListener(new MouseListener() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					// no op
				}

				@Override
				public void mouseDown(MouseEvent e) {
					// no op
				}

				@Override
				public void mouseUp(MouseEvent e) {
					if (e.button != 1 && e.button != 3) {
						return;
					}

					if (mTableViewer.getSelection().isEmpty()) {
						return;
					}

					IStructuredSelection selection = (IStructuredSelection) mTableViewer.getSelection();
					MemoryStatisticsItem item = (MemoryStatisticsItem) selection.getFirstElement();
					addFilter.accept(item, e.button != 1);
				}
			});
		}

		mContainer.setWeights(new int[] {1, 2});

		mTableViewer.setPieChartViewer(mPieChart);
		mPieChart.setTableViewer(mTableViewer);
	}

	@Override
	public Control getControl() {
		return mContainer;
	}

	@Override
	public ISelection getSelection() {
		return mTableViewer.getSelection();
	}

	@Override
	public void refresh() {
		mTableViewer.refresh();
		mPieChart.refresh();
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		mTableViewer.setSelection(selection, reveal);
		mPieChart.setSelection(selection, reveal);
	}

	private String getAncestorReferrer(RefChainElement referrer) {
		while (referrer != null) {
			if (referrer.getJavaClass() == null) {
				if (referrer.getReferer() != null) {
					FlightRecorderUI.getDefault().getLogger()
							.warning("JavaClass for " + referrer + " is null but referrer is " + referrer.getReferer());
				}
				break; // GC root
			} else if (referrer.toString().startsWith(mPrefix)) {
				return referrer.toString();
			}
			referrer = referrer.getReferer();
		}
		return null;
	}

	@Override
	public void include(ObjectCluster oc, RefChainElement ref) {
		if (mAllIncluded) {
			for (MemoryStatisticsItem item : items.values()) {
				item.reset();
			}
			mAllIncluded = false;
		}

		if (ref != lastRef) {
			lastRef = ref;
			String s = getAncestorReferrer(ref);
			lastItem = items.get(s);
			if (lastItem == null) {
				lastItem = new MemoryStatisticsItem(s, 0, 0, 0);
				items.put(s, lastItem);
			}
		}
		lastItem.addObjectCluster(oc);
	}

	@Override
	public void allIncluded() {
		Collection<MemoryStatisticsItem> values = items.values();

		((MemoryStatisticsTableViewer.MemoryStatisticsContentProvider) mTableViewer.getContentProvider())
				.setInput(values);
		mPieChart.setInput(values);

		mAllIncluded = true;
		lastRef = null;
	}

	private void updatePrefixFilter() {
		mPrefix = mInput.getText();

		if (mTableViewer != null) {
			notifyFilterChangedListeners();
		}
	}

	@Override
	public void setHeapSize(long size) {
		mTableViewer.setHeapSize(size);
	}

	@Override
	public boolean filter(ObjectCluster oc) {
		return true;
	}

	@Override
	public boolean filter(RefChainElement rce) {
		return mFilterList.filter(rce);
	}

	@Override
	public void reset() {
		mFilterList.reset();
		mInput.setText(""); //$NON-NLS-1$
		updatePrefixFilter();
	}
}
