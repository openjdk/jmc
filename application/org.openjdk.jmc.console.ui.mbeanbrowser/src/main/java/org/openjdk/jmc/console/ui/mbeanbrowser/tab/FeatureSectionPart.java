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
package org.openjdk.jmc.console.ui.mbeanbrowser.tab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.console.ui.mbeanbrowser.MBeanBrowserPlugin;
import org.openjdk.jmc.console.ui.mbeanbrowser.metadata.ConstructorSectionPart;
import org.openjdk.jmc.console.ui.mbeanbrowser.metadata.ItemSectionPart;
import org.openjdk.jmc.console.ui.mbeanbrowser.metadata.MetadataModel;
import org.openjdk.jmc.console.ui.mbeanbrowser.notifications.MBeanNotificationLogInspector;
import org.openjdk.jmc.console.ui.mbeanbrowser.notifications.NotificationInfoSectionPart;
import org.openjdk.jmc.console.ui.mbeanbrowser.notifications.NotificationsModel;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.ui.attributes.MRIAttribute;
import org.openjdk.jmc.rjmx.ui.attributes.MRIAttributeInspector;
import org.openjdk.jmc.rjmx.ui.attributes.MRIAttributeInspector.ErroneousAttribute;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer;
import org.openjdk.jmc.rjmx.ui.internal.SectionPartManager;
import org.openjdk.jmc.rjmx.ui.operations.ExecuteOperationForm;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;

/**
 * SectionPart that holds the details tabs.
 */
public class FeatureSectionPart extends MCSectionPart {
	private static final String ATTRIBUTES_ID = "attributes"; //$NON-NLS-1$
	private static final String OPERATIONS_ID = "operations"; //$NON-NLS-1$
	private static final String NOTIFICATIONS_ID = "notifications"; //$NON-NLS-1$
	private static final String MBEAN_INFO_ID = "mbeanInfo"; //$NON-NLS-1$
	private static final String CONSTRUCTORS_ID = "constructors"; //$NON-NLS-1$

	private final Map<ObjectName, NotificationsModel> m_notificationModels = new HashMap<>();
	private final MRIAttributeInspector attributeInspector;
	private final ExecuteOperationForm operationsPart;
	private final NotificationInfoSectionPart notificationsList;
	private final MBeanNotificationLogInspector notificationInspector;
	private final ItemSectionPart mbeanInfoPart;
	private final ConstructorSectionPart constructorsPart;
	private final IConnectionHandle ch;
	private volatile ObjectName lastName;

	public FeatureSectionPart(Composite parent, FormToolkit toolkit, IMemento state, IConnectionHandle ch,
			SectionPartManager spm) {
		super(parent, toolkit, DEFAULT_TITLE_STYLE);
		this.ch = ch;
		getSection().setText(Messages.FeatureSectionPart_MBEAN_FEATURES_TITLE_TEXT);

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		CTabFolder tabFolder = new CTabFolder(body, SWT.NONE);
		toolkit.adapt(tabFolder);
		tabFolder.setLayoutData(MCLayoutFactory.createFormPageLayoutData());

		CTabItem attributeTab = new CTabItem(tabFolder, SWT.NONE);
		IMemento aiState = state == null ? null : state.getChild(ATTRIBUTES_ID);
		attributeInspector = new MRIAttributeInspector(spm, tabFolder, aiState, ch, false);
		attributeTab.setControl(attributeInspector.getViewer().getTree());
		attributeTab.setText(Messages.FeatureSectionPart_ATTRIBUTES_TAB_TITLE_TEXT);

		CTabItem operationsTab = new CTabItem(tabFolder, SWT.NONE);
		IMemento operationsState = state == null ? null : state.getChild(OPERATIONS_ID);
		SashForm sash = new SashForm(tabFolder, SWT.VERTICAL);
		operationsPart = new ExecuteOperationForm(sash, toolkit, true, operationsState);
		operationsTab.setControl(sash);
		operationsTab.setText(Messages.FeatureSectionPart_OPERATIONS_TAB_TITLE_TEXT);

		CTabItem notificationsTab = new CTabItem(tabFolder, SWT.NONE);
		SashForm notificationsSash = new SashForm(tabFolder, SWT.VERTICAL);
		notificationsList = new NotificationInfoSectionPart(notificationsSash, toolkit, ch);
		IMemento niState = state == null ? null : state.getChild(NOTIFICATIONS_ID);
		notificationInspector = new MBeanNotificationLogInspector(notificationsSash, toolkit, niState);
		notificationsSash.setWeights(new int[] {1, 3});
		notificationsTab.setControl(notificationsSash);
		notificationsTab.setText(Messages.FeatureSectionPart_NOTIFICATIONS_TAB_TITLE_TEXT);

		CTabItem metadataTab = new CTabItem(tabFolder, SWT.NONE);
		SashForm metadataSash = new SashForm(tabFolder, SWT.VERTICAL);
		mbeanInfoPart = new ItemSectionPart(metadataSash, toolkit,
				state == null ? null : state.getChild(MBEAN_INFO_ID));
		constructorsPart = new ConstructorSectionPart(metadataSash, toolkit,
				state == null ? null : state.getChild(CONSTRUCTORS_ID));
		metadataTab.setControl(metadataSash);
		metadataTab.setText(Messages.FeatureSectionPart_INFORMATION_TAB_TITLE_TEXT);

		MBeanPropertiesOrderer.addPropertiesOrderChangedListener(mbeanInfoPart);

		tabFolder.setSelection(0);
	}

	public void saveState(IMemento state) {
		attributeInspector.saveState(state.createChild(ATTRIBUTES_ID));
		operationsPart.saveState(state.createChild(OPERATIONS_ID));
		notificationInspector.saveState(state.createChild(NOTIFICATIONS_ID));
		mbeanInfoPart.saveState(state.createChild(MBEAN_INFO_ID));
		constructorsPart.saveState(state.createChild(CONSTRUCTORS_ID));
	}

	@Override
	public void dispose() {
		m_notificationModels.values().forEach(NotificationsModel::dispose);
		MBeanPropertiesOrderer.removePropertiesOrderChangedListener(mbeanInfoPart);
		super.dispose();
	}

	public void showBean(ObjectName bean) {
		if (lastName != null && lastName.equals(bean)) {
			return;
		}
		lastName = bean;
		try {
			MBeanInfo info = ch.getServiceOrThrow(IMBeanHelperService.class).getMBeanInfo(bean);
			attributeInspector.setInput(createMBeanAttributes(bean, info));
			operationsPart.setOperations(
					((RJMXConnection) ch.getServiceOrThrow(IMBeanHelperService.class)).getOperations(bean));
			NotificationsModel model = getOrCreateNotificationsModel(bean);
			notificationsList.setModel(model);
			notificationInspector.show(model.getNotifications());
			MetadataModel metadatModel = new MetadataModel(bean, info);
			mbeanInfoPart.setModel(metadatModel);
			constructorsPart.setMBeanInfo(info);
		} catch (Exception e) {
			attributeInspector.setInput(Collections.emptyList());
			operationsPart.setOperations(Collections.<IOperation> emptyList());
			notificationsList.setModel(null);
			notificationInspector.show(Stream.empty());
			mbeanInfoPart.setModel(null);
			constructorsPart.setMBeanInfo(null);

			MBeanBrowserPlugin.getDefault().getLogger().log(Level.WARNING, "Failed to load attributes", e); //$NON-NLS-1$
			String error = Messages.MBeanAttributeSectionPart_FAILED_TO_LOAD_ATTRIBUTES;
			DialogToolkit.showException(getSection().getShell(), error, error, e);
		}
	}

	private void modelUpdated(NotificationsModel model) {
		if (model.getObjectName().equals(lastName)) {
			DisplayToolkit.safeAsyncExec(() -> {
				if (model.getObjectName().equals(lastName) && DisplayToolkit.isSafe(getSection())) {
					notificationsList.setModel(model);
					notificationInspector.show(model.getNotifications());
				}
			});
		}
	}

	private List<Object> createMBeanAttributes(ObjectName bean, MBeanInfo info) throws Exception {
		List<Object> attributes = new ArrayList<>();
		for (MBeanAttributeInfo attribute : info.getAttributes()) {
			if (attribute.getName() == null) {
				attributes.add(
						new ErroneousAttribute(attribute.getName(), attribute.getType(), attribute.getDescription()));
			} else {
				attributes.add(MRIAttribute.create(ch, new MRI(Type.ATTRIBUTE, bean, attribute.getName())));
			}
		}
		return attributes;
	}

	private NotificationsModel getOrCreateNotificationsModel(ObjectName mbean) throws Exception {
		NotificationsModel model = m_notificationModels.get(mbean);
		if (model == null) {
			model = new NotificationsModel(mbean, ch.getServiceOrThrow(MBeanServerConnection.class),
					this::modelUpdated);
			m_notificationModels.put(mbean, model);
		}
		return model;
	}
}
