/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules.jdk.exceptions;

import java.util.Collections;
import java.util.Map;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.AbstractRule;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class FatalErrorRule extends AbstractRule {

	private static final String RESULT_ID = "Fatal Errors"; //$NON-NLS-1$

	private static final String ERROR_REASON = "VM Error"; //$NON-NLS-1$
	private static final String INFO_REASON = "No remaining non-daemon Java threads"; //$NON-NLS-1$

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.VM_SHUTDOWN, EventAvailability.AVAILABLE).build();

	public FatalErrorRule() {
		super(RESULT_ID, Messages.getString(Messages.FatalErrorRule_RULE_NAME), JfrRuleTopics.JVM_INFORMATION,
				Collections.<TypedPreference<?>> emptyList(), Collections.<TypedResult<?>> emptyList(),
				REQUIRED_EVENTS);
	}

	@Override
	protected IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		IItemFilter shutdownFilter = ItemFilters.type(JdkTypeIDs.VM_SHUTDOWN);
		IItemCollection shutdownItems = items.apply(shutdownFilter);

		if (shutdownItems.hasItems()) {
			// Check the type of VM Shutdown, if it was a VM Error we should report it.
			if (shutdownItems.apply(ItemFilters.contains(JdkAttributes.SHUTDOWN_REASON, ERROR_REASON)).hasItems()) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING)
						.setSummary(Messages.getString(Messages.FatalErrorRule_TEXT_WARN)).build();
			} else if (shutdownItems.apply(ItemFilters.contains(JdkAttributes.SHUTDOWN_REASON, INFO_REASON))
					.hasItems()) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.INFO)
						.setSummary(Messages.getString(Messages.FatalErrorRule_TEXT_INFO)).build();
			}
		}
		return ResultBuilder.createFor(this, valueProvider)
				.setSummary(Messages.getString(Messages.FatalErrorRule_TEXT_OK)).build();
	}
}
