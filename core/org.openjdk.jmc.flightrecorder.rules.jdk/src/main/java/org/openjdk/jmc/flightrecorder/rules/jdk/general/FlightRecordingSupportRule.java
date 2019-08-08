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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import static org.openjdk.jmc.common.item.Attribute.attr;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

public class FlightRecordingSupportRule implements IRule {

	private static final String RESULT_ID = "FlightRecordingSupport"; //$NON-NLS-1$

	// JavaVersionSupport defines JDK_7_U_40 as U 12, instead of explicitly using U12 where warranted.
	// So, for now we define our own, real U_40.
	private static final JavaVersion JDK_7_U_40 = new JavaVersion(7, 0, 40);

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		EventAvailability eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.VM_INFO);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.VM_INFO);
		}

		Result versionResult = getVersionResult(items);
		Result timeConversionResult = getTimeConversionResult(items);

		double versionScore = versionResult.getScore();
		double timeConversionScore = timeConversionResult.getScore();

		if (versionScore > 0 || timeConversionScore > 0) {
			return versionResult.getScore() > timeConversionResult.getScore() ? versionResult : timeConversionResult;			
		}
		// If no rule reported a warning or error, return the rule with the lowest score,
		// meaning it was NotApplicable, Failed or Ignored.
		return versionScore < timeConversionScore ? versionResult : timeConversionResult;
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
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.FlightRecordingSupportRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION_TOPIC;
	}

	private Result getVersionResult(String versionString) {
		JavaVersion usedVersion = RulesToolkit.getJavaVersion(versionString);

		if (usedVersion == null) {
			return RulesToolkit.getNotApplicableResult(this,
                    Messages.getString(Messages.General_TEXT_COULD_NOT_DETERMINE_JAVA_VERSION));
		}

		if (!usedVersion.isGreaterOrEqualThan(JDK_7_U_40)) {
			return new Result(this, 100,
					Messages.getString(Messages.FlightRecordingSupportRule_UNSUPPORTED_TEXT_WARN_SHORT),
					MessageFormat.format(
							Messages.getString(Messages.FlightRecordingSupportRule_UNSUPPORTED_TEXT_WARN_LONG),
							versionString));
		}

		if (usedVersion.isEarlyAccess()) {
			return new Result(this, 80, Messages.getString(Messages.FlightRecordingSupportRule_EA_TEXT_WARN_SHORT),
					MessageFormat.format(Messages.getString(Messages.FlightRecordingSupportRule_EA_TEXT_WARN_LONG),
							versionString));
		}

		return new Result(this, 0, Messages.getString(Messages.FlightRecordingSupportRule_TEXT_OK));
	}

	private Result getVersionResult(IItemCollection items) {
		String jvmVersion = items
				.getAggregate(Aggregators.distinctAsString(JdkTypeIDs.VM_INFO, JdkAttributes.JVM_VERSION));
		if (jvmVersion != null) {
			return getVersionResult(jvmVersion);
		} else {
			return RulesToolkit.getNotApplicableResult(this,
					Messages.getString(Messages.FlightRecordingSupportRule_NO_JVM_VERSION_EVENTS_TEXT));
		}
	}

	private Result getTimeConversionResult(IItemCollection items) {
		EventAvailability eventAvailability;
		eventAvailability = RulesToolkit.getEventAvailability(items, JdkTypeIDs.TIME_CONVERSION);
		if (eventAvailability != EventAvailability.AVAILABLE) {
			return RulesToolkit.getEventAvailabilityResult(this, items, eventAvailability, JdkTypeIDs.TIME_CONVERSION);
		}

		// Check time conversion error
		IItemCollection timeConversionItems = items.apply(JdkFilters.TIME_CONVERSION);
		IQuantity conversionFactor = timeConversionItems
				.getAggregate(Aggregators.max(attr("fastTimeConversionAdjustments", null, //$NON-NLS-1$
						UnitLookup.NUMBER)));
		Boolean fastTimeEnabled = timeConversionItems
				.getAggregate(Aggregators.and(JdkTypeIDs.TIME_CONVERSION, attr("fastTimeEnabled", null, //$NON-NLS-1$
						UnitLookup.FLAG)));
		if (conversionFactor != null && fastTimeEnabled) {
			if (conversionFactor.longValue() != 0) {
				String shortMessage = Messages.getString(Messages.FasttimeRule_TEXT_WARN);
				String longMessage = shortMessage + " " + Messages.getString(Messages.FasttimeRule_TEXT_WARN_LONG); //$NON-NLS-1$
				return new Result(this, 100, shortMessage, longMessage);
			}
		}

		return new Result(this, 0, Messages.getString(Messages.FlightRecordingSupportRule_TEXT_OK));
	}
}
