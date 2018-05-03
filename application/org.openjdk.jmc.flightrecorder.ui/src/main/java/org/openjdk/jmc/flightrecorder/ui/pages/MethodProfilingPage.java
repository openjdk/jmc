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

import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
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
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogramWithInput;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class MethodProfilingPage extends AbstractDataPage {
	public static class MethodProfilingPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.MethodProfilingPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_METHOD);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.METHOD_PROFILING_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new MethodProfilingPage(dpd, items, editor);
		}

	}

	private static final IItemFilter TABLE_ITEMS = ItemFilters.type(JdkTypeIDs.EXECUTION_SAMPLE);
	private static final ItemHistogramBuilder PACKAGE_HISTOGRAM = new ItemHistogramBuilder();
	private static final ItemHistogramBuilder CLASS_HISTOGRAM = new ItemHistogramBuilder();

	static {
		PACKAGE_HISTOGRAM.addCountColumn();
		// FIXME: Add some top frame balance aggregate, but which? Tried similar to top frame balance but without dividing with total.

		CLASS_HISTOGRAM.addCountColumn();
	}

	private class MethodProfilingUi implements IPageUI {
		private static final String CLASS_FILTER = "classFilter"; //$NON-NLS-1$
		private static final String PACKAGE_FILTER = "packageFilter"; //$NON-NLS-1$
		private static final String SASH_ELEMENT = "sash"; //$NON-NLS-1$
		private static final String PACKAGE_TABLE_ELEMENT = "packageTable"; //$NON-NLS-1$
		private static final String CLASS_TABLE_ELEMENT = "classTable"; //$NON-NLS-1$

		private final ItemHistogram packageTable;
		private final ItemHistogram classTable;
		private final SashForm sash;
		private final IPageContainer pageContainer;
		private Consumer<IItemCollection> chained;
		private FilterComponent packageTableFilter;
		private FilterComponent classTableFilter;
		private IItemCollection selectionItems;
		private FlavorSelector flavorSelector;

		MethodProfilingUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			this.pageContainer = pageContainer;
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			sash = new SashForm(form.getBody(), SWT.VERTICAL);
			toolkit.adapt(sash);

			packageTable = PACKAGE_HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.STACK_TRACE_TOP_PACKAGE,
					getTableSettings(state.getChild(PACKAGE_TABLE_ELEMENT)));
			MCContextMenuManager mmPackage = MCContextMenuManager
					.create(packageTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(packageTable.getManager(), mmPackage);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), packageTable,
					Messages.MethodProfilingPage_PACKAGE_HISTOGRAM_SELECTION, mmPackage);

			classTable = CLASS_HISTOGRAM.buildWithoutBorder(sash, JdkAttributes.STACK_TRACE_TOP_CLASS,
					getTableSettings(state.getChild(CLASS_TABLE_ELEMENT)));
			MCContextMenuManager mmClass = MCContextMenuManager
					.create(classTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(classTable.getManager(), mmClass);
			SelectionStoreActionToolkit.addSelectionStoreActions(pageContainer.getSelectionStore(), classTable,
					Messages.MethodProfilingPage_CLASS_HISTOGRAM_SELECTION, mmClass);

			chained = ItemHistogramWithInput.chain(packageTable, this::updateDetails, classTable);

			packageTableFilter = FilterComponent.createFilterComponent(packageTable, packageFilter, getItems(),
					pageContainer.getSelectionStore()::getSelections, this::onPackageFilterChange);
			packageTableFilter.loadState(state.getChild(PACKAGE_FILTER));
			mmPackage.add(packageTableFilter.getShowFilterAction());
			mmPackage.add(packageTableFilter.getShowSearchAction());
			classTableFilter = FilterComponent.createFilterComponent(classTable, classFilter, getItems(),
					pageContainer.getSelectionStore()::getSelections, this::onClassFilterChange);
			classTableFilter.loadState(state.getChild(CLASS_FILTER));
			mmClass.add(classTableFilter.getShowFilterAction());
			mmClass.add(classTableFilter.getShowSearchAction());

			// FIXME: Create a bar chart or something of the samples, similar to the one on Java application?

			PersistableSashForm.loadState(sash, state.getChild(SASH_ELEMENT));

			flavorSelector = FlavorSelector.itemsWithTimerange(form, TABLE_ITEMS, getDataSource().getItems(),
					pageContainer, this::onInputSelected, flavorSelectorState);

			addResultActions(form);

			onPackageFilterChange(packageFilter);
			onClassFilterChange(classFilter);
			packageTable.getManager().setSelectionState(packageState);
			classTable.getManager().setSelectionState(classState);
		}

		private void onPackageFilterChange(IItemFilter filter) {
			packageFilter = filter;
			packageTableFilter.filterChangeHelper(filter, chained, packageTable.getAllRows()::getRowCount, getItems());
		}

		private void onClassFilterChange(IItemFilter filter) {
			classFilter = filter;
			IItemCollection items = packageTable.getSelection().getItems();
			items = items.hasItems() ? items : getItems();
			classTableFilter.filterChangeHelper(filter, classTable, items);
		}

		@Override
		public void saveTo(IWritableState writableState) {
			PersistableSashForm.saveState(sash, writableState.createChild(SASH_ELEMENT));
			packageTable.getManager().getSettings().saveState(writableState.createChild(PACKAGE_TABLE_ELEMENT));
			classTable.getManager().getSettings().saveState(writableState.createChild(CLASS_TABLE_ELEMENT));
			packageTableFilter.saveState(writableState.createChild(PACKAGE_FILTER));
			classTableFilter.saveState(writableState.createChild(CLASS_FILTER));
			saveToLocal();
		}

		private void saveToLocal() {
			packageState = packageTable.getManager().getSelectionState();
			classState = classTable.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void updateDetails(IItemCollection items) {
			pageContainer.showSelection(items);
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			selectionItems = items;
			chained.accept(getItems());
		}

		private IItemCollection getItems() {
			IItemCollection items = selectionItems != null ? selectionItems : getDataSource().getItems();
			return ItemCollectionToolkit.filterIfNotNull(
					ItemCollectionToolkit.filterIfNotNull(items.apply(TABLE_ITEMS), packageFilter), classFilter);
		}

	}

	private static TableSettings getTableSettings(IState state) {
		if (state == null) {
			return new TableSettings(ItemHistogram.COUNT_COL_ID,
					Arrays.asList(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 500, null),
							new ColumnSettings(ItemHistogram.COUNT_COL_ID, false, 120, false)));
		} else {
			return new TableSettings(state);
		}
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new MethodProfilingUi(parent, toolkit, pageContainer, state);
	}

	private IItemFilter packageFilter;
	private SelectionState packageState;
	private IItemFilter classFilter;
	private SelectionState classState;
	public FlavorSelectorState flavorSelectorState;

	public MethodProfilingPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return TABLE_ITEMS;
	}
}
