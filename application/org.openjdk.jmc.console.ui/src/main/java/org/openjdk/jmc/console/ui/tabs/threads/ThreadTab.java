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
package org.openjdk.jmc.console.ui.tabs.threads;

import javax.inject.Inject;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import org.openjdk.jmc.console.ui.actions.ResetToDefaultsAction;
import org.openjdk.jmc.console.ui.editor.IConsolePageContainer;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.console.ui.editor.internal.ConsoleFormPage;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ui.internal.CombinedChartSectionPart;
import org.openjdk.jmc.ui.misc.MCActionContributionItem;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;

/**
 * Threads Tab
 */
public class ThreadTab implements IConsolePageStateHandler {
	private static final String CHART_ID = "threadsChart"; //$NON-NLS-1$
	private static final String TABLE_ID = "threadsTable"; //$NON-NLS-1$
	private static final String TAB_PAGE_ID = "org.openjdk.jmc.console.ui.tabs.threads.ThreadTab"; //$NON-NLS-1$

	private boolean m_platformThreadingMBeanPresent = true;
	private IThreadsModel m_model;
	private CombinedChartSectionPart m_graphSectionPart;
	private ThreadMasterDetailBlock m_masterDetailBlock;

	@Inject
	protected void createPageContent(
		IConsolePageContainer page, IManagedForm managedForm, IConnectionHandle connection) {
		if (m_platformThreadingMBeanPresent) {
			m_model = new ThreadsModel(connection);
		} else {
			m_model = new DummyThreadsModel();
		}
		ScrolledForm form = managedForm.getForm();
		Composite container = managedForm.getForm().getBody();
		container.setLayout(MCLayoutFactory.createFormPageLayout());

		IToolBarManager toolbarManager = form.getToolBarManager();
		toolbarManager.appendToGroup(IConsolePageContainer.TB_FIRST_GROUP,
				new MCActionContributionItem(new ResetToDefaultsAction() {
					@Override
					protected void reset() {
						m_graphSectionPart.restoreState(page.getDefaultConfig().getChild(CHART_ID));
					}
				}));
		toolbarManager.update(true);
		IMemento state = page.loadConfig();

		int style = MCSectionPart.DEFAULT_TITLE_STYLE | ExpandableComposite.TWISTIE;
		m_graphSectionPart = new CombinedChartSectionPart(container, managedForm.getToolkit(), style, connection,
				state.getChild(CHART_ID));
		managedForm.addPart(m_graphSectionPart);
		m_graphSectionPart.getSection().setExpanded(false);
		MCLayoutFactory.addGrabOnExpandLayoutData(m_graphSectionPart.getSection());

		m_masterDetailBlock = new ThreadMasterDetailBlock(container, managedForm, ((ConsoleFormPage) page).getSite(),
				m_model, state.getChild(TABLE_ID));
		m_masterDetailBlock.getSashForm().setLayoutData(MCLayoutFactory.createFormPageLayoutData());

		page.getEditor().addPageChangedListener(new IPageChangedListener() {

			@Override
			public void pageChanged(PageChangedEvent event) {
				if (TAB_PAGE_ID.equals(((FormPage) event.getSelectedPage()).getId())) {
					m_model.getPollManager().resume();
				} else {
					m_model.getPollManager().pause();
				}
			}

		});
		validateDependencies(page, connection);
	}

	private void validateDependencies(IConsolePageContainer page, IConnectionHandle connectionHandle) {
		MBeanServerConnection connection = connectionHandle.getServiceOrNull(MBeanServerConnection.class);
		try {
			if (connection.queryNames(new ObjectName("java.lang:type=Threading"), null).isEmpty()) { //$NON-NLS-1$
				page.presentError(Messages.ConsoleEditor_PLATFORM_MBEANS_UNAVAILABLE);
				m_platformThreadingMBeanPresent = false;
			}
		} catch (Exception e) {
			page.presentError(Messages.ConsoleEditor_CONNECTION_LOST);
			m_platformThreadingMBeanPresent = false;
		}
	}

	@Override
	public void dispose() {
		if (m_model != null) {
			m_model.dispose();
			m_model = null;
		}
	}

	@Override
	public boolean saveState(IMemento state) {
		m_graphSectionPart.saveState(state.createChild(CHART_ID));
		m_masterDetailBlock.saveState(state.createChild(TABLE_ID));
		return true;
	}
}
