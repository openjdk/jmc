/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class FlightRecordingSupportRule implements IRule {

	private static final String RESULT_ID = "FlightRecordingSupport"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.VM_INFO, EventAvailability.AVAILABLE).build();

	public static final TypedResult<String> JDK_VERSION = new TypedResult<>("jdkVersion", "JDK Version", //$NON-NLS-1$
			"The version of the JDK that produced the recording.", UnitLookup.PLAIN_TEXT, String.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(JDK_VERSION);

	// JavaVersionSupport defines JDK_7_U_40 as U 12, instead of explicitly using U12 where warranted.
	// So, for now we define our own, real U_40.
	private static final JavaVersion JDK_7_U_40 = new JavaVersion(7, 0, 40);

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		String jvmVersion = items
				.getAggregate(Aggregators.distinctAsString(JdkTypeIDs.VM_INFO, JdkAttributes.JVM_VERSION));
		if (jvmVersion != null) {
			JavaVersion usedVersion = RulesToolkit.getJavaVersion(jvmVersion);

			if (usedVersion == null) {
				return RulesToolkit.getNotApplicableResult(this, valueProvider,
						Messages.getString(Messages.General_TEXT_COULD_NOT_DETERMINE_JAVA_VERSION));
			}

			if (!usedVersion.isGreaterOrEqualThan(JDK_7_U_40)) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING)
						.addResult(JDK_VERSION, jvmVersion)
						.setSummary(Messages.getString(Messages.FlightRecordingSupportRule_UNSUPPORTED_TEXT_WARN_SHORT))
						.setExplanation(
								Messages.getString(Messages.FlightRecordingSupportRule_UNSUPPORTED_TEXT_WARN_LONG))
						.build();
			}

			if (usedVersion.isEarlyAccess()) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING)
						.addResult(JDK_VERSION, jvmVersion)
						.setSummary(Messages.getString(Messages.FlightRecordingSupportRule_EA_TEXT_WARN_SHORT))
						.setExplanation(Messages.getString(Messages.FlightRecordingSupportRule_EA_TEXT_WARN_LONG))
						.build();
			}
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.FlightRecordingSupportRule_TEXT_OK)).build();
		} else {
			return RulesToolkit.getNotApplicableResult(this, valueProvider,
					Messages.getString(Messages.FlightRecordingSupportRule_NO_JVM_VERSION_EVENTS_TEXT));
		}
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
		return JfrRuleTopics.JVM_INFORMATION;
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}
}
