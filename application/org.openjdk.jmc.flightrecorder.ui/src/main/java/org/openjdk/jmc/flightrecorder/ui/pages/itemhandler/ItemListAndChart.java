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
package org.openjdk.jmc.flightrecorder.ui.pages.itemhandler;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

class ItemListAndChart {

	private static final String LIST_SETTINGS = "listSettings"; //$NON-NLS-1$
	private static final String CHART_SETTINGS = "chartSettings"; //$NON-NLS-1$
	private static final String SELECTED_SUBTAB = "selectedSubTab"; //$NON-NLS-1$
	private static final String ITEM_LIST_FILTER = "itemListFilter"; //$NON-NLS-1$
	private ItemList itemList;
	private final ItemChart itemChart;
	private MCContextMenuManager listContextMenuManager;
	private final CTabFolder tabFolder;
	private final Consumer<IItemCollection> selectionListener;
	private final CTabItem listTab;
	private final IPageContainer controller;
	private String pageName;
	private ToolBarManager toolBarManager;
	private FilterComponent itemListFilterComponent;
	private StreamModel streamModel;
	private FormToolkit toolkit;
	private IItemFilter inputFilter;
	private IItemFilter itemListFilter;

	private IItemCollection listItems;

	public ItemListAndChart(FormToolkit toolkit, IPageContainer controller, StreamModel streamModel, IItemFilter filter,
			IItemFilter itemListFilter, String pageName, Composite parent, IState state,
			AttributeComponentConfiguration acc, Consumer<IItemCollection> selectionListener) {

		this.toolkit = toolkit;
		this.controller = controller;
		this.streamModel = streamModel;
		this.inputFilter = filter;
		this.itemListFilter = itemListFilter;
		this.pageName = pageName;
		this.selectionListener = selectionListener;
		tabFolder = new CTabFolder(parent, SWT.NONE);
		toolkit.adapt(tabFolder);
		toolBarManager = new ToolBarManager(SWT.HORIZONTAL);
		tabFolder.setTopRight(toolBarManager.createControl(tabFolder));

		listTab = new CTabItem(tabFolder, SWT.NONE);
		listTab.setText(Messages.ITEMHANDLER_LIST_TITLE);
		listTab.setImage(FlightRecorderUI.getDefault().getImage(ImageConstants.ICON_TABLE));
		listTab.setToolTipText(Messages.ITEMHANDLER_LIST_DESCRIPTION);
		buildList(state, acc);

		CTabItem chartTab = new CTabItem(tabFolder, SWT.NONE);
		chartTab.setText(Messages.ITEMHANDLER_CHART_TITLE);
		chartTab.setImage(FlightRecorderUI.getDefault().getImage(ImageConstants.ICON_CHART_BAR));
		chartTab.setToolTipText(Messages.ITEMHANDLER_CHART_DESCRIPTION);
		itemChart = new ItemChart(tabFolder, toolkit, pageName, acc,
				state != null ? state.getChild(CHART_SETTINGS) : null, controller);
		chartTab.setControl(itemChart.getControl());

		tabFolder.setSelection(StateToolkit.readInt(state, SELECTED_SUBTAB, 0));
	}

	private void buildList(IState state, AttributeComponentConfiguration acc) {
		listItems = streamModel.getItems().apply(inputFilter);

		ItemListBuilder itemListBuilder = new ItemListBuilder();

		acc.getAllAttributes().entrySet().forEach(entry -> {
			String combinedId = entry.getKey();
			IAttribute<?> a = entry.getValue();
			ContentType<?> contentType = a.getContentType();
			IMemberAccessor<?, IItem> accessor = ItemToolkit.accessor(a);
			itemListBuilder.addColumn(combinedId, a.getName(), a.getDescription(),
					contentType instanceof LinearKindOfQuantity, accessor);

		});

		// FIXME: Should we use the state here, if the columns have been updated?
		// FIXME: Should we change the column state if the user explicitly has configured the columns?
		final TableSettings itemListSettings = state == null
				? DataPageToolkit.createTableSettingsByAllAndVisibleColumns(acc.getAllAttributes().keySet(),
						acc.getCommonAttributes().keySet())
				: TableSettings.forStateAndColumns(state.getChild(LIST_SETTINGS), acc.getAllAttributes().keySet(),
						acc.getCommonAttributes().keySet());

		Composite listComposite = toolkit.createComposite(tabFolder);
		listComposite.setLayout(GridLayoutFactory.swtDefaults().create());
		itemList = itemListBuilder.buildWithoutBorder(listComposite, itemListSettings);
		listTab.setControl(listComposite);
		itemList.getManager().getViewer()
				.addSelectionChangedListener(e -> selectionListener.accept(getListSelection()));
		ColumnManager columnsManager = itemList.getManager();
		listContextMenuManager = MCContextMenuManager.create(columnsManager.getViewer().getControl());

		ColumnMenusFactory.addDefaultMenus(columnsManager, listContextMenuManager);
		SelectionStoreActionToolkit.addSelectionStoreActions(controller.getSelectionStore(), itemList,
				NLS.bind(Messages.ITEMHANDLER_LOG_SELECTION, pageName), listContextMenuManager);

		itemListFilterComponent = FilterComponent.createFilterComponent(itemList, itemListFilter, listItems,
				controller.getSelectionStore()::getSelections, this::onFilterChange);
		listContextMenuManager.add(itemListFilterComponent.getShowFilterAction());
		listContextMenuManager.add(itemListFilterComponent.getShowSearchAction());
		if (state != null) {
			itemListFilterComponent.loadState(state.getChild(ITEM_LIST_FILTER));
		}
		onFilterChange(itemListFilter);
	}

	private void onFilterChange(IItemFilter itemListFilter) {
		this.itemListFilter = itemListFilter;
		itemListFilterComponent.filterChangeHelper(itemListFilter, itemList, listItems.apply(inputFilter));
	}

	private IItemCollection getListSelection() {
		return ItemCollectionToolkit.build(itemList.getSelection().get());
	}

	public void saveState(IWritableState state) {
		StateToolkit.writeInt(state, SELECTED_SUBTAB, tabFolder.getSelectionIndex());
		itemList.getManager().getSettings().saveState(state.createChild(LIST_SETTINGS));
		itemChart.saveState(state.createChild(CHART_SETTINGS));
		itemListFilterComponent.saveState(state.createChild(ITEM_LIST_FILTER));
	}

	public Control getControl() {
		return tabFolder;
	}

	void setVisibleRange(IRange<IQuantity> visibleRange) {
		itemChart.setVisibleRange(visibleRange);
	}

	IRange<IQuantity> getVisibleRange() {
		return itemChart.getVisibleRange();
	}

	void setListSelectionState(SelectionState state) {
		itemList.getManager().setSelectionState(state);
	}

	SelectionState getListSelectionState() {
		return itemList.getManager().getSelectionState();
	}

	void setTabFolderIndex(int index) {
		tabFolder.setSelection(index);
	}

	int getTabFolderIndex() {
		return tabFolder.getSelectionIndex();
	}

	IItemFilter getItemListFilter() {
		return itemListFilter;
	}

	// FIXME: Would like to merge the menu managers and the menu consumers.
	List<IContributionManager> getMenuManagers() {
		return Arrays.asList(listContextMenuManager, itemChart.getMenuManager());
	}

	List<TriConsumer<String, ImageDescriptor, IMenuListener>> getMenuConsumers() {
		// NOTE: Not inlined because some versions of Eclipse complain otherwise.
		TriConsumer<String, ImageDescriptor, IMenuListener> menuConsumer = this::addMenuListenerAction;
		return Arrays.asList(menuConsumer);
	}

	private void addMenuListenerAction(String text, ImageDescriptor image, IMenuListener menuListener) {
		MCContextMenuManager bm = MCContextMenuManager.create(tabFolder);
		bm.setRemoveAllWhenShown(true);
		bm.addMenuListener(menuListener);
		Action a = new Action(text, image) {
			@Override
			public void run() {
				tabFolder.getMenu().setVisible(true);
			}
		};
		toolBarManager.add(a);
		toolBarManager.update(true);
	}

	public void update(
		IItemCollection filteredItems, IRange<IQuantity> currentRange, HistogramSelection histogramSelection,
		Boolean grouped) {
		listItems = histogramSelection != null && histogramSelection.getRowCount() > 0 ? histogramSelection.getItems()
				: filteredItems;
		itemList.show(ItemCollectionToolkit.filterIfNotNull(listItems, itemListFilter));
		itemChart.update(filteredItems, currentRange, histogramSelection, grouped);
	}

	public void onUseRange(Boolean useRange) {
		itemChart.onUseRange(useRange);
	}
}
