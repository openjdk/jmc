/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.common.item.Aggregators.AggregatorBase;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.common.DurationHdrHistogram.DurationItemConsumer;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.misc.BackgroundFractionDrawer;
import org.openjdk.jmc.ui.misc.DelegatingLabelProvider;
import org.openjdk.jmc.ui.misc.OptimisticComparator;

/**
 * A table containing Flight Recorder event durations at various pre-defined percentiles.
 * Each row in the table contains values for a different percentile, and the columns contain
 * series of durations and event counts.
 *
 * @see DurationPercentileTableBuilder
 */
public class DurationPercentileTable {

	public static final String TABLE_NAME = "DurationPercentileTable"; //$NON-NLS-1$
	private static final String COL_ID_PERCENTILE = TABLE_NAME + ".percentile"; //$NON-NLS-1$

	private static final IQuantity[] PERCENTILES = {
			UnitLookup.NUMBER_UNITY.quantity(0.0),
			UnitLookup.NUMBER_UNITY.quantity(90.0),
			UnitLookup.NUMBER_UNITY.quantity(99.0),
			UnitLookup.NUMBER_UNITY.quantity(99.9),
			UnitLookup.NUMBER_UNITY.quantity(99.99),
			UnitLookup.NUMBER_UNITY.quantity(99.999),
			UnitLookup.NUMBER_UNITY.quantity(100.0),
	};

	private final DurationPercentileAggregator[] aggregators; // Correspond to column order
	private final ColumnManager manager;

	private DurationPercentileTable(ColumnManager manager, DurationPercentileAggregator[] aggregators) {
		this.manager = manager;
		this.aggregators = aggregators;
	}

	/**
	 * Builder class that is the sole means of creating {@link DurationPercentileTable} instances.
	 */
	public static class DurationPercentileTableBuilder {

		private final List<IColumn> columns;
		private final List<DurationPercentileAggregator> aggregators;

		public DurationPercentileTableBuilder() {
			this.columns = new ArrayList<>();
			this.aggregators = new ArrayList<>();
		}

		/**
		 * Adds a data series to this table, corresponding to an event type with a duration
		 * associated with it. Calling this method adds two columns to the resulting table.
		 * The first column contains duration values for the event at different percentiles,
		 * and the second column contains the number of events with duration <= the duration
		 * at that percentile.
		 *
		 * @param durationColId - the ID to be used for the duration column of this series
		 * @param durationColName - the user-visible name to appear for the duration column header
		 * @param countColId - the ID to be used for the event count column of this series
		 * @param countColName - the user-visible name to appear for the event count column header
		 * @param typeId - the event type ID used to match events belonging to this series
		 */
		public void addSeries(String durationColId, String durationColName,
				String countColId, String countColName, String typeId) {
			IColumn column = new ColumnBuilder(durationColName, durationColId, new ValueAccessor(durationColId)).style(SWT.RIGHT).build();
			columns.add(column);

			Function<DurationPercentileTableRow, IQuantity> fractionFunc = row -> row.getCountFraction(countColId);
			column = new ColumnBuilder(countColName, countColId, new ValueAccessor(countColId)).style(SWT.RIGHT)
					.columnDrawer(BackgroundFractionDrawer.unchecked(fractionFunc)).build();
			columns.add(column);

			DurationPercentileAggregator aggregator = new DurationPercentileAggregator(typeId, durationColId, countColId);
			aggregators.add(aggregator);
		}

		/**
		 * Builds the {@link DurationPercentileTable} after all series have been added.
		 * Calling this method results in the creation of the underlying {@link TableViewer}.
		 * Further changes to this builder will not affect the returned table.
		 * @param parent - the parent SWT composite that will contain this table
		 * @param ts - settings to adjust various attributes of the created table
		 * @return a fully constructed {@link DurationPercentileTable} with no data
		 */
		public DurationPercentileTable build(Composite parent, TableSettings ts) {
			TableViewer tableViewer = new TableViewer(parent,
					SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
			tableViewer.getControl().setData("name", TABLE_NAME); //$NON-NLS-1$
			tableViewer.setContentProvider(ArrayContentProvider.getInstance());
			ColumnViewerToolTipSupport.enableFor(tableViewer);
			if (UIPlugin.getDefault().getAccessibilityMode()) {
				FocusTracker.enableFocusTracking(tableViewer.getTable());
			}

			List<IColumn> columns = new ArrayList<>();
			ItemHistogram.KeyLabelProvider keyLP = new ItemHistogram.KeyLabelProvider(UnitLookup.NUMBER);
			PercentileAccessor cellAccessor = new PercentileAccessor();
			OptimisticComparator comp = new OptimisticComparator(cellAccessor, keyLP);
			columns.add(new ColumnBuilder(Messages.DurationPercentileTable_PERCENTILE_COL_NAME, COL_ID_PERCENTILE,
					new DelegatingLabelProvider(keyLP, cellAccessor)).comparator(comp).build());
			columns.addAll(this.columns);

			ColumnManager manager = ColumnManager.build(tableViewer, columns, ts);
			DurationPercentileAggregator[] aggregatorsCopy = aggregators.toArray(new DurationPercentileAggregator[aggregators.size()]);
			return new DurationPercentileTable(manager, aggregatorsCopy);
		}
	}

	/**
	 * Updates the data in this table with events from the item collection.
	 * Calling this method stores the input data into a histogram, which is then
	 * used to generate duration values at various percentiles.
	 *
	 * @param itemCol - a collection of events to use as input for this table
	 */
	public void update(IItemCollection itemCol) {
		// Add the value of each aggregate to our data model
		DurationPercentileTableModel model = new DurationPercentileTableModel(itemCol);
		Arrays.stream(aggregators).parallel().forEach(model::addAggregate);

		// Build rows for each percentile and set in the table
		List<DurationPercentileTableRow> rows = model.buildRows();
		updateColumnVisibilty(rows.get(0));
		manager.getViewer().setInput(rows);
	}

	private void updateColumnVisibilty(DurationPercentileTableRow row) {
		manager.getColumnStates().forEach(c -> {
			String id = c.getColumn().getId();
			if (!COL_ID_PERCENTILE.equals(id)) { // Percentile column always shown
				boolean shouldShow = row.hasValue(id);
				// Don't show if already shown, will duplicate column
				if (shouldShow != c.isVisible()) {
					manager.setColumnHidden(id, !shouldShow);
				}
			}
		});
	}

	/**
	 * Get the {@link ColumnManager} responsible for the underlying {@link TableViewer}.
	 * @return the manager
	 */
	public ColumnManager getManager() {
		return manager;
	}

	/**
	 * Gets a collection of items whose duration is at least as long as the percentile value
	 * in the currently selected row.
	 * @return the collection of matching items
	 */
	public IItemCollection getSelectedItems() {
		IStructuredSelection selection = manager.getViewer().getStructuredSelection();
		Object firstSelection = selection.getFirstElement();
		if (firstSelection instanceof DurationPercentileTableRow) {
			DurationPercentileTableRow row = (DurationPercentileTableRow) firstSelection;
			return row.getItemsForRow(aggregators);
		}
		return null;
	}

	private static class PercentileAccessor implements IMemberAccessor<IQuantity, Object> {

		@Override
		public IQuantity getMember(Object inObject) {
			if (inObject instanceof DurationPercentileTableRow) {
				return ((DurationPercentileTableRow) inObject).getPercentile();
			}
			return null;
		}

	}

	private static class ValueAccessor implements IMemberAccessor<IQuantity, DurationPercentileTableRow> {

		private final String columnId;

		public ValueAccessor(String columnId) {
			this.columnId = columnId;
		}

		@Override
		public IQuantity getMember(DurationPercentileTableRow inObject) {
			return inObject.getValue(columnId);
		}

	}

	/**
	 * Aggregator that inserts event durations into a histogram.
	 */
	private static class DurationPercentileAggregator extends AggregatorBase<Map<IQuantity, Map<String, IQuantity>>, DurationItemConsumer> {

		private final DurationHdrHistogram histogram;
		private final String typeId;
		private final String durationColId;
		private final String countColId;

		/**
		 * Creates a new aggregator.
		 * @param typeId - type ID used to match events
		 * @param durationColId - the column ID for the duration column of this series
		 * @param countColId - the column ID for the item count column of this series
		 */
		public DurationPercentileAggregator(String typeId, String durationColId, String countColId) {
			super(null, null, UnitLookup.UNKNOWN);
			this.histogram = new DurationHdrHistogram();
			this.typeId = typeId;
			this.durationColId = durationColId;
			this.countColId = countColId;
		}

		@Override
		public boolean acceptType(IType<IItem> type) {
			return typeId.equals(type.getIdentifier());
		}

		@Override
		public DurationItemConsumer newItemConsumer(IType<IItem> itemType) {
			return new DurationItemConsumer(histogram, JfrAttributes.DURATION.getAccessor(itemType));
		}

		@Override
		public Map<IQuantity, Map<String, IQuantity>> getValue(Iterator<DurationItemConsumer> source) {
			while (source.hasNext()) {
				source.next();
			}

			Map<IQuantity, Map<String, IQuantity>> result = new HashMap<>();
			for (IQuantity percentile : PERCENTILES) {
				Map<String, IQuantity> colValues = new HashMap<>();
				// Only add columns to model if there is data to show
				if (!histogram.isEmpty()) {
					Pair<IQuantity, IQuantity> data = histogram.getDurationAndCountAtPercentile(percentile);

					colValues.put(durationColId, data.left);
					colValues.put(countColId, data.right);
				}
				result.put(percentile, colValues);
			}
			return result;
		}

		/**
		 * @return the number of items stored in this aggregator's histogram
		 */
		public IQuantity getItemCount() {
			long total = histogram.getTotalCount();
			return UnitLookup.NUMBER_UNITY.quantity(total);
		}

		/**
		 * @return the ID for the duration column using this aggregator
		 */
		public String getDurationColId() {
			return durationColId;
		}

		/**
		 * @return the ID for the item count column using this aggregator
		 */
		public String getCountColId() {
			return countColId;
		}

		/**
		 * @return the type ID used to match items accepted by this aggregator
		 */
		public String getTypeId() {
			return typeId;
		}

		/**
		 * @param duration - a {@link UnitLookup#TIMESPAN} quantity
		 * @return a lower bound on values considered equivalent by this
		 * aggregator's underlying histogram
		 */
		public IQuantity getLowestEquivalentDuration(IQuantity duration) {
			return histogram.getLowestEquivalentDuration(duration);
		}

		/**
		 * Resets this aggregator's histogram to its initial state
		 */
		public void resetHistogram() {
			histogram.reset();
		}

	}

	/**
	 * A data model representing the content to be displayed in the {@link DurationPercentileTable}.
	 */
	private static class DurationPercentileTableModel {

		private final IItemCollection items;
		private final Map<IQuantity, Map<String, IQuantity>> valuesByPercentile;
		private final Map<String, IQuantity> itemTotals;

		public DurationPercentileTableModel(IItemCollection items) {
			this.items = items;
			this.valuesByPercentile = new ConcurrentHashMap<>();
			this.itemTotals = new ConcurrentHashMap<>();
		}

		/**
		 * Computes the aggregate of this model's items and adds the results to this model.
		 * @param aggregator - the aggregator to use
		 */
		public void addAggregate(DurationPercentileAggregator aggregator) {
			aggregator.resetHistogram();

			Map<IQuantity, Map<String, IQuantity>> newData = items.getAggregate(aggregator);
			addData(newData);

			String countCol = aggregator.getCountColId();
			IQuantity itemCount = aggregator.getItemCount();
			itemTotals.put(countCol, itemCount);
		}

		private void addData(Map<IQuantity, Map<String, IQuantity>> newValues) {
			newValues.forEach((key, val) -> valuesByPercentile.merge(key, val, (oldVal, newVal) -> {
				oldVal.putAll(newVal);
				return oldVal;
			}));
		}

		/**
		 * Builds a list of table rows from the data in this model, suitable as input
		 * to the {@link DurationPercentileTable}'s {@link ColumnViewer}.
		 * @return the list of rows
		 */
		public List<DurationPercentileTableRow> buildRows() {
			List<DurationPercentileTableRow> rows = new ArrayList<>();
			for (IQuantity percentile : PERCENTILES) {
				DurationPercentileTableRow row = new DurationPercentileTableRow(percentile,
						valuesByPercentile.get(percentile), itemTotals, items);
				rows.add(row);
			}
			return rows;
		}

	}

	/**
	 * Roughly equivalent to a row in the table, containing the percentile and list of
	 * associated quantities in column order.
	 */
	private static class DurationPercentileTableRow {

		private final IQuantity percentile;
		private final Map<String, IQuantity> valuesByColId;
		private final Map<String, IQuantity> totalsById;
		private final IItemCollection items;

		public DurationPercentileTableRow(IQuantity percentile, Map<String, IQuantity> values,
				Map<String, IQuantity> totals, IItemCollection items) {
			this.percentile = percentile;
			this.valuesByColId = values;
			this.totalsById = totals;
			this.items = items;
		}

		public IQuantity getPercentile() {
			return percentile;
		}

		public IQuantity getValue(String columnId) {
			return valuesByColId.get(columnId);
		}

		public boolean hasValue(String columnId) {
			return valuesByColId.containsKey(columnId);
		}

		/**
		 * Calculates the fraction of items in this row, compared to the total
		 * number of items in the series.
		 * @param columnId - the ID of the item count column
		 * @return a fraction quantity between 0 and 1
		 */
		public IQuantity getCountFraction(String columnId) {
			IQuantity count = valuesByColId.get(columnId);
			IQuantity total = totalsById.get(columnId);
			double fraction = 0.0;
			if (count != null && total != null && total.longValue() > 0) {
				fraction = count.doubleValue() / total.doubleValue();
			}
			return UnitLookup.NUMBER_UNITY.quantity(fraction);
		}

		/**
		 * Computes the collection of items that have duration at least as long as the
		 * corresponding values in this row.
		 * @param aggregators - an array of aggregators that produced the content of this row
		 * @return the matching items
		 */
		public IItemCollection getItemsForRow(DurationPercentileAggregator[] aggregators) {
			// Select all events with matching Type ID and duration greater or equal to the value
			// for the selected percentile in the histogram, subject to the histogram's precision.
			IItemFilter filter = Arrays.stream(aggregators).parallel().filter(a -> hasValue(a.getDurationColId()))
					.map(a -> ItemFilters.and(ItemFilters.type(a.getTypeId()),
							ItemFilters.moreOrEqual(JfrAttributes.DURATION,
									a.getLowestEquivalentDuration(getValue(a.getDurationColId())))))
					.reduce(ItemFilters::or).orElse(ItemFilters.none());
			return items.apply(filter);
		}

	}

}
