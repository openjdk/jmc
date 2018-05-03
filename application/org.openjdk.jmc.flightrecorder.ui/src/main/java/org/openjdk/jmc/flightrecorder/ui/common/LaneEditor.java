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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.forms.widgets.FormText;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemFilters.Types;
import org.openjdk.jmc.common.item.PersistableItemFilter;
import org.openjdk.jmc.common.util.PredicateToolkit;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.ui.EventTypeFolderNode;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.ActionUiToolkit;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;
import org.openjdk.jmc.ui.wizards.IPerformFinishable;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

public class LaneEditor {

	private static final IItemFilter TYPE_HAS_THREAD_AND_DURATION = new IItemFilter() {
		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			if (DataPageToolkit.isTypeWithThreadAndDuration(type)) {
				return PredicateToolkit.truePredicate();
			}
			return PredicateToolkit.falsePredicate();
		}
	};

	private static class EditLanesWizardPage extends WizardPage implements IPerformFinishable {

		private final EventTypeFolderNode root;
		private final List<LaneDefinition> lanes;
		private LaneDefinition restLane;
		private TypeFilterBuilder filterEditor;
		private CheckboxTableViewer lanesViewer;
		private Object selected;

		private EditLanesWizardPage(EventTypeFolderNode root, Collection<LaneDefinition> lanesInput) {
			super("EditFilterLanesPage"); //$NON-NLS-1$
			this.root = root;
			this.lanes = new ArrayList<>(lanesInput);
			restLane = ensureRestLane(lanes);
		}

		@Override
		public void createControl(Composite parent) {
			// FIXME: Do we want to group under categories somehow, or just hide the filters that don't have any existing event types.
			Composite container = new Composite(parent, SWT.NONE);
			container.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());

			Composite laneHeaderContainer = new Composite(container, SWT.NONE);
			laneHeaderContainer.setLayout(GridLayoutFactory.swtDefaults().create());
			laneHeaderContainer.setLayoutData(GridDataFactory.fillDefaults().create());

			// FIXME: Add a duplicate action?
			IAction moveUpAction = ActionToolkit.action(() -> moveSelected(true), Messages.LANES_MOVE_UP_ACTION,
					UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_NAV_UP));
			IAction moveDownAction = ActionToolkit.action(() -> moveSelected(false), Messages.LANES_MOVE_DOWN_ACTION,
					UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_NAV_DOWN));
			IAction addAction = ActionToolkit.action(this::addLane, Messages.LANES_ADD_LANE_ACTION,
					UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_ADD));
			IAction removeAction = ActionToolkit.commandAction(this::deleteSelected,
					IWorkbenchCommandConstants.EDIT_DELETE);
			Control toolbar = ActionUiToolkit.buildToolBar(laneHeaderContainer,
					Stream.of(moveUpAction, moveDownAction, addAction, removeAction), false);
			toolbar.setLayoutData(GridDataFactory.fillDefaults().create());

			Label lanesTitle = new Label(laneHeaderContainer, SWT.NONE);
			lanesTitle.setText(Messages.LANES_EDITOR_LABEL);
			lanesTitle.setLayoutData(GridDataFactory.fillDefaults().create());
			Label filterTitle = new Label(container, SWT.NONE);
			filterTitle.setText(Messages.LANES_FILTER_LABEL);
			filterTitle.setLayoutData(
					GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.END).create());

			lanesViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.V_SCROLL);
			TableViewerColumn viewerColumn = new TableViewerColumn(lanesViewer, SWT.NONE);
			viewerColumn.getColumn().setText(Messages.LANES_LANE_COLUMN);
			viewerColumn.getColumn().setWidth(200);
			// FIXME: Would like to enable editing by some other means than single-clicking, but seems a bit tricky.
			viewerColumn.setEditingSupport(new EditingSupport(lanesViewer) {

				private String currentName;

				@Override
				protected void setValue(Object element, Object value) {
					String newName = value.toString();
					if (currentName != null && currentName.equals(newName)) {
						return;
					}
					LaneDefinition oldLd = (LaneDefinition) element;
					LaneDefinition newLane = new LaneDefinition(value.toString(), oldLd.enabled, oldLd.filter,
							oldLd.isRestLane);
					int elementIndex = lanes.indexOf(element);
					lanes.set(elementIndex, newLane);
					lanesViewer.replace(newLane, elementIndex);
					getViewer().update(element, null);
				}

				@Override
				protected Object getValue(Object element) {
					currentName = ((LaneDefinition) element).getName();
					return currentName;
				}

				@Override
				protected CellEditor getCellEditor(Object element) {
					return new TextCellEditor((Composite) getViewer().getControl());
				}

				@Override
				protected boolean canEdit(Object element) {
					return true;

				}
			});

			lanesViewer.setLabelProvider(new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					if (element instanceof LaneDefinition) {
						if (element == selected) {
							return ((LaneDefinition) element).getNameOrCount(filterEditor.getCheckedTypeIds().count());
						} else {
							return ((LaneDefinition) element).getName();
						}
					}
					return super.getText(element);
				};

				// FIXME: Do we want to use italics for empty lanes?
//				@Override
//				public Font getFont(Object element) {
//					if (getTypesCount(element) > 0) {
//						return JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT);
//					} else {
//						return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
//					}
//				}
//
//				private long getTypesCount(Object element) {
//					if (element == selected) {
//						return filterEditor.getCheckedTypeIds().count();
//					} else if (element instanceof LaneDefinition) {
//						return ((LaneDefinition)element).getTypesCount();
//					}
//					return 0;
//				}
			});
			lanesViewer.setContentProvider(ArrayContentProvider.getInstance());
			// FIXME: Can we potentially reuse this tooltip in the legend as well?
			new ToolTip(lanesViewer.getControl(), ToolTip.NO_RECREATE, false) {

				@Override
				protected ViewerCell getToolTipArea(Event event) {
					return lanesViewer.getCell(new Point(event.x, event.y));
				}

				@Override
				protected Composite createToolTipContentArea(Event event, Composite parent) {
					FormText formText = CompositeToolkit.createInfoFormText(parent);
					Object element = getToolTipArea(event).getElement();
					Stream<String> ids = Stream.empty();
					if (element == selected) {
						ids = filterEditor.getCheckedTypeIds();
					} else if (element instanceof LaneDefinition
							&& ((LaneDefinition) element).filter instanceof Types) {
						ids = ((Types) ((LaneDefinition) element).filter).getTypes().stream();
					}
					StringBuilder sb = new StringBuilder();
					ids.forEach(typeId -> {
						Color color = TypeLabelProvider.getColorOrDefault(typeId);
						formText.setImage(typeId, SWTColorToolkit.getColorThumbnail(SWTColorToolkit.asRGB(color)));
						sb.append("<li style='image' value='" + typeId + "'>" + typeId + "</li>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					});
					if (sb.length() > 0) {
						sb.insert(0, "<form>"); //$NON-NLS-1$
						sb.append("</form>"); //$NON-NLS-1$
						formText.setText(sb.toString(), true, false);
					} else {
						formText.setText(Messages.LANES_CHECK_TO_INCLUDE, false, false);
					}
					return formText;
				}
			};
			lanesViewer.setInput(lanes);
			lanesViewer.setCheckedElements(lanes.stream().filter(ld -> ld.isEnabled()).toArray());
			MCContextMenuManager mm = MCContextMenuManager.create(lanesViewer.getControl());
			mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, moveUpAction);
			mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, moveDownAction);
			// FIXME: Add icon
			mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, addAction);

			mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, removeAction);

			filterEditor = new TypeFilterBuilder(container, this::onTypeFilterChange);
			filterEditor.setInput(root);
			filterEditor.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
			lanesViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(false, true).create());

			lanesViewer.addSelectionChangedListener(
					e -> laneSelectionChanges(((IStructuredSelection) e.getSelection()).getFirstElement()));
			LaneDefinition firstLane = lanes.get(0);
			lanesViewer.setSelection(new StructuredSelection(firstLane));

			setControl(container);
		}

		private void addLane() {
			int selectIndex = Math.max(0, lanes.indexOf(selected));
			IItemFilter emptyFilter = ItemFilters.type(Collections.emptySet());
			LaneDefinition newEmpty = new LaneDefinition(null, false, emptyFilter, false);
			lanes.add(selectIndex + 1, newEmpty);
			lanesViewer.insert(newEmpty, selectIndex + 1);
			lanesViewer.setSelection(new StructuredSelection(newEmpty));
		}

		private void onTypeFilterChange() {
			if (selected instanceof LaneDefinition) {
				LaneDefinition selectedLane = (LaneDefinition) selected;
				if (selectedLane.isRestLane()) {
					DialogToolkit.showWarningDialogAsync(lanesViewer.getControl().getDisplay(),
							Messages.LANES_EDIT_NOT_ALLOWED_WARNING,
							NLS.bind(Messages.LANES_EDIT_NOT_ALLOWED_WARNING_DESC, selectedLane.getName()));
					// FIXME: Can we refresh the filter editor to show that nothing has changed?
				}
			}
			lanesViewer.update(selected, null);
		}

		private void deleteSelected() {
			// FIXME: It's currently not possible to delete the last lane
			int selectIndex = Math.max(0, lanes.indexOf(selected) - 1);
			if (selected instanceof LaneDefinition && ((LaneDefinition) selected).isRestLane()) {
				lanes.remove(selected);
				lanesViewer.setSelection(new StructuredSelection(lanes.get(selectIndex)));
				lanesViewer.refresh();
			} else {
				DialogToolkit.showWarningDialogAsync(lanesViewer.getControl().getDisplay(),
						Messages.LANES_DELETE_NOT_ALLOWED_WARNING, NLS.bind(
								Messages.LANES_DELETE_NOT_ALLOWED_WARNING_DESC, ((LaneDefinition) selected).getName()));
			}
		}

		private void moveSelected(boolean up) {
			int fromIndex = lanes.indexOf(selected);
			int toIndex = fromIndex + (up ? -1 : 1);
			if (fromIndex >= 0 && toIndex >= 0 && toIndex < lanes.size()) {
				LaneDefinition removed = lanes.remove(fromIndex);
				lanes.add(toIndex, removed);
				lanesViewer.refresh();
			}
		}

		private void laneSelectionChanges(Object newSelected) {
			int selectedIndex = lanes.indexOf(newSelected);
			if (this.selected != newSelected) {
				saveFilter();
				this.selected = lanes.get(selectedIndex);
				if (selected instanceof LaneDefinition) {
					Types typesFilter;
					if (((LaneDefinition) selected).getFilter() instanceof Types) {
						typesFilter = ((Types) ((LaneDefinition) selected).getFilter());
					} else {
						typesFilter = (Types) ItemFilters.convertToTypes(((LaneDefinition) selected).getFilter(),
								filterEditor.getAllTypes());
					}
					filterEditor.selectTypes(typesFilter.getTypes());
				}
			}
		}

		private void saveFilter() {
			int selectedIndex = lanes.indexOf(selected);
			if (selectedIndex >= 0) {
				LaneDefinition ld = lanes.get(selectedIndex);
				if (!ld.isRestLane()) {
					IItemFilter newFilter = ItemFilters
							.type(filterEditor.getCheckedTypeIds().collect(Collectors.toSet()));
					LaneDefinition newLd = new LaneDefinition(ld.name, lanesViewer.getChecked(ld), newFilter,
							ld.isRestLane);
					lanes.set(selectedIndex, newLd);
					lanesViewer.replace(newLd, selectedIndex);
					if (restLane != null) {
						LaneDefinition newRest = new LaneDefinition(restLane.name, restLane.enabled,
								getRestFilter(lanes), true);
						int restIndex = lanes.indexOf(restLane);
						lanes.set(restIndex, newRest);
						lanesViewer.replace(newRest, restIndex);
						restLane = newRest;
					}
					lanesViewer.refresh();
				}
			}
		}

		@Override
		public boolean performFinish() {
			saveFilter();
			for (int i = 0; i < lanes.size(); i++) {
				LaneDefinition ld = lanes.get(i);
				if (ld.isEnabled() != lanesViewer.getChecked(ld)) {
					lanes.set(i, new LaneDefinition(ld.name, lanesViewer.getChecked(ld), ld.filter, ld.isRestLane));
				}
			}
			return true;
		}
	}

	public static class LaneDefinition implements IDescribable, IStateful {

		private static final String FILTER = "filter"; //$NON-NLS-1$
		private static final String NAME = "name"; //$NON-NLS-1$
		private static final String ENABLED = "enabled"; //$NON-NLS-1$
		private static final String IS_REST_LANE = "isRestLane"; //$NON-NLS-1$

		private final String name;
		private final IItemFilter filter;
		private final boolean enabled;
		private final boolean isRestLane;

		public LaneDefinition(String name, boolean enabled, IItemFilter filter, boolean isRestLane) {
			this.name = name;
			this.enabled = enabled;
			this.filter = filter;
			this.isRestLane = isRestLane;
		}

		@Override
		public String getName() {
			long count = filter instanceof Types ? ((Types) filter).getTypes().size() : 0;
			return getNameOrCount(count);
		}

		public String getNameOrCount(long count) {
			return name != null ? name
					: count == 1 && ((Types) filter).getTypes().iterator().hasNext()
							? ((Types) filter).getTypes().iterator().next()
							: count > 0 ? NLS.bind(Messages.LANES_DEFINITION_NAME, count) : Messages.LANES_EMPTY_LANE;
		}

		@Override
		public String getDescription() {
			return NLS.bind(Messages.LANES_DEFINITION_DESC, getName());
		}

		public IItemFilter getFilter() {
			return filter;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public boolean isRestLane() {
			return isRestLane;
		}

		@Override
		public void saveTo(IWritableState writableState) {
			writableState.putString(NAME, name);
			StateToolkit.writeBoolean(writableState, ENABLED, enabled);
			StateToolkit.writeBoolean(writableState, IS_REST_LANE, isRestLane);
			if (!isRestLane && filter != null) {
				((PersistableItemFilter) filter).saveTo(writableState.createChild(FILTER));
			}
		}

		public static LaneDefinition readFrom(IState memento) {
			String name = memento.getAttribute(NAME);
			boolean enabled = StateToolkit.readBoolean(memento, ENABLED, false);
			boolean isRestLane = StateToolkit.readBoolean(memento, IS_REST_LANE, false);
			IState filterState = memento.getChild(FILTER);
			IItemFilter filter;
			if (isRestLane) {
				filter = null;
			} else if (filterState != null) {
				filter = PersistableItemFilter.readFrom(filterState);
			} else {
				throw new UnsupportedOperationException("Null filter not allowed for thread lane: " + name); //$NON-NLS-1$
			}
			// FIXME: Should probably warn if filter is not an instance of Types, and possibly handle other type filter variants as well, like TypeMatches.
			return new LaneDefinition(name, enabled, filter, isRestLane);
		}

		@Override
		public String toString() {
			return getName() + "(" + enabled + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static List<LaneDefinition> openDialog(
		EventTypeFolderNode root, List<LaneDefinition> lanes, String title, String description) {
		EditLanesWizardPage page = new EditLanesWizardPage(root, lanes);
		page.setTitle(title);
		page.setDescription(description);
		if (OnePageWizardDialog.open(page, 500, 600) == Window.OK) {
			return page.lanes.stream().filter(LaneEditor::laneIncludesTypes).collect(Collectors.toList());
		}
		return lanes;
	}

	private static boolean laneIncludesTypes(LaneDefinition ld) {
		return ld.isRestLane() || ld.getFilter() instanceof Types && ((Types) ld.getFilter()).getTypes().size() > 0;
	}

	private static IItemFilter getRestFilter(List<LaneDefinition> lanesInput) {
		List<IItemFilter> laneFilters = lanesInput.stream().filter(ld -> !ld.isRestLane).map(ld -> ld.getFilter())
				.collect(Collectors.toList());
		IItemFilter laneFilter = ItemFilters.or(laneFilters.toArray(new IItemFilter[laneFilters.size()]));
		return ItemFilters.and(ItemFilters.not(laneFilter), TYPE_HAS_THREAD_AND_DURATION);
	}

	public static LaneDefinition ensureRestLane(List<LaneDefinition> lanesInput) {
		// FIXME: Should we react if there are several rest lanes specified, or just ignore the other ones?
		LaneDefinition oldRestLane = lanesInput.stream().filter(ld -> ld.isRestLane).findAny().orElse(null);
		LaneDefinition newRestLane;
		IItemFilter restFilter = getRestFilter(lanesInput);
		if (oldRestLane == null) {
			newRestLane = new LaneDefinition(Messages.LANES_OTHER_TYPES, false, restFilter, true);
			lanesInput.add(newRestLane);
		} else {
			newRestLane = new LaneDefinition(oldRestLane.name, oldRestLane.enabled, restFilter, true);
			lanesInput.set(lanesInput.indexOf(oldRestLane), newRestLane);
		}
		return newRestLane;
	}
}
