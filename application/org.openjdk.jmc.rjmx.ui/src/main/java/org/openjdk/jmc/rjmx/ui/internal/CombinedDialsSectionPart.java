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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.commands.ICommandImageService;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.ui.attributes.EditDisplayNameAction;
import org.openjdk.jmc.ui.dial.Dial;
import org.openjdk.jmc.ui.dial.DialConfiguration;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MCToolBarManager;
import org.openjdk.jmc.ui.misc.MoveControlAction;
import org.openjdk.jmc.ui.rate.RateCoordinator;
import org.openjdk.jmc.ui.rate.RateLimitedRefresher;
import org.openjdk.jmc.ui.rate.RefreshController;

/**
 * Combines an accessible component with a set of dials.
 */
public class CombinedDialsSectionPart extends MCSectionPart implements IAttributeSet {
	private static final String ATTRIBUTE_DIAL_TAG = "attribute"; //$NON-NLS-1$
	private static final String TABLE_TAG = "table"; //$NON-NLS-1$

	private final StatisticsTable m_statisticsTable;
	private final Composite m_dialsHolder;
	private final GridLayout m_dialsHolderLayout;
	private final RefreshController m_refreshController;
	private final IConnectionHandle m_connection;
	private final IMRIMetadataService m_mds;
	private final IMRIService m_mriService;
	private final FreezeModel m_freezeModel = new FreezeModel();
	private final ISubscriptionService m_subscriptionService;
	private final Map<MRI, StatisticsCalculator> m_model = new HashMap<>();
	private final FormToolkit toolkit;
	private Control[] m_orderedDials;
	private final AccessibleControlAction accessibleControlAction;
	private final RateCoordinator rateCoordinator;
	private final RateLimitedRefresher statisticsRefresher;

	// Assume this happens so seldom (user initiated) that it doesn't need to be rate limited.
	private final Observer metadataObserver = new Observer() {
		@Override
		public void update(Observable o, Object arg) {
			statisticsRefresher.setNeedsRefresh();
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!getSection().isDisposed()) {
						for (Control child : m_dialsHolder.getChildren()) {
							((AttributeDial) child).updateMetadata();
						}
					}
				}
			});
		}
	};

	private class AttributeDial extends Dial implements IMRIValueListener {

		final StatisticsCalculator statistics;
		final MRI mri;
		private final RateLimitedRefresher infoRefresher;

		AttributeDial(MRI mri, DialConfiguration dialConfiguration) {
			super(m_dialsHolder, toolkit, dialConfiguration);
			this.mri = mri;
			statistics = new StatisticsCalculator(mri);
			infoRefresher = new RateLimitedRefresher(rateCoordinator, 100) {
				@Override
				protected void doRefresh() {
					Number last, max;
					synchronized (statistics) {
						last = statistics.getLast();
						max = statistics.getMax();
					}
					setInput(last, max, false);
				}
			};
			updateMetadata();
			String desc = m_mds.getMetadata(mri).getDescription();
			String path = MBeanPropertiesOrderer.mriAsTooltip(mri);
			getDialViewer().setToolTipText(desc != null && desc.length() > 0 ? path + "\n" + desc : path); //$NON-NLS-1$
		}

		@Override
		public void valueChanged(MRIValueEvent event) {
			if (event.getValue() instanceof Number) {
				final double value = ((Number) event.getValue()).doubleValue();
				synchronized (statistics) {
					statistics.addValue(value);
					infoRefresher.setNeedsRefresh();
					statisticsRefresher.setNeedsRefresh();
				}
			}
		}

		void updateMetadata() {
			IUnit unit = UnitLookup.getUnitOrDefault(m_mds.getMetadata(mri).getUnitString());
			if (!unit.equals(statistics.getUnit())) {
				statistics.setUnit(unit);
				setUnit(unit);
			}
			setTitle(MRIMetadataToolkit.getDisplayName(m_mds, mri));
		}

		public boolean reset() {
			if (statistics.reset()) {
				setInput(statistics.getLast(), statistics.getMax(), true);
				return true;
			}
			return false;
		}
	}

	public CombinedDialsSectionPart(Composite parent, FormToolkit toolkit, int style, IConnectionHandle ch) {
		this(parent, toolkit, style, ch, null);
	}

	public CombinedDialsSectionPart(Composite parent, FormToolkit toolkit, int style, IConnectionHandle ch,
			IMemento state) {
		super(parent, toolkit, style);
		m_refreshController = RefreshController.createGroup(parent.getDisplay());
		this.toolkit = toolkit;
		m_connection = ch;
		m_subscriptionService = m_connection.getServiceOrNull(ISubscriptionService.class);
		m_mds = m_connection.getServiceOrNull(IMRIMetadataService.class);
		m_mriService = m_connection.getServiceOrNull(IMRIService.class);
		rateCoordinator = new RateCoordinator();
		m_refreshController.add(rateCoordinator);
		statisticsRefresher = new RateLimitedRefresher(rateCoordinator, 500) {
			@Override
			protected void doRefresh() {
				TableViewer viewer = m_statisticsTable.getViewer();
				Control control = viewer.getControl();
				if ((control != null) && !control.isDisposed()) {
					boolean visible = control.isVisible();
					if (visible) {
						control.setRedraw(false);
					}
					// Unfortunately we have to do this even if not visible, until we reliably can detect when the viewer becomes visible
					// (Ideally, the super class or rate coordinator would handle this transparently for us.)
					viewer.refresh();
					if (visible) {
						control.setRedraw(true);
						control.update();
					}
				}
			}
		};
		m_freezeModel.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if (m_freezeModel.isFreezed()) {
					m_refreshController.remove(rateCoordinator);
				} else {
					m_refreshController.add(rateCoordinator);
				}
			}
		});

		final StackLayout stackLayout = new StackLayout();
		Composite stackContainer = createSectionBody(stackLayout);

		m_dialsHolder = toolkit.createComposite(stackContainer);
		m_dialsHolder.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				m_orderedDials = m_dialsHolder.getChildren();
			}
		});
		m_dialsHolderLayout = new GridLayout(0, true);
		m_dialsHolderLayout.horizontalSpacing = 0;
		m_dialsHolder.setLayout(m_dialsHolderLayout);
		final Composite tableContainer = toolkit.createComposite(stackContainer);
		tableContainer.setLayout(MCLayoutFactory.createMarginFreeFormPageLayout());
		IMemento tableState = state == null ? null : state.getChild(TABLE_TAG);
		m_statisticsTable = new StatisticsTable(tableContainer, new AttributeLabelProvider(m_mds, m_mriService), this,
				true, tableState);
		m_statisticsTable.getViewer().getControl().setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		m_statisticsTable.getViewer().setInput(m_model.values());

		getMCToolBarManager().add(new AddAttibutesAction(m_mds, m_mriService, this), MCToolBarManager.ALIGN_LEFT);
		getMCToolBarManager().add(new ToggleFreezeAction(getMCToolBarManager(), m_freezeModel));
		accessibleControlAction = new AccessibleControlAction() {
			@Override
			public void run() {
				stackLayout.topControl = isChecked() ? tableContainer : m_dialsHolder;
				tableContainer.setVisible(isChecked());
				m_dialsHolder.setVisible(!isChecked());
				updateStructure();
			}
		};
		getMCToolBarManager().add(accessibleControlAction);
		m_mds.addObserver(metadataObserver);
		if (state != null) {
			restoreState(state);
		}
	}

	@Override
	public void dispose() {
		if (accessibleControlAction != null) {
			accessibleControlAction.dispose();
		}
		m_refreshController.stop();
		m_mds.deleteObserver(metadataObserver);
		super.dispose();
	}

	@Override
	public void remove(MRI ... mris) {
		for (MRI mri : mris) {
			m_model.remove(mri);
		}
		if (m_model.size() == 0) {
			m_refreshController.stop();
		}
		Set<MRI> removeMris = new HashSet<>(Arrays.asList(mris));
		for (Control child : m_dialsHolder.getChildren()) {
			AttributeDial dial = (AttributeDial) child;
			if (removeMris.contains(dial.mri)) {
				dial.dispose();
			}
		}
		updateStructure();
	}

	@Override
	public void add(MRI ... mris) {
		for (MRI mri : mris) {
			add(mri, new DialConfiguration());
		}
		updateStructure();
	}

	@Override
	public boolean isEmpty() {
		return m_model.isEmpty();
	}

	@Override
	public MRI[] elements() {
		Set<MRI> attributes = m_model.keySet();
		return attributes.toArray(new MRI[attributes.size()]);
	}

	private boolean add(final MRI mri, DialConfiguration dialConfiguration) {
		if (!m_model.containsKey(mri)) {
			final AttributeDial newDial = new AttributeDial(mri, dialConfiguration);
			newDial.addDisposeListener(new DisposeListener() {

				@Override
				public void widgetDisposed(DisposeEvent e) {
					m_subscriptionService.removeMRIValueListener(newDial);
					m_refreshController.remove(newDial.getDialViewer());
				}
			});
			m_model.put(mri, newDial.statistics);
			m_refreshController.add(newDial.getDialViewer());
			m_refreshController.start();
			m_subscriptionService.addMRIValueListener(mri, newDial);

			MenuManager menuManager = new MenuManager();
			menuManager.add(new Action(Messages.AttributeDialSectionPart_CLEAR_STATISTICS_MENU_TEXT) {
				@Override
				public void run() {
					newDial.reset();
				}
			});
			menuManager.add(new MoveControlAction(newDial, false));
			menuManager.add(new MoveControlAction(newDial, true));
			menuManager.add(new Action(Messages.DELETE_COMMAND_TEXT) {
				{
					setImageDescriptor(PlatformUI.getWorkbench().getService(ICommandImageService.class)
							.getImageDescriptor(ActionFactory.DELETE.getCommandId()));
				}

				@Override
				public void run() {
					remove(mri);
				}
			});
			menuManager.add(new Separator());
			menuManager.add(new EditDisplayNameAction(mri, m_mds));
			menuManager.add(newDial.getPropertiesAction());
			newDial.getDialViewer().setMenu(menuManager.createContextMenu(newDial.getDialViewer()));
			newDial.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			return true;
		}
		return false;
	}

	private static int calculateNumberOfColumns(int elementCount) {
		int rowCount = Math.max(1, ceilDivision(elementCount, 5));
		return ceilDivision(elementCount, rowCount);
	}

	private static int ceilDivision(int nominator, int denominator) {
		return (nominator + denominator - 1) / denominator;
	}

	public void saveState(IMemento state) {
		m_statisticsTable.saveState(state.createChild(TABLE_TAG));
		for (Control child : m_dialsHolder.isDisposed() ? m_orderedDials : m_dialsHolder.getChildren()) {
			AttributeDial dial = (AttributeDial) child;
			IMemento dialState = state.createChild(ATTRIBUTE_DIAL_TAG);
			dialState.putTextData(dial.mri.getQualifiedName());
			dial.getDialConfiguration().saveState(dialState);
		}
	}

	public void restoreState(IMemento state) {
		for (IMemento dialState : state.getChildren(ATTRIBUTE_DIAL_TAG)) {
			MRI attribute = MRI.createFromQualifiedName(dialState.getTextData().trim());
			DialConfiguration dc = new DialConfiguration();
			dc.restoreState(dialState);
			add(attribute, dc);
		}
		updateStructure();
	}

	private void updateStructure() {
		m_dialsHolderLayout.numColumns = calculateNumberOfColumns(m_model.size());
		getSection().getParent().layout(true);
		m_dialsHolder.layout(true, true);
		m_statisticsTable.getViewer().refresh();
	}
}
