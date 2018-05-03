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
package org.openjdk.jmc.flightrecorder.uitest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.test.MemoryLeakTestBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrNavigator;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JfrUi;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Class for memory leak related testing of the JFR UI (and parser).
 */

public class JfrMemoryLeakTest extends MemoryLeakTestBase {
	private static List<JfrUi.Tabs> tabNames = Arrays.asList(JfrUi.Tabs.values());
	private static final String JFR_RECORDING = "plain_recording.jfr";
	private static File recording;

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			recording = materialize("jfr", JFR_RECORDING, JfrMemoryLeakTest.class);
			loadTimeSpanProperties("mc.memusage.jfr.minsecondsreload", "mc.memusage.jfr.minsecondsnavigation");
		}
	};

	@Test
	public void testRepetitiveJfrOpening() {
		initializeTime(reloadTimeSpan);
		while (okToRun()) {
			JfrUi.openJfr(recording);
			walkUI();
			MCMenu.closeActiveEditor();
		}
		getLiveSetTrend("testRepetitiveJfrOpening");
	}

	@Test
	public void testRepetitiveJfrNavigation() {
		JfrUi.openJfr(recording);
		initializeTime(navigationTimeSpan);
		while (okToRun()) {
			walkUI();
		}
		MCMenu.closeActiveEditor();
		getLiveSetTrend("testRepetitiveJfrOpening");

	}

	private void walkUI() {
		for (JfrUi.Tabs tab : tabNames) {
			JfrNavigator.selectTab(tab);
			storeCurrentMemUsage();
		}
	}
}
