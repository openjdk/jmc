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
package org.openjdk.jmc.rjmx.ui.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import org.openjdk.jmc.ui.common.tree.ITreeNode;
import org.openjdk.jmc.ui.common.util.FilterMatcher;
import org.openjdk.jmc.ui.common.util.FilterMatcher.Where;

public class TreeNodeFilter {

	private static final int FILTER_DELAY = 500;
	private static final int MAX_MATCHES = 8;

	private final TreeViewer tree;
	private final ILabelProvider lp;
	private boolean active;
	private long lastUpdateTime;

	private String filterText;
	private final Runnable asyncUpdater = new Runnable() {

		@Override
		public void run() {
			int delay = (int) (System.currentTimeMillis() - lastUpdateTime);
			if (filterText.length() == 0 || tree.getControl().isDisposed()) {
				active = false;
			} else if (delay >= FILTER_DELAY) {
				createNewFilter();
				active = false;
			} else {
				Display.getCurrent().timerExec(FILTER_DELAY - delay, this);
			}
		}
	};

	public static void install(TreeViewer treeViewer, final Text text, final boolean async) {
		final TreeNodeFilter filter = new TreeNodeFilter(treeViewer);
		text.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (text.getText().length() > 0) {
					text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
					text.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					filter.updateFilter(text.getText(), async);
				} else {
					text.setBackground(null);
					text.setForeground(null);
					filter.reset();
				}
			}
		});
	}

	public TreeNodeFilter(TreeViewer treeViewer) {
		tree = treeViewer;
		lp = ((ILabelProvider) tree.getLabelProvider());
	}

	public void updateFilter(String text, boolean async) {
		filterText = FilterMatcher.autoAddKleene(text, Where.AFTER);
		if (async) {
			lastUpdateTime = System.currentTimeMillis();
			if (!active) {
				Display.getCurrent().timerExec(FILTER_DELAY, asyncUpdater);
				active = true;
			}
		} else {
			createNewFilter();
		}
	}

	public void reset() {
		filterText = ""; //$NON-NLS-1$
		tree.resetFilters();
		tree.collapseAll();
	}

	private void createNewFilter() {
		final Set<ITreeNode> includedNodes = new HashSet<>();
		List<ITreeNode> matches = new ArrayList<>(MAX_MATCHES);
		for (ITreeNode root : ((ITreeNode[]) tree.getInput())) {
			includeNode(root, false, includedNodes, matches);
		}
		tree.setFilters(new ViewerFilter[] {new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return includedNodes.contains(element);
			}

		}});
		if (matches.size() < MAX_MATCHES) {
			for (ITreeNode node : matches) {
				tree.expandToLevel(node, 0);
			}
		} else {
			tree.collapseAll();
		}
	}

	boolean includeNode(ITreeNode node, boolean hasIncludedParent, Set<ITreeNode> included, List<ITreeNode> matches) {
		if (FilterMatcher.getInstance().match(lp.getText(node), filterText, true)) {
			if (matches.size() < MAX_MATCHES) {
				matches.add(node);
			}
			hasIncludedParent = true;
		}
		boolean hasincludedChild = false;
		ITreeNode[] children = node.getChildren();
		if (children != null) {
			for (ITreeNode child : children) {
				hasincludedChild = includeNode(child, hasIncludedParent, included, matches) || hasincludedChild;
			}
		}
		if (hasincludedChild || hasIncludedParent) {
			included.add(node);
			return true;
		}
		return false;
	}

}
