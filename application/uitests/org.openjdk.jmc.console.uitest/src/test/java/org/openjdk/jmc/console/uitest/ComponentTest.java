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
package org.openjdk.jmc.console.uitest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JmxConsole;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTabFolder;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTree;

/**
 * Class for for Component related testing
 */
@SuppressWarnings("restriction")
public class ComponentTest extends MCJemmyTestBase {
	private static final String MBEANBROWSER_TREE_NAME = org.openjdk.jmc.console.ui.mbeanbrowser.tree.MBeanTreeSectionPart.MBEANBROWSER_MBEAN_TREE_NAME;
	private static final String ATTRIBUTES_TAB = org.openjdk.jmc.console.ui.mbeanbrowser.tab.Messages.FeatureSectionPart_ATTRIBUTES_TAB_TITLE_TEXT;
	private static final String ATTRIBUTES_TREE_NAME = org.openjdk.jmc.rjmx.ui.attributes.MRIAttributeInspector.MBEANBROWSER_ATTRIBUTESTAB_ATTRIBUTESTREE_NAME;

	private MCTree mbeanAttributesTree;
	private boolean initialized = false;

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			MCMenu.minimizeStackTraceView();
		}

		@Override
		public void after() {
			MCMenu.restoreStackTraceView();
		}
	};

	/**
	 * Purpose: - Tests that sorting of columns is correct in the MBean Browser tab. - Tests that
	 * columns can be added/removed. - Tests that newly added columns can be used for sorting.
	 */
	@Test
	public void testTableColumnSorting() {
		// Open up the console and initialize the Widgets we need further on
		initialized = initialize();

		// If there is a Type column, turn it off before continuing and make sure that the column isn't shown
		if (mbeanAttributesTree.getColumnIndex("Type", false) != -1) {
			mbeanAttributesTree.select("VmVendor");
			mbeanAttributesTree.contextChoose("Visible Columns", "Type");
			Assert.assertTrue("The tree contains the \"Type\" column!",
					mbeanAttributesTree.getColumnIndex("Type", false) == -1);
		}

		// Turn on and verify the "Type" column, used when checking the sort order on the "Value" column (which is now
		// grouped by type)
		mbeanAttributesTree.select("VmVendor");
		mbeanAttributesTree.contextChoose("Visible Columns", "Type");
		mbeanAttributesTree.getColumnIndex("Type");

		// Select an item in the tree and sort on Name - Ascending
		mbeanAttributesTree.select("VmVendor");
		mbeanAttributesTree.contextChoose("Sort Columns", "Name", "Ascending");
		List<String> namesColumnAscending = getColumn(mbeanAttributesTree.getAllItemTexts(), getColumnIndex("Name"));

		// Select an item in the tree and sort on Name - Descending
		mbeanAttributesTree.select("VmVendor");
		mbeanAttributesTree.contextChoose("Sort Columns", "Name", "Descending");
		List<String> namesColumnDescending = getColumn(mbeanAttributesTree.getAllItemTexts(), getColumnIndex("Name"));

		// Verify that resorting has happened.
		Assert.assertFalse("Name column, ascending and descending sorting are equal.",
				namesColumnDescending.equals(namesColumnAscending));
		Assert.assertTrue("Name column not correctly sorted ascending: " + formatForPrinting(namesColumnAscending),
				listIsSorted(namesColumnAscending, true));
		Assert.assertTrue("Name column not correctly sorted descending: " + formatForPrinting(namesColumnDescending),
				listIsSorted(namesColumnDescending, false));

		// Select an item in the tree and sort on Value - Ascending
		mbeanAttributesTree.select("VmVendor");
		mbeanAttributesTree.contextChoose("Sort Columns", "Value", "Ascending");
		Map<String, List<String>> valueColumnAscending = getColumnGroupedByType(mbeanAttributesTree.getAllItemTexts(),
				getColumnIndex("Value"), getColumnIndex("Type"));

		if (!ConnectionHelper.is9u0EAorLater(TEST_CONNECTION)) {
			// Select an item in the tree and sort on Value - Descending
			mbeanAttributesTree.select("VmVendor");
			mbeanAttributesTree.contextChoose("Sort Columns", "Value", "Descending");
			Map<String, List<String>> valueColumnDescending = getColumnGroupedByType(
					mbeanAttributesTree.getAllItemTexts(), getColumnIndex("Value"), getColumnIndex("Type"));

			// Verify that sorting is correct when grouped by type
			Assert.assertTrue("Value column not correctly sorted ascending: " + formatForPrinting(valueColumnAscending),
					listIsSorted(valueColumnAscending, true));
			Assert.assertTrue(
					"Value column not correctly sorted descending: " + formatForPrinting(valueColumnDescending),
					listIsSorted(valueColumnDescending, false));
		} else {
			// FIXME: JMC-4757 - Remove the if/else clause (so that the test is always performed) once column sorting is fixed 
			System.out.println("Skipping Value column sort test due to bug");
		}

		// Select an item in the tree and sort on Type - Ascending
		mbeanAttributesTree.select("VmVendor");
		mbeanAttributesTree.contextChoose("Sort Columns", "Type", "Ascending");
		List<String> typeColumnAscending = getColumn(mbeanAttributesTree.getAllItemTexts(), getColumnIndex("Type"));
		// Verify sorting by Type
		Assert.assertTrue("Type column not correctly sorted ascending: " + formatForPrinting(typeColumnAscending),
				listIsSorted(typeColumnAscending, true));

		// Remove the Type column and verify that the table doesn't contain that column (again)
		mbeanAttributesTree.select("VmVendor");
		mbeanAttributesTree.contextChoose("Visible Columns", "Type");
		Assert.assertTrue("The tree still contains the \"Type\" column!",
				mbeanAttributesTree.getColumnIndex("Type", false) == -1);

		// Resort on Name column again, ascending
		mbeanAttributesTree.select("VmVendor");
		mbeanAttributesTree.contextChoose("Sort Columns", "Name", "Ascending");
		namesColumnAscending = getColumn(mbeanAttributesTree.getAllItemTexts(), getColumnIndex("Name"));
		Assert.assertTrue("Name column not sorted ascending: " + formatForPrinting(namesColumnAscending),
				listIsSorted(namesColumnAscending, true));
	}

	private boolean initialize() {
		// Open a connection
		MC.jvmBrowser.connect();

		// Switch to the MBean Browser tab and select the "java.lang.Runtime" MBean
		JmxConsole.selectTab(JmxConsole.Tabs.MBEAN_BROWSER);
		MCTree.getByName(MBEANBROWSER_TREE_NAME).select("java.lang", "Runtime");

		// Select the attributes tab
		MCTabFolder.getByTabName(ATTRIBUTES_TAB).select(ATTRIBUTES_TAB);

		// Find the Attribute tree
		mbeanAttributesTree = MCTree.getByName(ATTRIBUTES_TREE_NAME);

		return true;
	}

	private int getColumnIndex(String columnName) {
		if (initialized) {
			return mbeanAttributesTree.getColumnIndex(columnName);
		} else {
			return -1;
		}
	}

	/**
	 * Returns a column from a table grouped on the type
	 *
	 * @param table
	 *            the table representation
	 * @param columnId
	 *            the column to get
	 * @param typeColumnId
	 *            the column to use for grouping the map
	 * @return the requested column
	 */
	private Map<String, List<String>> getColumnGroupedByType(List<List<String>> table, int columnId, int typeColumnId) {
		Map<String, List<String>> columnMap = new HashMap<>();
		for (List<String> row : table) {
			String type = row.get(typeColumnId);
			String value = row.get(columnId);
			if (columnMap.containsKey(type)) {
				columnMap.get(type).add(value);
			} else {
				List<String> typeList = new ArrayList<>();
				typeList.add(value);
				columnMap.put(type, typeList);
			}
		}
		return columnMap;
	}

	/**
	 * Returns the array of strings as it would be printed if it was a table column
	 */
	private String formatForPrinting(Map<String, List<String>> listMap) {
		StringBuilder formattedList = new StringBuilder("\n");
		for (String listName : listMap.keySet()) {
			List<String> list = listMap.get(listName);
			for (int i = 0; i < list.size(); i++) {
				formattedList.append(list.get(i) + "\n");
			}
		}
		return formattedList.toString();
	}

	/**
	 * The method returns true if the list is sorted according to the direction specified (ascending
	 * or descending).
	 */
	private Boolean listIsSorted(List<String> list, Boolean ascending) {
		Boolean result = true;
		for (int i = 0; i < list.size() - 1 && result; i++) {
			if (ascending) {
				result = result && 0 >= list.get(i).compareTo(list.get(i + 1));
			} else {
				result = result && 0 <= list.get(i).compareTo(list.get(i + 1));
			}
		}
		return result;
	}

	/**
	 * The method returns true if the lists are sorted according to the direction specified
	 * (ascending or descending).
	 */
	private Boolean listIsSorted(Map<String, List<String>> lists, Boolean ascending) {
		Boolean result = true;
		SortCheck: for (String type : lists.keySet()) {
			List<String> list = lists.get(type);
			for (int i = 0; i < list.size() - 1; i++) {
				if (ascending) {
					result = result && 0 >= list.get(i).compareToIgnoreCase(list.get(i + 1));
				} else {
					result = result && 0 <= list.get(i).compareToIgnoreCase(list.get(i + 1));
				}
				if (!result) {
					break SortCheck;
				}
			}
		}
		return result;
	}

	/**
	 * Returns the array of strings as it would be printed if it was a table column
	 *
	 * @param list
	 *            the list of String that is to be formatted
	 * @return a concatenated String with line breaks for each line
	 */
	private String formatForPrinting(List<String> list) {
		StringBuilder formattedList = new StringBuilder("\n");
		for (String text : list) {
			formattedList.append(text + "\n");
		}
		return formattedList.toString();
	}

	/**
	 * Returns a column from a table, useful for checking the sorting.
	 *
	 * @param table
	 *            The table representation
	 * @param columnId
	 *            The column to get
	 * @return The requested column
	 */
	protected List<String> getColumn(List<List<String>> table, int columnId) {
		List<String> column = new ArrayList<>();
		for (List<String> row : table) {
			column.add(row.get(columnId));
		}
		return column;
	}

}
