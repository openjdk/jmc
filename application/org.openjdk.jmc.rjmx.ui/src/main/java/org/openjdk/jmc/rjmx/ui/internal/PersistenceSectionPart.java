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

import java.io.IOException;
import java.util.logging.Level;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IPersistenceService;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.MCArrayContentProvider;
import org.openjdk.jmc.ui.misc.MCSectionPart;

public class PersistenceSectionPart extends MCSectionPart implements IAttributeSet {

	private final IPersistenceService manager;
	private final IMRIMetadataService mds;
	private final IMRIService attributes;
	private final TableViewer table;

	public PersistenceSectionPart(Composite parent, FormToolkit toolkit, int style, IConnectionHandle connection) {
		super(parent, toolkit, style);
		manager = connection.getServiceOrDummy(IPersistenceService.class);
		mds = connection.getServiceOrDummy(IMRIMetadataService.class);
		attributes = connection.getServiceOrDummy(IMRIService.class);

		Section section = getSection();
		section.setText(Messages.ConfigurePersistenceAction_TEXT);
		Composite container = toolkit.createComposite(section);
		section.setClient(container);
		table = new TableViewer(container);
		table.setContentProvider(MCArrayContentProvider.INSTANCE);
		table.setLabelProvider(new AttributeLabelProvider(mds, attributes));
		table.setInput(this);
		ColumnViewerToolTipSupport.enableFor(table);

		getMCToolBarManager().add(new AddAttibutesAction(mds, attributes, this));
		getMCToolBarManager().add(new Action(Messages.ConfigurePersistenceAction_ENABLE, IAction.AS_CHECK_BOX) {
			{
				setImageDescriptor(
						RJMXUIPlugin.getDefault().getMCImageDescriptor(IconConstants.ICON_PERSISTENCE_TOGGLE_ON));
				setDisabledImageDescriptor(
						RJMXUIPlugin.getDefault().getMCImageDescriptor(IconConstants.ICON_PERSISTENCE_TOGGLE_OFF));
				setToolTipText(Messages.ConfigurePersistenceAction_TOOLTIP_TEXT);
				setChecked(manager.isRunning());
			}

			@Override
			public void run() {
				if (isChecked()) {
					try {
						manager.start();
					} catch (IOException e1) {
						RJMXUIPlugin.getDefault().getLogger().log(Level.WARNING, "Problem activating persistence.", e1); //$NON-NLS-1$
					}
				} else {
					manager.stop();
				}
			}
		});
		container.setLayout(new GridLayout());
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd2.heightHint = 60;
		table.getControl().setLayoutData(gd2);
		MCContextMenuManager mm = MCContextMenuManager.create(table.getTable());
		RemoveAttributeAction removeAction = new RemoveAttributeAction(table, this);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, removeAction);
		InFocusHandlerActivator.install(table.getControl(), removeAction);
	}

	@Override
	public boolean isEmpty() {
		return manager.getAttributes().length == 0;
	}

	@Override
	public MRI[] elements() {
		return manager.getAttributes();
	}

	@Override
	public void add(MRI ... mris) {
		for (MRI mri : mris) {
			manager.add(mri);
		}
		table.refresh();
	}

	@Override
	public void remove(MRI ... mris) {
		for (MRI mri : mris) {
			manager.remove(mri);
		}
		table.refresh();
	}

}
