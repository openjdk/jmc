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

import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedCollectionResult;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class PasswordsInArgumentsRule implements IRule {
	static final String PASSWORD_MATCH_STRING = "PASSW"; //$NON-NLS-1$
	public static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i:" + PASSWORD_MATCH_STRING + ")"); //$NON-NLS-1$ //$NON-NLS-2$

	private static final String PWD_RESULT_ID = "PasswordsInArguments"; //$NON-NLS-1$

	public static final TypedPreference<String> EXCLUDED_STRINGS_REGEXP = new TypedPreference<>(
			"passwordsinargs.string.exclude.regexp", //$NON-NLS-1$
			Messages.getString(Messages.PasswordsInArgsRule_CONFIG_EXCLUDED_STRINGS),
			Messages.getString(Messages.PasswordsInArgsRule_CONFIG_EXCLUDED_STRINGS_LONG), PLAIN_TEXT.getPersister(),
			".*(passworld|passwise).*"); //$NON-NLS-1$

	private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES = Arrays
			.<TypedPreference<?>> asList(EXCLUDED_STRINGS_REGEXP);

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.VM_INFO, EventAvailability.AVAILABLE).build();

	public static final TypedCollectionResult<String> PASSWORDS = new TypedCollectionResult<>("suspiciousJavaArgs", //$NON-NLS-1$
			"Passwords", "Potential passwords found in command line arguments.", UnitLookup.PLAIN_TEXT, String.class);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays.<TypedResult<?>> asList(PASSWORDS);

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		String stringExcludeRegexp = valueProvider.getPreferenceValue(EXCLUDED_STRINGS_REGEXP).trim();
		if (!stringExcludeRegexp.isEmpty()) {
			IItemFilter matchesExclude = ItemFilters.matches(JdkAttributes.JAVA_ARGUMENTS, stringExcludeRegexp);
			IItemFilter stringsExcludingExclude = ItemFilters.and(ItemFilters.type(JdkTypeIDs.VM_INFO),
					ItemFilters.not(matchesExclude));
			items = items.apply(stringsExcludingExclude);
		}
		String pwds = RulesToolkit.findMatches(JdkTypeIDs.VM_INFO, items, JdkAttributes.JAVA_ARGUMENTS,
				PASSWORD_MATCH_STRING, true);
		if (pwds != null && pwds.length() > 0) {
			String[] args = pwds.split(" "); //$NON-NLS-1$
			List<String> passwords = new ArrayList<>();
			for (String arg : args) {
				Matcher matcher = PASSWORD_PATTERN.matcher(arg);
				if (matcher.find()) { // $NON-NLS-1$
					if (arg.contains("=")) { //$NON-NLS-1$
						passwords.add(arg.substring(0, arg.indexOf('=') + 1) + "[...]"); //$NON-NLS-1$
					} else {
						passwords.add(arg);
					}
				}
			}
			String explanation = Messages.getString(Messages.PasswordsInArgsRule_JAVAARGS_TEXT_INFO_LONG);
			if (!stringExcludeRegexp.isEmpty()) {
				explanation = explanation + " " //$NON-NLS-1$
						+ Messages.getString(Messages.PasswordsInArgsRule_TEXT_INFO_EXCLUDED);
			}
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING)
					.setSummary(Messages.getString(Messages.PasswordsInArgsRule_JAVAARGS_TEXT_INFO))
					.setExplanation(explanation)
					.setSolution(Messages.getString(Messages.PasswordsInArgsRule_JAVAARGS_TEXT_SOLUTION))
					.addResult(PASSWORDS, passwords).build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.PasswordsInArgsRule_TEXT_OK)).build();
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
		return PWD_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.PasswordsInArgsRule_RULE_NAME);
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
