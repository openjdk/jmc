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
package org.openjdk.jmc.console.ui.tabs.overview;

import javax.inject.Inject;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import org.openjdk.jmc.console.ui.actions.ResetToDefaultsAction;
import org.openjdk.jmc.console.ui.editor.IConsolePageContainer;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.console.ui.editor.internal.ConsoleEditor;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ui.internal.CombinedChartSectionPart;
import org.openjdk.jmc.rjmx.ui.internal.CombinedDialsSectionPart;
import org.openjdk.jmc.rjmx.ui.internal.NewChartAction;
import org.openjdk.jmc.rjmx.ui.internal.PersistenceSectionPart;
import org.openjdk.jmc.rjmx.ui.internal.SectionPartManager;
import org.openjdk.jmc.ui.misc.MCActionContributionItem;
import org.openjdk.jmc.ui.misc.MCSectionPart;

/**
 * Tab that shows an overview for the JMX Console
 */
public class OverviewTab implements IConsolePageStateHandler {
	private static final String CHART_SECTION_ID = "chart"; //$NON-NLS-1$
	private static final String DIAL_SECTION_ID = "dial"; //$NON-NLS-1$

	private IManagedForm m_managedForm;
	private SectionPartManager m_sectionPartManager;
	private CombinedDialsSectionPart dsp;

	@Inject
	protected void createPageContent(
		IConsolePageContainer page, IManagedForm managedForm, IConnectionHandle connection) {
		// Create SectionPartManager
		m_managedForm = managedForm;
		m_sectionPartManager = new SectionPartManager(managedForm);
		((ConsoleEditor) page.getEditor()).addSectionManager(m_sectionPartManager);

		IToolBarManager toolbar = managedForm.getForm().getToolBarManager();
		NewChartAction newChartAction = new NewChartAction(m_sectionPartManager, connection);
		toolbar.appendToGroup(IConsolePageContainer.TB_FIRST_GROUP, new MCActionContributionItem(newChartAction));
		toolbar.appendToGroup(IConsolePageContainer.TB_FIRST_GROUP,
				new MCActionContributionItem(new ResetToDefaultsAction() {

					@Override
					protected void reset() {
						m_sectionPartManager.destroyAllParts();
						restoreState(page.getDefaultConfig(), connection);
					}

				}));
		toolbar.update(true);
		restoreState(page.loadConfig(), connection);
		validateDependencies(page, connection);
	}

	private void validateDependencies(IConsolePageContainer page, IConnectionHandle connectionHandle) {
		MBeanServerConnection connection = connectionHandle.getServiceOrNull(MBeanServerConnection.class);
		try {
			if (connection.queryNames(new ObjectName("java.lang:type=Memory"), null).isEmpty() //$NON-NLS-1$
					|| connection.queryNames(new ObjectName("java.lang:type=OperatingSystem"), null).isEmpty()) { //$NON-NLS-1$
				page.presentError(Messages.ConsoleEditor_PLATFORM_MBEANS_UNAVAILABLE);
			}
		} catch (Exception e) {
			page.presentError(Messages.ConsoleEditor_CONNECTION_LOST);
		}
	}

	private void restoreState(IMemento state, IConnectionHandle connection) {
		PersistenceSectionPart persistenceSection = new PersistenceSectionPart(m_sectionPartManager.getContainer(),
				m_sectionPartManager.getFormToolkit(),
				MCSectionPart.DEFAULT_TITLE_STYLE | ExpandableComposite.TWISTIE | ExpandableComposite.COMPACT,
				connection);
		m_sectionPartManager.add(persistenceSection, true, false);
		dsp = new CombinedDialsSectionPart(m_sectionPartManager.getContainer(), m_sectionPartManager.getFormToolkit(),
				MCSectionPart.DEFAULT_TWISTIE_STYLE, connection, state.getChild(DIAL_SECTION_ID));
		m_sectionPartManager.add(dsp, false, false);
		dsp.getSection().setText(Messages.OverviewTab_SECTION_DASHBOARD_TEXT);
		for (IMemento chartState : state.getChildren(CHART_SECTION_ID)) {
			CombinedChartSectionPart csp = new CombinedChartSectionPart(m_sectionPartManager.getContainer(),
					m_sectionPartManager.getFormToolkit(), MCSectionPart.DEFAULT_TWISTIE_STYLE, connection, chartState);
			m_sectionPartManager.add(csp, true, true);
		}
	}

	@Override
	public boolean saveState(IMemento state) {
		dsp.saveState(state.createChild(DIAL_SECTION_ID));
		for (IFormPart part : m_managedForm.getParts()) {
			if (part instanceof CombinedChartSectionPart) {
				((CombinedChartSectionPart) part).saveState(state.createChild(CHART_SECTION_ID));
			}
		}
		return true;
	}

	@Override
	public void dispose() {
	}
}
