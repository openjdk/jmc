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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
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

public class DebugNonSafepointsRule implements IRule {

	private static final String DEBUG_NON_SAFEPOINTS_RESULT_ID = "DebugNonSafepoints"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = new HashMap<>();

	static {
		REQUIRED_EVENTS.put(JdkTypeIDs.SYSTEM_PROPERTIES, EventAvailability.AVAILABLE);
	}

	// FIXME: JMC-4617 - Merge with OptionsCheckRule?
	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		boolean dnsEnabled = null != RulesToolkit.findMatches(JdkTypeIDs.VM_INFO, items, JdkAttributes.JVM_ARGUMENTS,
				"\\-XX\\:\\+DebugNonSafepoints", false); //$NON-NLS-1$
		boolean dnsDisabled = null != RulesToolkit.findMatches(JdkTypeIDs.VM_INFO, items, JdkAttributes.JVM_ARGUMENTS,
				"\\-XX\\:\\-DebugNonSafepoints", false); //$NON-NLS-1$

		JavaVersion javaVersion = RulesToolkit.getJavaSpecVersion(items);
		if (javaVersion == null) {
			return ResultBuilder.createFor(this, valueProvider)
					.setSummary(Messages.getString(Messages.General_TEXT_COULD_NOT_DETERMINE_JAVA_VERSION))
					.setSeverity(Severity.NA).build();
		}

		boolean implicitlyEnabled = javaVersion
				.isGreaterOrEqualThan(JavaVersionSupport.DEBUG_NON_SAFEPOINTS_IMPLICITLY_ENABLED);
		if (!implicitlyEnabled) {
			if (dnsDisabled) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
						.setSummary(Messages.getString(Messages.DebugNonSafepointsRule_DISABLED_RESULT_SUMMARY))
						.setExplanation(
								Messages.getString(Messages.DebugNonSafepointsRule_NOT_ENABLED_RESULT_EXPLANATION))
						.setExplanation(Messages.getString(Messages.DebugNonSafepointsRule_NOT_ENABLED_RESULT_SOLUTION))
						.build();
			} else if (!dnsEnabled) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
						.setSummary(Messages.getString(Messages.DebugNonSafepointsRule_NOT_ENABLED_RESULT_SUMMARY))
						.setExplanation(
								Messages.getString(Messages.DebugNonSafepointsRule_NOT_ENABLED_RESULT_EXPLANATION))
						.setSolution(Messages.getString(Messages.DebugNonSafepointsRule_NOT_ENABLED_RESULT_SOLUTION))
						.build();
			}
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
					.setSummary(Messages.getString(Messages.DebugNonSafepointsRule_TEXT_OK)).build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.DebugNonSafepointsRule_IMPLICIT_TEXT_OK)).build();
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return DEBUG_NON_SAFEPOINTS_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.DebugNonSafepointsRule_RULE_NAME);
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
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider preferenceValueProvider,
		final IResultValueProvider dependencyResults) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, preferenceValueProvider, dependencyResults);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return Collections.emptyList();
	}
}
