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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.util.CompositeKey;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.ItemIterableToolkit;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.TypeAppearance;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.misc.BackgroundFractionDrawer;
import org.openjdk.jmc.ui.misc.DelegatingLabelProvider;

public class ItemHistogram {

	public static class CompositeKeyHistogramBuilder {

		private final ItemHistogramBuilder histogramBuilder = new ItemHistogramBuilder();
		private final CompositeKeyAccessorFactory factory = new CompositeKeyAccessorFactory();

		public void addKeyColumn(IAttribute<?> attribute) {
			addKeyColumn(ItemList.getColumnId(attribute), attribute.getName(), attribute);
		}

		public void addKeyColumn(String id, String name, IAccessorFactory<?> keyElementProvider) {
			IMemberAccessor<?, CompositeKey> keyElementAccessor = factory.add(keyElementProvider);
			IMemberAccessor<Object, ?> rowTokeyElementAccessor = row -> keyElementAccessor
					.getMember((CompositeKey) AggregationGrid.getKey(row));
			histogramBuilder.columns.add(new ColumnBuilder(name, id, rowTokeyElementAccessor).build());
		}

		public void addCountColumn() {
			histogramBuilder.addCountColumn();
		}

		public void addColumn(String colId, IAggregator<?, ?> a) {
			histogramBuilder.addColumn(colId, a);
		}

		public ItemHistogram build(Composite container, TableSettings tableSettings) {
			return histogramBuilder.build(container, histogramBuilder.columns, factory, tableSettings, SWT.BORDER);
		}

		public ItemHistogram buildWithoutBorder(Composite container, TableSettings tableSettings) {
			return histogramBuilder.build(container, histogramBuilder.columns, factory, tableSettings, SWT.NONE);
		}

	}

	public static class ItemHistogramBuilder {

		private final AggregationGrid grid = new AggregationGrid();
		private final List<IColumn> columns = new ArrayList<>();

		public void addCountColumn() {
			columns.add(new ColumnBuilder(Messages.COUNT_COLUMN_NAME, COUNT_COL_ID, AggregationGrid::getCount)
					.columnDrawer(COUNT_DRAWER).style(SWT.RIGHT).build());
		}

		public void addColumn(String colId, IAggregator<?, ?> a) {
			int style = a.getValueType() instanceof LinearKindOfQuantity ? SWT.RIGHT : SWT.NONE;
			addColumn(colId, ic -> ic.getAggregate(a), a.getName(), a.getDescription(), style);
		}

		public void addColumn(
			String colId, Function<IItemCollection, ?> valueFunction, String name, String description) {
			addColumn(colId, valueFunction, name, description, SWT.NONE);
		}

		public void addColumn(
			String colId, Function<IItemCollection, ?> valueFunction, String name, String description, int style) {
			columns.add(new ColumnBuilder(name, colId, grid.addColumn(valueFunction)).description(description)
					.style(style).build());
		}

		public <T> void addColumn(IAttribute<T> a) {
			// FIXME: Refactor/remove this method to avoid it being used instead of passing an IAggregator.
			// Accessing the thread-group is quite a special case as it is a property of the key (group by attribute).
			// The caller of this method should be responsible for passing a unique column id, as with aggregators.
			IMemberAccessor<Object, T> anyValueAccessor = row -> ItemCollectionToolkit
					.stream(AggregationGrid.getItems(row))
					.flatMap(is -> ItemIterableToolkit.stream(is).map(a.getAccessor(is.getType())::getMember))
					.filter(Objects::nonNull).findAny().orElse(null);
			columns.add(new ColumnBuilder(a.getName(), a.getIdentifier(), anyValueAccessor)
					.description(a.getDescription()).build());
		}

		public <T> ItemHistogram build(Composite container, IAttribute<T> classifier, TableSettings tableSettings) {
			return build(container, classifier.getName(), classifier.getContentType(), classifier, tableSettings);
		}

		public <T> ItemHistogram buildWithoutBorder(
			Composite container, IAttribute<T> classifier, TableSettings tableSettings) {
			return buildWithoutBorder(container, classifier.getName(), classifier.getContentType(), classifier,
					tableSettings);
		}

		public <T> ItemHistogram buildWithoutBorder(
			Composite container, String colLabel, ContentType<? super T> keyType, IAccessorFactory<T> classifier,
			TableSettings tableSettings) {
			return build(container, colLabel, keyType, classifier, tableSettings, SWT.NONE);
		}

		public <T> ItemHistogram build(
			Composite container, String colLabel, ContentType<? super T> keyType, IAccessorFactory<T> classifier,
			TableSettings tableSettings) {
			return build(container, colLabel, keyType, classifier, tableSettings, SWT.BORDER);
		}

		public <T> ItemHistogram build(
			Composite container, String colLabel, ContentType<? super T> keyType, IAccessorFactory<T> classifier,
			TableSettings tableSettings, int border) {
			List<IColumn> columns = new ArrayList<>();
			IMemberAccessor<?, Object> keyAccessor = AggregationGrid::getKey;
			ColumnLabelProvider keyLp = new DelegatingLabelProvider(new KeyLabelProvider(keyType), keyAccessor);
			columns.add(new ColumnBuilder(colLabel, KEY_COL_ID, keyAccessor).labelProvider(keyLp).build());
			columns.addAll(this.columns);
			return build(container, columns, classifier, tableSettings, border);
		}

		private <T> ItemHistogram build(
			Composite container, List<IColumn> columns, IAccessorFactory<T> classifier, TableSettings tableSettings,
			int border) {
			TableViewer tableViewer = new TableViewer(container,
					SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | border);
			tableViewer.setContentProvider(ArrayContentProvider.getInstance());
			ColumnViewerToolTipSupport.enableFor(tableViewer);
			if (UIPlugin.getDefault().getAccessibilityMode()) {
				FocusTracker.enableFocusTracking(tableViewer.getTable());
			}
			return new ItemHistogram(ColumnManager.build(tableViewer, columns, tableSettings), classifier, grid);
		}

	}

	/**
	 * Holds references to each selected row object. May be expensive to keep reference to as the
	 * row objects hold reference too all cell values.
	 */
	public static class HistogramSelection {
		private final List<?> selection;

		private HistogramSelection(List<?> selection) {
			this.selection = selection;
		}

		public int getRowCount() {
			return selection.size();
		}

		public IItemCollection getItems() {
			List<IItemCollection> rows = selection.stream().map(AggregationGrid::getItems).collect(Collectors.toList());
			return ItemCollectionToolkit.merge(rows::stream);
		}

		public <T> Stream<T> getSelectedRows(BiFunction<Object, IItemCollection, T> rowBuilder) {
			return selection.stream()
					.map(row -> rowBuilder.apply(AggregationGrid.getKey(row), AggregationGrid.getItems(row)));
		}

	}

	public static final String KEY_COL_ID = "itemhistogram.key"; //$NON-NLS-1$
	public static final String COUNT_COL_ID = "itemhistogram.count"; //$NON-NLS-1$

	private static final Listener COUNT_DRAWER = BackgroundFractionDrawer.unchecked(AggregationGrid::getCountFraction);
	private final AggregationGrid grid;
	private final ColumnManager columnManager;
	private final IAccessorFactory<?> classifier;

	private ItemHistogram(ColumnManager columnManager, IAccessorFactory<?> classifier, AggregationGrid grid) {
		this.columnManager = columnManager;
		this.grid = grid;
		this.classifier = classifier;
	}

	public ColumnManager getManager() {
		return columnManager;
	}

	public HistogramSelection getAllRows() {
		return new HistogramSelection(Stream.of(((Table) columnManager.getViewer().getControl()).getItems())
				.map(ti -> ti.getData()).collect(Collectors.toList()));
	}

	public HistogramSelection getSelection() {
		return new HistogramSelection(((IStructuredSelection) columnManager.getViewer().getSelection()).toList());
	}

	public void show(IItemCollection items) {
		columnManager.getViewer().setInput(grid.buildRows(ItemCollectionToolkit.stream(items), classifier));
	}

	/*
	 * FIXME: Consider some sharing with ColumnBuilder.DEFAULT_LP.
	 * 
	 * But remember that the context is different and at least affects both getText() and
	 * getToolTipText(), which should be complementary (thus perhaps abstract).
	 */
	static class KeyLabelProvider extends ColumnLabelProvider {

		private final Image image;

		KeyLabelProvider(ContentType<?> ct) {
			image = TypeAppearance.getImage(ct.getIdentifier());
		}

		@Override
		public Font getFont(Object key) {
			return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
		}

		@Override
		public Image getImage(Object key) {
			return image;
		}

		@Override
		public String getText(Object key) {
			if (key instanceof IDisplayable) {
				// Using EXACT here to make close key values user distinguishable. Could change to AUTO.
				return ((IDisplayable) key).displayUsing(IDisplayable.EXACT);
			}
			// Context-insensitive fallback
			return TypeHandling.getValueString(key);
		};

		@Override
		public String getToolTipText(Object key) {
			if (key instanceof IDisplayable) {
				/*
				 * Since VERBOSE often gives the same result as EXACT, one could argue that no
				 * tooltip should be shown in this case. (If the text doesn't fit in the column,
				 * Windows provides a "tooltip" of its own, so at least on Windows we can ignore
				 * that. On other platforms, I am not sure.) If this is desirable, it should be
				 * implemented throughout the application by comparing with getText(). Otherwise, it
				 * may be considered a glitch by users.
				 */
				return ((IDisplayable) key).displayUsing(IDisplayable.VERBOSE);
			}
			return null;
		}
	};

}
