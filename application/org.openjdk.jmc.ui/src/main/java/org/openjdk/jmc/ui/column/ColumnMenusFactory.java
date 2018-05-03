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
package org.openjdk.jmc.ui.column;

import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.openjdk.jmc.ui.column.ColumnManager.IColumnState;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.CopySettings;

public class ColumnMenusFactory {

	public static void addDefaultMenus(ColumnManager columnManager, MCContextMenuManager manager) {
		manager.appendToGroup(MCContextMenuManager.GROUP_VIEWER_SETUP, sortMenu(columnManager));
		manager.appendToGroup(MCContextMenuManager.GROUP_VIEWER_SETUP, visibilityMenu(columnManager));
		Function<Boolean, Stream<? extends IColumn>> columns = onlyVisible -> getColumns(onlyVisible,
				columnManager.getColumnStates());
		IAction copyColumnsAction = CopyColumnsAction.build(columnManager.getViewer(), columns);
		manager.appendToGroup(MCContextMenuManager.GROUP_EDIT, copyColumnsAction);
		InFocusHandlerActivator.install(columnManager.getViewer().getControl(), copyColumnsAction);
		manager.appendToGroup(MCContextMenuManager.GROUP_EDIT, CopySettings.getInstance().createContributionItem());
	}

	private static Stream<? extends IColumn> getColumns(
		Boolean onlyVisible, Stream<? extends IColumnState> columnStates) {
		if (onlyVisible) {
			columnStates = columnStates.filter(IColumnState::isVisible);
		}
		return columnStates.map(IColumnState::getColumn);
	}

	public static MenuManager visibilityMenu(ColumnManager columnManager) {
		MenuManager menu = new MenuManager(Messages.VISIBILE_COLUMNS_MENU_HEADER);

		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(new IMenuListener() {

			private Action checked = null;
			private int checkedCount = 0;

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				checked = null;
				checkedCount = 0;
				columnManager.getColumnStates().map(this::createVisibilityAction).forEach(manager::add);
				if (checkedCount == 1 && checked != null) {
					// Only one column visible according to createVisibilityAction
					checked.setEnabled(false);
				}
			}

			private Action createVisibilityAction(IColumnState state) {
				Action a = new Action(state.getColumn().getName(), IAction.AS_CHECK_BOX) {

					@Override
					public void run() {
						columnManager.setColumnHidden(state.getColumn().getId(), !isChecked());
					}
				};
				if (state.isVisible()) {
					a.setChecked(true);
					checked = a;
					checkedCount++;
				}
				return a;
			}

		});
		return menu;
	}

	public static MenuManager sortMenu(ColumnManager columnManager) {
		MenuManager menu = new MenuManager(Messages.SORT_COLUMNS_MENU_HEADER);

		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(createNoSortAction());
				columnManager.getColumnStates().filter(IColumnState::isVisible).map(IColumnState::getColumn)
						.map(this::createSortMenu).forEach(manager::add);
			}

			private MenuManager createSortMenu(IColumn column) {
				MenuManager sortColumnMenu = new MenuManager(column.getName(), column.getId());
				sortColumnMenu.add(createSortAction(true, column.getId()));
				sortColumnMenu.add(createSortAction(false, column.getId()));
				return sortColumnMenu;
			}

			private Action createSortAction(final boolean sortAscending, final String columnId) {
				String text = sortAscending ? Messages.SORT_COLUMN_ASCENDING_MENU_ITEM
						: Messages.SORT_COLUMN_DESCENDING_MENU_ITEM;
				return new Action(text) {
					@Override
					public void run() {
						columnManager.setSortColumn(columnId, sortAscending);
					}
				};
			}

			private Action createNoSortAction() {
				return new Action(Messages.SORT_COLUMN_NONE_MENU_ITEM) {
					@Override
					public void run() {
						columnManager.clearSortColumn();
					}
				};
			}
		});
		return menu;
	}

}
