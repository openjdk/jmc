/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
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
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemRow;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.charts.IXDataRenderer;
import org.openjdk.jmc.ui.charts.RendererToolkit;
import org.openjdk.jmc.ui.charts.XYChart;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.PersistableSashForm;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

public class SecurityPage extends AbstractDataPage {

	public static class SecurityPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.SecurityPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_SECURITY);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.SECURITY};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition definition, StreamModel items, IPageContainer editor) {
			return new SecurityPage(definition, items, editor);
		}

	}

	private static ColumnLabelProvider LEGEND_LP = new ColumnLabelProvider() {

		@Override
		public String getText(Object element) {
			return getText(element, IDescribable::getName);
		}

		@Override
		public String getToolTipText(Object element) {
			return getText(element, IDescribable::getDescription);
		};

		private String getText(Object element, Function<IDescribable, String> accessor) {
			return accessor.apply(JdkAggregators.X509_CERTIFICATE_COUNT);
		};

		@Override
		public Image getImage(Object element) {
			return SWTColorToolkit
					.getColorThumbnail(SWTColorToolkit.asRGB(DataPageToolkit.getFieldColor((String) element)));

		};
	};

	private static ColumnLabelProvider ICON_LP = new ColumnLabelProvider() {
		@Override
		public String getText(Object element) {
			return ""; // Return empty string for image-only column
		}

		@SuppressWarnings("unchecked")
		@Override
		public Image getImage(Object element) {
			if (element instanceof IItem) {
				IItem iItem = (IItem) element;
				IMemberAccessor<? extends String, IItem> iconAccessor = (IMemberAccessor<? extends String, IItem>) JdkAttributes.CRYPTO_ICON
						.getAccessor(iItem.getType());

				ImageDescriptor descriptor;
				if (iconAccessor != null) {
					String iconText = iconAccessor.getMember(iItem);
					if (iconText.contains("ACTION"))
						descriptor = UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_CRYPTO_ACTION);
					else if (iconText.contains("ATTENTION"))
						descriptor = UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_CRYPTO_ATTENTION);
					else
						descriptor = UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_CRYPTO_OK);

					if (descriptor != null) {
						return descriptor.createImage();
					}
				}
			}
			return null;
		}
	};

	private static final ItemListBuilder SECURITY_X509_ALGORITHM_LIST = new ItemListBuilder();

	static {

		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.CRYPTO_ICON, ICON_LP);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.CRYPTO_REMARK);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.SIGNATURE_ALGORITHM);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.KEY_TYPE);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.KEY_LENGTH);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.SERIAL_NUMBER);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.SUBJECT);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.VALID_FROM);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.VALID_UNTIL);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JfrAttributes.EVENT_THREAD);
		SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.CERTIFICATE_ISSUER);

	}

	private static final String SASH = "sash"; //$NON-NLS-1$
	private static final String SECURITY_X509_ALGORITHM_TABLE = "securityX509AlgorithmTable"; //$NON-NLS-1$
	private static final String SECURITY_X509_ALGORITHM_FILTER = "securityX509AlgorithmFilter"; //$NON-NLS-1$
	private static final String CHART = "chart"; //$NON-NLS-1$
	private static final String SERIES = "series"; //$NON-NLS-1$
	private static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	private static final String X509_CERTIFICATE_COUNT = "x509CertificateCount"; //$NON-NLS-1$

	private class SecurityPageUI implements IPageUI {

		private final SashForm sash;
		private final IPageContainer pageContainer;
		private final CheckboxTableViewer chartLegend;
		private final ChartCanvas chartCanvas;
		private final ItemList securityX509AlgorithmTable;
		private final FilterComponent securityX509AlgorithmFilter;
		private IItemCollection selectionItems;
		private XYChart chart;
		private IRange<IQuantity> currentRange;
		private FlavorSelector flavorSelector;

		SecurityPageUI(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);

			Boolean isCertificateIdQty = getDataSource().getItems().getAggregate(JdkAggregators.IS_CERTIFICATE_ID_QTY);

			if (isCertificateIdQty)
				SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.CERTIFICATE_ID_QTY);
			else
				SECURITY_X509_ALGORITHM_LIST.addColumn(JdkAttributes.CERTIFICATE_ID);

			//Algorithm table
			Section s2 = CompositeToolkit.createSection(sash, toolkit, Messages.SecurityPage_SECTION_X509_ALGORITHMS);
			securityX509AlgorithmTable = SECURITY_X509_ALGORITHM_LIST.buildWithoutBorder(s2,
					TableSettings.forState(state.getChild(SECURITY_X509_ALGORITHM_TABLE)));

			securityX509AlgorithmTable.getManager().getViewer().addSelectionChangedListener(e -> pageContainer
					.showSelection(ItemCollectionToolkit.build(securityX509AlgorithmTable.getSelection().get())));
			securityX509AlgorithmFilter = FilterComponent.createFilterComponent(securityX509AlgorithmTable,
					securityX509AlgorithmFilterState, getDataSource().getItems().apply(JdkFilters.SECURITY),
					pageContainer.getSelectionStore()::getSelections, this::onAlgorithmFilterChange);
			MCContextMenuManager mm1 = MCContextMenuManager
					.create(securityX509AlgorithmTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(securityX509AlgorithmTable.getManager(), mm1);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(),
					securityX509AlgorithmTable, Messages.SecurityPage_TABLE_SELECTION, mm1);
			mm1.add(securityX509AlgorithmFilter.getShowFilterAction());
			mm1.add(securityX509AlgorithmFilter.getShowSearchAction());
			securityX509AlgorithmFilter.loadState(state.getChild(SECURITY_X509_ALGORITHM_FILTER));
			s2.setClient(securityX509AlgorithmFilter.getComponent());

			PersistableSashForm.loadState(sash, state.getChild(SASH));
			flavorSelector = FlavorSelector.itemsWithTimerange(form, JdkFilters.SECURITY, getDataSource().getItems(),
					pageContainer, this::onInputSelected, this::onShow, flavorSelectorState);
			addResultActions(form);
			onAlgorithmFilterChange(securityX509AlgorithmFilterState);
			securityX509AlgorithmTable.getManager().setSelectionState(securityX509AlgorithmSelectionState);

			//Create Chart
			Composite chartContainer = toolkit.createComposite(sash);
			chartContainer.setLayout(new GridLayout(2, false));
			chartCanvas = new ChartCanvas(chartContainer);
			chart = new XYChart(pageContainer.getRecordingRange(), RendererToolkit.empty(), 120);
			chart.setVisibleRange(timelineRange.getStart(), timelineRange.getEnd());
			chart.addVisibleRangeListener(r -> timelineRange = r);
			chartCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			DataPageToolkit.setChart(chartCanvas, chart, pageContainer::showSelection);
			SelectionStoreActionToolkit.addSelectionStoreRangeActions(pageContainer.getSelectionStore(), chart,
					JfrAttributes.LIFETIME, Messages.SecurityPage_TIMELINE_SELECTION, chartCanvas.getContextMenu());
			DataPageToolkit.createChartTimestampTooltip(chartCanvas);

			chartLegend = CheckboxTableViewer.newCheckList(chartContainer, SWT.BORDER);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
			gd.widthHint = 120;
			chartLegend.getTable().setLayoutData(gd);
			chartLegend.setContentProvider(ArrayContentProvider.getInstance());
			chartLegend.setLabelProvider(LEGEND_LP);
			chartLegend.addCheckStateListener(e -> buildChart());
			chartLegend.addSelectionChangedListener(e -> buildChart());
			ColumnViewerToolTipSupport.enableFor(chartLegend);
			List<Object> chartSeries = new ArrayList<>();
			chartSeries.addAll(Arrays.asList(X509_CERTIFICATE_COUNT));
			chartLegend.setInput(chartSeries.toArray());
			IState chartState = state.getChild(CHART);
			if (chartState != null) {
				for (IState c : chartState.getChildren()) {
					chartLegend.setChecked(c.getAttribute(ID_ATTRIBUTE), true);
				}
			}

			onShow(true);
		}

		private Optional<ItemRow> buildBarChart(
			IItemCollection items, IAggregator<IQuantity, ?> aggregator, String id) {
			if (chartLegend.getChecked(id)) {
				return Optional.of(DataPageToolkit.buildTimestampHistogram(aggregator.getName(),
						aggregator.getDescription(), items, aggregator, DataPageToolkit.getFieldColor(id)));
			}
			return Optional.empty();
		}

		private void buildChart() {
			IItemCollection itemsInRange = getItems();
			List<IXDataRenderer> rows = new ArrayList<>();
			IItemCollection securityEvents = itemsInRange.apply(JdkFilters.SECURITY);
			buildBarChart(securityEvents, JdkAggregators.X509_CERTIFICATE_COUNT, X509_CERTIFICATE_COUNT)
					.ifPresent(rows::add);

			IXDataRenderer root = RendererToolkit.uniformRows(rows);
			chartCanvas.replaceRenderer(root);
		}

		private void onShow(Boolean show) {
			IRange<IQuantity> range = show ? currentRange : pageContainer.getRecordingRange();
			chart.setVisibleRange(range.getStart(), range.getEnd());
			if (chartLegend != null)
				buildChart();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			this.currentRange = timeRange;
			selectionItems = items;
			securityX509AlgorithmTable.show(getItems().apply(JdkFilters.SECURITY));
			if (chartLegend != null)
				buildChart();
		}

		private IItemCollection getItems() {
			return selectionItems != null ? selectionItems.apply(JdkFilters.SECURITY)
					: getDataSource().getItems().apply(JdkFilters.SECURITY);
		}

		private void onAlgorithmFilterChange(IItemFilter filter) {
			securityX509AlgorithmFilter.filterChangeHelper(filter, securityX509AlgorithmTable,
					getDataSource().getItems().apply(JdkFilters.SECURITY));
			securityX509AlgorithmFilterState = filter;
			if (chartLegend != null)
				buildChart();
		}

		@Override
		public void saveTo(IWritableState state) {
			PersistableSashForm.saveState(sash, state.createChild(SASH));
			IWritableState chartState = state.createChild(CHART);
			securityX509AlgorithmTable.getManager().getSettings()
					.saveState(state.createChild(SECURITY_X509_ALGORITHM_TABLE));
			securityX509AlgorithmFilter.saveState(state.createChild(SECURITY_X509_ALGORITHM_FILTER));
			for (Object o : chartLegend.getCheckedElements()) {
				chartState.createChild(SERIES).putString(ID_ATTRIBUTE, ((String) o));
			}

			saveToLocal();
		}

		private void saveToLocal() {
			securityX509AlgorithmSelectionState = securityX509AlgorithmTable.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}
	}

	private IRange<IQuantity> timelineRange;
	private FlavorSelectorState flavorSelectorState;
	private IItemFilter securityX509AlgorithmFilterState;
	private SelectionState securityX509AlgorithmSelectionState;

	public SecurityPage(IPageDefinition defintion, StreamModel items, IPageContainer editor) {
		super(defintion, items, editor);
		timelineRange = editor.getRecordingRange();
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.type(JdkTypeIDs.X509_CERTIFICATE);
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new SecurityPageUI(parent, toolkit, editor, state);
	}

}
