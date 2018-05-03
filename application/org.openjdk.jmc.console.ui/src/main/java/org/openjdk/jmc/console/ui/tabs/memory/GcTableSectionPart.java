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
package org.openjdk.jmc.console.ui.tabs.memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.ui.attributes.MRIAttribute;
import org.openjdk.jmc.rjmx.ui.attributes.MRIAttributeInspector;
import org.openjdk.jmc.rjmx.ui.attributes.ReadOnlyMRIAttribute;
import org.openjdk.jmc.rjmx.ui.internal.SectionPartManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;

class GcTableSectionPart extends MCSectionPart {

	private static final String GC_TABLE_ID = "gc"; //$NON-NLS-1$
	private static final String GC_NAME_ID = "gcName"; //$NON-NLS-1$
	private final Map<String, MRIAttributeInspector> tabs = new HashMap<>();

	public GcTableSectionPart(SectionPartManager sectionPartManager, Composite parent, FormToolkit toolkit,
			IConnectionHandle ch, IMemento state) {
		super(parent, toolkit, Messages.GcTableSectionPart_GC_TABLE_SECTION_TITLE);

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		CTabFolder tabFolder = new CTabFolder(body, SWT.NONE);
		tabFolder.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		toolkit.adapt(tabFolder);
		Map<String, IMemento> gcStates = new HashMap<>();
		if (state != null) {
			for (IMemento m : state.getChildren(GC_TABLE_ID)) {
				gcStates.put(m.getString(GC_NAME_ID), m);
			}
		}
		for (ObjectName gcName : findGcNames(ch)) {
			String name = gcName.getKeyProperty("name"); //$NON-NLS-1$
			IMemento gcState = gcStates.get(name);
			if (gcState == null && !gcStates.isEmpty()) {
				gcState = gcStates.values().iterator().next();
			}
			MRIAttributeInspector ai = new MRIAttributeInspector(sectionPartManager, tabFolder, gcState, ch, true);
			tabs.put(name, ai);
			CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
			tabItem.setControl(ai.getViewer().getTree());
			tabItem.setText(name);
			List<ReadOnlyMRIAttribute> l = new ArrayList<>();
			l.add(createGcAttribute(new MRI(Type.ATTRIBUTE, gcName, "CollectionTime"), ai, ch)); //$NON-NLS-1$
			l.add(createGcAttribute(new MRI(Type.ATTRIBUTE, gcName, "CollectionCount"), ai, ch)); //$NON-NLS-1$
			l.add(createGcAttribute(new MRI(Type.ATTRIBUTE, gcName, "LastGcInfo/startTime"), ai, ch)); //$NON-NLS-1$
			l.add(createGcAttribute(new MRI(Type.ATTRIBUTE, gcName, "LastGcInfo/endTime"), ai, ch)); //$NON-NLS-1$
			l.add(createGcAttribute(new MRI(Type.ATTRIBUTE, gcName, "LastGcInfo/duration"), ai, ch)); //$NON-NLS-1$
			l.add(createGcAttribute(new MRI(Type.ATTRIBUTE, gcName, "LastGcInfo/id"), ai, ch)); //$NON-NLS-1$
			l.add(createGcAttribute(new MRI(Type.ATTRIBUTE, gcName, "LastGcInfo/GcThreadCount"), ai, ch)); //$NON-NLS-1$
			ai.setInput(l);
		}
		tabFolder.setSelection(0);
	}

	void saveState(IMemento memento) {
		for (Entry<String, MRIAttributeInspector> e : tabs.entrySet()) {
			IMemento createChild = memento.createChild(GC_TABLE_ID);
			e.getValue().saveState(createChild);
			createChild.putString(GC_NAME_ID, e.getKey());
		}
	}

	private static List<ObjectName> findGcNames(IConnectionHandle connectionHandle) {
		List<ObjectName> gcNames = new ArrayList<>();
		try {
			IMBeanHelperService h = connectionHandle.getServiceOrThrow(IMBeanHelperService.class);
			for (Entry<ObjectName, MBeanInfo> entry : h.getMBeanInfos().entrySet()) {
				ObjectName o = entry.getKey();
				if (o.getDomain().equals("java.lang")) { //$NON-NLS-1$
					if ("GarbageCollector".equals(o.getKeyProperty("type"))) { //$NON-NLS-1$ //$NON-NLS-2$
						gcNames.add(o);
					}
				}
			}
		} catch (IOException e1) {
		} catch (ServiceNotAvailableException e) {
		}
		return gcNames;
	}

	private static ReadOnlyMRIAttribute createGcAttribute(
		MRI mri, final MRIAttributeInspector ai, final IConnectionHandle ch) {
		final ReadOnlyMRIAttribute attribute = MRIAttribute.create(ch, mri);
		ch.getServiceOrDummy(ISubscriptionService.class).addMRIValueListener(mri, new IMRIValueListener() {

			@Override
			public void valueChanged(final MRIValueEvent event) {
				DisplayToolkit.safeAsyncExec(new Runnable() {
					@Override
					public void run() {
						if (!ai.getViewer().getControl().isDisposed()) {
							attribute.updateValue(event.getValue());
							ai.getViewer().update(attribute, null);
						}
					}
				});
			}
		});
		return attribute;
	}

}
