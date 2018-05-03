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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.ui.attributes.MRIAttribute;
import org.openjdk.jmc.rjmx.ui.attributes.MRIAttributeInspector;
import org.openjdk.jmc.rjmx.ui.attributes.ReadOnlyMRIAttribute;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MCToolBarManager;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public class AttributeSectionPart extends MCSectionPart implements IAttributeSet {
	private static final String ATTRIBUTE_ID = "attribute"; //$NON-NLS-1$

	private MRIAttributeInspector inspector;
	private final IConnectionHandle connection;
	private final Map<MRI, ReadOnlyMRIAttribute> model = new HashMap<>();
	private final IMRIValueListener listener = new IMRIValueListener() {

		@Override
		public void valueChanged(final MRIValueEvent event) {
			DisplayToolkit.safeAsyncExec(getSection(), new Runnable() {
				@Override
				public void run() {
					if (!inspector.getViewer().getControl().isDisposed()) {
						ReadOnlyMRIAttribute attribute = model.get(event.getMRI());
						if (attribute != null) {
							attribute.updateValue(event.getValue());
							inspector.getViewer().update(attribute, null);
						}
					}
				}
			});
		}
	};

	public AttributeSectionPart(SectionPartManager sectionPartManager, Composite parent, FormToolkit toolkit,
			String title, IConnectionHandle connection, IMemento settings) {
		super(parent, toolkit, title);
		this.connection = connection;

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		IColumn objectNameColumn = new ColumnBuilder(Messages.AttributeSectionPart_OBJECT_NAME_COLUMN_HEADER,
				"objectName", new TypedLabelProvider<ReadOnlyMRIAttribute>( //$NON-NLS-1$
						ReadOnlyMRIAttribute.class) {
					@Override
					protected String getTextTyped(ReadOnlyMRIAttribute element) {
						return element.getMRI().getObjectName().getCanonicalName();
					}
				}).build();
		inspector = new MRIAttributeInspector(sectionPartManager, body, settings, connection, true, objectNameColumn);
		getMCToolBarManager().add(new AddAttibutesAction(connection.getServiceOrDummy(IMRIMetadataService.class),
				connection.getServiceOrDummy(IMRIService.class), this, false), MCToolBarManager.ALIGN_LEFT);
		TreeViewer tree = inspector.getViewer();
		tree.getTree().setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		RemoveAttributeAction removeAction = new RemoveAttributeAction(tree, this);
		inspector.getMenuManager().appendToGroup(MCContextMenuManager.GROUP_EDIT, removeAction);
		InFocusHandlerActivator.install(tree.getControl(), removeAction);

		if (settings != null) {
			restoreState(settings);
		}
	}

	@Override
	public void add(MRI ... mris) {
		for (MRI mri : mris) {
			if (!model.containsKey(mri)) {
				model.put(mri, MRIAttribute.create(connection, mri));
				connection.getServiceOrDummy(ISubscriptionService.class).addMRIValueListener(mri, listener);
			}
		}
		inspector.setInput(model.values());
	}

	@Override
	public void remove(MRI ... mris) {
		for (MRI mri : mris) {
			model.remove(mri);
			connection.getServiceOrDummy(ISubscriptionService.class).removeMRIValueListener(mri, listener);
		}
		inspector.setInput(model.values());
	}

	public void saveState(IMemento state) {
		inspector.saveState(state);
		for (MRI mri : model.keySet()) {
			state.createChild(ATTRIBUTE_ID).putTextData(mri.getQualifiedName());
		}
	}

	public void restoreState(IMemento state) {
		model.clear();
		for (IMemento child : state.getChildren(ATTRIBUTE_ID)) {
			add(MRI.createFromQualifiedName(child.getTextData().trim()));
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public MRI[] elements() {
		return model.keySet().toArray(new MRI[model.keySet().size()]);
	}

}
