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
import org.openjdk.jmc.flightrecorder.rules.jdk.general.PasswordsInArgumentsRule;
import org.openjdk.jmc.flightrecorder.test.rules.jdk.MockEventCollection;
import org.openjdk.jmc.flightrecorder.test.rules.jdk.TestEvent;
import org.openjdk.jmc.flightrecorder.test.rules.jdk.VMInfoTestEvent;

@SuppressWarnings("restriction")
public class PasswordsInArgumentsRuleTest {

	@Test
	public void containsPassword() {
		TestEvent[] testEvents = new TestEvent[] {new VMInfoTestEvent("", "-Dpassword=foo")};
		testPasswordsInArgsRule(testEvents,
				"The following suspicious application arguments were found in this recording: -Dpassword=[...]. The following regular expression was used to exclude strings from this rule: ''.*(passworld|passwise).*''.If you do not want to have your passwords directly as arguments to the Java process, there are usually other means to provide them to your software. If you wish to keep using passwords as arguments, but want to be able to share recordings without also sharing the passwords, please disable the ''JVM Information'' event. Note that disabling the ''JVM Information'' event can limit functionality in the Flight Recorder automated analysis.");
	}

	@Test
	public void NotContainPassword() {
		TestEvent[] testEvents = new TestEvent[] {new VMInfoTestEvent("", "-Dpaswrd=foo")};
		testPasswordsInArgsRule(testEvents,
				"The recording does not seem to contain passwords in the application arguments.");
	}

	@Test
	public void containsExcludedStrings() {
		TestEvent[] testEvents = new TestEvent[] {new VMInfoTestEvent("", "-Dpassworld=foo")};
		testPasswordsInArgsRule(testEvents,
				"The recording does not seem to contain passwords in the application arguments.");
	}

	private void testPasswordsInArgsRule(TestEvent[] testEvents, String descriptionExpected) {
		IItemCollection events = new MockEventCollection(testEvents);
		PasswordsInArgumentsRule passwordsInArgsRule = new PasswordsInArgumentsRule();
		RunnableFuture<IResult> future = passwordsInArgsRule.createEvaluation(events,
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
