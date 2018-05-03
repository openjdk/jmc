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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable.TableRow;

/**
 * Basic class for testing that accessibility for the JMX Console can be turned on and that graphs are
 * presented as accessible tables
 */
public class AccessibilityTest extends MCJemmyTestBase {
	private final static String CELL_REGEXP = "NaN|[0-9]*[\\.]*[0-9]+.*[%|B]?";

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			skipIfEarlierThan7u4(TEST_CONNECTION);
			MC.setAccessibility(true);
		}

		@Override
		public void after() {
			if (testsRun()) {
				MC.jvmBrowser.disconnect();
				MC.setAccessibility(false);
			}
		}
	};

	/**
	 * A simple test testing that accessibility mode can be used to read the graphs in the JMX
	 * Console Overview page (and that the data is of an expected format)
	 */
	@Test
	public void checkTables() {
		MC.jvmBrowser.connect();
		MCJemmyBase.focusSectionByTitle("Dashboard");
		List<MCTable> tables = MCJemmyBase.getTables();
		sleep(10000);
		Assert.assertTrue("Only one table should be present. Found " + tables.size() + " tables", tables.size() == 1);
		Assert.assertTrue("At least three rows were expected (one for each dial). Found "
				+ tables.get(0).getRows().size() + " rows", tables.get(0).getRows().size() >= 3);
		Pattern pattern = Pattern.compile(CELL_REGEXP);
		for (TableRow row : tables.get(0).getRows()) {
			Assert.assertTrue("At least six columns were expected. Found " + row.getColumns().size() + " columns",
					row.getColumns().size() >= 6);
			for (int i = 1; i < row.getColumns().size(); i++) {
				String cell = row.getColumns().get(i);
				Matcher matcher = pattern.matcher(cell);
				Assert.assertTrue("Table field failed to match the following regular expression \"" + CELL_REGEXP
						+ "\", Value: \"" + cell + "\". ", matcher.matches());
			}
		}
	}

}
