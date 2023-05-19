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
import java.util.Arrays;
import java.util.List;

import javax.management.ObjectName;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
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

import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.Property;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.PropertyWithMBean;
import org.openjdk.jmc.ui.common.tree.ITreeNode;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

public class AttributeSelectorComponent extends Composite {

	private static final int MAX_ATTRIBUTES_SYNC_FILTER = 5000;

	private Text filterText;
	private TreeViewer mbeanTreeViewer;
	private final AttributeSelectionViewModel m_viewModel;
	private AttributeSelectionContentModel m_selectorModel;
	private final ListenerList<ISelectionChangedListener> selectionChangedListeners;

	public AttributeSelectorComponent(Composite parent, int style, AttributeSelectionViewModel viewModel) {
		super(parent, style);
		m_viewModel = viewModel;
		selectionChangedListeners = new ListenerList<>();
		initComponent();
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.add(listener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListeners.remove(listener);
	}

	/**
	 * Notifies any selection changed listeners that the viewer's selection has changed. Only
	 * listeners registered at the time this method is called are notified.
	 *
	 * @param event
	 *            a selection changed event
	 * @see ISelectionChangedListener#selectionChanged
	 */
	protected void fireSelectionChanged(final SelectionChangedEvent event) {
		for (ISelectionChangedListener listener : selectionChangedListeners) {
			final ISelectionChangedListener l = listener;
			SafeRunnable.run(new SafeRunnable() {
				@Override
				public void run() {
					l.selectionChanged(event);
				}
			});
		}
	}

	public MRI[] getSelection() {
		if (m_selectorModel == null) {
			return new MRI[0];
		}
		return m_selectorModel.getSelectedAttributes();
	}

	public void setInput(AttributeSelectionContentModel selectorModel) {
		m_selectorModel = selectorModel;
		IMRIMetadataService mds = selectorModel.getMetadataService();
		// Build model
		TreeNodeBuilder root = new TreeNodeBuilder();
		int mriCount = 0;
		for (MRI mri : selectorModel.getAvailableAttributes()) {
			ObjectName bean = mri.getObjectName();
			IMRIMetadata md = mds.getMetadata(mri);
			if (!m_viewModel.isNumericalOnly() || MRIMetadataToolkit.isNumerical(md)) {
				TreeNodeBuilder node = root.getUniqueChild(bean.getDomain());
				Property[] properties = MBeanPropertiesOrderer.getOrderedProperties(bean);
				for (Property p : properties) {
					node = node.get(p.getStringRepresentation());
					if (p instanceof PropertyWithMBean || node.getValue() == null) {
						node.setValue(p);
					}
				}
				for (MRI parentMri : mri.getParentMRIs()) {
					node = node.get(parentMri, mds.getMetadata(parentMri));
				}
				node.get(mri, md);
				mriCount++;
			}
		}

		ITreeNode[] tree = root.getChildren(null);
		mbeanTreeViewer.setInput(tree);
		// Select and expand
		List<ITreeNode> selectList = new ArrayList<>();
		List<ITreeNode> expandList = new ArrayList<>();
		List<ITreeNode> search = new ArrayList<>();
		search.addAll(Arrays.asList(tree));
		while (!search.isEmpty()) {
			ITreeNode node = search.remove(0);
			if (node.getUserData() instanceof IMRIMetadata) {
				MRI mri = ((IMRIMetadata) node.getUserData()).getMRI();
				for (MRI e : selectorModel.getInitialExpandedAttributes()) {
					if (mri.equals(e)) {
						expandList.add(node);
					}
				}
				for (MRI s : selectorModel.getSelectedAttributes()) {
					if (mri.equals(s)) {
						selectList.add(node);
					}
				}
			}
			ITreeNode[] children = node.getChildren();
			if (children != null) {
				search.addAll(Arrays.asList(children));
			}
		}
		if (!selectList.isEmpty()) {
			mbeanTreeViewer.setSelection(new StructuredSelection(selectList), true);
		}
		for (ITreeNode e : expandList) {
			mbeanTreeViewer.expandToLevel(e, 0);
		}
		TreeNodeFilter.install(mbeanTreeViewer, filterText, mriCount > MAX_ATTRIBUTES_SYNC_FILTER);
	}

	protected int getTreeStyle() {
		int style = SWT.BORDER;
		if (m_viewModel.isMultiSelectionAllowed()) {
			style = style | SWT.MULTI;
		} else {
			style = style | SWT.SINGLE;
		}
		return style;
	}

	protected TreeViewer createTreeViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, getTreeStyle());
		viewer.setContentProvider(new TreeStructureContentProvider());
		viewer.setLabelProvider(new MBeanTreeLabelProvider(m_viewModel.getContentType()));
		ColumnViewerToolTipSupport.enableFor(viewer);
		return viewer;
	}

	private void initComponent() {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		setLayout(layout);

		Composite filterAreaComposite = new Composite(this, SWT.NONE);
		GridLayout filterLayout = new GridLayout(2, false);
		filterLayout.marginWidth = 0;
		filterLayout.marginHeight = 0;
		filterAreaComposite.setLayout(filterLayout);
		filterAreaComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		Label filterLabel = new Label(filterAreaComposite, SWT.NONE);
		filterLabel.setText(Messages.AttributeSelectorDialog_LABEL_FILTER_TEXT);
		filterLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		filterText = new Text(filterAreaComposite, SWT.BORDER);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mbeanTreeViewer = createTreeViewer(this);
		mbeanTreeViewer.setUseHashlookup(true);
		mbeanTreeViewer.setComparator(new MBeanTreeSorter());
		mbeanTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				List<IMRIMetadata> addList = new ArrayList<>();
				List<MRI> selectedAttributes = new ArrayList<>();
				for (Object o : ((IStructuredSelection) event.getSelection()).toList()) {
					Object e = ((ITreeNode) o).getUserData();
					if (e instanceof IMRIMetadata) {
						addList.add((IMRIMetadata) e);
					}
				}
				for (int i = 0; i < addList.size(); i++) {
					IMRIMetadata metadata = addList.get(i);
					if (!MRIMetadataToolkit.isComposite(metadata)) {
						String unitString = metadata.getUnitString();
						if (m_viewModel.getContentType() == null || unitString == null
								|| m_viewModel.getContentType().equals(UnitLookup.getContentType(unitString))) {
							selectedAttributes.add(metadata.getMRI());
						}
					} else if (m_viewModel.isMultiSelectionAllowed()) {
						for (MRI child : m_selectorModel.getAvailableAttributes()) {
							if (metadata.getMRI().isChild(child)) {
								IMRIMetadata childMd = m_selectorModel.getMetadataService().getMetadata(child);
								if (!m_viewModel.isNumericalOnly() || MRIMetadataToolkit.isNumerical(childMd)) {
									addList.add(childMd);
								}
							}
						}
					}
				}
				m_selectorModel.setSelectedAttributes(selectedAttributes.toArray(new MRI[selectedAttributes.size()]));
				fireSelectionChanged(
						new SelectionChangedEvent(mbeanTreeViewer, new StructuredSelection(getSelection())));
			}
		});
		mbeanTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object element = selection.getFirstElement();
				mbeanTreeViewer.setExpandedState(element, !mbeanTreeViewer.getExpandedState(element));
			}
		});
		mbeanTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));

	}
}
