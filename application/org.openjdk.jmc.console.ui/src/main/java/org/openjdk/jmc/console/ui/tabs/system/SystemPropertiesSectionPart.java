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
package org.openjdk.jmc.console.ui.tabs.system;

import java.util.Arrays;

import javax.management.openmbean.TabularData;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.util.MemberAccessorToolkit;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.ColumnsFilter;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MementoToolkit;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

public class SystemPropertiesSectionPart extends MCSectionPart {

	final ColumnManager columnManager;

	public SystemPropertiesSectionPart(Composite parent, FormToolkit toolkit, ISubscriptionService subscriptionService,
			IMemento state) {
		super(parent, toolkit, Messages.SystemTab_SECTION_SYSTEM_PROPERTIES_TEXT);

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		Composite filterComposite = toolkit.createComposite(body);
		filterComposite.setLayoutData(MCLayoutFactory.createFormPageLayoutData(SWT.DEFAULT, SWT.DEFAULT, true, false));
		Table table = toolkit.createTable(body,
				SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(new TreeStructureContentProvider());
		IColumn categoryColumn = new ColumnBuilder(Messages.COLUMN_KEY_TEXT, "key", //$NON-NLS-1$
				MemberAccessorToolkit.compositeElement("key")).build(); //$NON-NLS-1$
		IColumn valueColumn = new ColumnBuilder(Messages.COLUMN_VALUE_TEXT, "value", //$NON-NLS-1$
				MemberAccessorToolkit.compositeElement("value")).build(); //$NON-NLS-1$
		columnManager = ColumnManager.build(viewer, Arrays.asList(categoryColumn, valueColumn),
				TableSettings.forState(MementoToolkit.asState(state)));
		ColumnMenusFactory.addDefaultMenus(columnManager, MCContextMenuManager.create(table));

		addSubscription(subscriptionService);
		filterComposite.setLayout(new GridLayout(2, false));
		ColumnsFilter.addFilterControl(filterComposite, toolkit, columnManager);
	}

	public void saveState(IMemento state) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(state));
	}

	private void addSubscription(ISubscriptionService subscriptionService) {
		MRI attributeDescriptor = new MRI(Type.ATTRIBUTE, "java.lang:type=Runtime", "SystemProperties"); //$NON-NLS-1$ //$NON-NLS-2$
		subscriptionService.addMRIValueListener(attributeDescriptor, new IMRIValueListener() {
			@Override
			public void valueChanged(final MRIValueEvent event) {
				DisplayToolkit.safeAsyncExec(getSection(), new Runnable() {
					@Override
					public void run() {
						if (!columnManager.getViewer().getControl().isDisposed()) {
							setInputAndSort(event);
						}
					}
				});
			}
		});
		MRIValueEvent e = subscriptionService.getLastMRIValueEvent(attributeDescriptor);
		if (e != null) {
			setInputAndSort(e);
		}
	}

	private void setInputAndSort(final MRIValueEvent event) {
		Object v = event.getValue();
		if (v instanceof TabularData) {
			columnManager.getViewer().setInput(((TabularData) v).values());
		}
	}
}
