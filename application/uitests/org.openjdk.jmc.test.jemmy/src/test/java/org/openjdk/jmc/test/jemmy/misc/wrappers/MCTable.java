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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jemmy.Point;
import org.jemmy.control.Wrap;
import org.jemmy.input.StringPopupOwner;
import org.jemmy.input.StringPopupSelectableOwner;
import org.jemmy.interfaces.Keyboard.KeyboardButtons;
import org.jemmy.interfaces.Keyboard.KeyboardModifiers;
import org.jemmy.interfaces.Parent;
import org.jemmy.interfaces.Selectable;
import org.jemmy.lookup.Lookup;
import org.jemmy.resources.StringComparePolicy;
import org.jemmy.swt.ItemWrap;
import org.jemmy.swt.TableWrap;
import org.jemmy.swt.lookup.ByName;
import org.junit.Assert;

import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;

/**
 * The Jemmy base wrapper for tables
 */
public class MCTable extends MCJemmyBase {

	/**
	 * A small representation of a row in a table, contains both the row text and a list of strings
	 * representing all cells in the row. If no tests actually require this, we should change the
	 * scope of this inner class to private or, at least, package private.
	 */
	public class TableRow {

		private final String text;
		private final List<String> columnTexts;
		private Map<String, Integer> columnNameMap;

		TableRow(String text, List<String> columns, Map<String, Integer> columnNameMap) {
			this.text = text;
			columnTexts = columns;
			this.columnNameMap = columnNameMap;
		}

		/**
		 * @param text
		 *            The text, separate from any column texts, to match
		 * @return {@code true} if the text matches that of this TableRow
		 */
		boolean hasText(String text) {
			return policy.compare(text, this.text);
		}

		/**
		 * @param text
		 *            the text, separate from any column texts, to match
		 * @param policy
		 *            the policy to use when matching
		 * @return {@code true} if the text matches that of this {@link TableRow}
		 */
		boolean hasText(String text, StringComparePolicy policy) {
			return policy.compare(text, this.text);
		}

		/**
		 * @param text
		 *            the text to be found
		 * @return whether or not the text has been found in any column
		 */
		boolean hasColumnText(String text) {
			return hasColumnText(text, policy);
		}

		/**
		 * @param text
		 *            the text to be found
		 * @param policy
		 *            the policy to use when matching
		 * @return whether or not the text has been found in any column
		 */
		boolean hasColumnText(String text, StringComparePolicy policy) {
			for (String col : columnTexts) {
				if (policy.compare(text, col)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * @return the text of a row.
		 */
		public String getText() {
			return text;
		}

		/**
		 * Returns the row text for the provided column index
		 *
		 * @param columnIndex
		 *            the column index
		 * @return the text of the field of the provided column
		 */
		public String getText(int columnIndex) {
			return columnTexts.get(columnIndex);
		}

		/**
		 * Returns the row text for the provided column header
		 *
		 * @param columnHeader
		 *            the string header of the column
		 * @return the text of the field of the provided column
		 */
		public String getText(String columnHeader) {
			return columnTexts.get(columnNameMap.get(columnHeader));
		}

		/**
		 * @return the texts in the columns of a row
		 */
		public List<String> getColumns() {
			return columnTexts;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(text);
			sb.append(":[");
			for (String col : columnTexts) {
				sb.append(col);
				sb.append(' ');
			}
			sb.append("]");
			return sb.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof TableRow)) {
				return false;
			}
			return toString().equals(((TableRow) o).toString());
		}

		public Map<String, Integer> getColumnNameMap() {
			return columnNameMap;
		}
	}

	/**
	 * The policy used in comparisons in McTables
	 */
	public static StringComparePolicy policy = StringComparePolicy.SUBSTRING;

	private MCTable(Wrap<? extends Table> tableWrap) {
		this.control = tableWrap;
	}

	/**
	 * @return a list of all the tables in the default shell.
	 */
	public static List<MCTable> getAll() {
		return getAll(getShell());
	}

	/**
	 * Returns all currently visible tables as McTables in a list.
	 *
	 * @param shell
	 *            the shell to search for tables.
	 * @return a {@link List} of {@link MCTable}
	 */
	public static List<MCTable> getAll(Wrap<? extends Shell> shell) {
		return getAll(shell, true);
	}

	/**
	 * Returns all currently visible tables as McTables in a list.
	 *
	 * @param shell
	 *            the shell to search for tables.
	 * @param waitForIdle
	 *            {@code true} if supposed to wait for the UI to be idle before performing the
	 *            lookup
	 * @return a {@link List} of {@link MCTable}
	 */
	@SuppressWarnings("unchecked")
	public static List<MCTable> getAll(Wrap<? extends Shell> shell, boolean waitForIdle) {
		List<Wrap<? extends Table>> list = getVisible(shell.as(Parent.class, Table.class).lookup(Table.class),
				waitForIdle, false);
		List<MCTable> tables = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			tables.add(new MCTable(list.get(i)));
		}
		return tables;
	}

	/**
	 * Returns all currently visible tables as {@link MCTable} in a list.
	 *
	 * @param dialog
	 *            the {@link MCDialog} to search for tables.
	 * @return a {@link List} of {@link MCTable}
	 */
	public static List<MCTable> getAll(MCDialog dialog) {
		return getAll(dialog.getDialogShell());
	}

	/**
	 * Finds tables by index, generally you should not use this method, but rather get all tables
	 * and keep the list up-to-date.
	 *
	 * @param shell
	 *            the shell to search
	 * @param index
	 *            the index in the list of tables
	 * @return the {@link MCTable} representing the table at the specified index, or {@code null}
	 *         if index is out of range
	 */
	@SuppressWarnings("unchecked")
	static MCTable getByIndex(Wrap<? extends Shell> shell, int index) {
		Lookup<Table> lookup = shell.as(Parent.class, Table.class).lookup(Table.class);
		return (index < lookup.size()) ? new MCTable(lookup.wrap(index)) : null;
	}

	/**
	 * Finds tables by column header (first match only)
	 *
	 * @param headerName
	 *            the name of the column header
	 * @return a {@link MCTable}
	 */
	public static MCTable getByColumnHeader(String headerName) {
		return getByColumnHeader(getShell(), headerName);
	}

	/**
	 * Finds tables by column header (first match only)
	 *
	 * @param shell
	 *            the shell in which to look for the table
	 * @param headerName
	 *            the name of the column header
	 * @return a {@link MCTable}
	 */
	public static MCTable getByColumnHeader(Wrap<? extends Shell> shell, String headerName) {
		for (MCTable table : getAll(shell)) {
			if (table.getColumnIndex(headerName) != null) {
				return table;
			}
		}
		return null;
	}

	/**
	 * Finds a table by name (data set by the key "name")
	 *
	 * @param name
	 *            the name of the table
	 * @return a {@link MCTable}
	 */
	public static MCTable getByName(String name) {
		return getByName(getShell(), name);
	}

	/**
	 * Finds a table by name (data set by the key "name") that is child of the provided dialog
	 *
	 * @param dialog
	 *            the dialog from where to start the search (ancestor)
	 * @param name
	 *            the name of the table
	 * @return a {@link MCTable}
	 */
	public static MCTable getByName(MCDialog dialog, String name) {
		return getByName(dialog.getDialogShell(), name);
	}

	/**
	 * Finds a table by name (data set by the key "name") that is child of the provided shell
	 *
	 * @param shell
	 *            the shell from where to start the search (ancestor)
	 * @param name
	 *            the name of the table
	 * @return a {@link MCTable}
	 */
	@SuppressWarnings("unchecked")
	public static MCTable getByName(Wrap<? extends Shell> shell, String name) {
		return new MCTable(shell.as(Parent.class, Table.class)
				.lookup(Table.class, new ByName<>(name, StringComparePolicy.EXACT)).wrap());
	}

	/**
	 * Returns a List of string lists containing the table's complete table item text values.
	 *
	 * @return a {@link List} of {@link List} of {@link String}
	 */
	public List<List<String>> getAllColumnItemTexts() {
		List<List<String>> result = new ArrayList<>();
		for (TableRow tableRow : getRows()) {
			result.add(tableRow.getColumns());
		}
		return result;
	}

	/**
	 * Returns a column from a table
	 *
	 * @param columnId
	 *            the column to get
	 * @return the requested column's text value(s)
	 */
	public List<String> getColumnItemTexts(int columnId) {
		List<String> column = new ArrayList<>();
		for (TableRow row : getRows()) {
			column.add(row.getText(columnId));
		}
		return column;
	}

	/**
	 * Returns a column from a table
	 *
	 * @param columnHeader
	 *            the column to get
	 * @return the requested column's text value(s)
	 */
	public List<String> getColumnItemTexts(String columnHeader) {
		List<String> column = new ArrayList<>();
		for (TableRow row : getRows()) {
			column.add(row.getText(columnHeader));
		}
		return column;
	}

	/**
	 * @param columnHeader
	 *            the header of the column
	 * @return the index of the column
	 */
	public Integer getColumnIndex(String columnHeader) {
		return getColumnNameMap().get(columnHeader);
	}

	private Map<String, Integer> getColumnNameMap() {
		final Table table = getWrap().getControl();
		Fetcher<Map<String, Integer>> fetcher = new Fetcher<Map<String, Integer>>() {
			@Override
			public void run() {
				TableColumn[] tableColumns = table.getColumns();
				Map<String, Integer> columnNameMap = new HashMap<>();
				int columnIndex = 0;
				for (TableColumn tc : tableColumns) {
					columnNameMap.put(tc.getText(), columnIndex);
					columnIndex++;
				}
				setOutput(columnNameMap);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Returns a list of strings for the table item of the specified index.
	 *
	 * @param rowIndex
	 *            the index of the item to get the text for
	 * @return a {@link List} of {@link String}
	 */
	public List<String> getItemTexts(int rowIndex) {
		TableRow row = getRow(rowIndex);
		return row.getColumns();
	}

	/**
	 * Gets a TableRow for the row index provided.
	 *
	 * @param index
	 *            the index of the row to get data from
	 * @return a {@link TableRow} with the data from the table row
	 */
	public TableRow getRow(int index) {
		return getRow(index, getColumnNameMap());
	}

	/**
	 * Gets a TableRow for the row index provided.
	 *
	 * @param index
	 *            the index of the row to get data from
	 * @param columnNameMap
	 *            a map of the columns' headers and indexes
	 * @return a {@link TableRow} with the data from the table row
	 */
	public TableRow getRow(int index, Map<String, Integer> columnNameMap) {
		final Table table = getWrap().getControl();
		Fetcher<TableRow> fetcher = new Fetcher<TableRow>() {
			@Override
			public void run() {
				int columns = columnNameMap.size();
				TableRow output;
				TableItem item = table.getItem(index);
				String text = item.getText();
				List<String> texts = new ArrayList<>();
				for (int i = 0; i < columns; i++) {
					texts.add(item.getText(i));
				}
				output = new TableRow(text, texts, columnNameMap);
				setOutput(output);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Gets all the row and column data of the table
	 *
	 * @return a {@link List} of {@link TableRow}
	 */
	public List<TableRow> getRows() {
		int numberOfItems = this.getItemCount();
		List<TableRow> allRows = new ArrayList<>();

		Map<String, Integer> columnNameMap = getColumnNameMap();
		for (int i = 0; i < numberOfItems; i++) {
			allRows.add(getRow(i, columnNameMap));
		}

		return allRows;
	}

	/**
	 * Gets an Image for a specific row of the table
	 *
	 * @param rowIndex
	 *            index of the row to get
	 * @return an {@link Image}
	 */
	public Image getItemImage(int rowIndex) {
		final Table table = getWrap().getControl();
		Fetcher<Image> fetcher = new Fetcher<Image>() {
			@Override
			public void run() {
				TableItem item = table.getItem(rowIndex);
				Image icon = item.getImage();
				setOutput(icon);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Gets the number of items in the table
	 *
	 * @return the number of items in the table
	 */
	public int getItemCount() {
		final Table table = getWrap().getControl();
		Fetcher<Integer> fetcher = new Fetcher<Integer>() {
			@Override
			public void run() {
				int count = table.getItemCount();
				setOutput(count);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput().intValue();
	}

	/**
	 * Whether or not the table contains the text given
	 *
	 * @param item
	 *            the text
	 * @return {@code true} if found.
	 */
	public boolean hasItem(String item) {
		return (getItemIndex(item) != -1) ? true : false;
	}

	/**
	 * Returns the number of (exactly) matching table items
	 *
	 * @param itemText
	 *            the text
	 * @return the number of matching items in the table
	 */
	public int numberOfMatchingItems(String itemText) {
		return numberOfMatchingItems(itemText, StringComparePolicy.EXACT);
	}

	/**
	 * Returns the number of matching table items
	 *
	 * @param itemText
	 *            the text of the items to match
	 * @param policy
	 *            the policy to use when matching
	 * @return the number of matching items in the table
	 */
	public int numberOfMatchingItems(String itemText, StringComparePolicy policy) {
		return getItemIndexes(itemText, policy).size();
	}

	/**
	 * Returns the indexes of matching table items (Exact matching)
	 *
	 * @param itemText
	 *            the text of the items to match
	 * @return a {@link List} of {@link Integer} of the matching indexes
	 */
	public List<Integer> getItemIndexes(String itemText) {
		return getItemIndexes(itemText, StringComparePolicy.EXACT);
	}

	/**
	 * Returns the indexes of matching table items
	 *
	 * @param itemText
	 *            the text of the matching table item
	 * @param policy
	 *            the matching policy to use
	 * @return a {@link List} of {@link Integer} of the matching indexes
	 */
	public List<Integer> getItemIndexes(String itemText, StringComparePolicy policy) {
		List<TableRow> rows = getRows();
		List<Integer> index = new ArrayList<>();
		for (int i = 0; i < rows.size(); i++) {
			TableRow row = rows.get(i);
			if (row.hasColumnText(itemText, policy) || row.hasText(itemText, policy)) {
				index.add(i);
			}
		}
		return index;
	}

	/**
	 * Selects the given item (if found). This could also be done using the Selector of the Table
	 * (like "tableWrap.as(Selectable.class).selector().select(goalIndex)") but there seems to be an
	 * issue with TableItem.getBounds() on OS X where we run into some nasty ArrayIndexOutOfBounds
	 * exceptions because that code relies on mouse().click(). Another drawback with that approach
	 * is that we might actually be trying to click outside of what's visible. Keyboard navigation
	 * is safer so the Jemmy IndexItemSelector class (as well as TextItemSelector) should be fixed
	 * to do that instead
	 *
	 * @param item
	 *            the item to select
	 */
	public void select(String item) {
		Assert.assertTrue("Unable to select " + item + ".", select(getItemIndex(item)));
	}

	/**
	 * Selects the given item (if found). This could also be done using the Selector of the Table
	 * (like "tableWrap.as(Selectable.class).selector().select(goalIndex)") but there seems to be an
	 * issue with TableItem.getBounds() on OS X where we run into some nasty ArrayIndexOutOfBounds
	 * exceptions because that code relies on mouse().click(). Another drawback with that approach
	 * is that we might actually be trying to click outside of what's visible. Keyboard navigation
	 * is safer so the Jemmy IndexItemSelector class (as well as TextItemSelector) should be fixed
	 * to do that instead
	 *
	 * @param item
	 *            the item to select
	 * @param columnIndex
	 *            the column index to select
	 */
	public void select(String item, int columnIndex) {
		Assert.assertTrue("Unable to select " + item + ".", select(getItemIndex(item), columnIndex));
	}

	/**
	 * Performs a mouse click at a specified column index of an item
	 * 
	 * @param item
	 *            the item to click
	 * @param columnIndex
	 *            the column index where to click
	 */
	public void clickItem(String item, int columnIndex) {
		select(getItemIndex(item), columnIndex);
		scrollbarSafeSelection();
		control.mouse().click(1, getRelativeClickPoint(getSelectedItem(), columnIndex));
	}

	/**
	 * Performs a mouse click at a specified column header's index of an item
	 * 
	 * @param item
	 *            the item to click
	 * @param columnHeader
	 *            the column header
	 */
	public void clickItem(String item, String columnHeader) {
		clickItem(item, getColumnIndex(columnHeader));
	}

	/**
	 * Selects the given item (if found). This could also be done using the Selector of the Table
	 * (like "tableWrap.as(Selectable.class).selector().select(goalIndex)") but there seems to be an
	 * issue with TableItem.getBounds() on OS X where we run into ArrayIndexOutOfBounds exceptions
	 * because that code relies on mouse().click(). Another drawback with that approach is that we
	 * might actually be trying to click outside of what's visible. Keyboard navigation is safer so
	 * the Jemmy IndexItemSelector class (as well as TextItemSelector) should be fixed to do that
	 * instead
	 *
	 * @param item
	 *            the item to select
	 * @param columnHeader
	 *            the column header to select
	 */
	public void select(String item, String columnHeader) {
		Assert.assertTrue("Unable to select " + item + ".", select(getItemIndex(item), getColumnIndex(columnHeader)));
	}

	/**
	 * Selects the given item (if found). This could also be done using the Selector of the Table
	 * (like "tableWrap.as(Selectable.class).selector().select(goalIndex)") but there seems to be an
	 * issue with TableItem.getBounds() on OS X where we run into ArrayIndexOutOfBounds exceptions
	 * because that code relies on mouse().click(). Another drawback with that approach is that we
	 * might actually be trying to click outside of what's visible. Keyboard navigation is safer so
	 * the Jemmy IndexItemSelector class (as well as TextItemSelector) should be fixed to do that
	 * instead
	 *
	 * @param item
	 *            the item to select
	 * @param exactMatching
	 *            if {@code true} {@link StringComparePolicy.EXACT} is used. Otherwise
	 *            {@link StringComparePolicy.SUBSTRING} will be used
	 */
	public void select(String item, boolean exactMatching) {
		StringComparePolicy thisPolicy = (exactMatching) ? StringComparePolicy.EXACT : StringComparePolicy.SUBSTRING;
		Assert.assertTrue("Unable to select " + item + ".", select(getItemIndex(item, thisPolicy)));
	}

	/**
	 * Selects the item at the given index (if not -1)). Will retry the selection a maximum number
	 * of three times just to make sure that lost and regained focus doesn't break things
	 *
	 * @param index
	 *            the index of the item
	 * @param columnIndex
	 *            the column index of the item to select
	 * @return {@code true} if selected index is the same as the provided. {@code false} otherwise
	 */
	public boolean select(int index, int columnIndex) {
		if (index != -1) {
			ensureFocus();
			int maxRetries = 3;
			while (control.getProperty(Integer.class, Selectable.STATE_PROP_NAME) != index && maxRetries > 0) {
				maxRetries--;
				int startIndex = control.getProperty(Integer.class, Selectable.STATE_PROP_NAME);
				if (startIndex == -1) {
					control.keyboard().pushKey(KeyboardButtons.DOWN);
					control.keyboard().pushKey(KeyboardButtons.UP);
					startIndex = control.getProperty(Integer.class, Selectable.STATE_PROP_NAME);
				}
				if (startIndex != -1) {
					int steps = index - startIndex;
					KeyboardButtons stepButton = (index > startIndex) ? KeyboardButtons.DOWN : KeyboardButtons.UP;
					for (int i = 0; i < Math.abs(steps); i++) {
						control.keyboard().pushKey(stepButton);
					}
					// if we have a column > 0 do some side stepping
					for (int i = 0; i < columnIndex; i++) {
						control.keyboard().pushKey(KeyboardButtons.RIGHT);
					}
				}
			}
			return (control.getProperty(Integer.class, Selectable.STATE_PROP_NAME) == index && index != -1);
		} else {
			return false;
		}
	}

	/**
	 * Selects the item at the given index (if not -1))
	 *
	 * @param index
	 *            the index of the item
	 * @return {@code true} if selected index is the same as the provided. {@code false} otherwise
	 */
	public boolean select(int index) {
		return select(index, 0);
	}

	/**
	 * Selects the table row at a specified "start" index, and uses SHIFT+DOWN to
	 * add to the selection up until a specified "end" index
	 *
	 * @param from
	 *            the row index to start from
	 * @param to
	 *            the row index to stop selecting
	 */
	public void selectItems(int start, int end) {
		focusMc();
		select(start);
		for (int i = 0; i < end; i++) {
			getShell().keyboard().pushKey(KeyboardButtons.DOWN, new KeyboardModifiers[] {KeyboardModifiers.SHIFT_DOWN_MASK});
		}
	}

	/**
	 * Context clicks the currently selected table item and chooses the supplied option
	 *
	 * @param desiredState
	 *            the selection state to which the context choice is to be to set to
	 * @param choice
	 *            the context menu path to the option
	 */
	@SuppressWarnings("unchecked")
	public void contextChoose(boolean desiredState, String ... choice) {
		scrollbarSafeSelection();
		StringPopupSelectableOwner<Table> spo = control.as(StringPopupSelectableOwner.class);
		spo.setPolicy(policy);
		spo.push(desiredState, getRelativeClickPoint(getSelectedItem()), choice);
	}

	/**
	 * Context clicks the currently selected table item and finds out the selection status of the
	 * supplied option
	 *
	 * @param choice
	 *            the context menu path to the option
	 * @return the selection status of the item
	 */
	@SuppressWarnings("unchecked")
	public boolean getContextOptionState(String ... choice) {
		scrollbarSafeSelection();
		StringPopupSelectableOwner<Table> spo = control.as(StringPopupSelectableOwner.class);
		spo.setPolicy(policy);
		return spo.getState(getRelativeClickPoint(getSelectedItem()), choice);
	}

	/**
	 * Context clicks the currently selected table item and chooses the supplied option
	 *
	 * @param choice
	 *            the context menu path to the option
	 */
	@SuppressWarnings("unchecked")
	public void contextChoose(String ... choice) {
		scrollbarSafeSelection();
		StringPopupOwner<Table> spo = control.as(StringPopupOwner.class);
		spo.setPolicy(policy);
		spo.push(getRelativeClickPoint(getSelectedItem()), choice);
	}

	private Wrap<? extends TableItem> getSelectedItem() {
		Fetcher<TableItem> fetcher = new Fetcher<TableItem>() {
			@Override
			public void run() {
				setOutput(getWrap().getControl().getSelection()[0]);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return new ItemWrap<>(getWrap(), fetcher.getOutput());
	}

	/**
	 * Calculates the click point of the child relative to the parent provided. Uses a rather
	 * cumbersome way of getting the bounds because {@link ArrayIndexOutOfBoundsException} in some
	 * cases getting thrown on Mac OS X.
	 *
	 * @param child
	 *            the wrapped child control
	 * @return the {@link Point} of the child relative to the parent
	 */
	private Point getRelativeClickPoint(final Wrap<? extends TableItem> child) {
		return getRelativeClickPoint(child, null);
	}

	/**
	 * Calculates the click point of the child relative to the parent. Uses a rather cumbersome way
	 * of getting the bounds because {@link ArrayIndexOutOfBoundsException} in some cases getting
	 * thrown on Mac OS X.
	 *
	 * @param child
	 *            the wrapped child control
	 * @param columnIndex
	 *            the column index of the table item for which to get the click point. May be null
	 *            if no column
	 * @return the {@link Point} of the child relative to the parent
	 */
	private Point getRelativeClickPoint(final Wrap<? extends TableItem> child, final Integer columnIndex) {
		Fetcher<Point> fetcher = new Fetcher<Point>() {
			@Override
			public void run() {
				Rectangle childRect = null;
				if (columnIndex != null) {
					childRect = child.getControl().getBounds(columnIndex);
				} else {
					try {
						childRect = child.getControl().getBounds();
					} catch (ArrayIndexOutOfBoundsException e) {
						childRect = child.getControl().getBounds(0);
					}
				}
				setOutput(new Point(childRect.x + childRect.width / 2, childRect.y + childRect.height / 2));
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	private int getItemIndex(String itemText) {
		return getItemIndex(itemText, policy);
	}

	private int getItemIndex(String itemText, StringComparePolicy policy) {
		List<TableRow> rows = getRows();
		int index = -1;
		for (int i = 0; i < rows.size(); i++) {
			TableRow row = rows.get(i);
			if (row.hasColumnText(itemText, policy) || row.hasText(itemText, policy)) {
				index = i;
				break;
			}
		}
		return index;
	}

	@SuppressWarnings("unchecked")
	private Wrap<? extends Table> getWrap() {
		return control.as(TableWrap.class);
	}

	private void scrollbarSafeSelection() {
		int index = control.getProperty(Integer.class, Selectable.STATE_PROP_NAME);
		control.keyboard().pushKey(KeyboardButtons.DOWN);
		control.keyboard().pushKey(KeyboardButtons.UP);
		select(index);
	}
}
