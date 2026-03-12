/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
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

public class CryptoSecurityRule implements IRule {

	private static final String CRYPTO_SECURITY_RESULT_ID = "CryptoSecurityRule"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.X509_CERTIFICATE, EventAvailability.AVAILABLE).build();

	private static final String ACTION = Messages.getString(Messages.Crypto_ACTION);

	private static final String ATTENTION = Messages.getString(Messages.Crypto_ATTENTION);

	private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES = Arrays
			.<TypedResult<?>> asList(TypedResult.SCORE);

	@Override
	public String getId() {
		return CRYPTO_SECURITY_RESULT_ID;
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.SECURITY;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.CryptoSecurity_RULE_NAME);
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {

		boolean actionNeeded = (RulesToolkit.findMatches(JdkTypeIDs.X509_CERTIFICATE, items,
				JdkAttributes.CRYPTO_REMARK, Messages.getString(Messages.Crypto_ACTION), false) != null); //$NON-NLS-1$
		boolean attentionNeeded = (RulesToolkit.findMatches(JdkTypeIDs.X509_CERTIFICATE, items,
				JdkAttributes.CRYPTO_REMARK, Messages.getString(Messages.Crypto_ATTENTION), false) != null); //$NON-NLS-1$

		String ruleResult = RulesToolkit.findAttribute(JdkTypeIDs.X509_CERTIFICATE, items,
				JdkAttributes.CRYPTO_RULE_RESULT);
		Result processedResult = processResult(ruleResult);

		ruleResult = processedResult.cleanedAllActions + "\n" + processedResult.cleanedAllAttentions;

		if (actionNeeded) {
			if (attentionNeeded) {
				return ResultBuilder.createFor(CryptoSecurityRule.this, valueProvider).setSeverity(Severity.WARNING)
						.setSummary(Messages.getString(Messages.Crypto_ACTION_ATTENTION_INFO)
								+ MessageFormat.format(Messages.getString(Messages.Crypto_REFERENCE_INFO), ruleResult))
						.build();
			} else {
				return ResultBuilder.createFor(CryptoSecurityRule.this, valueProvider).setSeverity(Severity.WARNING)
						.setSummary(Messages.getString(Messages.Crypto_ACTION_INFO)
								+ MessageFormat.format(Messages.getString(Messages.Crypto_REFERENCE_INFO), ruleResult))
						.build();
			}
		} else if (attentionNeeded) {
			return ResultBuilder.createFor(CryptoSecurityRule.this, valueProvider).setSeverity(Severity.WARNING)
					.setSummary(Messages.getString(Messages.Crypto_ATTENTION_INFO)
							+ MessageFormat.format(Messages.getString(Messages.Crypto_REFERENCE_INFO), ruleResult))
					.build();
		} else {
			return ResultBuilder.createFor(CryptoSecurityRule.this, valueProvider).setSeverity(Severity.OK)
					.setSummary("OK").build();
		}

	}

	public static class Result {
		public final String allActions; // joined by '~'
		public final String allAttentions; // joined by '~'
		public final int allActionCount;
		public final int allAttentionCount;
		public final String cleanedAllActions; // allActions with "Action Required." removed
		public final String cleanedAllAttentions;// allAttentions with "Attention Needed." removed

		public Result(String allActions, String allAttentions, int allActionCount, int allAttentionCount,
				String cleanedAllActions, String cleanedAllAttentions) {
			this.allActions = allActions;
			this.allAttentions = allAttentions;
			this.allActionCount = allActionCount;
			this.allAttentionCount = allAttentionCount;
			this.cleanedAllActions = cleanedAllActions;
			this.cleanedAllAttentions = cleanedAllAttentions;
		}
	}

	private Result processResult(String input) {

		if (input == null)
			input = "";

		final String ACTION_BULLET = "🔴 ";
		final String ATTENTION_BULLET = "🟠 ";

		String[] tokens = input.split("~", -1);

		List<String> actionFragments = new ArrayList<>();
		List<String> attentionFragments = new ArrayList<>();
		List<String> originalFragments = new ArrayList<>();

		String bulletChars = "[\\u2022\\u2023\\u2027•\\*]";
		String patternString = "(?s)\\b(?:Action Required\\.|Attention Needed\\.).*?(?=(?:" + bulletChars + ")|$)";
		Pattern fragmentPattern = Pattern.compile(patternString);

		for (String token : tokens) {
			if (token == null || token.trim().isEmpty())
				continue;

			Matcher m = fragmentPattern.matcher(token);
			while (m.find()) {
				String frag = m.group().trim();
				frag = frag.replaceAll("^[\\s\\u2022\\u2023\\u2027•\\*]+", "").trim();

				if (frag.isEmpty())
					continue;

				boolean isAction = frag.contains(ACTION);
				boolean isAttention = frag.contains(ATTENTION);

				if (isAction) {
					actionFragments.add(ACTION_BULLET + frag);
				}
				if (isAttention) {
					attentionFragments.add(ATTENTION_BULLET + frag);
				}

				String prefix;
				if (isAction && isAttention)
					prefix = ACTION_BULLET + ATTENTION_BULLET;
				else if (isAction)
					prefix = ACTION_BULLET;
				else if (isAttention)
					prefix = ATTENTION_BULLET;
				else
					prefix = "";

				String fragClean = frag.replace(ACTION, "").replace(ATTENTION, "").trim();

				if (!fragClean.isEmpty()) {
					originalFragments.add(prefix + fragClean);
				}
			}
		}

		int allActionCount = actionFragments.size();
		int allAttentionCount = attentionFragments.size();

		// Build titled sections
		String allActions = "\nAction Required (" + allActionCount + ")\n\n"
				+ (actionFragments.isEmpty() ? "" : String.join("\n", actionFragments)) + "\n";

		String allAttentions = "\nAttention Needed (" + allAttentionCount + ")\n\n"
				+ (attentionFragments.isEmpty() ? "" : String.join("\n", attentionFragments)) + "\n";

		// Clean versions
		String cleanedAllActions = "\n\nAction Required (" + allActionCount + ")\n\n" + actionFragments.stream()
				.map(s -> s.replace(ACTION, "").trim()).filter(s -> !s.isEmpty()).collect(Collectors.joining("\n"))
				+ "\n";

		String cleanedAllAttentions = "\nAttention Needed (" + allAttentionCount + ")\n\n" + attentionFragments.stream()
				.map(s -> s.replace(ATTENTION, "").trim()).filter(s -> !s.isEmpty()).collect(Collectors.joining("\n"))
				+ "\n";

		return new Result(allActions, allAttentions, allActionCount, allAttentionCount, cleanedAllActions,
				cleanedAllAttentions);
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
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return RESULT_ATTRIBUTES;
	}

}
