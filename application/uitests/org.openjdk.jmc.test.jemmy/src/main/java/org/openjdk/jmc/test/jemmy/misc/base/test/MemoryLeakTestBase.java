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
package org.openjdk.jmc.test.jemmy.misc.base.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;

/**
 * Base class for memory leak related testing of Mission Control
 */
public abstract class MemoryLeakTestBase extends MCJemmyTestBase {
	protected List<Long> memUsage;
	protected static long reloadTimeSpan = 30000;
	protected static long navigationTimeSpan = 30000;
	private static long endTime;
	private static long currentTime;

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			memUsage = new ArrayList<>();
		}
	};

	/**
	 * Stores the current memory usage into the list memUsage
	 */
	protected void storeCurrentMemUsage() {
		memUsage.add(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
	}

	/**
	 * Calculates an approximate live set trend
	 *
	 * @param testName
	 *            The name of the test for which the live set trend is to be calculated
	 */
	protected void getLiveSetTrend(String testName) {
		ArrayList<Long> liveSet = new ArrayList<>();
		long aggregatedLiveSet = 0;
		long previous = 0;
		for (Long value : memUsage) {
			if (value < previous) {
				liveSet.add(value);
				aggregatedLiveSet += value;
			}
			previous = value;
		}

		if (liveSet.size() > 0) {
			System.out.println("\n" + testName + " - Approximate live set values (" + liveSet.size() + "):");
			System.out.println("First value: " + liveSet.get(0));
			System.out.println("Last value: " + liveSet.get(liveSet.size() - 1));
			System.out.println("Mean value: " + aggregatedLiveSet / liveSet.size());
		} else {
			System.out.println(testName
					+ ": No live set trend could be detected (no GC detected in allocated memory usage). Try increasing the number of iterations to force some GCs");
		}
	}

	/**
	 * Loads values for reloads and navigations from system properties (set in test policies). If
	 * not set then running with default values
	 *
	 * @param minReloadTime
	 *            The name of the system property holding the value for minimum reload time
	 *            (seconds)
	 * @param minNavigationTime
	 *            The name of the system property holding the value for minimum navigation time
	 *            (seconds)
	 */
	protected static void loadTimeSpanProperties(String minReloadTime, String minNavigationTime) {
		if (System.getProperty(minReloadTime) != null) {
			reloadTimeSpan = Integer.parseInt(System.getProperty(minReloadTime)) * 1000;
		}
		if (System.getProperty(minNavigationTime) != null) {
			navigationTimeSpan = Integer.parseInt(System.getProperty(minNavigationTime)) * 1000;
		}
	}

	/**
	 * Initializes the timestamp to, at least, keep on running
	 *
	 * @param minTimeSpan
	 *            the time span to run
	 */
	protected static void initializeTime(long minTimeSpan) {
		updateTime();
		endTime = currentTime + minTimeSpan;
	}

	/**
	 * Update the current timestamp
	 */
	private static void updateTime() {
		currentTime = System.currentTimeMillis();
	}

	/**
	 * Test if it's still OK to keep on running the test
	 *
	 * @return true if minimum time hasn't been reached, false otherwise
	 */
	protected static boolean okToRun() {
		updateTime();
		return currentTime < endTime;
	}
}
