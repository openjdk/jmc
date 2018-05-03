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
package org.openjdk.jmc.console.ui.mbeanbrowser.notifications;

import java.io.IOException;

import javax.management.JMException;
import javax.management.MBeanNotificationInfo;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.ui.handlers.CopySelectionAction;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.CopySettings;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.FormatToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;

/**
 * Class responsible for showing information about an MBean and provide a user interface where the
 * user can turn on/off JMX-notification subscription. This class cooperates with
 * {@link NotificationLogSectionPart}
 */
public class NotificationInfoSectionPart {
	private NotificationsModel m_model;
	private Button m_subscribeButton;
	private final TreeViewer viewer;

	public NotificationInfoSectionPart(Composite parent, FormToolkit toolkit, IConnectionHandle connectionHandle) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.setText(Messages.NotificationInfoSectionPart_NOTICATION_INFORMATION_TITLE_TEXT);
		Composite container = toolkit.createComposite(section);
		section.setClient(container);
		container.setLayout(new GridLayout(2, false));

		Tree tree = new Tree(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new InformationProvider());
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		ColumnLabelProvider lp = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MBeanNotificationInfo) {
					// FIXME: Improve formatting of info
					MBeanNotificationInfo info = ((MBeanNotificationInfo) element);
					return String.format("%s [%s]", info.getDescription(), info.getName()); //$NON-NLS-1$
				} else {
					return super.getText(element);
				}
			}
		};
		viewer.setLabelProvider(lp);
		CopySelectionAction copyAction = new CopySelectionAction(viewer, FormatToolkit.selectionFormatter(lp));
		InFocusHandlerActivator.install(tree, copyAction);
		IContributionItem copyMenu = CopySettings.getInstance().createContributionItem();
		MCContextMenuManager.create(tree).addAll(new ActionContributionItem(copyAction), copyMenu);

		Composite buttonContainer = toolkit.createComposite(container);
		buttonContainer.setLayout(MCLayoutFactory.createPaintBordersMarginFreeFormPageLayout(1));
		Button button = createSubscribeButton(toolkit, buttonContainer, connectionHandle);
		button.setLayoutData(MCLayoutFactory.createFormPageLayoutData(SWT.DEFAULT, SWT.DEFAULT, true, false));
		buttonContainer.setLayout(MCLayoutFactory.createPaintBordersMarginFreeFormPageLayout(1));
		buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
	}

	private Button createSubscribeButton(
		FormToolkit toolkit, Composite parent, final IConnectionHandle connectionHandle) {
		m_subscribeButton = toolkit.createButton(parent, Messages.NotificationInfoSectionPart_SUBSCRIBE_BUTTON_TEXT,
				SWT.CHECK);
		m_subscribeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (m_model != null) {
					String errorTitle = Messages.NotificationInfoSectionPart_ERROR_REMOVING_JMX_SUBSCRIPTION_TEXT;
					if (!m_model.getSubscriptionEnabled()) {
						errorTitle = Messages.NotificationInfoSectionPart_ERROR_ENABLE_JMX_SUBSCRIPTION_TEXT;
					}
					try {
						m_model.setSubscriptionEnabled(!m_model.getSubscriptionEnabled());
					} catch (IOException e1) {
						handleException(errorTitle, e1);
					} catch (JMException e1) {
						handleException(errorTitle, e1);
					}
				}
			}

			private void handleException(String errorTitle, Exception e) {
				if (connectionHandle.isConnected()) {
					DialogToolkit.showExceptionDialogAsync(Display.getDefault(), errorTitle, e.getMessage(), e);
				} else {
					m_subscribeButton.setSelection(false);
					m_subscribeButton.setEnabled(false);
				}
			}
		});
		return m_subscribeButton;
	}

	public void setModel(NotificationsModel model) {
		m_model = model;
		viewer.setInput(model);
		m_subscribeButton.setSelection(model != null && model.getSubscriptionEnabled());
		m_subscribeButton.setEnabled(model != null && model.supportsSubscriptions());
	}
}
