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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.ui.EventTypeFolderNode.EventTypeNode;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.PageManager;
import org.openjdk.jmc.flightrecorder.ui.RuleManager;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList;
import org.openjdk.jmc.flightrecorder.ui.common.ItemList.ItemListBuilder;
import org.openjdk.jmc.flightrecorder.ui.common.TypeFilterBuilder;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.pages.itemhandler.ItemHandlerPage;
import org.openjdk.jmc.flightrecorder.ui.pages.itemhandler.ItemHandlerPage.ItemHandlerUiStandIn;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStoreActionToolkit;
import org.openjdk.jmc.ui.OrientationAction;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.column.TableSettings.ColumnSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class EventBrowserPage extends AbstractDataPage {
	private static final ImageDescriptor NEW_PAGE_ICON = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_NEW_PAGE);

	public static class Factory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.EventBrowserPage_PAGE_NAME;
		}

		@Override
		public String getDescription(IState state) {
			return Messages.EventBrowserPage_PAGE_DESC;
		}

		@Override
		public String[] getTopics(IState state) {
			// All topics
			return new String[] {RuleManager.UNMAPPED_REMAINDER_TOPIC};
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_EVENT_TYPE_SELECTOR);
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new EventBrowserPage(dpd, items, editor);
		}

	}

	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new EventBrowserUI(parent, toolkit, state, editor);
	}

	private SelectionState tableSelection;
	private ISelection treeSelection;
	public TreePath[] treeExpansion;
	public FlavorSelectorState flavorSelectorState;
//	public int topIndex;

	public EventBrowserPage(IPageDefinition definition, StreamModel items, IPageContainer editor) {
		super(definition, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.all();
	}

	class EventBrowserUI implements IPageUI {

		private static final String TREE_SASH = "treeSash"; //$NON-NLS-1$
		private static final String ITEM_LIST = "itemList"; //$NON-NLS-1$
		private static final String SHOW_TYPES_WITHOUT_EVENTS = "showTypesWithoutEvents"; //$NON-NLS-1$
		private ItemList list;
		private final SashForm treeSash;
		private final IPageContainer container;
		private final List<ColumnSettings> listColumns = new ArrayList<>();
		private String listOrderBy;
		private Set<IType<?>> selectedTypes = Collections.emptySet();
		private final TypeFilterBuilder typeFilterTree;
		private IItemCollection selectionItems;
		private FlavorSelector flavorSelector;
		private Boolean showTypesWithoutEvents;

		EventBrowserUI(Composite parent, FormToolkit toolkit, IState state, IPageContainer container) {
			this.container = container;

			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());

			treeSash = new SashForm(form.getBody(), SWT.HORIZONTAL);
			toolkit.adapt(treeSash);
			typeFilterTree = DataPageToolkit.buildEventTypeTree(treeSash, toolkit, this::onTypeChange, false);
			MCContextMenuManager mm = typeFilterTree.getMenuManager();
			IAction addPageAction = ActionToolkit.action(this::addPage,
					Messages.EventBrowserPage_NEW_PAGE_USING_TYPES_ACTION, NEW_PAGE_ICON);
			mm.appendToGroup(MCContextMenuManager.GROUP_NEW, addPageAction);

			IAction typesWithoutEventsAction = ActionToolkit.checkAction(this::setTypesWithoutEvents,
					Messages.EventBrowserPage_DISPLAY_TYPES_WITHOUT_EVENTS, null);
			showTypesWithoutEvents = StateToolkit.readBoolean(state, SHOW_TYPES_WITHOUT_EVENTS, true);
			typesWithoutEventsAction.setChecked(showTypesWithoutEvents);
			mm.appendToGroup(MCContextMenuManager.GROUP_OPEN, typesWithoutEventsAction);

			SelectionStoreActionToolkit.addSelectionStoreActions(typeFilterTree.getViewer(), () -> selectedTypes,
					container.getSelectionStore(), Messages.EventBrowserPage_EVENT_TYPE_TREE_SELECTION, mm);

			Composite listParent = toolkit.createComposite(treeSash);
			listParent.setLayout(new FillLayout());
			PersistableSashForm.loadState(treeSash, state.getChild(TREE_SASH));

			form.getToolBarManager().add(addPageAction);
			form.getToolBarManager().add(new Separator());
			OrientationAction.installActions(form, treeSash);

			IState itemListState = state.getChild(ITEM_LIST);
			if (itemListState != null) {
				TableSettings settings = new TableSettings(itemListState);
				listColumns.addAll(settings.getColumns());
				listOrderBy = settings.getOrderBy();
			}
			list = new ItemListBuilder().build(listParent, null);

			flavorSelector = FlavorSelector.itemsWithTimerange(form, null, getDataSource().getItems(), container,
					this::onInputSelected, flavorSelectorState);

			addResultActions(form);
			if (treeExpansion != null) {
				typeFilterTree.getViewer().setExpandedTreePaths(treeExpansion);
			} else {
				typeFilterTree.getViewer().expandAll();
			}
			typeFilterTree.getViewer().setSelection(treeSelection);
//			if (topIndex >= 0) {
//				typeFilterTree.getViewer().getTree().setTopItem(typeFilterTree.getViewer().getTree().getItem(topIndex));
//			}
			list.getManager().setSelectionState(tableSelection);
		}

		private void addPage() {
			PageManager pm = FlightRecorderUI.getDefault().getPageManager();
			pm.makeRoot(pm.createPage(ItemHandlerPage.Factory.class, new ItemHandlerUiStandIn(selectedTypes)));
		}

		private void setTypesWithoutEvents(boolean checked) {
			showTypesWithoutEvents = checked;
			refreshTree();
		}

		private void onInputSelected(IItemCollection items, IRange<IQuantity> timeRange) {
			this.selectionItems = (items == null) ? getDataSource().getItems() : items;
			refreshTree();
		}

		private void refreshTree() {
			boolean noTypesWereSelected = selectedTypes.isEmpty();

			typeFilterTree.getViewer().getControl().setRedraw(false);
			TreePath[] expansion = typeFilterTree.getViewer().getExpandedTreePaths();
			ISelection selection = typeFilterTree.getViewer().getSelection();
			typeFilterTree.setInput(getDataSource().getTypeTree((ItemCollectionToolkit.stream(selectionItems)
					.filter(ii -> showTypesWithoutEvents || ii.hasItems()))));
			typeFilterTree.getViewer().setExpandedTreePaths(expansion);
			typeFilterTree.getViewer().setSelection(selection);
			typeFilterTree.getViewer().getControl().setRedraw(true);
			typeFilterTree.getViewer().getControl().redraw();

			if (noTypesWereSelected) {
				// force re-interpretation of empty type selection
				rebuildItemList();
			}
		}

		private IItemCollection getFilteredItems() {
			if (!selectedTypes.isEmpty()) {
				Set<String> types = selectedTypes.stream().map(t -> t.getIdentifier()).collect(Collectors.toSet());
				return selectionItems.apply(ItemFilters.type(types));
			}
			return selectionItems;
		}

		private void onTypeChange() {
			Set<IType<?>> oldSelectedTypes = selectedTypes;
			selectedTypes = typeFilterTree.getSelectedTypes().map(EventTypeNode::getType).collect(Collectors.toSet());
			if (!Objects.equals(selectedTypes, oldSelectedTypes)) {
				container.showSelection(getFilteredItems());
				rebuildItemList();
			}
		}

		private void rebuildItemList() {
			mergeListSettings();

			Iterator<? extends IType<?>> types = selectedTypes.iterator();
			IItemCollection filteredItems = getFilteredItems();
			if (selectedTypes.isEmpty()) {
				types = ItemCollectionToolkit.stream(selectionItems).map(is -> is.getType()).distinct().iterator();
			}

			// FIXME: Possibly move to attribute toolkit/handler?
			// FIXME: Make sure to get Event Type as the first column
			// FIXME: Stream<IType> -> Stream<IAttribute> should be delegated to some context (e.g. the editor)
			Stream<IAttribute<?>> commonAttributes = Stream.empty();
			if (types.hasNext()) {
				List<IAttribute<?>> attributes = types.next().getAttributes();
				if (types.hasNext()) {
					while (types.hasNext()) {
						attributes = types.next().getAttributes().stream().filter(attributes::contains)
								.collect(Collectors.toList());
					}
					commonAttributes = attributes.stream();
				} else {
					commonAttributes = attributes.stream().filter(a -> !a.equals(JfrAttributes.EVENT_TYPE));
				}
				commonAttributes = commonAttributes.filter(a -> !a.equals(JfrAttributes.EVENT_STACKTRACE));
			}

			String orderBy = listOrderBy;
			Set<String> existingColumnIds = listColumns.stream().map(ColumnSettings::getId).collect(Collectors.toSet());
			List<ColumnSettings> newColumns = new ArrayList<>();
			ItemListBuilder itemListBuilder = new ItemListBuilder();
			commonAttributes.forEach(a -> {
				String combinedId = ItemList.getColumnId(a);
				ContentType<?> contentType = a.getContentType();
				IMemberAccessor<?, IItem> accessor = ItemToolkit.accessor(a);
				// FIXME: This is duplicated in JfrPropertySheet, where we also create a tooltip for an attribute.
				itemListBuilder.addColumn(combinedId, a.getName(),
						NLS.bind(Messages.ATTRIBUTE_ID_LABEL, a.getIdentifier()) + System.getProperty("line.separator") //$NON-NLS-1$
								+ NLS.bind(Messages.ATTRIBUTE_DESCRIPTION_LABEL, a.getDescription()),
						contentType instanceof LinearKindOfQuantity, accessor);
				if (combinedId.equals(listOrderBy)) {
					// the list now has the most current order, to allow the list to clear it
					listOrderBy = null;
				}
				if (!existingColumnIds.contains(combinedId)) {
					newColumns.add(0, new ColumnSettings(combinedId, false, null, null));
				}
			});
			listColumns.addAll(0, newColumns);

			Control oldListControl = list.getManager().getViewer().getControl();
			Composite parent = oldListControl.getParent();
			oldListControl.dispose();
			list = DataPageToolkit.createSimpleItemList(parent, itemListBuilder, container,
					DataPageToolkit.createTableSettingsByOrderByAndColumnsWithDefaultOrdering(orderBy, listColumns),
					Messages.EventBrowserPage_EVENT_BROWSER_SELECTION);
			parent.layout();
			list.show(filteredItems);
		}

		private void mergeListSettings() {
			TableSettings settings = list.getManager().getSettings();
			Set<String> columns = settings.getColumns().stream().map(ColumnSettings::getId).collect(Collectors.toSet());
			List<Integer> replaceIndexs = new ArrayList<>(columns.size());
			for (int i = 0; i < listColumns.size(); i++) {
				if (columns.contains(listColumns.get(i).getId())) {
					replaceIndexs.add(i);
				}
			}
			Iterator<ColumnSettings> replacements = settings.getColumns().iterator();
			Iterator<Integer> indexs = replaceIndexs.iterator();
			while (indexs.hasNext() && replacements.hasNext()) {
				listColumns.set(indexs.next(), replacements.next());
			}
			if (settings.getOrderBy() != null) {
				listOrderBy = settings.getOrderBy();
			}
		}

		@Override
		public void saveTo(IWritableState state) {
			PersistableSashForm.saveState(treeSash, state.createChild(TREE_SASH));
			mergeListSettings();
			new TableSettings(listOrderBy, listColumns).saveState(state.createChild(ITEM_LIST));
			StateToolkit.writeBoolean(state, SHOW_TYPES_WITHOUT_EVENTS, showTypesWithoutEvents);
			saveToLocal();
		}

		private void saveToLocal() {
			treeSelection = typeFilterTree.getViewer().getSelection();
			treeExpansion = typeFilterTree.getViewer().getExpandedTreePaths();
			// FIXME: indexOf doesn't seem to work for some reason, probably an SWT bug
//			topIndex = typeFilterTree.getViewer().getTree().indexOf(typeFilterTree.getViewer().getTree().getTopItem());
			tableSelection = list.getManager().getSelectionState();
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}
	}
}
