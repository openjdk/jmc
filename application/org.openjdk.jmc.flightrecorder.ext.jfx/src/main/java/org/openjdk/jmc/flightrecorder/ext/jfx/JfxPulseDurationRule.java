/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

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
import org.openjdk.jmc.flightrecorder.ext.jfx.JfxVersionUtil.JavaFxEventAvailability;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class JfxPulseDurationRule implements IRule {
	private static final String RESULT_ID = "pulseDuration"; //$NON-NLS-1$

	/*
	 * TODO: Add detection for if the recording was from an embedded JVM for 33.34ms (30 Hz) target.
	 * This preference is a workaround because it was deemed too time consuming to add automatic
	 * detection.
	 */
	public static final TypedPreference<IQuantity> CONFIG_TARGET_FRAME_RATE = new TypedPreference<>(
			"jfr.pulse.target.framerate", //$NON-NLS-1$
			Messages.JfxPulseDurationRule_CAPTION_PREFERENCE_TARGET_FRAME_RATE,
			Messages.JfxPulseDurationRule_DESCRIPTION_PREFERENCE_TARGET_FRAME_RATE, UnitLookup.FREQUENCY,
			UnitLookup.HERTZ.quantity(60));

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(CONFIG_TARGET_FRAME_RATE);

	public static final TypedResult<IQuantity> SLOW_PHASES = new TypedResult<>("slowPhaseRatio", "Slow Phase Ratio", //$NON-NLS-1$
			"Percentage of JFX phases that were slow to render.", UnitLookup.PERCENTAGE, IQuantity.class);
	public static final TypedResult<IQuantity> TARGET_TIME = new TypedResult<>("targetTime", "Target Time", //$NON-NLS-1$
			"The target time to render each frame.", UnitLookup.TIMESPAN, IQuantity.class);
	public static final TypedResult<IQuantity> RENDER_TARGET = new TypedResult<>("renderTarget", "Render Target", //$NON-NLS-1$
			"The target rendering frequency.", UnitLookup.FREQUENCY, IQuantity.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE, SLOW_PHASES, TARGET_TIME, RENDER_TARGET);

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		JavaFxEventAvailability availability = JfxVersionUtil.getAvailability(items);
		if (availability == JavaFxEventAvailability.None) {
			// Could possibly check the JVM version for better suggestions here, but not very important
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.NA).build();
		}
		IQuantity targetFramerate = valueProvider.getPreferenceValue(CONFIG_TARGET_FRAME_RATE);
		ITypedQuantity<LinearUnit> targetPhaseTime = UnitLookup.MILLISECOND
				.quantity(1000.0 / targetFramerate.longValue());
		IItemFilter longDurationFilter = ItemFilters.more(JfrAttributes.DURATION, targetPhaseTime);
		IItemFilter longPhasesFilter = ItemFilters.and(longDurationFilter,
				ItemFilters.type(JfxVersionUtil.getPulseTypeId(availability)));
		IQuantity longPhases = items.getAggregate(Aggregators.count(longPhasesFilter));
		IQuantity allPhases = items
				.getAggregate(Aggregators.count(ItemFilters.type(JfxVersionUtil.getPulseTypeId(availability))));
		if (longPhases != null && longPhases.doubleValue() > 0) {
			double ratioOfLongPhases = longPhases.ratioTo(allPhases);
			double mappedScore = RulesToolkit.mapExp100(ratioOfLongPhases, 0.05, 0.5);
			mappedScore = mappedScore < 1 ? 1 : mappedScore;
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.get(mappedScore))
					.setSummary(Messages.JfxPulseDurationRule_WARNING)
					.setExplanation(Messages.JfxPulseDurationRule_WARNING_LONG)
					.addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(mappedScore))
					.addResult(SLOW_PHASES, UnitLookup.PERCENT_UNITY.quantity(ratioOfLongPhases))
					.addResult(TARGET_TIME, targetPhaseTime).addResult(RENDER_TARGET, targetFramerate).build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.JfxPulseDurationRule_OK).build();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider valueProvider,
		final IResultValueProvider resultProvider) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, valueProvider, resultProvider);
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

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return Collections.emptyMap();
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}
}
