/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

package org.openjdk.jmc.flightrecorder.uitest.pages;

import org.junit.Rule;
import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCLabel;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;

public class JVMInformationPageTest extends MCJemmyTestBase {

	private static final String FLAGS_CHANGED_JFR = "flags_changed.jfr";
	private static final String PLAIN_JFR = "plain_recording.jfr";

	private static MCTable JVMFlagsLogTable;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	@Test
	public void testJVMFlagsLogTablePopulated() {
		JfrUi.openJfr(materialize("jfr", FLAGS_CHANGED_JFR, JVMInformationPageTest.class));
		JfrNavigator.selectTab(JfrUi.Tabs.JVM_INTERNALS);
		JVMFlagsLogTable = MCTable.getByColumnHeader(Messages.JVMInformationPage_COLUMN_OLD_VALUE);

		final int numEvents = JVMFlagsLogTable.getItemCount();
		Assert.assertTrue(numEvents > 0);
	}

	@Test
	public void testJVMFlagsLogTableEmpty() {
		JfrUi.openJfr(materialize("jfr", PLAIN_JFR, JVMInformationPageTest.class));
		JfrNavigator.selectTab(JfrUi.Tabs.JVM_INTERNALS);
		JVMFlagsLogTable = MCTable.getByColumnHeader(Messages.JVMInformationPage_COLUMN_OLD_VALUE);

		final int numEvents = JVMFlagsLogTable.getItemCount();
		Assert.assertTrue(numEvents == 0);

		MCLabel emptytext = MCLabel.getByLabel(Messages.JVMInformationPage_EMPTY_TABLE);
		Assert.assertNotNull(emptytext);
	}
}
