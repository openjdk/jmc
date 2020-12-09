/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.rules.jdk.general;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.ResultProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultToolkit;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.jdk.general.PasswordsInSystemPropertiesRule;
import org.openjdk.jmc.flightrecorder.test.rules.jdk.MockEventCollection;
import org.openjdk.jmc.flightrecorder.test.rules.jdk.TestEvent;

@SuppressWarnings("restriction")
public class PasswordsInSystemPropertiesRuleTest {

	@Test
	public void containsPassword() {
		TestEvent[] testEvents = new TestEvent[] {new SystemPropertiesTestEvent("password")};
		testPasswordsInSystemPropertiesRule(testEvents,
				"The following suspicious system properties were found in this recording: password. The following regular expression was used to exclude strings from this rule: ''(passworld|passwise)''.If you wish to keep having passwords in your system properties, but want to be able to share recordings without also sharing the passwords, please disable the ''Initial System Property'' event.");
	}

	@Test
	public void notContainPassword() {
		TestEvent[] testEvents = new TestEvent[] {new SystemPropertiesTestEvent("paswrd")};
		testPasswordsInSystemPropertiesRule(testEvents,
				"The recording does not seem to contain passwords in the system properties.");
	}

	@Test
	public void containsExcludedStrings() {
		TestEvent[] testEvents = new TestEvent[] {new SystemPropertiesTestEvent("passworld")};
		testPasswordsInSystemPropertiesRule(testEvents,
				"The recording does not seem to contain passwords in the system properties.");
	}

	private void testPasswordsInSystemPropertiesRule(TestEvent[] testEvents, String descriptionExpected) {
		IItemCollection events = new MockEventCollection(testEvents);
		PasswordsInSystemPropertiesRule passwordsInSystemPropertiesRule = new PasswordsInSystemPropertiesRule();
		RunnableFuture<IResult> future = passwordsInSystemPropertiesRule.createEvaluation(events,
				IPreferenceValueProvider.DEFAULT_VALUES, new ResultProvider());
		try {
			future.run();
			IResult res = future.get();
			String message;
			if (res.getSeverity() == Severity.OK) {
				message = res.getSummary();
			} else {
				message = ResultToolkit.populateMessage(res, res.getExplanation(), false) + res.getSolution();
			}
			Assert.assertEquals(descriptionExpected, message);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
