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

import javax.inject.Inject;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import org.openjdk.jmc.console.ui.actions.ResetToDefaultsAction;
import org.openjdk.jmc.console.ui.editor.IConsolePageContainer;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.console.ui.editor.internal.ConsoleEditor;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.ui.internal.AttributeSectionPart;
import org.openjdk.jmc.ui.misc.MCActionContributionItem;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;

public class SystemTab implements IConsolePageStateHandler {
	private static final String JVM_STATISTICS_ID = "JVMStatistics"; //$NON-NLS-1$
	private static final String SERVER_INFO_ID = "ServerInformation"; //$NON-NLS-1$
	private static final String SYSTEM_PROPERTIES_ID = "SystemProperties"; //$NON-NLS-1$
	private AttributeSectionPart systemPart;
	private TableInformationSectionPart infoPart;
	private SystemPropertiesSectionPart systemProperties;

	@Inject
	protected void createPageContent(IConsolePageContainer page, IManagedForm managedForm, IConnectionHandle ch) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		Composite container = managedForm.getForm().getBody();
		container.setLayout(MCLayoutFactory.createFormPageLayout());

		form.getToolBarManager().appendToGroup(IConsolePageContainer.TB_FIRST_GROUP,
				new MCActionContributionItem(new ResetToDefaultsAction() {
					@Override
					protected void reset() {
						systemPart.restoreState(page.getDefaultConfig().getChild(JVM_STATISTICS_ID));
					}
				}));
		form.getToolBarManager().update(true);

		IMemento currentState = page.loadConfig();

		infoPart = new TableInformationSectionPart(container, toolkit, ch, currentState.getChild(SERVER_INFO_ID));
		managedForm.addPart(infoPart);
		MCLayoutFactory.addGrabOnExpandLayoutData(infoPart.getSection());

		systemPart = new AttributeSectionPart(((ConsoleEditor) page.getEditor()).getSectionPartManagers().get(0),
				container, toolkit, Messages.SystemTab_SECTION_SYSTEM_STATISTICS_TEXT, ch,
				currentState.getChild(JVM_STATISTICS_ID));
		managedForm.addPart(systemPart);
		MCLayoutFactory.addGrabOnExpandLayoutData(systemPart.getSection());

		ISubscriptionService subscriptionService = ch.getServiceOrDummy(ISubscriptionService.class);
		systemProperties = new SystemPropertiesSectionPart(container, toolkit, subscriptionService,
				currentState.getChild(SYSTEM_PROPERTIES_ID));
		managedForm.addPart(systemProperties);
		MCLayoutFactory.addGrabOnExpandLayoutData(systemProperties.getSection());
		validateDependencies(page, ch);
	}

	private void validateDependencies(IConsolePageContainer page, IConnectionHandle connectionHandle) {
		MBeanServerConnection connection = connectionHandle.getServiceOrNull(MBeanServerConnection.class);
		try {
			if (connection.queryNames(new ObjectName("java.lang:type=Runtime"), null).isEmpty() //$NON-NLS-1$
					|| connection.queryNames(new ObjectName("java.lang:type=ClassLoading"), null).isEmpty() //$NON-NLS-1$
					|| connection.queryNames(new ObjectName("java.lang:type=OperatingSystem"), null).isEmpty()) { //$NON-NLS-1$
				page.presentError(Messages.ConsoleEditor_PLATFORM_MBEANS_UNAVAILABLE);
			}
		} catch (Exception e) {
			page.presentError(Messages.ConsoleEditor_CONNECTION_LOST);
		}
	}

	@Override
	public boolean saveState(IMemento state) {
		infoPart.saveState(state.createChild(SERVER_INFO_ID));
		systemPart.saveState(state.createChild(JVM_STATISTICS_ID));
		systemProperties.saveState(state.createChild(SYSTEM_PROPERTIES_ID));
		return true;
	}

	@Override
	public void dispose() {
	}
}
