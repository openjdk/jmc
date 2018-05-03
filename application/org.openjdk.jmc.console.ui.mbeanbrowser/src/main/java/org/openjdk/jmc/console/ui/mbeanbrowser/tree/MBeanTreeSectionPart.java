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
package org.openjdk.jmc.console.ui.mbeanbrowser.tree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.console.ui.actions.MBeanAutomaticRefreshAction;
import org.openjdk.jmc.console.ui.mbeanbrowser.MBeanBrowserPlugin;
import org.openjdk.jmc.console.ui.mbeanbrowser.messages.internal.Messages;
import org.openjdk.jmc.console.ui.mbeanbrowser.tab.FeatureSectionPart;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.IMBeanPropertiesOrderChangedListener;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.Property;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.PropertyWithMBean;
import org.openjdk.jmc.rjmx.ui.internal.MBeanTreeLabelProvider;
import org.openjdk.jmc.rjmx.ui.internal.MBeanTreeSorter;
import org.openjdk.jmc.rjmx.ui.internal.RJMXUIConstants;
import org.openjdk.jmc.rjmx.ui.internal.TreeNodeBuilder;
import org.openjdk.jmc.rjmx.ui.internal.TreeNodeFilter;
import org.openjdk.jmc.ui.common.tree.ITreeNode;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.IRefreshable;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

/**
 * Class that shows MBeans in a tree.
 */
public class MBeanTreeSectionPart extends MCSectionPart implements IMBeanPropertiesOrderChangedListener {
	public static final String MBEANBROWSER_MBEAN_TREE_NAME = "mbeanbrowser.MBeanTree"; //$NON-NLS-1$

	private final TreeViewer viewer;
	private final MBeanServerConnection mbeanServer;
	private final IMBeanHelperService mbeanService;

	public MBeanTreeSectionPart(Composite parent, FormToolkit toolkit, MBeanServerConnection mbeanServer, String guid,
			IMBeanHelperService mbeanService) {
		super(parent, toolkit, DEFAULT_TITLE_STYLE);
		this.mbeanServer = mbeanServer;
		this.mbeanService = mbeanService;
		getSection().setText(Messages.MBeanTreeSectionPart_MBEAN_TREE_TITLE_TEXT);

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());

		Composite filterComposite = toolkit.createComposite(body);
		toolkit.paintBordersFor(filterComposite);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		filterComposite.setLayout(layout);
		Label filterLabel = toolkit.createLabel(filterComposite, Messages.MBeanTreeSectionPart_MBEAN_TREE_FILTER_TEXT);
		filterLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Text filterText = toolkit.createText(filterComposite, ""); //$NON-NLS-1$
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		filterText.setToolTipText(org.openjdk.jmc.ui.Messages.SEARCH_KLEENE_OR_REGEXP_TOOLTIP);
		filterComposite.setLayoutData(MCLayoutFactory.createFormPageLayoutData(SWT.DEFAULT, SWT.DEFAULT, true, false));

		viewer = createViewer(body, toolkit);
		viewer.getControl()
				.setLayoutData(MCLayoutFactory.createFormPageLayoutData(SWT.DEFAULT, SWT.DEFAULT, true, true));
		TreeNodeFilter.install(viewer, filterText, false);

		ITreeNode[] nodes = buildTreeModel();
		viewer.setInput(nodes);
		getMCToolBarManager().add(new AddMBeanAction(mbeanServer, guid));
		getMCToolBarManager().add(refreshAction);
		mbeanService.addMBeanServerChangeListener(refreshAction);
		setupDoubleClickListener();
	}

	@Override
	public void dispose() {
		MBeanPropertiesOrderer.removePropertiesOrderChangedListener(this);
		mbeanService.removeMBeanServerChangeListener(refreshAction);
		super.dispose();
	}

	public void selectDefaultBean() {
		try {
			ObjectName bean = ObjectName.getInstance("java.lang", "type", "OperatingSystem"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			List<ITreeNode> search = new ArrayList<>();
			search.addAll(Arrays.asList((ITreeNode[]) viewer.getInput()));
			while (!search.isEmpty()) {
				ITreeNode node = search.remove(0);
				if (node.getUserData() instanceof PropertyWithMBean
						&& bean.equals(((PropertyWithMBean) node.getUserData()).getBean())) {
					viewer.setSelection(new StructuredSelection(node), true);
					return;
				}
				ITreeNode[] children = node.getChildren();
				if (children != null) {
					search.addAll(Arrays.asList(children));
				}
			}
			MBeanBrowserPlugin.getDefault().getLogger().warning("Couldn't find " + bean + " in MBean tree"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			e.printStackTrace();
			MBeanBrowserPlugin.getDefault().getLogger().warning("Failed to select OperatingSystem bean: " + e); //$NON-NLS-1$
		}
	}

	private final IRefreshable viewerRefresher = new IRefreshable() {
		@Override
		public boolean refresh() {
			try {
				final ITreeNode[] nodes = buildTreeModel();
				DisplayToolkit.safeAsyncExec(new Runnable() {
					@Override
					public void run() {
						if (!getSection().isDisposed()) {
							// Save state
							Object[] expandedElements = viewer.getExpandedElements();
							TreeItem topItem = viewer.getTree().getTopItem();
							Object topObject = topItem != null ? topItem.getData() : null;
							// Set the new input model
							viewer.getControl().setRedraw(false);
							viewer.setInput(nodes);
							// Restore state
							viewer.setExpandedElements(expandedElements);
							if (topObject != null) {
								TreeItem newTopItem = findItem(topObject, viewer.getTree().getItems());
								if (newTopItem != null) {
									viewer.getTree().setTopItem(newTopItem);
								}
							}
							// Redraw
							viewer.getControl().setRedraw(true);
							viewer.getControl().redraw();
						}
					}

					/**
					 * Traverse the tree items and search for one that matches the desired object.
					 * The tree must not contain any cycles.
					 *
					 * @param object
					 * @param items
					 * @return Matching tree item if found. Null otherwise.
					 */
					private TreeItem findItem(Object object, TreeItem[] items) {
						if (items == null) {
							return null;
						}
						for (TreeItem item : items) {
							if (object.equals(item.getData())) {
								return item;
							}
							TreeItem childItem = findItem(object, item.getItems());
							if (childItem != null) {
								return childItem;
							}
						}
						return null;
					}
				});
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return true;
		}
	};
	private final MBeanAutomaticRefreshAction refreshAction = new MBeanAutomaticRefreshAction(viewerRefresher);

	private TreeViewer createViewer(Composite parent, FormToolkit formToolkit) {
		Tree tree = formToolkit.createTree(parent, SWT.NONE);
		tree.setData("name", MBEANBROWSER_MBEAN_TREE_NAME); //$NON-NLS-1$
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		TreeViewer viewer = new TreeViewer(tree);
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new TreeStructureContentProvider());
		viewer.setComparator(new MBeanTreeSorter());
		viewer.setLabelProvider(new MBeanTreeLabelProvider(null));
		MBeanPropertiesOrderer.addPropertiesOrderChangedListener(this);
		ColumnViewerToolTipSupport.enableFor(viewer);

		MCContextMenuManager mm = MCContextMenuManager.create(tree);
		mm.add(new UnregisterMBeanAction(mbeanServer, viewer));

		return viewer;
	}

	public void addMBeanListener(final FeatureSectionPart infoPart) {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selected = ((IStructuredSelection) event.getSelection()).getFirstElement();
				ObjectName converted = AdapterUtil.getAdapter(selected, ObjectName.class);
				if (converted != null) {
					infoPart.showBean(converted);
				}
			}
		});
	}

	private void setupDoubleClickListener() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				ITreeNode elementNode = (ITreeNode) selection.getFirstElement();
				if (elementNode.getAdapter(ObjectName.class) == null) {
					viewer.setExpandedState(elementNode, !viewer.getExpandedState(elementNode));
				}
			}
		});
	}

	@Override
	public void propertiesOrderChanged(PropertyChangeEvent e) {
		if (!e.getProperty().equals(RJMXUIConstants.PROPERTY_MBEAN_SHOW_COMPRESSED_PATHS)) {
			viewerRefresher.refresh();
		}
	}

	private ITreeNode[] buildTreeModel() {
		try {
			Iterable<ObjectName> ons = mbeanService.getMBeanNames();
			TreeNodeBuilder root = new TreeNodeBuilder();
			for (ObjectName bean : ons) {
				TreeNodeBuilder node = root.getUniqueChild(bean.getDomain());
				Property[] properties = MBeanPropertiesOrderer.getOrderedProperties(bean);
				for (Property p : properties) {
					node = node.get(p.getStringRepresentation());
					if (p instanceof PropertyWithMBean || node.getValue() == null) {
						node.setValue(p);
					}
				}
			}
			return root.getChildren(null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
