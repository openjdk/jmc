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
package org.openjdk.jmc.flightrecorder.ext.jfx;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class JfxPulseDurationRule implements IRule {
	private static final String RESULT_ID = "pulseDuration"; //$NON-NLS-1$

	/*
	 * TODO: Add detection for if the recording was from an embedded JVM for 33.34ms (30 Hz) target.
	 * This preference is a workaround because it was deemed too time consuming to add automatic
	 * detection.
	 */
	// FIXME: This should really be in Hz, but could not find it in the UnitLookup. Using count for now.
	public static final TypedPreference<IQuantity> CONFIG_TARGET_FRAME_RATE = new TypedPreference<>(
			"jfr.pulse.target.framerate", //$NON-NLS-1$
			Messages.JfxPulseDurationRule_CAPTION_PREFERENCE_TARGET_FRAME_RATE,
			Messages.JfxPulseDurationRule_DESCRIPTION_PREFERENCE_TARGET_FRAME_RATE, UnitLookup.NUMBER,
			UnitLookup.NUMBER_UNITY.quantity(60));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(CONFIG_TARGET_FRAME_RATE);

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JfxConstants.JFX_PULSE_ID);
		if (eventAvailability == EventAvailability.DISABLED || eventAvailability == EventAvailability.UNAVAILABLE
				|| eventAvailability == EventAvailability.NONE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JfxConstants.JFX_PULSE_ID);
		}
		IQuantity targetFramerate = valueProvider.getPreferenceValue(CONFIG_TARGET_FRAME_RATE);
		ITypedQuantity<LinearUnit> targetPhaseTime = UnitLookup.MILLISECOND
				.quantity(1000.0 / targetFramerate.longValue());
		IItemFilter longDurationFilter = ItemFilters.more(JfrAttributes.DURATION, targetPhaseTime);
		IItemFilter longPhasesFilter = ItemFilters.and(longDurationFilter, ItemFilters.type(JfxConstants.JFX_PULSE_ID));
		IQuantity longPhases = items.getAggregate(Aggregators.count(longPhasesFilter));
		IQuantity allPhases = items.getAggregate(Aggregators.count(ItemFilters.type(JfxConstants.JFX_PULSE_ID)));
		if (longPhases != null && longPhases.doubleValue() > 0) {
			double ratioOfLongPhases = longPhases.ratioTo(allPhases);
			double mappedScore = RulesToolkit.mapExp100(ratioOfLongPhases, 0.05, 0.5);
			mappedScore = mappedScore < 1 ? 1 : mappedScore;
			return new Result(this, mappedScore,
					MessageFormat.format(Messages.JfxPulseDurationRule_WARNING,
							UnitLookup.PERCENT_UNITY.quantity(ratioOfLongPhases).displayUsing(IDisplayable.AUTO),
							targetPhaseTime.displayUsing(IDisplayable.AUTO)),
					MessageFormat.format(Messages.JfxPulseDurationRule_WARNING_LONG, targetFramerate));
		}
		return new Result(this, 0, Messages.JfxPulseDurationRule_OK);
	}

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return getResult(items, valueProvider);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return CONFIG_ATTRIBUTES;
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.JfxPulseDurationRule_NAME;
	}

	@Override
	public String getTopic() {
		return JfxConstants.JFX_RULE_PATH;
	}
}
