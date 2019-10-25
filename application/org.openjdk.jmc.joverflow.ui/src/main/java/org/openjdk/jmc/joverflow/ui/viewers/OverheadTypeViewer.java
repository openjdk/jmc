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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ClusterType;
import org.openjdk.jmc.joverflow.ui.model.MemoryStatisticsItem;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

public class OverheadTypeViewer extends BaseViewer {

	private final MemoryStatisticsTableViewer mTableViewer;

	private final MemoryStatisticsItem[] mItems = new MemoryStatisticsItem[ClusterType.values().length];
	private ClusterType mCurrentType = ClusterType.ALL_OBJECTS;

	private boolean mAllIncluded = false;

	public OverheadTypeViewer(Composite parent, int style) {
		for (ClusterType t : ClusterType.values()) {
			mItems[t.ordinal()] = new MemoryStatisticsItem(t, 0, 0, 0);
		}

		mTableViewer = new MemoryStatisticsTableViewer(parent, style | SWT.FULL_SELECTION | SWT.BORDER);
		mTableViewer.setPrimaryColumnText("Object Selection");

		mTableViewer.addSelectionChangedListener(event -> setCurrentType(getSelectedType()));
	}

	@Override
	public Control getControl() {
		return mTableViewer.getControl();
	}

	@Override
	public void refresh() {
		mTableViewer.refresh();
	}

	@Override
	public ISelection getSelection() {
		return mTableViewer.getSelection();
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		mTableViewer.setSelection(selection, reveal);
	}

	@Override
	public void include(ObjectCluster oc, RefChainElement ref) {
		if (mAllIncluded) {
			for (MemoryStatisticsItem item : mItems) {
				item.reset();
			}
			mAllIncluded = false;
		}

		if (oc.getType() != null) {
			mItems[oc.getType().ordinal()].addObjectCluster(oc);
		}
	}

	@Override
	public void allIncluded() {
		((MemoryStatisticsTableViewer.MemoryStatisticsContentProvider) mTableViewer.getContentProvider())
				.setInput(mItems);
		mAllIncluded = true;
	}

	@Override
	public void setHeapSize(long size) {
		mTableViewer.setHeapSize(size);
	}

	public ClusterType getCurrentType() {
		return mCurrentType;
	}

	public void setCurrentType(ClusterType type) {
		ClusterType oldType = mCurrentType;
		mCurrentType = type;

		if (oldType != mCurrentType) {
			notifyFilterChangedListeners();
		}
	}

	private ClusterType getSelectedType() {
		ClusterType type = ClusterType.ALL_OBJECTS;
		if (!getSelection().isEmpty()) {
			if (getSelection() instanceof IStructuredSelection) {
				IStructuredSelection selection = (IStructuredSelection) getSelection();
				MemoryStatisticsItem item = ((MemoryStatisticsItem) selection.getFirstElement());
				if (item != null && item.getId() != null) {
					type = (ClusterType) item.getId();
				}
			}
		}

		return type;
	}

	@Override
	public boolean filter(ObjectCluster oc) {
		return getCurrentType() == oc.getType();
	}

	@Override
	public void reset() {
		setCurrentType(ClusterType.ALL_OBJECTS);
	}
}
