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
package org.openjdk.jmc.flightrecorder.ui.pages;

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.item.ItemQueryBuilder.fromWhere;
import static org.openjdk.jmc.common.unit.UnitLookup.MEMORY;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.LONGEST_GC_PAUSE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.TOTAL_GC_PAUSE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkQueries.HEAP_SUMMARY;
import static org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit.createAggregatorCheckAction;
import static org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit.createAttributeCheckAction;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemQueryBuilder;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.jdk.memory.ReferenceStatisticsType;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.common.ThreadGraphLanes;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.AWTChartToolkit;
import org.openjdk.jmc.ui.charts.ISpanSeries;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.QuantitySeries;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.SpanRenderer;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ActionUiToolkit;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class GarbageCollectionsPage extends AbstractDataPage {
	public static class GarbageCollectionPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.GarbageCollectionsPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_GC);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.GARBAGE_COLLECTION_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new GarbageCollectionsPage(dpd, items, editor);
		}
	}

	private static final ReferenceStatisticsType[] REF_TYPE = ReferenceStatisticsType.values();
	private static final String SASH = "sash"; //$NON-NLS-1$
	private static final String TABLE_SASH = "tableSash"; //$NON-NLS-1$
	private static final String THREAD_LANES = "threadLane"; //$NON-NLS-1$
	private static final String GCS = "gcs"; //$NON-NLS-1$
	private static final String CHART = "chart"; //$NON-NLS-1$
	private static final String PHASE_TABLE_FILTER = "phaseTableFilter"; //$NON-NLS-1$
	private static final String GC_TABLE_FILTER = "gcTableFilter"; //$NON-NLS-1$
	private static final String METASPACE_TABLE_FILTER = "metaspaceTableFilter"; //$NON-NLS-1$
	private static final String PHASE_LIST = "phaseList"; //$NON-NLS-1$
	private static final String METASPACE_LIST = "metaspaceList"; //$NON-NLS-1$
	private static final String ACTIVITY_LANES_ID = "threadActivityLanes"; //$NON-NLS-1$

	private final static Color LONGEST_PAUSE_COLOR = DataPageToolkit.GC_BASE_COLOR.brighter();
	private final static Color SUM_OF_PAUSES_COLOR = DataPageToolkit.GC_BASE_COLOR.brighter().brighter();

	public static final IAttribute<IQuantity> HEAP_USED_POST_GC = attr("heapUsed", Messages.ATTR_HEAP_USED_POST_GC, //$NON-NLS-1$
			Messages.ATTR_HEAP_USED_POST_GC_DESC, MEMORY);

	public static final IItemQuery HEAP_SUMMARY_POST_GC = fromWhere(JdkFilters.HEAP_SUMMARY_AFTER_GC)
			.select(HEAP_USED_POST_GC).build();
	private final static IItemQuery METASPACE_SUMMARY = ItemQueryBuilder.fromWhere(JdkFilters.METASPACE_SUMMARY)
			.select(JdkAttributes.GC_METASPACE_USED, JdkAttributes.GC_METASPACE_CAPACITY,
					JdkAttributes.GC_METASPACE_COMMITTED, JdkAttributes.GC_METASPACE_RESERVED)
			.build();

	private static class GC {
		final IType<IItem> type;
		final IItem gcItem;
		final Object[] referenceStatisticsData;
		IQuantity gcId;
		IQuantity duration;
		String gcCause;
		String gcName;
		IQuantity longestPause;
		IQuantity sumOfPauses;
		IQuantity startTime;
		IQuantity endTime;
		IQuantity usedDelta;
		IQuantity committedDelta;
		IQuantity usedMetaspaceDelta;
		IQuantity committedMetaspaceDelta;

		GC(IItem gcItem, IType<IItem> type) {
			this.type = type;
			this.gcItem = gcItem;
			referenceStatisticsData = new Object[REF_TYPE.length];
			usedDelta = UnitLookup.BYTE.quantity(0);
			committedDelta = UnitLookup.BYTE.quantity(0);
			usedMetaspaceDelta = UnitLookup.BYTE.quantity(0);
			committedMetaspaceDelta = UnitLookup.BYTE.quantity(0);
		}

		Object getRefCount(ReferenceStatisticsType type) {
			return referenceStatisticsData[type.ordinal()];
		}

		void setRefCount(Object type, Object count) {
			for (int i = 0; i < REF_TYPE.length; i++) {
				if (REF_TYPE[i].typeValue.equals(type)) {
					referenceStatisticsData[i] = count;
					break;
				}
			}
		}
	}

	private static final ItemListBuilder PHASES = new ItemListBuilder();
	private static final ItemListBuilder METASPACE = new ItemListBuilder();
	static {
		PHASES.addColumn(JfrAttributes.EVENT_TYPE);
		PHASES.addColumn(JdkAttributes.GC_PHASE_NAME);
		PHASES.addColumn(JfrAttributes.DURATION);
		PHASES.addColumn(JfrAttributes.START_TIME);
		PHASES.addColumn(JfrAttributes.EVENT_THREAD);
		PHASES.addColumn(JdkAttributes.GC_ID);

		METASPACE.addColumn(JdkAttributes.GC_METASPACE_USED);
		METASPACE.addColumn(JdkAttributes.GC_DATASPACE_COMMITTED);
		METASPACE.addColumn(JdkAttributes.GC_DATASPACE_RESERVED);
		METASPACE.addColumn(JdkAttributes.GC_DATASPACE_USED);
		METASPACE.addColumn(JdkAttributes.GC_CLASSSPACE_COMMITTED);
		METASPACE.addColumn(JdkAttributes.GC_CLASSSPACE_RESERVED);
		METASPACE.addColumn(JdkAttributes.GC_CLASSSPACE_USED);
		METASPACE.addColumn(JdkAttributes.GC_THRESHOLD);
		METASPACE.addColumn(JdkAttributes.GC_WHEN);
		METASPACE.addColumn(JdkAttributes.GC_ID);
		METASPACE.addColumn(JfrAttributes.START_TIME);
	}

	private class GarbageCollectionsUi implements IPageUI {

		private final SashForm sash;
		private final SashForm tableSash;
		private final IPageContainer pageContainer;
		private final ChartCanvas chartCanvas;
		private final ColumnManager gcList;
		private IXDataRenderer renderRoot = RendererToolkit.empty();
		private IAction GCEventThread = DataPageToolkit.createCheckAction(
				Messages.JavaApplicationPage_THREAD_ACTIVITY_ACTION,
				Messages.JavaApplicationPage_THREAD_ACTIVITY_ACTION_DESC, ACTIVITY_LANES_ID,
				FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_LANES), b -> buildChart());
		private final IAction enablePhases = ActionToolkit.checkAction(b -> buildChart(),
				Messages.GarbageCollectionsPage_ROW_PAUSE_PHASES, Messages.GarbageCollectionsPage_ROW_PAUSE_PHASES_DESC,
				FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_PARTS), "phases"); //$NON-NLS-1$
		private final IAction longestPause = createAggregatorCheckAction(LONGEST_GC_PAUSE, "longestPause", //$NON-NLS-1$
				LONGEST_PAUSE_COLOR, b -> buildChart());
		private final IAction sumOfPauses = createAggregatorCheckAction(TOTAL_GC_PAUSE, "sumOfPauses", //$NON-NLS-1$
				SUM_OF_PAUSES_COLOR, b -> buildChart());
		private final List<IAction> allChartSeriesActions = Stream.concat(
				Stream.concat(HEAP_SUMMARY.getAttributes().stream(),
						Stream.concat(HEAP_SUMMARY_POST_GC.getAttributes().stream(), METASPACE_SUMMARY.getAttributes().stream()))
						.map(a -> createAttributeCheckAction(a, b -> buildChart())),
				Stream.of(longestPause, sumOfPauses, enablePhases, GCEventThread)).collect(Collectors.toList());
		private final Set<String> excludedAttributeIds;
		private FilterComponent tableFilter;
		private XYChart gcChart;
		private IRange<IQuantity> currentRange;
		private ItemList phasesList;
		private FilterComponent phasesFilter;
		private ItemList metaspaceList;
		private FilterComponent metaspaceFilter;
		private CTabFolder gcInfoFolder;
		private IItemCollection selectionItems;
		private FlavorSelector flavorSelector;
		private ThreadGraphLanes lanes;
		private MCContextMenuManager mm;

		GarbageCollectionsUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			excludedAttributeIds = calculateExcludedAttributeIds(getDataSource().getItems());
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);
			tableSash = new SashForm(sash, SWT.HORIZONTAL);
			toolkit.adapt(tableSash);

			TableViewer tableViewer = new TableViewer(tableSash,
					SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
			tableViewer.setContentProvider(ArrayContentProvider.getInstance());
			ColumnViewerToolTipSupport.enableFor(tableViewer);
			List<IColumn> columns = new ArrayList<>();
			columns.add(buildGCItemAttributeColumn(JdkAttributes.GC_ID, o -> ((GC) o).gcId));
			columns.add(buildGCItemAttributeColumn(JfrAttributes.DURATION, o -> ((GC) o).duration));
			columns.add(buildGCItemAttributeColumn(JdkAttributes.GC_CAUSE, o -> ((GC) o).gcCause));
			columns.add(buildGCItemAttributeColumn(JdkAttributes.GC_NAME, o -> ((GC) o).gcName));
			columns.add(buildGCItemAttributeColumn(JdkAttributes.GC_LONGEST_PAUSE, o -> ((GC) o).longestPause));
			columns.add(buildGCItemAttributeColumn(JdkAttributes.GC_SUM_OF_PAUSES, o -> ((GC) o).sumOfPauses));
			columns.add(buildGCItemAttributeColumn(JfrAttributes.START_TIME, o -> ((GC) o).startTime));
			columns.add(buildGCItemAttributeColumn(JfrAttributes.END_TIME, o -> ((GC) o).endTime));
			for (ReferenceStatisticsType t : REF_TYPE) {
				columns.add(new ColumnBuilder(t.localizedName, "ReferenceStatisticsType-" + t.name(), //$NON-NLS-1$
						o -> ((GC) o).getRefCount(t)).style(SWT.RIGHT).build());
			}
			columns.add(new ColumnBuilder(Messages.GarbageCollectionsPage_USED_HEAP_DELTA, "usedHeapDelta", //$NON-NLS-1$
					o -> ((GC) o).usedDelta).style(SWT.RIGHT).build());
			columns.add(new ColumnBuilder(Messages.GarbageCollectionsPage_COMMITTED_HEAP_DELTA, "committedHeapDelta", //$NON-NLS-1$
					o -> ((GC) o).committedDelta).style(SWT.RIGHT).build());
			columns.add(new ColumnBuilder(Messages.GarbageCollectionsPage_USED_METASPACE_DELTA, "usedMetaspaceDelta", //$NON-NLS-1$
					o -> ((GC) o).usedMetaspaceDelta).style(SWT.RIGHT).build());
			columns.add(new ColumnBuilder(Messages.GarbageCollectionsPage_COMMITTED_METASPACE_DELTA,
					"committedMetaspaceDelta", o -> ((GC) o).committedMetaspaceDelta).style(SWT.RIGHT).build()); //$NON-NLS-1$

			gcList = ColumnManager.build(tableViewer, columns, TableSettings.forState(state.getChild(GCS)));
			MCContextMenuManager itemListMm = MCContextMenuManager.create(gcList.getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(gcList, itemListMm);
			gcList.getViewer().addSelectionChangedListener(e -> {
				buildChart();
				pageContainer.showSelection(ItemCollectionToolkit.build(gcSelectedGcItems()));
				updatePhaseList();
				updateMetaspaceList();
			});

			SelectionStoreActionToolkit.addSelectionStoreActions(gcList.getViewer(), pageContainer.getSelectionStore(),
					() -> ItemCollectionToolkit.build(gcSelectedGcItems()),
					Messages.GarbageCollectionsPage_LIST_SELECTION, itemListMm);
			tableFilter = FilterComponent.createFilterComponent(tableViewer.getControl(), gcList, tableFilterState,
					getDataSource().getItems().apply(JdkFilters.GARBAGE_COLLECTION),
					pageContainer.getSelectionStore()::getSelections, this::onFilterChange);
			itemListMm.add(tableFilter.getShowFilterAction());
			itemListMm.add(tableFilter.getShowSearchAction());

			gcInfoFolder = new CTabFolder(tableSash, SWT.NONE);
			phasesList = PHASES.buildWithoutBorder(gcInfoFolder, TableSettings.forState(state.getChild(PHASE_LIST)));
			phasesList.getManager().getViewer().addSelectionChangedListener(e -> {
					buildChart();	
					pageContainer.showSelection(ItemCollectionToolkit.build(phasesList.getSelection().get()));
			});
			phasesFilter = FilterComponent.createFilterComponent(phasesList, phasesFilterState,
					getDataSource().getItems().apply(JdkFilters.GC_PAUSE_PHASE),
					pageContainer.getSelectionStore()::getSelections, this::onPhasesFilterChange);
			MCContextMenuManager phasesMm = MCContextMenuManager
					.create(phasesList.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(phasesList.getManager(), phasesMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), phasesList,
					Messages.GarbageCollectionsPage_PAUSE_PHASE_SELECTION, phasesMm);
			phasesMm.add(phasesFilter.getShowFilterAction());
			phasesMm.add(phasesFilter.getShowSearchAction());
			DataPageToolkit.addTabItem(gcInfoFolder, phasesFilter.getComponent(),
					Messages.GarbageCollectionsPage_PAUSE_PHASES_TITLE);

			metaspaceList = METASPACE.buildWithoutBorder(gcInfoFolder,
					TableSettings.forState(state.getChild(METASPACE_LIST)));
			metaspaceList.getManager().getViewer().addSelectionChangedListener(
					e -> pageContainer.showSelection(ItemCollectionToolkit.build(metaspaceList.getSelection().get())));
			metaspaceFilter = FilterComponent.createFilterComponent(metaspaceList, metaspaceFilterState,
					getDataSource().getItems().apply(JdkFilters.METASPACE_SUMMARY),
					pageContainer.getSelectionStore()::getSelections, this::onMetaspaceFilterChange);
			MCContextMenuManager metaspaceMm = MCContextMenuManager
					.create(metaspaceList.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(metaspaceList.getManager(), metaspaceMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), metaspaceList,
					Messages.GarbageCollectionsPage_METASPACE_SELECTION, metaspaceMm);
			metaspaceMm.add(metaspaceFilter.getShowFilterAction());
			metaspaceMm.add(metaspaceFilter.getShowSearchAction());
			DataPageToolkit.addTabItem(gcInfoFolder, metaspaceFilter.getComponent(),
					Messages.GarbageCollectionsPage_METASPACE_TITLE);

			Composite chartContainer = toolkit.createComposite(sash);
			chartContainer.setLayout(new GridLayout(2, false));
			chartCanvas = new ChartCanvas(chartContainer);
			chartCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			ActionToolkit.loadCheckState(state.getChild(CHART), allChartSeriesActions.stream());
			CheckboxTableViewer chartLegend = ActionUiToolkit.buildCheckboxViewer(chartContainer,
					allChartSeriesActions.stream().filter(a -> includeAttribute(a.getId())));
			GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
			gd.widthHint = 180;
			chartLegend.getControl().setLayoutData(gd);
			lanes = new ThreadGraphLanes(() -> getDataSource(), () -> buildChart());
			lanes.initializeChartConfiguration(Stream.of(state.getChildren(THREAD_LANES)));
			IAction editLanesAction = ActionToolkit.action(() -> lanes.openEditLanesDialog(mm, false),
					Messages.ThreadsPage_EDIT_LANES, FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_LANES_EDIT));
			form.getToolBarManager().add(editLanesAction);
			
			DataPageToolkit.createChartTimestampTooltip(chartCanvas);
			gcChart = new XYChart(pageContainer.getRecordingRange(), renderRoot, 180);
			gcChart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			gcChart.addVisibleRangeListener(r -> timelineRange = r);

			PersistableSashForm.loadState(sash, state.getChild(SASH));
			PersistableSashForm.loadState(tableSash, state.getChild(TABLE_SASH));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, JdkFilters.GARBAGE_COLLECTION,
					getDataSource().getItems(), pageContainer, this::onInputSelected, this::onShow,
					flavorSelectorState);

			gcInfoFolder.setSelection(gcInfoTabSelection);
			addResultActions(form);
			tableFilter.loadState(state.getChild(GC_TABLE_FILTER));
			phasesFilter.loadState(state.getChild(PHASE_TABLE_FILTER));
			metaspaceFilter.loadState(state.getChild(METASPACE_TABLE_FILTER));
			gcList.setSelectionState(gcListSelection);
			phasesList.getManager().setSelectionState(phasesSelection);
			metaspaceList.getManager().setSelectionState(metaspaceSelection);
			mm = (MCContextMenuManager) chartCanvas.getContextMenu();
			lanes.updateContextMenu(mm, false);
			lanes.updateContextMenu(MCContextMenuManager.create(chartLegend.getControl()), true);
			
			// Older recordings may not have thread information in pause events.
			// In those cases there is no need for the thread activity actions.
			if (!getDataSource().getItems().apply(ItemFilters.and(ItemFilters.hasAttribute(JfrAttributes.EVENT_THREAD),
					JdkFilters.GC_PAUSE)).hasItems()) {
				editLanesAction.setEnabled(false);
				editLanesAction.setToolTipText(Messages.GarbageCollectionsPage_DISABLED_TOOLTIP);
				GCEventThread.setEnabled(false);
				GCEventThread.setDescription(Messages.GarbageCollectionsPage_DISABLED_TOOLTIP);
				for (IAction action : lanes.getContextMenuActions()) {
					action.setEnabled(false);
				}
			}
		}

		private void updatePhaseList() {
			phasesList.show(ItemCollectionToolkit.filterIfNotNull(getPhaseItems(), phasesFilterState));
		}

		private void updateMetaspaceList() {
			metaspaceList.show(ItemCollectionToolkit.filterIfNotNull(getMetaspaceItems(), metaspaceFilterState));
		}

		private IItemCollection getMetaspaceItems() {
			Set<IQuantity> selectedGcIds = getSelectedGcIds();
			IItemCollection metaspaceItems = getDataSource().getItems().apply(JdkFilters.METASPACE_SUMMARY)
					.apply(ItemFilters.memberOf(JdkAttributes.GC_ID, selectedGcIds));
			return metaspaceItems;
		}

		private IItemCollection getPhaseItems() {
			Set<IQuantity> gcIds = getSelectedGcIds();
			IItemCollection gcIdPausePhases = getDataSource().getItems().apply(JdkFilters.GC_PAUSE_PHASE)
					.apply(ItemFilters.memberOf(JdkAttributes.GC_ID, gcIds));
			return gcIdPausePhases;
		}

		private Set<IQuantity> getSelectedGcIds() {
			@SuppressWarnings("unchecked")
			List<GC> selected = ((IStructuredSelection) gcList.getViewer().getSelection()).toList();
			Set<IQuantity> gcIds = selected.stream()
					.map(gc -> gc.type.getAccessor(JdkAttributes.GC_ID.getKey()).getMember(gc.gcItem))
					.collect(Collectors.toSet());
			return gcIds;
		}

		private void onFilterChange(IItemFilter newFilter) {
			IItemCollection items = selectionItems != null ? selectionItems : getDataSource().getItems();
			items = items.apply(JdkFilters.GARBAGE_COLLECTION);
			if (tableFilter.isVisible()) {
				updateTable(ItemCollectionToolkit.filterIfNotNull(items, newFilter));
				Object input = gcList.getViewer().getInput();
				tableFilter.setColor(input instanceof Object[] && ((Object[]) input).length > 0 ? 1 : 0);
			} else {
				updateTable(items);
			}
			tableFilterState = newFilter;
		}

		private void onPhasesFilterChange(IItemFilter filter) {
			phasesFilter.filterChangeHelper(filter, phasesList,
					getDataSource().getItems().apply(JdkFilters.GC_PAUSE_PHASE));
			phasesFilterState = filter;
		}

		private void onMetaspaceFilterChange(IItemFilter filter) {
			metaspaceFilter.filterChangeHelper(filter, metaspaceList,
					getDataSource().getItems().apply(JdkFilters.METASPACE_SUMMARY));
			metaspaceFilterState = filter;
		}

		private ItemRow buildSpanRow(IItemCollection items, String typeId) {
			IItemCollection filtered = items.apply(ItemFilters.type(typeId));
			return new ItemRow(DataPageToolkit.buildSpanRenderer(filtered,
					AWTChartToolkit.staticColor(TypeLabelProvider.getColorOrDefault(typeId))), filtered);
		}

		private void buildChart() {
			IItemCollection allItems = getDataSource().getItems();
			List<IXDataRenderer> rows = new ArrayList<>();
			Predicate<IAttribute<IQuantity>> legendFilter = this::isAttributeEnabled;
			DataPageToolkit.buildLinesRow(Messages.GarbageCollectionsPage_ROW_HEAP,
					Messages.GarbageCollectionsPage_ROW_HEAP_DESC, allItems, false, HEAP_SUMMARY, legendFilter,
					UnitLookup.BYTE.quantity(0), null).ifPresent(rows::add);
			DataPageToolkit.buildLinesRow(Messages.GarbageCollectionsPage_ROW_HEAP_POST_GC,
					Messages.GarbageCollectionsPage_ROW_HEAP_POST_GC_DESC, allItems, false, HEAP_SUMMARY_POST_GC, legendFilter,
					UnitLookup.BYTE.quantity(0), null).ifPresent(rows::add);
			DataPageToolkit.buildLinesRow(Messages.GarbageCollectionsPage_ROW_METASPACE,
					Messages.GarbageCollectionsPage_ROW_METASPACE_DESC, allItems, false, METASPACE_SUMMARY,
					legendFilter, UnitLookup.BYTE.quantity(0), null).ifPresent(rows::add);
			// Pauses
			List<IXDataRenderer> gcPauseRows = new ArrayList<>();
			IItemCollection pauseEvents = allItems.apply(JdkFilters.GC_PAUSE);
			if (longestPause.isChecked()) {
				gcPauseRows.add(DataPageToolkit.buildTimestampHistogramRenderer(pauseEvents, LONGEST_GC_PAUSE,
						LONGEST_PAUSE_COLOR));
			}
			if (sumOfPauses.isChecked()) {
				gcPauseRows.add(DataPageToolkit.buildTimestampHistogramRenderer(pauseEvents, TOTAL_GC_PAUSE,
						SUM_OF_PAUSES_COLOR));
			}
			if (!gcPauseRows.isEmpty()) {
				rows.add(RendererToolkit.layers(DataPageToolkit.buildGcPauseRow(allItems),
						RendererToolkit.uniformRows(gcPauseRows)));
			}
			// Phases
			if (enablePhases.isChecked()) {
				ItemRow pauses = buildSpanRow(allItems, JdkTypeIDs.GC_PAUSE);
				ItemRow l1 = buildSpanRow(allItems, JdkTypeIDs.GC_PAUSE_L1);
				ItemRow l2 = buildSpanRow(allItems, JdkTypeIDs.GC_PAUSE_L2);
				ItemRow l3 = buildSpanRow(allItems, JdkTypeIDs.GC_PAUSE_L3);
				ItemRow l4 = buildSpanRow(allItems, JdkTypeIDs.GC_PAUSE_L4);
				rows.add(RendererToolkit.uniformRows(Arrays.asList(pauses, l1, l2, l3, l4), enablePhases.getText()));
			}
			IItemFilter pauseThreadsFilter = ItemFilters.and(JdkFilters.GC_PAUSE, ItemFilters.hasAttribute(JfrAttributes.EVENT_THREAD));
			// Thread information may not be available in earlier recordings, ensure we actually have items before proceeding
			if (GCEventThread.isChecked() && phasesList.getSelection().get().count() > 0 
					&& allItems.apply(pauseThreadsFilter).hasItems()) {
				// Get the event threads from the selected events
				IAggregator<Set<IMCThread>, ?> distinctThreadsAggregator = Aggregators.distinct(JfrAttributes.EVENT_THREAD);
				IItemCollection items = ItemCollectionToolkit.build(phasesList.getSelection().get());
				Set<IMCThread> threads = items.getAggregate(distinctThreadsAggregator);
				List<IXDataRenderer> renderers = threads.stream().map((thread) ->lanes.buildThreadRenderer(thread,
						getDataSource().getItems().apply(ItemFilters.equals(JfrAttributes.EVENT_THREAD, thread))))
						.collect(Collectors.toList());
				rows.add(RendererToolkit.uniformRows(renderers));
			}

			renderRoot = RendererToolkit.layers(RendererToolkit.uniformRows(rows), buildTableSelectionRenderer());
			chartCanvas.replaceRenderer(renderRoot);
		}

		private boolean isAttributeEnabled(IAttribute<IQuantity> attr) {
			String id = attr.getIdentifier();
			String name = attr.getName();
			return includeAttribute(id)
					&& allChartSeriesActions.stream().filter(a -> name.equals(a.getText())).findAny().get().isChecked();
		}

		private boolean includeAttribute(String attrId) {
			return !excludedAttributeIds.contains(attrId);
		}

		private IXDataRenderer buildTableSelectionRenderer() {
			Supplier<Stream<? extends IItem>> phaseSelection = phasesList.getSelection();
			Stream<? extends IItem> gcItems = phaseSelection.get().count() > 0 ? phaseSelection.get()
					: gcSelectedGcItems();
			ISpanSeries<IItem> gcBackdrop = QuantitySeries.max(ItemCollectionToolkit.build(gcItems),
					JfrAttributes.START_TIME, JfrAttributes.END_TIME);
			return SpanRenderer.build(gcBackdrop, AWTChartToolkit.staticColor(new Color(100, 180, 220, 150)));
		}

		@Override
		public void saveTo(IWritableState memento) {
			PersistableSashForm.saveState(sash, memento.createChild(SASH));
			PersistableSashForm.saveState(tableSash, memento.createChild(TABLE_SASH));
			gcList.getSettings().saveState(memento.createChild(GCS));
			phasesList.getManager().getSettings().saveState(memento.createChild(PHASE_LIST));
			metaspaceList.getManager().getSettings().saveState(memento.createChild(METASPACE_LIST));
			ActionToolkit.saveCheckState(memento.createChild(CHART), allChartSeriesActions.stream());
			tableFilter.saveState(memento.createChild(GC_TABLE_FILTER));
			phasesFilter.saveState(memento.createChild(PHASE_TABLE_FILTER));
			metaspaceFilter.saveState(memento.createChild(METASPACE_TABLE_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			gcListSelection = gcList.getSelectionState();
			phasesSelection = phasesList.getManager().getSelectionState();
			metaspaceSelection = metaspaceList.getManager().getSelectionState();
			gcInfoTabSelection = gcInfoFolder.getSelectionIndex();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void updateTable(IItemCollection gcs) {
			Map<Object, GC> gcMap = new HashMap<>();
			gcs.forEach(is -> {
				IMemberAccessor<IQuantity, IItem> gcIdAccessor = JdkAttributes.GC_ID.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> durationAccessor = JfrAttributes.DURATION.getAccessor(is.getType());
				IMemberAccessor<String, IItem> causeAccessor = JdkAttributes.GC_CAUSE.getAccessor(is.getType());
				IMemberAccessor<String, IItem> nameAccessor = JdkAttributes.GC_NAME.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> longestPauseAccessor = JdkAttributes.GC_LONGEST_PAUSE
						.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> sumPauseAccessor = JdkAttributes.GC_SUM_OF_PAUSES
						.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME
						.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> endTimeAccessor = JfrAttributes.END_TIME.getAccessor(is.getType());

				is.forEach(item -> {
					GC value = new GC(item, is.getType());
					value.gcId = gcIdAccessor.getMember(item);
					value.duration = durationAccessor.getMember(item);
					value.gcCause = causeAccessor.getMember(item);
					value.gcName = nameAccessor.getMember(item);
					value.longestPause = longestPauseAccessor.getMember(item);
					value.sumOfPauses = sumPauseAccessor.getMember(item);
					value.startTime = startTimeAccessor.getMember(item);
					value.endTime = endTimeAccessor.getMember(item);
					logDuplicateGcId(gcMap.put(gcIdAccessor.getMember(item), value));
				});
			});
			IItemCollection refItems = getDataSource().getItems().apply(JdkFilters.REFERENCE_STATISTICS);
			refItems.forEach(is -> {
				IMemberAccessor<IQuantity, IItem> gdIdAccessor = JdkAttributes.GC_ID.getAccessor(is.getType());
				IMemberAccessor<String, IItem> typeAccessor = JdkAttributes.REFERENCE_STATISTICS_TYPE
						.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> countAccessor = JdkAttributes.REFERENCE_STATISTICS_COUNT
						.getAccessor(is.getType());
				is.forEach(item -> {
					GC gc = gcMap.get(gdIdAccessor.getMember(item));
					if (gc != null) {
						gc.setRefCount(typeAccessor.getMember(item), countAccessor.getMember(item));
					}
				});
			});
			IItemCollection heapItems = getDataSource().getItems().apply(JdkFilters.HEAP_SUMMARY);
			heapItems.forEach(is -> {
				IMemberAccessor<IQuantity, IItem> gcIdAccessor = JdkAttributes.GC_ID.getAccessor(is.getType());
				IMemberAccessor<String, IItem> gcWhenAccessor = JdkAttributes.GC_WHEN.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> usedHeapAccessor = JdkAttributes.HEAP_USED.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> committedHeapAccessor = JdkAttributes.GC_HEAPSPACE_COMMITTED
						.getAccessor(is.getType());

				is.forEach(item -> {
					GC gc = gcMap.get(gcIdAccessor.getMember(item));
					if (gc != null) {
						String when = gcWhenAccessor.getMember(item);
						if ("Before GC".equals(when)) { //$NON-NLS-1$
							gc.usedDelta = gc.usedDelta.subtract(usedHeapAccessor.getMember(item));
							gc.committedDelta = gc.committedDelta.subtract(committedHeapAccessor.getMember(item));
						} else {
							gc.usedDelta = gc.usedDelta.add(usedHeapAccessor.getMember(item));
							gc.committedDelta = gc.committedDelta.add(committedHeapAccessor.getMember(item));
						}
					}
				});
			});

			IItemCollection metaspaceItems = getDataSource().getItems().apply(JdkFilters.METASPACE_SUMMARY);
			metaspaceItems.forEach(is -> {
				IMemberAccessor<IQuantity, IItem> gcIdAccessor = JdkAttributes.GC_ID.getAccessor(is.getType());
				IMemberAccessor<String, IItem> gcWhenAccessor = JdkAttributes.GC_WHEN.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> usedMetaspaceAccessor = JdkAttributes.GC_METASPACE_USED
						.getAccessor(is.getType());
				IMemberAccessor<IQuantity, IItem> committedMetaspaceAccessor = JdkAttributes.GC_METASPACE_COMMITTED
						.getAccessor(is.getType());

				is.forEach(item -> {
					GC gc = gcMap.get(gcIdAccessor.getMember(item));
					if (gc != null && usedMetaspaceAccessor != null && committedMetaspaceAccessor != null
							&& gcWhenAccessor != null) {
						String when = gcWhenAccessor.getMember(item);
						if ("Before GC".equals(when)) { //$NON-NLS-1$
							gc.usedMetaspaceDelta = gc.usedMetaspaceDelta
									.subtract(usedMetaspaceAccessor.getMember(item));
							gc.committedMetaspaceDelta = gc.committedMetaspaceDelta
									.subtract(committedMetaspaceAccessor.getMember(item));
						} else {
							gc.usedMetaspaceDelta = gc.usedMetaspaceDelta.add(usedMetaspaceAccessor.getMember(item));
							gc.committedMetaspaceDelta = gc.committedMetaspaceDelta
									.add(committedMetaspaceAccessor.getMember(item));
						}
					}
				});
			});
			gcList.getViewer().setInput(gcMap.values().toArray());
		}

		private void onShow(Boolean show) {
			IRange<IQuantity> range = show ? currentRange : pageContainer.getRecordingRange();
			gcChart.setVisibleRange(range.getStart(), range.getEnd());
			buildChart();
		}

		private void updateChart() {
			DataPageToolkit.setChart(chartCanvas, gcChart, pageContainer::showSelection);
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), gcChart,
					JfrAttributes.LIFETIME, Messages.GarbageCollectionsPage_TIMELINE_SELECTION,
					chartCanvas.getContextMenu());
			buildChart();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			this.currentRange = timeRange;
			selectionItems = items;
			IItemCollection gcs = items != null ? items : getDataSource().getItems();
			updateTable(gcs.apply(JdkFilters.GARBAGE_COLLECTION));
			updateChart();
		}

		private Stream<? extends IItem> gcSelectedGcItems() {
			@SuppressWarnings("unchecked")
			List<GC> sel = ((IStructuredSelection) gcList.getViewer().getSelection()).toList();
			return sel.stream().map(gc -> gc.gcItem);
		}
	}

	private static void logDuplicateGcId(GC duplicateGC) {
		if (duplicateGC != null) {
			IQuantity gcID = JdkAttributes.GC_ID.getAccessor(ItemToolkit.getItemType(duplicateGC.gcItem))
					.getMember(duplicateGC.gcItem);
			FlightRecorderUI.getDefault().getLogger().severe("GC with id " + gcID + " is duplicated"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static IColumn buildGCItemAttributeColumn(IAttribute<?> a, IMemberAccessor<?, Object> cellAccessor) {
		int style = a.getContentType() instanceof LinearKindOfQuantity ? SWT.RIGHT : SWT.NONE;
		return new ColumnBuilder(a.getName(), a.getIdentifier(), cellAccessor).description(a.getDescription())
				.style(style).build();
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new GarbageCollectionsUi(parent, toolkit, pageContainer, state);
	}

	private IItemFilter tableFilterState;
	private IItemFilter phasesFilterState;
	private IItemFilter metaspaceFilterState;
	private IRange<IQuantity> timelineRange;
	private SelectionState gcListSelection;
	private SelectionState phasesSelection;
	private SelectionState metaspaceSelection;
	private int gcInfoTabSelection = 0;
	public FlavorSelectorState flavorSelectorState;

	public GarbageCollectionsPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		timelineRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.or(JdkFilters.GC_PAUSE, JdkFilters.GC_PAUSE_PHASE, JdkFilters.HEAP_SUMMARY,
				JdkFilters.METASPACE_SUMMARY);
	}

	private static Set<String> calculateExcludedAttributeIds(IItemCollection items) {
		// In JDK7 there are no metaspace events. In early JDK8
		// metaspace:committed is missing. In later JDK8 metaspace:capacity is
		// missing.
		Stream<IAttribute<?>> exclude = METASPACE_SUMMARY.getAttributes().stream();
		Iterator<IItemIterable> iterator = items.apply(METASPACE_SUMMARY.getFilter()).iterator();
		if (iterator.hasNext()) {
			IType<IItem> type = iterator.next().getType();
			exclude = exclude.filter(a -> a.getAccessor(type) == null);
		}
		return exclude.map(IAttribute::getIdentifier).collect(Collectors.toSet());
	}
}
