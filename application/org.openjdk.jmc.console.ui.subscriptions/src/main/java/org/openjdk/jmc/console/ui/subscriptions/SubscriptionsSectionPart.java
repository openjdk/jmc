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
package org.openjdk.jmc.console.ui.subscriptions;

import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.util.MemberAccessorToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.internal.EmptySubscriptionDebugService;
import org.openjdk.jmc.rjmx.subscription.internal.ISubscriptionDebugService;
import org.openjdk.jmc.rjmx.ui.internal.FreezeModel;
import org.openjdk.jmc.rjmx.ui.internal.ToggleFreezeAction;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MementoToolkit;

public class SubscriptionsSectionPart extends MCSectionPart {

	private class ClearStatisticsAction extends Action {
		public ClearStatisticsAction() {
			super(Messages.CLEAR_LABEL);
			setToolTipText(Messages.CLEAR_STATISTICS_TOOLTIP);
			setImageDescriptor(
					SubscriptionsPlugin.getDefault().getMCImageDescriptor(SubscriptionsPlugin.ICON_GARBAGE_BIN));
		}

		@Override
		public void run() {
			clearDebugInformation();
			DisplayToolkit.safeAsyncExec(new Runnable() {
				@Override
				public void run() {
					if (!columnManager.getViewer().getControl().isDisposed()) {
						columnManager.getViewer().refresh();
					}
				}
			});
		}
	}

	private static final int DEFAULT_REFRESH_TIME = 3000;
	private final IConnectionHandle m_connection;
	private final FreezeModel m_freezeModel;
	private final Observer m_observer;
	private Runnable m_tabRefresher;
	private final ColumnManager columnManager;

	public SubscriptionsSectionPart(Composite parent, FormToolkit toolkit, IConnectionHandle connection,
			IMemento stateMemento) {
		super(parent, toolkit, MCSectionPart.DEFAULT_TITLE_STYLE);
		getSection().setText(Messages.SUBSCRIPTIONS_LABEL);
		m_freezeModel = new FreezeModel();
		m_freezeModel.setFreezed(false);
		m_observer = createObserver();
		m_freezeModel.addObserver(m_observer);
		m_connection = connection;

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		Table table = toolkit.createTable(body,
				SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(new SubscriptionTableContentProvider());
		viewer.setInput(connection);

		IColumn mri = new ColumnBuilder(Messages.MRI_NAME_TEXT, "mri", MemberAccessorToolkit.arrayElement(0)) //$NON-NLS-1$
				.description(Messages.MRI_DESCRIPTION_TEXT).build();
		IColumn state = new ColumnBuilder(Messages.STATE_NAME_TEXT, "state", MemberAccessorToolkit.arrayElement(1)) //$NON-NLS-1$
				.description(Messages.STATE_DESCRIPTION_TEXT).build();
		IColumn connectionCount = new ColumnBuilder(Messages.CONNECTION_COUNT_NAME_TEXT, "connectionCount", //$NON-NLS-1$
				MemberAccessorToolkit.arrayElement(2)).description(Messages.CONNECTION_COUNT_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn disconnectionCount = new ColumnBuilder(Messages.DISCONNECTION_COUNT_NAME_TEXT, "disconnectionCount", //$NON-NLS-1$
				MemberAccessorToolkit.arrayElement(3)).description(Messages.DISCONNECTION_COUNT_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn eventCount = new ColumnBuilder(Messages.EVENT_COUNT_NAME_TEXT, "eventCount", //$NON-NLS-1$
				MemberAccessorToolkit.arrayElement(4)).description(Messages.EVENT_COUNT_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn retainedEventCount = new ColumnBuilder(Messages.RETAINED_EVENT_COUNT_NAME_TEXT, "retainedEventCount", //$NON-NLS-1$
				MemberAccessorToolkit.arrayElement(5)).description(Messages.RETAINED_EVENT_COUNT_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn lastEventValue = new ColumnBuilder(Messages.LAST_EVENT_VALUE_NAME_TEXT, "lastEventValue", //$NON-NLS-1$
				MemberAccessorToolkit.arrayElement(6)).description(Messages.LAST_EVENT_VALUE_DESCRIPTION_TEXT).build();
		IColumn lastEventPayload = new ColumnBuilder(Messages.LAST_EVENT_PAYLOAD_NAME_TEXT, "lastEventPayload", //$NON-NLS-1$
				MemberAccessorToolkit.arrayElement(7)).description(Messages.LAST_EVENT_PAYLOAD_DESCRIPTION_TEXT)
						.build();
		IColumn connectionLostCount = new ColumnBuilder(Messages.CONNECTION_LOST_COUNT_NAME_TEXT, "connectionLostCount", //$NON-NLS-1$
				MemberAccessorToolkit.arrayElement(8)).description(Messages.CONNECTION_LOST_COUNT_DESCRIPTION_TEXT)
						.style(SWT.RIGHT).build();
		IColumn triedReconnectionCount = new ColumnBuilder(Messages.TRIED_RECONNECTION_COUNT_NAME_TEXT,
				"triedReconnectionCount", MemberAccessorToolkit.arrayElement(9)).description( //$NON-NLS-1$
						Messages.TRIED_RECONNECTION_COUNT_DESCRIPTION_TEXT).style(SWT.RIGHT).build();
		IColumn succeededReconnectionCount = new ColumnBuilder(Messages.SUCCEEDED_RECONNECTION_COUNT_NAME_TEXT,
				"succeededReconnectionCount", MemberAccessorToolkit.arrayElement(10)).description( //$NON-NLS-1$
						Messages.SUCCEEDED_RECONNECTION_COUNT_DESCRIPTION_TEXT).style(SWT.RIGHT).build();
		List<IColumn> columns = Arrays.asList(mri, state, connectionCount, disconnectionCount, eventCount,
				retainedEventCount, lastEventValue, lastEventPayload, connectionLostCount, triedReconnectionCount,
				succeededReconnectionCount);
		columnManager = ColumnManager.build(viewer, columns,
				TableSettings.forState(MementoToolkit.asState(stateMemento)));
		ColumnMenusFactory.addDefaultMenus(columnManager, MCContextMenuManager.create(table));

		startDebugInformation();

		getMCToolBarManager().add(new ToggleFreezeAction(getMCToolBarManager(), m_freezeModel));
		getMCToolBarManager().add(new ClearStatisticsAction());
	}

	private Observer createObserver() {
		return new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if (m_freezeModel.isFreezed()) {
					stopDebugInformation();
				} else {
					startDebugInformation();
				}
			}
		};
	}

	@Override
	public void dispose() {
		stopDebugInformation();
		clearDebugInformation();
		m_freezeModel.deleteObserver(m_observer);
	}

	void saveState(IMemento state) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(state));
	}

	private void startDebugInformation() {
		getSubscriptionDebugService().collectDebugInformation(true);
		if (m_tabRefresher == null) {
			m_tabRefresher = createTabRefresher();
			new Thread(m_tabRefresher).start();
		}
	}

	private void stopDebugInformation() {
		m_tabRefresher = null;
		getSubscriptionDebugService().collectDebugInformation(false);
	}

	private void clearDebugInformation() {
		getSubscriptionDebugService().clearDebugInformation();
	}

	private ISubscriptionDebugService getSubscriptionDebugService() {
		ISubscriptionService service = m_connection.getServiceOrNull(ISubscriptionService.class);
		if (service instanceof ISubscriptionDebugService) {
			return (ISubscriptionDebugService) service;
		}
		return new EmptySubscriptionDebugService();
	}

	private Runnable createTabRefresher() {
		return new Runnable() {
			@Override
			public void run() {
				while (this == m_tabRefresher) {
					refreshTable();
					try {
						Thread.sleep(DEFAULT_REFRESH_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			private void refreshTable() {
				DisplayToolkit.safeAsyncExec(new Runnable() {
					@Override
					public void run() {
						if (!columnManager.getViewer().getControl().isDisposed()) {
							columnManager.getViewer().refresh();
						}
					}
				});
			}
		};
	}
}
