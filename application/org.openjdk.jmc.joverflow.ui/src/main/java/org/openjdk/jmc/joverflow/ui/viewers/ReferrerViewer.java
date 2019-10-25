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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;
import org.openjdk.jmc.joverflow.ui.model.ReferrerItem;
import org.openjdk.jmc.joverflow.ui.model.ReferrerItemBuilder;

public class ReferrerViewer extends BaseViewer {

	private final ReferrerTreeViewer mTreeViewer;
	private ReferrerItemBuilder mItemBuilder;

	private ReferrerItem mSelectedItem;

	public ReferrerViewer(Composite parent, int style) {
		mTreeViewer = new ReferrerTreeViewer(parent, style | SWT.FULL_SELECTION | SWT.BORDER);

		mTreeViewer.getControl().addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// no op
			}

			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1) { // left button
					if (mTreeViewer.getSelection().isEmpty()) {
						return;
					}
					IStructuredSelection selection = (IStructuredSelection) mTreeViewer.getSelection();
					mSelectedItem = (ReferrerItem) selection.getFirstElement();

					notifyFilterChangedListeners();
				}
				if (e.button == 3) { // right button
					reset();
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {
				// no op
			}
		});
	}

	@Override
	public Control getControl() {
		return mTreeViewer.getControl();
	}

	@Override
	public ISelection getSelection() {
		return mTreeViewer.getSelection();
	}

	@Override
	public void refresh() {
		mTreeViewer.refresh();
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		mTreeViewer.setSelection(selection, reveal);
	}

	@Override
	public void include(ObjectCluster oc, RefChainElement ref) {
		if (mItemBuilder == null) {
			mItemBuilder = new ReferrerItemBuilder(oc, ref);
		} else {
			mItemBuilder.addCluster(oc, ref);
		}
	}

	@Override
	public void allIncluded() {
		if (mItemBuilder == null) {
			((ReferrerTreeViewer.ReferrerTreeContentProvider) mTreeViewer.getContentProvider()).setInput(null);
		} else {
			((ReferrerTreeViewer.ReferrerTreeContentProvider) mTreeViewer.getContentProvider())
					.setInput(mItemBuilder.buildReferrerList());
			mItemBuilder = null;
		}
	}

	@Override
	public void setHeapSize(long size) {
		mTreeViewer.setHeapSize(size);
	}

	@Override
	public void reset() {
		mSelectedItem = null;
		notifyFilterChangedListeners();
	}

	@Override
	public boolean filter(RefChainElement rce) {
		if (mSelectedItem == null) {
			return true;
		}
		return mSelectedItem.check(rce);
	}
}
