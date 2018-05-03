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

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore.SelectionStoreEntry;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnsFilter;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.misc.FilterEditor;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

/**
 * This class is meant as a container for code relating to using the FilterEditor in combination
 * with a SWT Control in a SashForm. This makes the use of the FilterEditor easier to implement in
 * the pages needing one.
 */
public class FilterComponent {

	private static final String SHOW_SEARCH = "showSearch"; //$NON-NLS-1$
	private static final String SHOW_FILTER = "showFilter"; //$NON-NLS-1$
	private static final String FILTER_EDITOR = "filterEditor"; //$NON-NLS-1$

	private SashForm mainSash;
	private boolean isVisible;
	private Consumer<IItemFilter> onChange;
	private IAction showFilterAction;
	private IAction showSearchAction;
	private FilterEditor editor;
	private GridData searchLayoutData;
	private Text searchText;
	private String searchString;

	public static FilterComponent createFilterComponent(
		ItemList list, IItemFilter filter, IItemCollection items, Supplier<Stream<SelectionStoreEntry>> selections,
		Consumer<IItemFilter> onSelect) {
		return createFilterComponent(list.getManager().getViewer().getControl(), list.getManager(), filter, items,
				selections, onSelect);
	}

	public static FilterComponent createFilterComponent(
		ItemHistogram histogram, IItemFilter filter, IItemCollection items,
		Supplier<Stream<SelectionStoreEntry>> selections, Consumer<IItemFilter> onSelect) {
		return createFilterComponent(histogram.getManager().getViewer().getControl(), histogram.getManager(), filter,
				items, selections, onSelect);
	}

	public static FilterComponent createFilterComponent(
		Control component, ColumnManager table, IItemFilter filter, IItemCollection items,
		Supplier<Stream<SelectionStoreEntry>> selections, Consumer<IItemFilter> onSelect) {

		FormToolkit toolkit = new FormToolkit(Display.getCurrent());

		SashForm mainSash = new SashForm(component.getParent(), SWT.VERTICAL | SWT.BORDER);

		mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		FilterEditor editor = DataPageToolkit.buildFilterSelector(mainSash, filter, items, selections, onSelect, false);

		Composite body = toolkit.createComposite(mainSash, SWT.NONE);
		GridLayout bodyLayout = new GridLayout(1, true);
		bodyLayout.marginWidth = 0;
		bodyLayout.verticalSpacing = 0;
		bodyLayout.marginHeight = 0;
		body.setLayout(bodyLayout);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		Composite filterComposite = toolkit.createComposite(body);
		body.setBackground(new Color(Display.getCurrent(), 128, 128, 128));
		GridLayout filterCompositeLayout = new GridLayout(1, false);
		filterCompositeLayout.marginWidth = 0;
		filterCompositeLayout.marginHeight = 0;
		filterComposite.setLayout(filterCompositeLayout);
		GridData filterCompositeData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		filterCompositeData.heightHint = 0;
		filterComposite.setLayoutData(filterCompositeData);
		Text filterText = ColumnsFilter.addFilterControl(filterComposite, toolkit, table);
		table.getViewer().getControl().setParent(body);
		table.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		toolkit.dispose();

		mainSash.setWeights(new int[] {15, 50});

		return new FilterComponent(mainSash, onSelect, editor, filterCompositeData, filterText);
	}

	private FilterComponent(SashForm mainSash, Consumer<IItemFilter> onChange, FilterEditor editor, GridData filterData,
			Text filterText) {
		this.mainSash = mainSash;
		this.onChange = onChange;
		this.editor = editor;
		this.searchLayoutData = filterData;
		this.searchText = filterText;
		searchString = filterText.getText();
		isVisible = false;
		showFilterAction = createShowFilterAction();
		showSearchAction = createShowSearchAction();
		setColor(1); // Because the default filter of the page will show all relevant items, even if there aren't any
	}

	public void loadState(IState state) {
		PersistableSashForm.loadState(mainSash, state);
		showFilterAction.setChecked(StateToolkit.readBoolean(state, SHOW_FILTER, false));
		showSearchAction.setChecked(StateToolkit.readBoolean(state, SHOW_SEARCH, false));
		showFilterAction.run();
		showSearchAction.run();
		if (state != null) {
			editor.loadState(state.getChild(FILTER_EDITOR));
		}
	}

	public void saveState(IWritableState state) {
		PersistableSashForm.saveState(mainSash, state);
		StateToolkit.writeBoolean(state, SHOW_FILTER, showFilterAction.isChecked());
		StateToolkit.writeBoolean(state, SHOW_SEARCH, showSearchAction.isChecked());
		editor.saveState(state.createChild(FILTER_EDITOR));
	}

	public IAction getShowSearchAction() {
		return showSearchAction;
	}

	public IAction getShowFilterAction() {
		return showFilterAction;
	}

	public SashForm getComponent() {
		return mainSash;
	}

	private IAction createShowSearchAction() {
		IAction checkAction = ActionToolkit.checkAction(max -> {
			// it would be better to use setVisible here instead of heightHint, but that doesn't work properly
			if (max) {
				searchLayoutData.heightHint = SWT.DEFAULT;
				searchText.setEnabled(true);
				searchText.setText(searchString);
			} else {
				searchLayoutData.heightHint = 0;
				searchText.setEnabled(false);
				searchString = searchText.getText();
				searchText.setText(""); //$NON-NLS-1$
			}
			searchLayoutData.heightHint = max ? SWT.DEFAULT : 0;
			mainSash.layout(true, true);
		}, Messages.FILTER_SHOW_SEARCH_ACTION, CoreImages.FIND);
		return checkAction;
	}

	private IAction createShowFilterAction() {
		IAction checkAction = ActionToolkit.checkAction(max -> {
			isVisible = max;
			if (!isVisible) {
				onChange.accept(null);
			} else {
				onChange.accept(editor.getFilter());
			}
			editor.getControl().setVisible(max);
			mainSash.layout(false);
		}, Messages.FILTER_SHOW_FILTER_ACTION,
				FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.FILTER_ICON));
		return checkAction;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void notifyListener() {
		editor.notifyListener();
	}

	public void filterChangeHelper(
		IItemFilter filter, Consumer<IItemCollection> itemConsumer, Supplier<Integer> countSupplier,
		IItemCollection items) {
		if (isVisible()) {
			itemConsumer.accept(items.apply(filter));
			setColor(countSupplier.get());
		} else {
			itemConsumer.accept(items);
		}
	}

	public void filterChangeHelper(IItemFilter filter, ItemHistogram table, IItemCollection items) {
		filterChangeHelper(filter, table::show, table.getAllRows()::getRowCount, items);
	}

	public void filterChangeHelper(IItemFilter filter, ItemList table, IItemCollection items) {
		filterChangeHelper(filter, table::show, () -> {
			Object input = table.getManager().getViewer().getInput();
			return input instanceof IItem[] ? ((IItem[]) input).length : 0;
		}, items);
	}

	/**
	 * This is used to set the background color of the sash containing both the filter and the table
	 * to show if any items have been filtered out based on the number of datapoints in the table
	 *
	 * @param datapoints
	 *            E.g. rows in a table
	 */
	public void setColor(int datapoints) {
		if (datapoints == 0) {
			mainSash.setBackground(new Color(Display.getCurrent(), new RGB(180, 0, 0)));
		} else {
			mainSash.setBackground(new Color(Display.getCurrent(), new RGB(0, 180, 0)));
		}
	}

}
