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

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.ColorToolkit;
import org.openjdk.jmc.greychart.ui.views.ChartComposite;
import org.openjdk.jmc.greychart.ui.views.ChartSampleTooltipProvider;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IAttributeStorage;
import org.openjdk.jmc.rjmx.services.IAttributeStorageService;
import org.openjdk.jmc.rjmx.services.MRIDataSeries;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.ui.attributes.EditDisplayNameAction;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.tree.IArray;
import org.openjdk.jmc.ui.common.xydata.DataSeries;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.MCArrayContentProvider;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.misc.MCToolBarManager;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;
import org.openjdk.jmc.ui.rate.RateCoordinator;
import org.openjdk.jmc.ui.rate.RateLimitedObserver;
import org.openjdk.jmc.ui.rate.RefreshController;

import org.openjdk.jmc.greychart.DefaultMetadataProvider;
import org.openjdk.jmc.greychart.data.RenderingMode;
import org.openjdk.jmc.greychart.data.SeriesProviderSet;

/**
 * Combines an accessible component with a graph chart.
 */
public class CombinedChartSectionPart extends MCSectionPart implements IAttributeSet {

	private static final String ATTRIBUTE_ID = "attribute"; //$NON-NLS-1$
	private static final String ENABLED_ID = "enabled"; //$NON-NLS-1$
	private static final String TABLE_TAG = "table"; //$NON-NLS-1$

	private final IMRIMetadataService mds;
	private final IAttributeStorageService m_storageService;

	private final Set<MRI> m_enabledAttributes = new HashSet<>();
	private final SeriesProviderSet<ITimestampedData> m_dataProvider = new SeriesProviderSet<>();
	private final Map<MRI, IAttributeStorage> m_attributeStorages = new HashMap<>();

	private StatisticsTable m_statisticsTable;
	private ChartComposite chart;
	private CheckboxTableViewer legend;
	private AccessibleControlAction accessibleControlAction;
	private final RefreshController m_refreshController;
	private final RateCoordinator rateCoordinator;
	private final Observer chartDataObserver;
	private final Observer statisticsDataObserver;

	private final Observer metadataObserver = new Observer() {
		@Override
		public void update(Observable o, Object arg) {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					if (!legend.getTable().isDisposed() && !chart.isDisposed()) {
						updateQuantityKind(chart.getChartModel().getYAxis().getKindOfQuantity());
						refreshAll();
					}
				}
			});
		}
	};

	private final IArray<StatisticsCalculator> statisticsProvider = new IArray<StatisticsCalculator>() {

		@Override
		public boolean isEmpty() {
			return m_attributeStorages.isEmpty();
		}

		@Override
		public StatisticsCalculator[] elements() {
			StatisticsCalculator[] statisticsArray = new StatisticsCalculator[m_attributeStorages.size()];
			int i = 0;
			for (Entry<MRI, IAttributeStorage> entry : m_attributeStorages.entrySet()) {
				MRI mri = entry.getKey();
				StatisticsCalculator stats = new StatisticsCalculator(mri);
				stats.setUnit(UnitLookup.getUnitOrDefault(mds.getMetadata(mri).getUnitString()));
				long min = chart.getChart().getXAxis().getMin().longValue();
				long max = chart.getChart().getXAxis().getMax().longValue();
				for (MRIDataSeries ds : entry.getValue().getDataSeries()) {
					Iterator<ITimestampedData> it = ds.createIterator(min, max);
					while (it.hasNext()) {
						ITimestampedData data = it.next();
						long timestamp = data.getX().longValue();
						if (min <= timestamp && timestamp <= max) {
							stats.addValue(data.getY().doubleValue());
						}
					}
				}
				statisticsArray[i++] = stats;
			}
			return statisticsArray;
		}
	};

	public CombinedChartSectionPart(Composite parent, FormToolkit toolkit, int style,
			IMRIMetadataService metadataService, IMRIService mris, IAttributeStorageService ss, IMemento state) {
		super(parent, toolkit, style);
		mds = metadataService;
		m_storageService = ss;
		mds.addObserver(metadataObserver);

		m_refreshController = RefreshController.createGroup(parent.getDisplay());
		rateCoordinator = new RateCoordinator();
		m_refreshController.add(rateCoordinator);
		chartDataObserver = new RateLimitedObserver(rateCoordinator, 200) {
			@Override
			protected void doRefresh(Object arg) {
				if (!chart.isDisposed()) {
					if (arg instanceof ITimestampedData) {
						long timestamp = ((ITimestampedData) arg).getX();
						chart.extendsDataRangeToInclude(timestamp);
					} else {
						chart.refresh();
					}
				}
			}
		};
		statisticsDataObserver = new RateLimitedObserver(rateCoordinator, 500) {
			@Override
			protected void doRefresh(Object arg) {
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

		Composite body = createSectionBody(MCLayoutFactory.createMarginFreeFormPageLayout());
		// FIXME: Long method, try to break out at least some anonymous classes
		final Composite stackContainer = toolkit.createComposite(body);
		stackContainer.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		final StackLayout stackLayout = new StackLayout();
		stackContainer.setLayout(stackLayout);
		final Composite chartContainer = toolkit.createComposite(stackContainer);
		chartContainer.setLayout(new GridLayout(2, false));
		IMemento tableState = state == null ? null : state.getChild(TABLE_TAG);
		m_statisticsTable = new StatisticsTable(stackContainer, new AttributeLabelProvider(mds, mris), this, false,
				tableState);
		m_statisticsTable.getViewer().setInput(statisticsProvider);
		chart = new ChartComposite(chartContainer, SWT.NONE, createEnableUpdatesCallback());
		chart.setChartSampleTooltipProvider(new ChartSampleTooltipProvider() {
			@Override
			public String getTooltip(DataSeries<?> series, double value) {
				if (series instanceof MRIDataSeries) {
					MRI mri = ((MRIDataSeries) series).getAttribute();
					IMRIMetadata metadata = mds.getMetadata(mri);
					IUnit unit = UnitLookup.getUnitOrNull(metadata.getUnitString());
					if (unit != null) {
						IQuantity quantity = unit.quantity(value);
						return MRIMetadataToolkit.getDisplayName(mds, mri) + ": " //$NON-NLS-1$
								+ quantity.displayUsing(IDisplayable.VERBOSE) + " (" //$NON-NLS-1$
								+ metadata.getValueType() + ", " //$NON-NLS-1$
								+ MBeanPropertiesOrderer.mriAsTooltip(mri) + ")"; //$NON-NLS-1$
					} else {
						return MRIMetadataToolkit.getDisplayName(mds, mri) + ": " //$NON-NLS-1$
								+ UnitLookup.NUMBER_UNITY.quantity(value).displayUsing(IDisplayable.AUTO) + " (" //$NON-NLS-1$
								+ metadata.getValueType() + ", " //$NON-NLS-1$
								+ MBeanPropertiesOrderer.mriAsTooltip(mri) + ")"; //$NON-NLS-1$
					}
				}
				return String.valueOf(value);
			}
		});
		chart.getChart().setMetadataProvider(new DefaultMetadataProvider() {
			@Override
			public Color getLineColor(DataSeries<?> ds) {
				MRI mri = ((MRIDataSeries) ds).getAttribute();
				return MRIMetadataToolkit.getColor(mds.getMetadata(mri));
			}

			@Override
			public RenderingMode getMode(DataSeries<?> ds) {
				return chart.getChartModel().getRenderingMode();
			}

			@Override
			public double getMultiplier(DataSeries<?> ds) {
				MRI mri = ((MRIDataSeries) ds).getAttribute();
				IUnit unit = UnitLookup.getUnitOrNull(mds.getMetadata(mri).getUnitString());
				if (unit instanceof LinearUnit) {
					LinearUnit linearUnit = (LinearUnit) unit;
					// FIXME: Determine outUnit through autorange, or pick one?
					LinearKindOfQuantity kind = linearUnit.getContentType();
					LinearUnit outUnit = kind.getDefaultUnit();
					return linearUnit.valueTransformTo(outUnit).getMultiplier();
				}
				return 1.0;
			}
		});

		chart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chart.showLast(ChartComposite.ONE_MINUTE);
		chart.getChart().setDataProvider(m_dataProvider);
		chart.getChartModel().addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				getSection().setText(chart.getChartModel().getChartTitle());
				chart.refresh();
			}
		});
		createLegend(toolkit, chartContainer, mris);
		getMCToolBarManager().add(new AddAttibutesAction(mds, mris, this) {

			@Override
			protected ContentType<?> getContentType() {
				return isEmpty() ? null : chart.getChartModel().getYAxis().getKindOfQuantity();
			}

			@Override
			protected boolean allowMultiple() {
				return !isEmpty();
			}

		}, MCToolBarManager.ALIGN_LEFT);
		accessibleControlAction = new AccessibleControlAction() {
			@Override
			public void run() {
				stackLayout.topControl = isChecked() ? m_statisticsTable.getViewer().getControl() : chartContainer;
				m_statisticsTable.getViewer().getControl().setVisible(isChecked());
				chartContainer.setVisible(!isChecked());
				refreshAll();
			}
		};
		getMCToolBarManager().add(accessibleControlAction);

		if (state != null) {
			restoreState(state);
		}
	}

	public CombinedChartSectionPart(Composite parent, FormToolkit toolkit, int style, IConnectionHandle connection,
			IMemento state) {
		this(parent, toolkit, style, connection.getServiceOrDummy(IMRIMetadataService.class),
				connection.getServiceOrDummy(IMRIService.class),
				connection.getServiceOrDummy(IAttributeStorageService.class), state);
	}

	public CombinedChartSectionPart(Composite parent, FormToolkit toolkit, int style, IConnectionHandle connection) {
		this(parent, toolkit, style, connection, null);
	}

	protected Consumer<Boolean> createEnableUpdatesCallback() {
		final IAction updatesAction = new Action(Messages.UpdatesAction_ACTION_NAME, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				chart.setUpdatesEnabled(isChecked());
				if (isChecked()) {
					m_refreshController.start();
				} else {
					m_refreshController.stop();
				}
			}
		};
		updatesAction.setChecked(false);
		updatesAction
				.setDisabledImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_REFRESH_GRAY));
		updatesAction.setImageDescriptor(UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_REFRESH));
		updatesAction.setToolTipText(Messages.UpdatesAction_TOOLTIP_TEXT);
		updatesAction.setId("toggle.freeze"); //$NON-NLS-1$
		getMCToolBarManager().add(updatesAction);
		return new Consumer<Boolean>() {

			@Override
			public void accept(Boolean enableUpdates) {
				chart.setUpdatesEnabled(enableUpdates);
				updatesAction.setChecked(enableUpdates);
				getMCToolBarManager().update();
				if (enableUpdates) {
					m_refreshController.start();
				} else {
					m_refreshController.stop();
				}
			}
		};
	}

	@Override
	public void dispose() {
		m_refreshController.stop();
		for (IAttributeStorage as : m_attributeStorages.values()) {
			as.deleteObserver(statisticsDataObserver);
			as.deleteObserver(chartDataObserver);
		}
		if (accessibleControlAction != null) {
			accessibleControlAction.dispose();
		}
		mds.deleteObserver(metadataObserver);
		super.dispose();
	}

	public void saveState(IMemento state) {
		m_statisticsTable.saveState(state.createChild(TABLE_TAG));
		chart.getChartModel().saveState(state);
		for (MRI mri : m_attributeStorages.keySet()) {
			IMemento child = state.createChild(ATTRIBUTE_ID);
			child.putTextData(mri.getQualifiedName());
			child.putBoolean(ENABLED_ID, isEnabled(mri));
		}
	}

	public void restoreState(IMemento state) {
		chart.getChartModel().restoreState(state);
		Set<MRI> current = new HashSet<>(m_attributeStorages.keySet());
		for (IMemento child : state.getChildren(ATTRIBUTE_ID)) {
			MRI mri = MRI.createFromQualifiedName(child.getTextData().trim());
			current.remove(mri);
			doAdd(mri);
			Boolean enabled = child.getBoolean(ENABLED_ID);
			setEnabled(mri, enabled == null || enabled);
		}
		if (!m_enabledAttributes.isEmpty()) {
			setQuantityKindFromAttribute(m_enabledAttributes.iterator().next());
		}
		for (MRI mri : current) {
			doRemove(mri);
		}
		refreshAll();
	}

	public ChartComposite getChart() {
		return chart;
	}

	@Override
	public void add(MRI ... mris) {
		for (MRI mri : mris) {
			doAdd(mri);
		}
		if (mris.length > 0) {
			setQuantityKindFromAttribute(mris[0]);
		}
		refreshAll();
	}

	private void doAdd(MRI mri) {
		IAttributeStorage storage = m_storageService.getAttributeStorage(mri);
		if (storage != null) {
			storage.addObserver(chartDataObserver);
			storage.addObserver(statisticsDataObserver);
			m_attributeStorages.put(mri, storage);
			doSetEnabled(mri, storage, true);
		}
	}

	@Override
	public void remove(MRI ... mris) {
		for (MRI mri : mris) {
			doRemove(mri);
		}
		refreshAll();
	}

	private void doRemove(MRI mri) {
		IAttributeStorage storage = m_attributeStorages.remove(mri);
		if (storage != null) {
			storage.deleteObserver(statisticsDataObserver);
			storage.deleteObserver(chartDataObserver);
			doSetEnabled(mri, storage, false);
		}
	}

	private void setEnabled(MRI mri, boolean enabled) {
		IAttributeStorage storage = m_attributeStorages.get(mri);
		if (storage != null && isEnabled(mri) != enabled) {
			doSetEnabled(mri, storage, enabled);
		}

	}

	private void doSetEnabled(MRI mri, IAttributeStorage storage, boolean enabled) {
		if (enabled) {
			m_enabledAttributes.add(mri);
			m_dataProvider.addDataSeries(storage.getDataSeries());
		} else {
			m_enabledAttributes.remove(mri);
			m_dataProvider.removeDataSeries(storage.getDataSeries());
		}
	}

	private boolean isEnabled(MRI attributeDescriptor) {
		return m_enabledAttributes.contains(attributeDescriptor);
	}

	@Override
	public MRI[] elements() {
		return m_attributeStorages.keySet().toArray(new MRI[m_attributeStorages.keySet().size()]);
	}

	@Override
	public boolean isEmpty() {
		return m_attributeStorages.isEmpty();
	}

	private void refreshAll() {
		updateDataRange();
		chart.refresh();
		if (m_statisticsTable.getViewer().getControl().isVisible()) {
			m_statisticsTable.getViewer().refresh();
		} else {
			legend.refresh();
		}
	}

	private void updateDataRange() {
		long dataStart = Long.MAX_VALUE;
		long dataEnd = Long.MIN_VALUE;
		for (MRI enabledAttribute : m_enabledAttributes) {
			IAttributeStorage storage = m_attributeStorages.get(enabledAttribute);
			dataStart = Math.min(dataStart, storage.getDataStart());
			dataEnd = Math.max(dataEnd, storage.getDataEnd());
		}
		chart.setDataRange(dataStart, dataEnd);
	}

	private void createLegend(FormToolkit formToolkit, Composite container, IMRIService mris) {
		Table table = formToolkit.createTable(container, SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		legend = new CheckboxTableViewer(table);
		legend.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				MRI mri = (MRI) event.getElement();
				boolean enable = event.getChecked();
				setEnabled(mri, enable);
				if (enable) {
					setQuantityKindFromAttribute(mri);
				}
				refreshAll();

			}

		});
		legend.setCheckStateProvider(new ICheckStateProvider() {

			@Override
			public boolean isGrayed(Object element) {
				return false;
			}

			@Override
			public boolean isChecked(Object element) {
				return isEnabled((MRI) element);
			}
		});
		legend.setComparator(new ViewerComparator());
		legend.setContentProvider(MCArrayContentProvider.INSTANCE);
		legend.setLabelProvider(new AttributeLabelProvider(mds, mris));
		legend.setInput(this);
		ColumnViewerToolTipSupport.enableFor(legend);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd2.heightHint = 60;
		gd2.widthHint = 210;
		table.setLayoutData(gd2);
		SelectionProviderAction editColorAction = new SelectionProviderAction(legend,
				Messages.ChartSectionPart_EDIT_COLOR_TEXT) {
			@Override
			public void run() {
				editColor((MRI) getStructuredSelection().getFirstElement());
			}

			@Override
			public void selectionChanged(IStructuredSelection selection) {
				setEnabled(selection.size() == 1);
			}
		};
		editColorAction.setEnabled(false);

		MCContextMenuManager mm = MCContextMenuManager.create(legend.getTable());
		RemoveAttributeAction removeAction = new RemoveAttributeAction(legend, this);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, removeAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_PROPETIES, editColorAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_PROPETIES, new EditDisplayNameAction(mds, legend));
		InFocusHandlerActivator.install(legend.getControl(), removeAction);
	}

	private void editColor(MRI descriptor) {
		ColorDialog dialog = new ColorDialog(Display.getCurrent().getActiveShell());
		RGB rgb = SWTColorToolkit.asRGB(MRIMetadataToolkit.getColor(mds.getMetadata(descriptor)));
		dialog.setRGB(rgb);
		RGB newRGB = dialog.open();
		if (newRGB != null) {
			String colorAsString = ColorToolkit.encode(new Color(newRGB.red, newRGB.green, newRGB.blue));
			mds.setMetadata(descriptor, IMRIMetadataProvider.KEY_COLOR, colorAsString);
		}
	}

	private void setQuantityKindFromAttribute(MRI attribute) {
		KindOfQuantity<?> kind = UnitLookup.getUnitOrDefault(mds.getMetadata(attribute).getUnitString())
				.getContentType();
		if (!chart.getChartModel().getYAxis().getKindOfQuantity().equals(kind)) {
			chart.getChartModel().getYAxis().setKindOfQuantity(kind);
			chart.getChartModel().getYAxis().notifyObservers();
		}
		updateQuantityKind(kind);
	}

	private void updateQuantityKind(KindOfQuantity<?> currentKind) {
		for (MRI mri : m_attributeStorages.keySet()) {
			IUnit unit = UnitLookup.getUnitOrDefault(mds.getMetadata(mri).getUnitString());
			if (!currentKind.equals(unit.getContentType())) {
				setEnabled(mri, false);
			}
		}
	}

}
