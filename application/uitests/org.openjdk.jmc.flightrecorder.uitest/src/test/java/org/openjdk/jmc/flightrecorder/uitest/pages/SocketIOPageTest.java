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
import org.junit.Test;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

public class SocketIOPageTest extends IOPageTestBase {

	private static final String DURATIONS_TAB = Messages.PAGES_DURATIONS;
	private static final String SOCKET_READ_COL = Messages.SocketIOPage_ROW_SOCKET_READ;
	private static final String READ_COUNT_COL = JdkAggregators.SOCKET_READ_COUNT.getName();
	private static final String SOCKET_WRITE_COL = Messages.SocketIOPage_ROW_SOCKET_WRITE;
	private static final String WRITE_COUNT_COL = JdkAggregators.SOCKET_WRITE_COUNT.getName();
	private static final String RECORDING = "io_test.jfr";

	private static final long[][] TABLE_VALUES = {
		{      3218, 209,    4624, 373 },
		{ 998768639,  22,  611327,  38 },
		{ 999292927,   7, 1814527,   4 },
		{ 999817215,   1, 4288511,   1 },
		{ 999817215,   1, 4288511,   1 },
		{ 999817215,   1, 4288511,   1 },
		{ 999817215,   1, 4288511,   1 },
	};

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	@Test
	public void testPercentileTable() {
		JfrUi.openJfr(materialize("jfr", RECORDING, SocketIOPageTest.class));
		JfrNavigator.selectTab(JfrUi.Tabs.SOCKET_IO);

		MC.selectMcTab(DURATIONS_TAB);
		checkPercentileTable(SOCKET_READ_COL, READ_COUNT_COL, SOCKET_WRITE_COL, WRITE_COUNT_COL, TABLE_VALUES);
	}

}
