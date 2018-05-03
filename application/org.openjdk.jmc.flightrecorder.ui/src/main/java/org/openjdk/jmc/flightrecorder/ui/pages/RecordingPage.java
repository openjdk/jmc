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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.ColorToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
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
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class RecordingPage extends AbstractDataPage {
	public static class RecordingPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.RecordingPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_RECORDING);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.RECORDING_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new RecordingPage(dpd, items, editor);
		}
	}

	private class RecordingPageUi implements IPageUI {

		private final ItemHistogram concurrentRecordingTable;
		private final ItemHistogram eventSettingsTable;
		private final SashForm sash;
		private IPageContainer pageContainer;
		private ChartCanvas timelineCanvas;
		private SashForm eventSettingsSash;
		private FilterComponent concurrentRecordingFilter;
		private FilterComponent eventSettingsFilter;
		private XYChart timelineChart;

		RecordingPageUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());

			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			Section recInfoSection = CompositeToolkit.createSection(sash, toolkit,
					Messages.RecordingPage_SECTION_RECORDING_INFORMATION);
			ItemAggregateViewer infoViewer = new ItemAggregateViewer(recInfoSection, toolkit, 2);
			// TODO: JMC-5409 - We don't know the start and end of this recording. Improve how the user is informed about this.
			// Start/end/extent values might be in ticks. We limit them to nanoseconds that is better handled in formatting.
			infoViewer.addValueFunction(
					ic -> UnitLookup.EPOCH_NS.quantity(
							pageContainer.getRecordingRange().getStart().clampedLongValueIn(UnitLookup.EPOCH_NS)),
					Messages.RecordingPage_RECORDING_EVENTS_START, Messages.RecordingPage_RECORDING_EVENT_START_DESC);
			infoViewer.addAggregate(JdkAggregators.ITEM_COUNT);
			infoViewer.addValueFunction(
					ic -> UnitLookup.EPOCH_NS
							.quantity(pageContainer.getRecordingRange().getEnd().clampedLongValueIn(UnitLookup.EPOCH_NS)),
					Messages.RecordingPage_RECORDING_EVENTS_END, Messages.RecordingPage_RECORDING_EVENT_END_DESC);
			infoViewer.addAggregate(JdkAggregators.DUMP_REASON);
			infoViewer.addValueFunction(
					ic -> UnitLookup.NANOSECOND.quantity(
							pageContainer.getRecordingRange().getExtent().clampedLongValueIn(UnitLookup.NANOSECOND)),
					Messages.RecordingPage_RECORDING_EVENT_DURATION,
					Messages.RecordingPage_RECORDING_EVENT_DURATION_DESC);
			recInfoSection.setClient(infoViewer.getControl());

			Section s1 = CompositeToolkit.createSection(sash, toolkit,
					Messages.RecordingPage_SECTION_CONCURRENT_RECORDINGS);
			concurrentRecordingTable = DataPageToolkit.createDistinctItemsTable(s1, getDataSource().getItems(),
					JdkQueries.RECORDINGS, new TableSettings(state.getChild(CONCURRENT_RECORDINGS)));
			concurrentRecordingFilter = FilterComponent.createFilterComponent(concurrentRecordingTable,
					recordingsTableFilter, getDataSource().getItems().apply(JdkQueries.RECORDINGS.getFilter()),
					pageContainer.getSelectionStore()::getSelections, this::onRecordingsFilterChange);
			MCContextMenuManager concurrentRecordingMm = MCContextMenuManager
					.create(concurrentRecordingTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(concurrentRecordingTable.getManager(), concurrentRecordingMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(),
					concurrentRecordingTable, Messages.RecordingPage_CONCURRENT_RECORDINGS_SELECTION,
					concurrentRecordingMm);
			concurrentRecordingMm.add(concurrentRecordingFilter.getShowFilterAction());
			concurrentRecordingMm.add(concurrentRecordingFilter.getShowSearchAction());
			ColumnViewer v1 = concurrentRecordingTable.getManager().getViewer();
			v1.addSelectionChangedListener(
					e -> pageContainer.showSelection(concurrentRecordingTable.getSelection().getItems()));
			concurrentRecordingFilter.loadState(state.getChild(CONCURRENT_RECORDINGS_FILTER));
			s1.setClient(concurrentRecordingFilter.getComponent());

			Section s2 = CompositeToolkit.createSection(sash, toolkit, Messages.RecordingPage_SECTION_EVENT_SETTINGS);
			eventSettingsSash = new SashForm(s2, SWT.VERTICAL);
			timelineCanvas = new ChartCanvas(eventSettingsSash);
			toolkit.adapt(timelineCanvas);
			timelineChart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 60, 10);
			timelineChart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			timelineChart.addVisibleRangeListener(r -> timelineRange = r);
			timelineCanvas.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			DataPageToolkit.createChartTimestampTooltip(timelineCanvas);

			eventSettingsTable = DataPageToolkit.createDistinctItemsTable(eventSettingsSash, getDataSource().getItems(),
					JdkQueries.RECORDING_SETTINGS, new TableSettings(state.getChild(EVENT_SETTINGS)));
			eventSettingsFilter = FilterComponent.createFilterComponent(eventSettingsTable, settingsTableFilter,
					getDataSource().getItems().apply(JdkQueries.RECORDING_SETTINGS.getFilter()),
					pageContainer.getSelectionStore()::getSelections, this::onSettingsFilterChange);
			MCContextMenuManager eventSettingsMm = MCContextMenuManager
					.create(eventSettingsTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(eventSettingsTable.getManager(), eventSettingsMm);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), eventSettingsTable,
					Messages.RecordingPage_EVENT_SETTINGS_SELECTION, eventSettingsMm);
			eventSettingsMm.add(eventSettingsFilter.getShowFilterAction());
			eventSettingsMm.add(eventSettingsFilter.getShowSearchAction());
			ColumnViewer v2 = eventSettingsTable.getManager().getViewer();
			v2.addSelectionChangedListener(
					e -> pageContainer.showSelection(eventSettingsTable.getSelection().getItems()));
			eventSettingsFilter.loadState(state.getChild(EVENT_SETTINGS_FILTER));
			s2.setClient(eventSettingsSash);

			PersistableSashForm.loadState(sash, state.getChild(SASH));
			PersistableSashForm.loadState(eventSettingsSash, state.getChild(SASH2));

			IItemCollection items = getDataSource().getItems();
			infoViewer.setValues(items);
			updateRecordingTable(items);
			updateSettingsBarChart(items);
			updateSettingsTable(items);

			addResultActions(form);

			onRecordingsFilterChange(recordingsTableFilter);
			onSettingsFilterChange(settingsTableFilter);

			concurrentRecordingTable.getManager().setSelectionState(recordingsTableSelection);
			eventSettingsTable.getManager().setSelectionState(settingsTableSelection);
		}

		private void updateRecordingTable(IItemCollection items) {
			concurrentRecordingTable.show(items.apply(JdkQueries.RECORDINGS.getFilter()));
		}

		private void updateSettingsBarChart(IItemCollection items) {
			IItemCollection settingsItems = items.apply(JdkQueries.RECORDING_SETTINGS.getFilter());
			ItemRow eventCount = DataPageToolkit.buildTimestampHistogram(Aggregators.count().getName(),
					Aggregators.count().getDescription(), settingsItems, Aggregators.count(),
					ColorToolkit.getDistinguishableColor(JdkTypeIDs.RECORDING_SETTING));
			// FIXME: Would like to have a span chart for the recording event, but the metadata for that is not suitable.
			timelineChart.setRendererRoot(eventCount);
			DataPageToolkit.setChart(timelineCanvas, timelineChart, pageContainer::showSelection,
					this::onChartRangeSelection);
		}

		private void onChartRangeSelection(IRange<IQuantity> range) {
			range = range != null ? range : pageContainer.getRecordingRange();
			// FIXME: Do we want to use the timerange from the chart, or the actually selected items?
			IItemCollection itemsInRange = getDataSource().getItems(range);
			updateSettingsTable(itemsInRange);
		}

		private void updateSettingsTable(IItemCollection items) {
			eventSettingsTable.show(items.apply(JdkQueries.RECORDING_SETTINGS.getFilter()));
		}

		private void onRecordingsFilterChange(IItemFilter filter) {
			concurrentRecordingFilter.filterChangeHelper(filter, concurrentRecordingTable,
					getDataSource().getItems().apply(JdkQueries.RECORDINGS.getFilter()));
			recordingsTableFilter = filter;
		}

		private void onSettingsFilterChange(IItemFilter filter) {
			eventSettingsFilter.filterChangeHelper(filter, eventSettingsTable,
					getDataSource().getItems().apply(JdkQueries.RECORDING_SETTINGS.getFilter()));
			settingsTableFilter = filter;
		}

		@Override
		public void saveTo(IWritableState memento) {
			PersistableSashForm.saveState(sash, memento.createChild(SASH));
			PersistableSashForm.saveState(eventSettingsSash, memento.createChild(SASH2));
			concurrentRecordingTable.getManager().getSettings().saveState(memento.createChild(CONCURRENT_RECORDINGS));
			eventSettingsTable.getManager().getSettings().saveState(memento.createChild(EVENT_SETTINGS));
			concurrentRecordingFilter.saveState(memento.createChild(CONCURRENT_RECORDINGS_FILTER));
			eventSettingsFilter.saveState(memento.createChild(EVENT_SETTINGS_FILTER));

			saveToLocal();
		}

		private void saveToLocal() {
			recordingsTableSelection = concurrentRecordingTable.getManager().getSelectionState();
			settingsTableSelection = eventSettingsTable.getManager().getSelectionState();
		}

	}

	private static final String SASH = "sash"; //$NON-NLS-1$
	private static final String SASH2 = "eventSettingsSash"; //$NON-NLS-1$
	private static final String EVENT_SETTINGS = "eventSettings"; //$NON-NLS-1$
	private static final String CONCURRENT_RECORDINGS = "concurrentRecordings"; //$NON-NLS-1$
	private static final String EVENT_SETTINGS_FILTER = "eventSettingsFilter"; //$NON-NLS-1$
	private static final String CONCURRENT_RECORDINGS_FILTER = "concurrentRecordingsFilter"; //$NON-NLS-1$

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new RecordingPageUi(parent, toolkit, pageContainer, state);
	}

	private IItemFilter recordingsTableFilter;
	private IItemFilter settingsTableFilter;
	private SelectionState recordingsTableSelection;
	private SelectionState settingsTableSelection;
	private IRange<IQuantity> timelineRange;

	public RecordingPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		timelineRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.or(JdkFilters.RECORDING_SETTING, JdkFilters.RECORDINGS);
	}
}
