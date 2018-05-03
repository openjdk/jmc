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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTabFolder;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable.TableRow;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTree;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JmxConsole;

/**
 * Class for for testing System tab related actions in the console.
 */
@SuppressWarnings("restriction")
public class SystemTabTest extends MCJemmyTestBase {
	private static final String MBEANBROWSER_TREE_NAME = org.openjdk.jmc.console.ui.mbeanbrowser.tree.MBeanTreeSectionPart.MBEANBROWSER_MBEAN_TREE_NAME;
	private static final String ATTRIBUTES_TAB = org.openjdk.jmc.console.ui.mbeanbrowser.tab.Messages.FeatureSectionPart_ATTRIBUTES_TAB_TITLE_TEXT;
	private static final String ATTRIBUTES_TREE_NAME = org.openjdk.jmc.rjmx.ui.attributes.MRIAttributeInspector.MBEANBROWSER_ATTRIBUTESTAB_ATTRIBUTESTREE_NAME;

	@ClassRule
	public static MCUITestRule classRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			// This test should not run if the "disablebrowser" property is set,
			// e.g. when running the tests on Linux.
			String browserProperty = System.getProperty("org.openjdk.jmc.console.ui.information.disablebrowser");
			Assert.assertTrue(browserProperty == null || browserProperty.equals("false"));

			MC.jvmBrowser.connect();
			JmxConsole.selectTab(JmxConsole.Tabs.SYSTEM);
		}
	};

	/**
	 * The purpose of the test is to verify that the values displayed in the Server Information tab
	 * is correct. Currently only some of the values are checked, since others require a larger
	 * effort (i.e. combining different MBean attribute values into one string, or due to formatting
	 * of for example the time values.
	 */
	@Test
	public void testServerInformation() {
		/*
		 * Create a map between values displayed in the tab and the corresponding MBean attribute
		 * path. Regarding the path: the last part identifies the attribute name while the previous
		 * parts identify the MBean tree item
		 */
		Map<String, String[]> mbeanInfo = new HashMap<>();
		mbeanInfo.put("OS Architecture", new String[] {"java.lang", "OperatingSystem", "Arch"});
		mbeanInfo.put("Class Path", new String[] {"java.lang", "Runtime", "ClassPath"});
		mbeanInfo.put("VM Vendor", new String[] {"java.lang", "Runtime", "VmVendor"});
		mbeanInfo.put("Library Path", new String[] {"java.lang", "Runtime", "LibraryPath"});
		mbeanInfo.put("Number of Processors", new String[] {"java.lang", "OperatingSystem", "AvailableProcessors"});
		mbeanInfo.put("Start Time", new String[] {"java.lang", "Runtime", "StartTime"});

		MCJemmyBase.focusSectionByTitle("Server Information");
		MCTable settingsTable = MCJemmyBase.getTables().get(0);
		List<TableRow> tableData = settingsTable.getRows();

		JmxConsole.selectTab(JmxConsole.Tabs.MBEAN_BROWSER);
		MCTree mBeanTree = MCTree.getByName(MBEANBROWSER_TREE_NAME);

		for (TableRow row : tableData) {
			String category = row.getText("Category");
			// only verify rows for which we have a corresponding entry in the map
			if (mbeanInfo.containsKey(category)) {
				String systemTableValue = row.getText("Value");
				String[] pathTokens = mbeanInfo.get(category);

				// Select the MBean path
				mBeanTree.select(Arrays.copyOf(pathTokens, pathTokens.length - 1));

				// Select the attributes tab
				MCTabFolder.getByTabName(ATTRIBUTES_TAB).select(ATTRIBUTES_TAB);

				// Find the Attribute tree
				MCTree attributeTree = MCTree.getByName(ATTRIBUTES_TREE_NAME);

				// find the fifth thread and retrieve the value (in the column with header "Value")
				attributeTree.select(true, pathTokens[pathTokens.length - 1]);

				// repeating the read of the value until it doesn't start with a "<" (indicates a not yet initialized value)
				String mbeanValue = null;
				do {
					mbeanValue = attributeTree.getSelectedItemText("Value");
				} while (mbeanValue.startsWith("<"));

				Assert.assertTrue(
						"Value mismatch for category \"" + category + "\"! MBean Browser tab value: \"" + mbeanValue
								+ "\". System tab value: \"" + systemTableValue + "\"",
						mbeanValue.equals(systemTableValue));
			}
		}
	}
}
