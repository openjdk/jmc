/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.jmc.flightrecorder.ext.jfx.test;

import static org.junit.Assert.assertEquals;

import java.text.MessageFormat;
import java.util.concurrent.RunnableFuture;

import org.junit.Test;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.test.TestToolkit;
import org.openjdk.jmc.common.test.io.IOResource;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.ext.jfx.JfxPulseDurationRule;
import org.openjdk.jmc.flightrecorder.ext.jfx.Messages;
import org.openjdk.jmc.flightrecorder.rules.Result;

public class JfxPulseDurationRuleTest {
	
	private static final String RECORDINGS_DIR = "jfr";
	private static final String JFR_FILENAME = "pulseduration.jfr";
	private static final IQuantity TARGET_HZ = UnitLookup.HERTZ.quantity(60);
	private static final IQuantity LONG_PHASES_PERCENT = UnitLookup.PERCENT_UNITY.quantity(0.08106355382619974);
	private static final IQuantity TARGET_PHASE_TIME = UnitLookup.MILLISECOND.quantity(16.666666666666668);
	private static final double SCORE = 30.477476733926004;
	private static final double DELTA = 0.0000000000001; // Account for rounding error
	private static final String WARNING_SHORT = Messages.JfxPulseDurationRule_WARNING;
	private static final String WARNING_LONG = Messages.JfxPulseDurationRule_WARNING_LONG;

	@Test
	public void testPulseDurationRule() throws Exception {
		// Load a saved recording containing javafx.PulsePhase events
		IOResource jfr = TestToolkit.getNamedResource(JfxPulseDurationRuleTest.class, RECORDINGS_DIR, JFR_FILENAME);
		IItemCollection events = JfrLoaderToolkit.loadEvents(jfr.open());
		
		// Execute the rule on our test recording
		JfxPulseDurationRule rule = new JfxPulseDurationRule();
		RunnableFuture<Result> future = rule.evaluate(events, new IPreferenceValueProvider() {
			
			@Override
			@SuppressWarnings("unchecked")
			public <T> T getPreferenceValue(TypedPreference<T> preference) {
				// Use hard-coded 60Hz instead of relying on default, which could change
				if (JfxPulseDurationRule.CONFIG_TARGET_FRAME_RATE.equals(preference)) {
					return (T) TARGET_HZ;
				}
				return DEFAULT_VALUES.getPreferenceValue(preference);
			}
		});
		future.run();
		Result result = future.get();
		
		// Check that score and warnings match expected values
		assertEquals(SCORE, result.getScore(), DELTA);
		assertEquals(MessageFormat.format(WARNING_SHORT, LONG_PHASES_PERCENT.displayUsing(IDisplayable.AUTO),
				TARGET_PHASE_TIME.displayUsing(IDisplayable.AUTO)), result.getShortDescription());
		assertEquals(MessageFormat.format(WARNING_LONG, TARGET_HZ.displayUsing(IDisplayable.AUTO)), result.getLongDescription());
	}

}
