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
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
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
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
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
	
	
	
	private static final String JVM_FLAGS = "jvmFlags"; //$NON-NLS-1$
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
	private static final IItemFilter FLAGS_FILTER = ItemFilters.and(ItemFilters.type(FLAGS), ItemFilters.contains(JdkAttributes.FLAG_NAME, "Shenandoah"));
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
		FLAG_HISTOGRAM.addKeyColumn(FLAG_VALUE_COL_ID, Messages.JVMInformationPage_COLUMN_VALUE, FLAG_VALUE_FIELD);
	}
	private SashForm flagSash;
	private ItemHistogram allFlagsTable;
	private IItemFilter flagsFilter;
	private FilterComponent allFlagsFilter;
	private SelectionState flagsSelection;


	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
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

		
		IItemFilter FLAGS_FILTER = ItemFilters.and(ItemFilters.type(FLAGS), ItemFilters.contains(JdkAttributes.FLAG_NAME, "Shenandoah"));
		
		Section allFlagsSection = CompositeToolkit.createSection(flagSash, toolkit,
				Messages.JVMInformationPage_SECTION_JVM_FLAGS);
		allFlagsTable = FLAG_HISTOGRAM.buildWithoutBorder(allFlagsSection,
				new TableSettings(state.getChild(JVM_FLAGS)));
		allFlagsFilter = FilterComponent.createFilterComponent(allFlagsTable, flagsFilter,
				getDataSource().getItems().apply(FLAGS_FILTER), pageContainer.getSelectionStore()::getSelections,
				this::onFlagsFilterChange);
		MCContextMenuManager flagsMm = MCContextMenuManager
				.create(allFlagsTable.getManager().getViewer().getControl());
		ColumnMenusFactory.addDefaultMenus(allFlagsTable.getManager(), flagsMm);
		flagsMm.add(allFlagsFilter.getShowFilterAction());
		flagsMm.add(allFlagsFilter.getShowSearchAction());
		allFlagsSection.setClient(allFlagsFilter.getComponent());

		ColumnViewer flagViewer = allFlagsTable.getManager().getViewer();
		flagViewer.addSelectionChangedListener(
				e -> pageContainer.showSelection(allFlagsTable.getSelection().getItems()));
		
		allFlagsFilter.loadState(getState().getChild(JVM_FLAGS_FILTER));
		
		allFlagsTable.show(getDataSource().getItems().apply(FLAGS_FILTER));
		onFlagsFilterChange(flagsFilter);
		allFlagsTable.getManager().setSelectionState(flagsSelection);
		
		addResultActions(form);

		return null;
	}
	
	private void onFlagsFilterChange(IItemFilter filter) {
		allFlagsFilter.filterChangeHelper(filter, allFlagsTable, getDataSource().getItems().apply(FLAGS_FILTER));
		flagsFilter = filter;
	}


	public GCConfigurationPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.or(FLAGS_FILTER, JdkFilters.GC_CONFIG);
	}
}
