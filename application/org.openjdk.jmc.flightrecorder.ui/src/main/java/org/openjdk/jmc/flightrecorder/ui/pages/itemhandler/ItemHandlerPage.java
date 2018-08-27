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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemFilters.Type;
import org.openjdk.jmc.common.item.PersistableItemFilter;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.RulesUiToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.HistogramSelection;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.ItemHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.LabeledPageFactory;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.FilterEditor;
import org.openjdk.jmc.ui.misc.PersistableSashForm;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;

public class ItemHandlerPage extends AbstractDataPage {

	/**
	 * Used to write ItemHandlerUI state without creating UI controls
	 */
	public static class ItemHandlerUiStandIn implements IStateful {

		private final Set<? extends IType<?>> types;

		public ItemHandlerUiStandIn(Set<? extends IType<?>> types) {
			this.types = types;
		}

		@Override
		public void saveTo(IWritableState state) {
			Set<String> typesIds = types.stream().map(IType::getIdentifier).collect(Collectors.toSet());
			/*
			 * FIXME: JMC-4326 - Should build the page using the IType instances instead. All
			 * included attributes (including type and attribute names, descriptions etc) could be
			 * persisted and used when the page is shown, instead of recalculating the set of
			 * types/attributes each time the page is shown.
			 */
			IItemFilter filter = null;

			if (typesIds.size() == 1) {
				IType<?> type = types.iterator().next();
				filter = ItemFilters.type(typesIds.iterator().next());
				state.putString(ELEMENT_DEFAULT_NAME, type.getName());
			} else {
				filter = ItemFilters.type(typesIds);
			}
			((PersistableItemFilter) filter).saveTo(state.createChild(ELEMENT_FILTER));
		}
	}

	public static class Factory extends LabeledPageFactory {

		@Override
		protected String getDefaultName(IState state) {
			String name = state.getAttribute(ELEMENT_DEFAULT_NAME);
			if (name != null) {
				return name;
			}
			IItemFilter cf = getPageFilter(state);
			return cf instanceof Type ? ((Type) cf).getTypeId() : Messages.ItemHandlerPage_DEFAULT_PAGE_NAME;
		}

		@Override
		protected ImageDescriptor getDefaultImageDescriptor(IState state) {
			IItemFilter cf = getPageFilter(state);
			if (cf instanceof Type) {
				return SWTColorToolkit.getColorThumbnailDescriptor(
						SWTColorToolkit.asRGB(TypeLabelProvider.getColorOrDefault(((Type) cf).getTypeId())));
			} else {
				return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_EVENT);
			}
		}

		@Override
		public String[] getTopics(IState state) {
			return ItemHandlerPage.getTopics(state);
		}

		@Override
		public void resetToDefault(IState currentState, IWritableState newState) {
			super.resetToDefault(currentState, newState);
			IState filterElement = currentState.getChild(ELEMENT_FILTER);
			if (filterElement != null) {
				((PersistableItemFilter) PersistableItemFilter.readFrom(filterElement))
						.saveTo(newState.createChild(ELEMENT_FILTER));
			}
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new ItemHandlerPage(dpd, items, editor);
		}
	}

	private static enum AggregatorIdPrefix {
		SUM, AVG, MIN, MAX, STDDEVP, COUNT, DISTINCT
	}

	private static final String ELEMENT_DEFAULT_NAME = "defaultName"; //$NON-NLS-1$
	private static final String ELEMENT_FILTER = "pageFilter"; //$NON-NLS-1$
	private static final String ELEMENT_TOPIC = "topic"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
	private static final String FILTER_EDITOR = "pageFilterEditor"; //$NON-NLS-1$

	private class ItemHandlerUI implements IPageUI {

		private static final String FILTER_SASH = "filterSash"; //$NON-NLS-1$
		private static final String HISTOGRAM_SASH = "histogramSash"; //$NON-NLS-1$
		private static final String LIST_AND_CHART_ELEMENT = "listAndChart"; //$NON-NLS-1$
		private static final String HISTOGRAM_ELEMENT = "histogram"; //$NON-NLS-1$
		private static final String SHOW_FILTER = "showFilter"; //$NON-NLS-1$

		private final HistogramSequence histogramSequence;
		private final IPageContainer pageContainer;
		private IItemFilter pageFilter;
		private final SashForm filterSash;
		private final SashForm histogramSash;
		private IAction showFilter;
		private final ItemListAndChart itemListAndChart;
		private IRange<IQuantity> currentRange;
		private IItemCollection filteredItems;
		private AttributeComponentConfiguration acc;
		private IItemCollection selectionItems;
		private String[] topics;

		private final String description;
		private FilterEditor filterEditor;
		private final StreamModel items;
		private FlavorSelector flavorSelector;

		ItemHandlerUI(Composite parent, FormToolkit toolkit, String pageName, String description, IState state,
				StreamModel items, IPageContainer c) {
			this.description = description;
			this.items = items;
			pageFilter = getPageFilter(state);

			pageContainer = c;

			form = DataPageToolkit.createForm(parent, toolkit, pageName, getImageDescriptor().createImage());
			form.addDisposeListener(e -> DisplayToolkit.dispose(form.getImage()));
			DataPageToolkit.addRenameAction(form, c);
			DataPageToolkit.addIconChangeAction(form, c, i -> icon = i);
			topics = ItemHandlerPage.getTopics(state);
			form.getMenuManager().add(createSetTopicAction());

			Composite formBody = form.getBody();
			formBody.setLayout(new GridLayout());
			if (description != null) {
				Label desc = new Label(formBody, SWT.NONE);
				desc.setText(description);
				desc.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
			}
			filterSash = new SashForm(formBody, SWT.VERTICAL);
			filterSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			filterSash.setSashWidth(filterSash.getSashWidth() * 2);
			toolkit.adapt(filterSash);

			// FIXME: Check state to decide if this should be shown
			filterEditor = DataPageToolkit.buildFilterSelector(filterSash, pageFilter, items.getItems(),
					c.getSelectionStore()::getSelections, this::onFilterChange, true);
			if (state != null) {
				filterEditor.loadState(state.getChild(FILTER_EDITOR));
			}

			acc = new AttributeComponentConfiguration(
					ItemCollectionToolkit.filterIfNotNull(items.getItems(), pageFilter));

			ItemHistogramBuilder histogramBuilder = new ItemHistogramBuilder();
			histogramBuilder.addCountColumn();

			for (Entry<String, IAttribute<?>> entry : acc.getAllAttributes().entrySet()) {
				String combinedId = entry.getKey();
				IAttribute<?> a = entry.getValue();
				ContentType<?> contentType = a.getContentType();
				if (contentType instanceof KindOfQuantity<?>) {
					@SuppressWarnings("unchecked")
					IAttribute<IQuantity> quantityAttribute = (IAttribute<IQuantity>) a;
					if (contentType instanceof LinearKindOfQuantity) {
						histogramBuilder.addColumn(AggregatorIdPrefix.SUM + combinedId,
								Aggregators.sum(quantityAttribute));
					}
					histogramBuilder.addColumn(AggregatorIdPrefix.AVG + combinedId, Aggregators.avg(quantityAttribute));
					histogramBuilder.addColumn(AggregatorIdPrefix.MIN + combinedId, Aggregators.min(quantityAttribute));
					histogramBuilder.addColumn(AggregatorIdPrefix.MAX + combinedId, Aggregators.max(quantityAttribute));
					if (contentType instanceof LinearKindOfQuantity) {
						histogramBuilder.addColumn(AggregatorIdPrefix.STDDEVP + combinedId,
								Aggregators.stddevp(quantityAttribute));
					}
				}
			}

			HistogramSettingsTree histogramSettings = null;
			IState listAndChartState = null;
			if (state != null) {
				listAndChartState = state.getChild(LIST_AND_CHART_ELEMENT);
				IState histogramState = state.getChild(HISTOGRAM_ELEMENT);
				if (histogramState != null) {
					try {
						histogramSettings = new HistogramSettingsTree(histogramState);
						/*
						 * Only enable the attribute and count columns that have been configured in
						 * the state, to avoid bloating the visible columns menu.
						 */
						for (Entry<String, IAttribute<?>> entry : acc.getCommonAttributes().entrySet()) {
							IAttribute<?> a = entry.getValue();
							if (histogramSettings.tableSettings.getColumns().stream().map(cs -> cs.getId())
									.anyMatch(s -> s.equals(a.getIdentifier()))) {
								histogramBuilder.addColumn(a);
							}
						}
						for (Entry<String, IType<?>> typeEntry : acc.getAllTypes().entrySet()) {
							String colId = AggregatorIdPrefix.COUNT + typeEntry.getKey();
							if (histogramSettings.tableSettings.getColumns().stream().map(cs -> cs.getId())
									.anyMatch(s -> s.equals(colId))) {
								histogramBuilder.addColumn(colId, Aggregators.count(typeEntry.getValue()));
							}
						}
					} catch (RuntimeException e) {
						FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Broken settings", e); //$NON-NLS-1$
					}
				}
			}

			if (histogramSettings == null) {
				List<ColumnSettings> defaultHistogramCols = new ArrayList<>();
				defaultHistogramCols.add(new ColumnSettings(ItemHistogram.KEY_COL_ID, false, 500, null));
				defaultHistogramCols.add(new ColumnSettings(ItemHistogram.COUNT_COL_ID, false, 150, false));
				for (Entry<String, IAttribute<?>> entry : acc.getAllAttributes().entrySet()) {
					String combinedId = entry.getKey();
					IAttribute<?> a = entry.getValue();
					ContentType<?> contentType = a.getContentType();
					boolean commonAttribute = acc.getCommonAttributes().containsKey(entry.getKey());
					if (contentType instanceof KindOfQuantity<?>) {
						if (contentType instanceof LinearKindOfQuantity) {
							defaultHistogramCols.add(new ColumnSettings(AggregatorIdPrefix.SUM + combinedId,
									!commonAttribute || contentType != UnitLookup.MEMORY, null, null));
						}
						defaultHistogramCols.add(new ColumnSettings(AggregatorIdPrefix.AVG + combinedId,
								!commonAttribute || contentType != UnitLookup.PERCENTAGE, null, null));
						defaultHistogramCols
								.add(new ColumnSettings(AggregatorIdPrefix.MIN + combinedId, true, null, null));
						defaultHistogramCols
								.add(new ColumnSettings(AggregatorIdPrefix.MAX + combinedId, true, null, null));
						defaultHistogramCols
								.add(new ColumnSettings(AggregatorIdPrefix.STDDEVP + combinedId, true, null, null));
					}
				}
				histogramSettings = new HistogramSettingsTree(null,
						new TableSettings(ItemHistogram.COUNT_COL_ID, defaultHistogramCols));
			}

			histogramSash = new SashForm(filterSash, SWT.VERTICAL);
			toolkit.adapt(histogramSash);
			itemListAndChart = new ItemListAndChart(toolkit, pageContainer, getDataSource(), pageFilter, itemListFilter,
					pageName, histogramSash, listAndChartState, acc, this::onListItemsSelected);

			histogramSequence = new HistogramSequence(itemListAndChart.getControl(), itemListAndChart.getMenuManagers(),
					itemListAndChart.getMenuConsumers(), pageName, histogramSettings, histogramBuilder,
					this::onHistogramItemsSelected, this::onGrouped, acc, pageContainer.getSelectionStore());

			if (state != null && state.getChild(HISTOGRAM_SASH) != null) {
				PersistableSashForm.loadState(histogramSash, state.getChild(HISTOGRAM_SASH));
			} else {
				int[] sashWeights = getDefaultSashWeights(histogramSash);
				histogramSash.setWeights(sashWeights);
			}
			if (state != null && state.getChild(FILTER_SASH) != null) {
				PersistableSashForm.loadState(filterSash, state.getChild(FILTER_SASH));
			} else {
				filterSash.setWeights(new int[] {1, 6});
			}

			addShowFilterAction(state);

			flavorSelector = FlavorSelector.itemsWithTimerange(form, pageFilter, items.getItems(), pageContainer,
					this::onInputSelected, this::onUseRange, flavorSelectorState);

			addResultActions(form);

			onGrouped(histogramSettings.groupBy != null);

			Consumer<Result> listener = r -> {
				for (String topic : getTopics()) {
					if (form != null && r.getRule().getTopic().equals(topic)) {
						DisplayToolkit.safeAsyncExec(() -> form.setImage(getImageDescriptor().createImage()));
						return;
					}
				}
			};
			pageContainer.getRuleManager().addResultListener(listener);
			form.addDisposeListener(e -> pageContainer.getRuleManager().removeResultListener(listener));

			histogramSequence.setSelectionStates(selectionStates);
			itemListAndChart.setTabFolderIndex(tabFolderIndex);
			itemListAndChart.setVisibleRange(itemChartRange);
			itemListAndChart.setListSelectionState(itemListSelection);
		}

		private IAction createSetTopicAction() {
			return ActionToolkit.action(() -> {
				ListSelectionDialog dialog = new ListSelectionDialog(form.getShell(),
						RulesUiToolkit.getTopics().stream().sorted().toArray(), new ArrayContentProvider(),
						new ColumnLabelProvider(), Messages.ItemHandlerPage_SET_TOPICS_DIALOG_MESSAGE) {
					// ListSelectionDialog is not intended to be subclassed, but let's do it anyway to enable wrapping text
					@Override
					protected Label createMessageArea(Composite parent) {
						Composite composite = new Composite(parent, SWT.NONE);

						composite.setLayout(GridLayoutFactory.swtDefaults().margins(0, 0).create());
						composite.setLayoutData(GridDataFactory.fillDefaults().create());

						Label label = new Label(composite, SWT.WRAP);
						label.setLayoutData(GridDataFactory.swtDefaults().hint(300, SWT.DEFAULT).grab(true, false)
								.align(GridData.FILL, GridData.BEGINNING).create());

						if (getMessage() != null) {
							label.setText(getMessage());
						}
						label.setFont(parent.getFont());
						return label;
					}
				};
				dialog.setTitle(Messages.ItemHandlerPage_SET_TOPICS_TITLE);
				if (topics != null) {
					dialog.setInitialSelections((Object[]) topics);
				}
				if (dialog.open() == Window.OK) {
					Object[] result = dialog.getResult();
					topics = new String[result.length];
					for (int i = 0; i < result.length; i++) {
						topics[i] = result[i].toString();
					}
					pageContainer.currentPageRefresh();
				}
			}, Messages.ItemHandlerPage_SET_TOPICS_ACTION);
		}

		private int[] getDefaultSashWeights(SashForm sash) {
			int length = sash.getChildren().length;
			int[] weights = new int[length];
			for (int i = 0; i < length - 1; i++) {
				weights[i] = 1;
			}
			weights[length - 1] = Math.max(1, 6 - length);
			return weights;
		}

		private void addShowFilterAction(IState state) {
			showFilter = ActionToolkit.checkAction(b -> showFilter(b), Messages.ItemHandlerPage_SHOW_FILTER_ACTION,
					FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.FILTER_ICON));
			form.getToolBarManager().add(showFilter);
			form.getToolBarManager().update(true);
			showFilter.setChecked(StateToolkit.readBoolean(state, SHOW_FILTER, true));
			showFilter(showFilter.isChecked());
		}

		private void showFilter(boolean showFilter) {
			filterEditor.getControl().setVisible(showFilter);
			filterSash.layout(false);
		}

		@Override
		public void saveTo(IWritableState state) {
			if (pageFilter instanceof PersistableItemFilter) {
				((PersistableItemFilter) pageFilter).saveTo(state.createChild(ELEMENT_FILTER));
			}
			for (String topic : topics) {
				state.createChild(ELEMENT_TOPIC).putString(ATTRIBUTE_VALUE, topic);
			}
			ImageDescriptor icon = Optional.ofNullable(getIcon()).map(ImageDescriptor::createFromImage).orElse(null);
			LabeledPageFactory.writeLabel(state, form.getText(), description, icon);
			itemListAndChart.saveState(state.createChild(LIST_AND_CHART_ELEMENT));
			histogramSequence.getHistogramSettings().saveState(state.createChild(HISTOGRAM_ELEMENT));
			PersistableSashForm.saveState(histogramSash, state.createChild(HISTOGRAM_SASH));
			PersistableSashForm.saveState(filterSash, state.createChild(FILTER_SASH));
			StateToolkit.writeBoolean(state, SHOW_FILTER, showFilter.isChecked());
			filterEditor.saveState(state.createChild(FILTER_EDITOR));

			saveToLocal();
		}

		private void saveToLocal() {
			selectionStates = histogramSequence.getSelectionStates();
			itemListSelection = itemListAndChart.getListSelectionState();
			tabFolderIndex = itemListAndChart.getTabFolderIndex();
			itemChartRange = itemListAndChart.getVisibleRange();
			itemListFilter = itemListAndChart.getItemListFilter();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}

		private void onFilterChange(IItemFilter filter) {
			/*
			 * The filter can have changed in a way that makes some of the flavors non-applicable,
			 * and other new flavors applicable, therefore we clear the maps, even though this might
			 * also be annoying.
			 */
			if (flavorSelectorState != null) {
				flavorSelectorState.clearFlavorMaps();
			}
			this.pageFilter = filter;
			updateFilteredItems();
			refreshData();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			this.currentRange = timeRange;
			this.selectionItems = items;
			updateFilteredItems();
			refreshData();
		}

		private void onUseRange(Boolean useRange) {
			itemListAndChart.onUseRange(useRange);
		}

		private IItemCollection getItems() {
			return selectionItems != null ? selectionItems : items.getItems();
		}

		private void updateFilteredItems() {
			filteredItems = ItemCollectionToolkit.filterIfNotNull(getItems(), pageFilter);
		}

		private void refreshData() {
			updateFilteredItems();
			histogramSequence.setItems(filteredItems);
			itemListAndChart.update(filteredItems, currentRange, histogramSelection(), histogramSequence.isGrouped());
		}

		private void onHistogramItemsSelected(IItemCollection histogramItems) {
			if (filteredItems != null && currentRange != null) {
				// Using filteredItems (for some charts this means all items, and for other charts the histogram selection)
				itemListAndChart.update(filteredItems, currentRange, histogramSelection(),
						histogramSequence.isGrouped());
				pageContainer.showSelection(histogramItems);
			}
		}

		private void onListItemsSelected(IItemCollection listItems) {
			pageContainer.showSelection(listItems);
		}

		private void onGrouped(Boolean grouped) {
			if (filteredItems != null && currentRange != null) {
				itemListAndChart.update(filteredItems, currentRange, histogramSelection(), grouped);
			}
		}

		private HistogramSelection histogramSelection() {
			if (histogramSequence != null && histogramSequence.isGrouped()) {
				HistogramSelection histogramSelection = histogramSequence.getSelection();
				return histogramSelection != null && histogramSelection.getRowCount() > 0 ? histogramSelection
						: histogramSequence.getAllRows();
			}
			return null;
		}

	}

	static IItemFilter getPageFilter(IState state) {
		if (state == null) {
			return null;
		}
		IState filterElement = state.getChild(ELEMENT_FILTER);
		return filterElement == null ? null : PersistableItemFilter.readFrom(filterElement);
	}

	static String[] getTopics(IState state) {
		if (state == null) {
			return new String[0];
		}
		return Stream.of(state.getChildren(ELEMENT_TOPIC)).map(topicState -> topicState.getAttribute(ATTRIBUTE_VALUE))
				.toArray(String[]::new);
	}

	private LinkedList<SelectionState> selectionStates;
	private int tabFolderIndex = 0;
	private SelectionState itemListSelection;
	private IRange<IQuantity> itemChartRange;
	public IItemFilter itemListFilter;
	private FlavorSelectorState flavorSelectorState;

	public ItemHandlerPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		itemChartRange = editor.getRecordingRange();
	}

	private Image icon;
	private Form form;

	@Override
	protected Image getIcon() {
		return icon == null ? super.getIcon() : icon;
	}

	@Override
	public String getDescription() {
		IQuantity itemCount = null;
		IItemFilter filter = getPageFilter(getState());
		if (filter != null) {
			IItemCollection filtered = getDataSource().getItems().apply(filter);
			itemCount = filtered.getAggregate(Aggregators.count());
		}
		if (itemCount == null) {
			itemCount = UnitLookup.NUMBER_UNITY.quantity(0);
		}
		String countMessage = NLS.bind(Messages.ItemHandlerPage_PAGE_EVENTS_COUNT_TOOLTIP,
				itemCount.displayUsing(IDisplayable.AUTO));

		String superDescription = super.getDescription();
		if (superDescription != null && superDescription.length() > 0) {
			return superDescription + System.getProperty("line.separator") //$NON-NLS-1$
					+ countMessage;
		}
		return countMessage;
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return getPageFilter(getState());
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new ItemHandlerUI(parent, toolkit, super.getName(), getConfiguredDescription(), state, getDataSource(),
				editor);
	}

}
