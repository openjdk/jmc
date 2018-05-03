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
package org.openjdk.jmc.console.ui.mbeanbrowser.metadata;

import java.util.Arrays;

import javax.management.ObjectName;

import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.IMBeanPropertiesOrderChangedListener;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.MementoToolkit;

/**
 * {@link SectionPart} responsible for showing MBean Item information
 */
public final class ItemSectionPart implements IMBeanPropertiesOrderChangedListener {

	private final ColumnManager columnManager;

	public ItemSectionPart(Composite parent, FormToolkit toolkit, IMemento state) {

		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.setText(Messages.ItemSectionPart_MBEAN_TITLE_TEXT);
		Tree tree = new Tree(section, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		section.setClient(tree);

		TreeViewer viewer = new TreeViewer(tree);

		IColumn itemColumn = new ColumnBuilder(Messages.ItemSectionPart_INFO_ITEM_NAME_TEXT, "item", //$NON-NLS-1$
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ObjectName) {
							return Messages.ItemContentProvider_MBEAN_NAME_TEXT;
						} else if (element instanceof String[]) {
							return ((String[]) element)[0];
						} else {
							return super.getText(element);
						}
					}
				}).description(Messages.ItemSectionPart_INFO_ITEM_DESCRIPTION_TEXT).build();

		IColumn valueColumn = new ColumnBuilder(Messages.ItemSectionPart_INFO_VALUE_NAME_TEXT, "value", //$NON-NLS-1$
				new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ObjectName) {
							return MBeanPropertiesOrderer.getMBeanPath((ObjectName) element);
						} else if (element instanceof String[]) {
							return ((String[]) element)[1];
						} else {
							return super.getText(element);
						}
					}
				}).description(Messages.ItemSectionPart_INFO_VALUE_DESCRIPTION_TEXT).build();

		columnManager = ColumnManager.build(viewer, Arrays.asList(itemColumn, valueColumn),
				TableSettings.forState(MementoToolkit.asState(state)));
		viewer.setContentProvider(new ItemContentProvider());
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		ColumnMenusFactory.addDefaultMenus(columnManager, MCContextMenuManager.create(tree));

	}

	public void saveState(IMemento state) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(state));
	}

	public void setModel(MetadataModel model) {
		columnManager.getViewer().setInput(model);
	}

	@Override
	public void propertiesOrderChanged(PropertyChangeEvent e) {
		columnManager.getViewer().refresh();
	}

}
