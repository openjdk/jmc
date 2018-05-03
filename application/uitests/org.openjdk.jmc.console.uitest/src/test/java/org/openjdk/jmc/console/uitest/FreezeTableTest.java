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

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable.TableRow;
import org.openjdk.jmc.ui.common.util.Environment;

/**
 * Tests that the freeze updates options in the JMX Console work as intended. Improvements to this
 * test should probably be to include a deterministic MBean back end so that we won't get any false
 * negatives.
 */
public class FreezeTableTest extends MCJemmyTestBase {

	private static final String ACCESSIBLE_CONTROL_ACTION_TOOLTIP = org.openjdk.jmc.rjmx.ui.internal.Messages.ToggleAccessibleControlAction_TOOLTIP_TEXT;
	private static final String UPDATES_ACTION_TOOLTIP = org.openjdk.jmc.rjmx.ui.internal.Messages.UpdatesAction_TOOLTIP_TEXT;

	@ClassRule
	public static MCUITestRule classRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			if (Environment.getOSType() == Environment.OSType.LINUX) {
				skipIfEarlierThan7u40(TEST_CONNECTION);
			}
			MC.jvmBrowser.connect();
		}

		@Override
		public void after() {
			if (testsRun()) {
				MC.jvmBrowser.disconnect();
			}
		}
	};

	@Test
	public void freeze() {
		MCJemmyBase.focusSectionByTitle("Dashboard");
		MC.jmxConsole.clickToolbarButton(ACCESSIBLE_CONTROL_ACTION_TOOLTIP);
		MC.jmxConsole.clickToolbarButton(UPDATES_ACTION_TOOLTIP);
		MCTable table = MCJemmyBase.getTables().get(0);
		List<TableRow> before = table.getRows();
		MC.jmxConsole.clickToolbarButton(UPDATES_ACTION_TOOLTIP);
		sleep(5000);
		List<TableRow> after = table.getRows();
		Assert.assertFalse("Table wasn't updated", before.equals(after));
	}
}
