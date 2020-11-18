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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class ManagementAgentRule implements IRule {

	private static final String RESULT_ID = "ManagementAgent"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.SYSTEM_PROPERTIES, EventAvailability.AVAILABLE).build();

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {

		// FIXME: Move the filter inside the aggregate.
		IItemCollection properties = items.apply(JdkFilters.SYSTEM_PROPERTIES);

		Set<String> portStr = properties
				.apply(ItemFilters.equals(JdkAttributes.ENVIRONMENT_KEY, "com.sun.management.jmxremote.port")) //$NON-NLS-1$
				.getAggregate((IAggregator<Set<String>, ?>) Aggregators.distinct(JdkAttributes.ENVIRONMENT_VALUE));
		Set<String> authStr = properties
				.apply(ItemFilters.equals(JdkAttributes.ENVIRONMENT_KEY, "com.sun.management.jmxremote.authenticate")) //$NON-NLS-1$
				.getAggregate((IAggregator<Set<String>, ?>) Aggregators.distinct(JdkAttributes.ENVIRONMENT_VALUE));
		Set<String> sslStr = properties
				.apply(ItemFilters.equals(JdkAttributes.ENVIRONMENT_KEY, "com.sun.management.jmxremote.ssl")) //$NON-NLS-1$
				.getAggregate((IAggregator<Set<String>, ?>) Aggregators.distinct(JdkAttributes.ENVIRONMENT_VALUE));

		if (size(portStr) > 1 || size(authStr) > 1 || size(sslStr) > 1) {
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
					.setSummary(Messages.getString(Messages.ManagementAgentRule_TEXT_INFO))
					.setSummary(Messages.getString(Messages.ManagementAgentRule_TEXT_INFO_LONG)).build();
		}
		if (size(portStr) > 0) {
			// Default is true.
			boolean auth = size(authStr) > 0 ? Boolean.parseBoolean(authStr.iterator().next()) : true;
			boolean ssl = size(sslStr) > 0 ? Boolean.parseBoolean(sslStr.iterator().next()) : true;
			if (auth || ssl) {
				ResultBuilder builder = ResultBuilder.createFor(this, valueProvider);
				if (!auth && !ssl) {
					builder = builder.setSeverity(Severity.WARNING)
							.setSummary(Messages.getString(Messages.ManagmentAgentRuleFactory_TEXT_WARN_BOTH_DISABLED))
							.setExplanation(Messages.ManagmentAgentRuleFactory_TEXT_WARN_BOTH_DISABLED_LONG);
				} else if (!auth) {
					builder = builder.setSeverity(Severity.WARNING)
							.setSummary(Messages.getString(Messages.ManagmentAgentRuleFactory_TEXT_WARN_AUTH_DISABLED))
							.setExplanation(Messages
									.getString(Messages.ManagmentAgentRuleFactory_TEXT_WARN_AUTH_DISABLED_LONG));
				} else if (!ssl) {
					builder = builder.setSeverity(Severity.INFO)
							.setSummary(Messages.getString(Messages.ManagmentAgentRuleFactory_TEXT_INFO_SSL_DISABLED))
							.setExplanation(
									Messages.getString(Messages.ManagmentAgentRuleFactory_TEXT_INFO_SSL_DISABLED_LONG));
				}
				return builder
						.setSolution(Messages.getString(Messages.ManagmentAgentRuleFactory_TEXT_WARN_CONFIGURE_GUIDE))
						.build();
			}
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.ManagmentAgentRuleFactory_TEXT_OK)).build();
	}

	private static int size(Collection<?> set) {
		return set == null ? 0 : set.size();
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
		return Messages.getString(Messages.ManagmentAgentRuleFactory_RULE_NAME);
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
		return Collections.emptyList();
	}
}
