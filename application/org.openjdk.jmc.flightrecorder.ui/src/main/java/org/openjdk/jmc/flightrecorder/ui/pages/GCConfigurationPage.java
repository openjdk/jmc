/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemAggregateViewer;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.CompositeKeyHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.accessibility.SimpleTraverseListener;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class GCConfigurationPage extends AbstractDataPage {
	public static class GCConfigurationPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.GCConfigurationPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_GC_CONFIGURATION);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.GC_CONFIGURATION};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new GCConfigurationPage(dpd, items, editor);
		}

	}

	private static final String GC_FLAG_SASH = "gcFlagSash"; //$NON-NLS-1$
	private static final String JVM_GC_FLAGS = "jvmFlags"; //$NON-NLS-1$
	private static final String JVM_FLAGS_FILTER = "jvmFlagsFilter"; //$NON-NLS-1$s
	private static final Set<String> FLAGS;

	static {
		Set<String> types = new HashSet<>();
		types.add(JdkTypeIDs.LONG_FLAG);
		types.add(JdkTypeIDs.ULONG_FLAG);
		types.add(JdkTypeIDs.DOUBLE_FLAG);
		types.add(JdkTypeIDs.BOOLEAN_FLAG);
		types.add(JdkTypeIDs.STRING_FLAG);
		types.add(JdkTypeIDs.INT_FLAG);
		types.add(JdkTypeIDs.UINT_FLAG);
		FLAGS = Collections.unmodifiableSet(types);
	}

	private static final IItemFilter FLAGS_FILTER = ItemFilters.type(FLAGS);

	private static final IAccessorFactory<?> FLAG_VALUE_FIELD = new IAccessorFactory<Object>() {

		@Override
		public <T> IMemberAccessor<?, T> getAccessor(IType<T> type) {
			switch (type.getIdentifier()) {
			case JdkTypeIDs.LONG_FLAG:
			case JdkTypeIDs.ULONG_FLAG:
			case JdkTypeIDs.DOUBLE_FLAG:
			case JdkTypeIDs.INT_FLAG:
			case JdkTypeIDs.UINT_FLAG:
				return JdkAttributes.FLAG_VALUE_NUMBER.getAccessor(type);
			case JdkTypeIDs.BOOLEAN_FLAG:
				return JdkAttributes.FLAG_VALUE_BOOLEAN.getAccessor(type);
			case JdkTypeIDs.STRING_FLAG:
				return JdkAttributes.FLAG_VALUE_TEXT.getAccessor(type);
			default:
				// FIXME: Return fallback function instead?
				return null;
			}
		}

	};
	private static final String FLAG_VALUE_COL_ID = "value"; //$NON-NLS-1$
	private static final CompositeKeyHistogramBuilder FLAG_HISTOGRAM = new CompositeKeyHistogramBuilder();
	static {
		FLAG_HISTOGRAM.addKeyColumn(JdkAttributes.FLAG_NAME);
		FLAG_HISTOGRAM.addKeyColumn(JdkAttributes.FLAG_ORIGIN);
		FLAG_HISTOGRAM.addKeyColumn(FLAG_VALUE_COL_ID, Messages.GCConfigurationPage_COLUMN_VALUE, FLAG_VALUE_FIELD);
	}
	private IItemFilter perGCFagsFilter;
	private IItemFilter userInputFlagsFilter;
	private SelectionState flagsSelection;

	private class GCInformationUi implements IPageUI {
		private final SashForm flagSash;
		private final ItemHistogram allFlagsTable;
		private final FilterComponent allFlagsFilter;

		public GCInformationUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			SashForm container = new SashForm(form.getBody(), SWT.VERTICAL);

			SashForm gcConfigSash = new SashForm(container, SWT.HORIZONTAL);
			gcConfigSash.setSashWidth(5);
			gcConfigSash.addTraverseListener(new SimpleTraverseListener());

			Section gcConfigSection = CompositeToolkit.createSection(gcConfigSash, toolkit,
					Messages.GCConfigurationPage_SECTION_GC_CONFIG);
			ItemAggregateViewer gcConfig = new ItemAggregateViewer(gcConfigSection, toolkit);
			gcConfig.addAggregate(JdkAggregators.YOUNG_COLLECTOR);
			gcConfig.addAggregate(JdkAggregators.OLD_COLLECTOR);
			gcConfig.addAggregate(JdkAggregators.CONCURRENT_GC_THREAD_COUNT_MIN);
			gcConfig.addAggregate(JdkAggregators.PARALLEL_GC_THREAD_COUNT_MIN);
			gcConfig.addAggregate(JdkAggregators.EXPLICIT_GC_CONCURRENT);
			gcConfig.addAggregate(JdkAggregators.EXPLICIT_GC_DISABLED);
			gcConfig.addAggregate(JdkAggregators.USE_DYNAMIC_GC_THREADS);
			gcConfig.addAggregate(JdkAggregators.GC_TIME_RATIO_MIN);
			gcConfigSection.setClient(gcConfig.getControl());

			Section heapConfigSection = CompositeToolkit.createSection(gcConfigSash, toolkit,
					Messages.GCConfigurationPage_SECTION_HEAP_CONFIG);
			ItemAggregateViewer heapConfig = new ItemAggregateViewer(heapConfigSection, toolkit);
			heapConfig.addAggregate(JdkAggregators.HEAP_CONF_INITIAL_SIZE_MIN);
			heapConfig.addAggregate(JdkAggregators.HEAP_CONF_MIN_SIZE);
			heapConfig.addAggregate(JdkAggregators.HEAP_CONF_MAX_SIZE);
			heapConfig.addAggregate(JdkAggregators.USE_COMPRESSED_OOPS);
			heapConfig.addAggregate(JdkAggregators.COMPRESSED_OOPS_MODE);
			heapConfig.addAggregate(JdkAggregators.HEAP_ADDRESS_SIZE_MIN);
			heapConfig.addAggregate(JdkAggregators.HEAP_OBJECT_ALIGNMENT_MIN);
			heapConfigSection.setClient(heapConfig.getControl());

			Section ycConfigSection = CompositeToolkit.createSection(gcConfigSash, toolkit,
					Messages.GCConfigurationPage_SECTION_YOUNG_CONFIG);
			ItemAggregateViewer ycConfig = new ItemAggregateViewer(ycConfigSection, toolkit);
			ycConfig.addAggregate(JdkAggregators.YOUNG_GENERATION_MIN_SIZE);
			ycConfig.addAggregate(JdkAggregators.YOUNG_GENERATION_MAX_SIZE);
			ycConfig.addAggregate(JdkAggregators.NEW_RATIO_MIN);
			ycConfig.addAggregate(JdkAggregators.TENURING_THRESHOLD_INITIAL_MIN);
			ycConfig.addAggregate(JdkAggregators.TENURING_THRESHOLD_MAX);
			ycConfig.addAggregate(JdkAggregators.USES_TLABS);
			ycConfig.addAggregate(JdkAggregators.TLAB_MIN_SIZE);
			ycConfig.addAggregate(JdkAggregators.TLAB_REFILL_WASTE_LIMIT_MIN);
			ycConfigSection.setClient(ycConfig.getControl());

			gcConfig.setValues(getDataSource().getItems());
			heapConfig.setValues(getDataSource().getItems());
			ycConfig.setValues(getDataSource().getItems());

			flagSash = new SashForm(container, SWT.VERTICAL);
			toolkit.adapt(flagSash);

			perGCFagsFilter = ItemFilters.and(FLAGS_FILTER, GCFlagFilters.collectorFlags(getDataSource().getItems()));

			Section gcFlagsSection = CompositeToolkit.createSection(flagSash, toolkit,
					Messages.GCConfigurationPage_SECTION_JVM_GC_FLAGS);
			allFlagsTable = FLAG_HISTOGRAM.buildWithoutBorder(gcFlagsSection,
					new TableSettings(state.getChild(JVM_GC_FLAGS)));
			allFlagsFilter = FilterComponent.createFilterComponent(allFlagsTable, userInputFlagsFilter,
					getDataSource().getItems().apply(perGCFagsFilter), pageContainer.getSelectionStore()::getSelections,
					this::onFlagsFilterChange);
			MCContextMenuManager flagsMm = MCContextMenuManager
					.create(allFlagsTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(allFlagsTable.getManager(), flagsMm);
			flagsMm.add(allFlagsFilter.getShowFilterAction());
			flagsMm.add(allFlagsFilter.getShowSearchAction());
			gcFlagsSection.setClient(allFlagsFilter.getComponent());

			ColumnViewer flagViewer = allFlagsTable.getManager().getViewer();
			flagViewer.addSelectionChangedListener(
					e -> pageContainer.showSelection(allFlagsTable.getSelection().getItems()));

			PersistableSashForm.loadState(flagSash, state.getChild(GC_FLAG_SASH));
			allFlagsFilter.loadState(getState().getChild(JVM_FLAGS_FILTER));

			allFlagsTable.show(getDataSource().getItems().apply(perGCFagsFilter));
			onFlagsFilterChange(userInputFlagsFilter);
			addResultActions(form);
			allFlagsTable.getManager().setSelectionState(flagsSelection);
		}

		@Override
		public void saveTo(IWritableState memento) {
			allFlagsTable.getManager().getSettings().saveState(memento.createChild(JVM_GC_FLAGS));
			allFlagsFilter.saveState(memento.createChild(JVM_FLAGS_FILTER));
			PersistableSashForm.saveState(flagSash, memento.createChild(GC_FLAG_SASH));

			flagsSelection = allFlagsTable.getManager().getSelectionState();
		}

		private void onFlagsFilterChange(IItemFilter filter) {
			allFlagsFilter.filterChangeHelper(filter, allFlagsTable, getDataSource().getItems().apply(perGCFagsFilter));
			userInputFlagsFilter = filter;
		}
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new GCInformationUi(parent, toolkit, pageContainer, state);
	}

	static class GCFlagFilters {

		private static IItemFilter useGCFlag() {
			return ItemFilters.and(ItemFilters.or(ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseEpsilonGC"), //$NON-NLS-1$
					ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseG1GC"), //$NON-NLS-1$
					ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseZGC"), //$NON-NLS-1$
					ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseShenandoahGC"), //$NON-NLS-1$
					ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseConcMarkSweepGC"), //$NON-NLS-1$
					ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseSerialGC"), //$NON-NLS-1$
					ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseParallelGC") //$NON-NLS-1$
			), ItemFilters.equals(JdkAttributes.FLAG_VALUE_BOOLEAN, true));

		}

		private static IItemFilter collectorFlags(String usedGCFlag) {
			// This may happen for JFR files without GC configuration events, like those of async-profiler
			if (usedGCFlag == null) {
				return ItemFilters.all();
			}

			// Flags like ParallelGCThreads, ConcGCThreads, Xmx, NewRatio are left as this information is contained in the gc configuration event
			// Most flags from https://github.com/openjdk/jdk11u/blob/master/src/hotspot/share/gc/shared/gc_globals.hpp
			// are not added at this time.
			switch (usedGCFlag) {
			case "UseEpsilonGC":
				// https://github.com/openjdk/jdk11u/blob/6c31ac2acdc2b2efa63fe92de8368ab964d847e9/src/hotspot/share/gc/epsilon/epsilon_globals.hpp
				return ItemFilters.or(ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseEpsilonGC"), //$NON-NLS-1$
						ItemFilters.matches(JdkAttributes.FLAG_NAME, "^Epsilon.+")); //$NON-NLS-1$

			case "UseG1GC":
				// https://github.com/openjdk/jdk11u/blob/6c31ac2acdc2b2efa63fe92de8368ab964d847e9/src/hotspot/share/gc/g1/g1_globals.hpp
				return ItemFilters.or(ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseG1GC"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "InitiatingHeapOccupancyPercent"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ClassUnloading"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ClassUnloadingWithConcurrentMark"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseStringDeduplication"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ParallelRefProcEnabled"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "MaxGCPauseMillis"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "GCPauseIntervalMillis"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseLargePages"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseTransparentHugePages"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseNUMA"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "AlwaysPreTouch"), //$NON-NLS-1$
						ItemFilters.matches(JdkAttributes.FLAG_NAME, "^G1.+")); //$NON-NLS-1$

			case "UseZGC":
				//  https://github.com/openjdk/jdk11u/blob/6c31ac2acdc2b2efa63fe92de8368ab964d847e9/src/hotspot/share/gc/z/z_globals.hpp
				return ItemFilters.or(ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseZGC"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ClassUnloading"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ClassUnloadingWithConcurrentMark"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "SoftMaxHeapSize"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseLargePages"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseTransparentHugePages"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseNUMA"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseStringDeduplication"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "SoftRefLRUPolicyMSPerMB"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "AlwaysPreTouch"), //$NON-NLS-1$
						ItemFilters.matches(JdkAttributes.FLAG_NAME, "^Z[A-Z].+")); //$NON-NLS-1$

			case "UseShenandoahGC":
				// from https://github.com/openjdk/jdk11u/blob/6c31ac2acdc2b2efa63fe92de8368ab964d847e9/src/hotspot/share/gc/shenandoah/shenandoah_globals.hpp
				return ItemFilters.or(ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseShenandoahGC"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ClassUnloading"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ClassUnloadingWithConcurrentMark"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "SoftMaxHeapSize"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseStringDeduplication"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseLargePages"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseTransparentHugePages"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseNUMA"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "SoftRefLRUPolicyMSPerMB"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "AlwaysPreTouch"), //$NON-NLS-1$
						ItemFilters.matches(JdkAttributes.FLAG_NAME, "^Shenandoah[A-Z].+")); //$NON-NLS-1$

			case "UseSerialGC":
				// from https://github.com/openjdk/jdk11u/blob/6c31ac2acdc2b2efa63fe92de8368ab964d847e9/src/hotspot/share/gc/serial/serial_globals.hpp
				return ItemFilters.or(ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseSerialGC")); //$NON-NLS-1$

			case "UseParallelGC":
				// from https://github.com/openjdk/jdk11u/blob/6c31ac2acdc2b2efa63fe92de8368ab964d847e9/src/hotspot/share/gc/parallel/parallel_globals.hpp
				return ItemFilters.or(ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseParallelGC"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "HeapMaximumCompactionInterval"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "HeapFirstMaximumCompactionCount"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseMaximumCompactionOnSystemGC"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ParallelOldDeadWoodLimiterMean"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ParallelOldDeadWoodLimiterStdDev"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "GCWorkerDelayMillis"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "PSChunkLargeArrays"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "HeapMaximumCompactionInterval"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseNUMA"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "AlwaysPreTouch"), //$NON-NLS-1$
						ItemFilters.matches(JdkAttributes.FLAG_NAME, "^CMS[A-Z].+")); //$NON-NLS-1$

			case "UseConcMarkSweepGC":
				// from https://github.com/openjdk/jdk11u/blob/6c31ac2acdc2b2efa63fe92de8368ab964d847e9/src/hotspot/share/gc/cms/cms_globals.hpp
				return ItemFilters.or(ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseConcMarkSweepGC"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "UseCMSInitiatingOccupancyOnly"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "BindCMSThreadToCPU"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "CPUForCMSThread"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ParallelRefProcEnabled"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "ScavengeBeforeFullGC"), //$NON-NLS-1$
						ItemFilters.equals(JdkAttributes.FLAG_NAME, "AlwaysPreTouch"), //$NON-NLS-1$
						ItemFilters.matches(JdkAttributes.FLAG_NAME, "^(CMS|FLS|ParGC)[A-Z_].+")); //$NON-NLS-1$

			default:
				return ItemFilters.all();
			}
		}

		public static IItemFilter collectorFlags(IItemCollection items) {
			String usedGC = items.apply(useGCFlag()).values(JdkAttributes.FLAG_NAME).get().distinct().findFirst()
					.orElse("");

			return collectorFlags(usedGC);
		}

	}

	public GCConfigurationPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.or(FLAGS_FILTER, JdkFilters.GC_CONFIG);
	}
}
