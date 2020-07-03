/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.joverflow.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.Page;
import org.openjdk.jmc.joverflow.ui.swt.Breadcrumb;
import org.openjdk.jmc.joverflow.ui.swt.BreadcrumbItem;
import org.openjdk.jmc.joverflow.ui.swt.Treemap;
import org.openjdk.jmc.joverflow.ui.swt.TreemapItem;
import org.openjdk.jmc.joverflow.ui.swt.events.TreemapListener;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

class TreemapPage extends Page implements ModelListener {
	private static final Color[] COLORS = { //
			new Color(Display.getCurrent(), 250, 206, 210), // red
			new Color(Display.getCurrent(), 185, 214, 255), // blue
			new Color(Display.getCurrent(), 229, 229, 229), // grey
			new Color(Display.getCurrent(), 255, 231, 199), // orange
			new Color(Display.getCurrent(), 171, 235, 238), // aqua
			new Color(Display.getCurrent(), 228, 209, 252), // purple
			new Color(Display.getCurrent(), 255, 255, 255), // white
			new Color(Display.getCurrent(), 205, 249, 212), // green
	};
	private static final String LABEL_ROOT = "[ROOT]"; //$NON-NLS-1$

	private final JOverflowEditor editor;
	private final TreemapAction[] treemapActions;

	private Composite container;
	private StackLayout containerLayout;
	private Composite messageContainer;
	private Composite treemapContainer;

	private Label message;
	private Treemap treemap;
	private Breadcrumb breadcrumb;

	private HashMap<String, Double> classes = new HashMap<>();

	TreemapPage(JOverflowEditor editor, TreemapAction[] treemapActions) {
		this.editor = Objects.requireNonNull(editor);
		this.treemapActions = Objects.requireNonNull(treemapActions);
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.PUSH);
		containerLayout = new StackLayout();
		container.setLayout(containerLayout);

		messageContainer = new Composite(container, SWT.NONE);
		FillLayout layout = new FillLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		messageContainer.setLayout(layout);

		message = new Label(messageContainer, SWT.NONE);
		message.setText(Messages.TreemapPage_NO_INSTANCES_SELECTED);

		treemapContainer = new Composite(container, SWT.NONE);
		treemapContainer.setLayout(new FormLayout());

		breadcrumb = new Breadcrumb(treemapContainer, SWT.NONE);
		{
			FormData bcLayoutData = new FormData();
			bcLayoutData.top = new FormAttachment(0, 0);
			bcLayoutData.left = new FormAttachment(0, 0);
			bcLayoutData.right = new FormAttachment(100, 0);
			breadcrumb.setLayoutData(bcLayoutData);
		}

		treemap = new Treemap(treemapContainer, SWT.NONE);
		{
			FormData tmLayoutData = new FormData();
			tmLayoutData.bottom = new FormAttachment(100);
			tmLayoutData.top = new FormAttachment(breadcrumb);
			tmLayoutData.left = new FormAttachment(0);
			tmLayoutData.right = new FormAttachment(100, 0);
			treemap.setLayoutData(tmLayoutData);
		}
		treemap.setText(LABEL_ROOT);

		// set "[ROOT]" item
		{
			TreemapItem rootItem = treemap.getRootItem();
			BreadcrumbItem breadcrumbItem = new BreadcrumbItem(breadcrumb, SWT.NONE);
			breadcrumbItem.setData(rootItem);
			breadcrumbItem.setText(rootItem.getText());
		}

		// links treemap and breadcrumb events
		{
			breadcrumb.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {
				if (!(selectionEvent.data instanceof TreemapItem)) {
					return;
				}

				TreemapItem item = (TreemapItem) selectionEvent.data;
				treemap.setTopItem(item);
			}));

			treemap.addTreemapListener(TreemapListener.treemapTopChangedAdapter(treemapEvent -> {
				TreemapItem item = (TreemapItem) treemapEvent.item;
				breadcrumb.removeAll();

				List<TreemapItem> path = new ArrayList<>();
				do {
					path.add(item);
					item = item.getParentItem();
				} while (item != null);

				Collections.reverse(path);
				for (TreemapItem i : path) {
					BreadcrumbItem breadcrumbItem = new BreadcrumbItem(breadcrumb, SWT.NONE);
					breadcrumbItem.setData(i);
					breadcrumbItem.setText(i.getText());
				}
			}));
		}

		// rebind action buttons
		{
			treemap.addSelectionListener(
					SelectionListener.widgetSelectedAdapter(selectionEvent -> bindTreemapActions()));
			treemap.addTreemapListener(TreemapListener.treemapTopChangedAdapter(treemapEvent -> bindTreemapActions()));
		}

		containerLayout.topControl = messageContainer;
		updateInput();
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public void setFocus() {
		getControl().setFocus();
	}

	@Override
	public void include(ObjectCluster cluster, RefChainElement referenceChain) {
		if (cluster.getObjectCount() == 0) {
			return;
		}

		JavaClass clazz = getObjectAtPosition(cluster.getGlobalObjectIndex(0)).getClazz();
		String className = clazz.getName();
		if (className.charAt(0) == '[') {
			className = cluster.getClassName();
		}

		classes.putIfAbsent(className, 0.0);
		double size = classes.get(className);
		size += cluster.getMemory();
		classes.put(className, size);
	}

	@Override
	public void allIncluded() {
		updateInput();
		classes.clear();

		bindTreemapActions();
	}

	public void bindTreemapActions() {
		if (containerLayout == null || containerLayout.topControl != treemapContainer) {
			Stream.of(treemapActions).forEach((action) -> action.setEnabled(false));
			return;
		}

		TreemapItem selected = treemap.getSelection();
		TreemapItem root = treemap.getRootItem();
		TreemapItem top = treemap.getTopItem();

		Stream.of(treemapActions).forEach((action) -> {
			switch (action.getType()) {
			case ZOOM_IN:
				action.setEnabled(selected != null && selected != top
						&& !(selected.getItemCount() == 0 && selected.getParentItem() == top));
				action.setRunnable(() -> treemap.setTopItem(selected));
				break;
			case ZOOM_OUT:
				action.setEnabled(top.getParentItem() != null);
				action.setRunnable(() -> treemap.setTopItem(top.getParentItem()));
				break;
			case ZOOM_RESET:
				action.setEnabled(top != root);
				action.setRunnable(() -> treemap.setTopItem(root));
			}
		});
	}

	private void updateInput() {
		if (classes.size() == 0) {
			containerLayout.topControl = messageContainer;
			container.layout();
			return;
		}

		if (treemap == null) {
			return;
		}

		treemap.removeAll();
		HashMap<String, TreemapItem> items = new HashMap<>();
		for (Map.Entry<String, Double> entry : classes.entrySet()) {
			addTreemapItem(treemap, items, entry.getKey(), entry.getValue());
		}

		TreemapItem rootItem = treemap.getRootItem();
		rootItem.setToolTipText(LABEL_ROOT);
		setColorAndToolTip(rootItem, 0);
		treemap.setTopItem(rootItem);
		treemap.setSelection(null);

		containerLayout.topControl = treemapContainer;
		container.layout();
	}

	private void addTreemapItem(Treemap parent, Map<String, TreemapItem> items, String fullName, double size) {
		if (items.containsKey(fullName) && size != 0) {
			TreemapItem item = items.get(fullName);
			double bytes = item.getWeight() + size;
			item.setWeight(bytes);
			item.setToolTipText(fullName);
			return;
		}

		if (fullName.indexOf('.') == -1) {
			TreemapItem item = new TreemapItem(parent, SWT.NONE);
			item.setText(fullName);
			if (size != 0) {
				item.setWeight(size);
			}
			item.setToolTipText(fullName);
			items.put(fullName, item);
			return;
		}

		String parentName = fullName.substring(0, fullName.lastIndexOf('.'));
		if (!items.containsKey(parentName)) {
			addTreemapItem(parent, items, parentName, 0);
		}

		TreemapItem parentItem = items.get(parentName);
		TreemapItem item = new TreemapItem(parentItem, SWT.NONE);
		item.setText(fullName.substring(parentName.length() + 1));
		item.setToolTipText(fullName);
		if (size != 0) {
			item.setWeight(size);
		}
		items.put(fullName, item);
	}

	private void setColorAndToolTip(TreemapItem item, int depth) {
		item.setToolTipText(item.getToolTipText() + "\n" + getHumanReadableSize(item.getWeight())); //$NON-NLS-1$
		item.setBackground(COLORS[depth % COLORS.length]);

		for (TreemapItem child : item.getItems()) {
			setColorAndToolTip(child, depth + 1);
		}
	}

	private String getHumanReadableSize(double bytes) {
		String unit = "B"; //$NON-NLS-1$
		double quantity = bytes;
		if (quantity > 1024) {
			quantity /= 1024;
			unit = "KiB"; //$NON-NLS-1$
		}
		if (quantity > 1024) {
			quantity /= 1024;
			unit = "MiB"; //$NON-NLS-1$
		}
		if (quantity > 1024) {
			quantity /= 1024;
			unit = "GiB"; //$NON-NLS-1$
		}
		if (quantity > 1024) {
			quantity /= 1024;
			unit = "TiB"; //$NON-NLS-1$
		}

		return String.format("%.2f %s", quantity, unit); //$NON-NLS-1$
	}

	private JavaHeapObject getObjectAtPosition(int globalObjectPos) {
		return editor.getSnapshot().getObjectAtGlobalIndex(globalObjectPos);
	}
}
