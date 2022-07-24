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
package org.openjdk.jmc.flightrecorder.test.rules.jdk;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.ResultProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultToolkit;
import org.openjdk.jmc.flightrecorder.rules.jdk.general.VerifyNoneRule;

@SuppressWarnings("restriction")
public class VerifyNoneRuleTest {

	@Test
	public void verifyNone_jvmArguments() {
		TestEvent[] testEvents = new TestEvent[] {new VMInfoTestEvent("-Xverify:none", "")};
		testVerifyNoneRule(testEvents,
				"The application ran with bytecode verification disabled with '-Xverify:none' argument. Disabling bytecode verification is unsafe and should not be done in a production system. If it is not necessary for the application, then dont use -Xverify:none or -noverify on the command line. See the [Secure Coding Standard for Java](https://www.securecoding.cert.org/confluence/display/java/ENV04-J.+Do+not+disable+bytecode+verification)."); //$NON-NLS-1$
	}

	@Test
	public void verifyNone_javaArguments() {
		TestEvent[] testEvents = new TestEvent[] {new VMInfoTestEvent("", "-Xverify:none")};
		testVerifyNoneRule(testEvents,
				"The application ran with bytecode verification disabled with '-Xverify:none' argument. Disabling bytecode verification is unsafe and should not be done in a production system. If it is not necessary for the application, then dont use -Xverify:none or -noverify on the command line. See the [Secure Coding Standard for Java](https://www.securecoding.cert.org/confluence/display/java/ENV04-J.+Do+not+disable+bytecode+verification)."); //$NON-NLS-1$
	}

	@Test
	public void noVerify_jvmArguments() {
		TestEvent[] testEvents = new TestEvent[] {new VMInfoTestEvent("-noverify", "")};
		testVerifyNoneRule(testEvents,
				"The application ran with bytecode verification disabled with '-noverify' argument. Disabling bytecode verification is unsafe and should not be done in a production system. If it is not necessary for the application, then dont use -Xverify:none or -noverify on the command line. See the [Secure Coding Standard for Java](https://www.securecoding.cert.org/confluence/display/java/ENV04-J.+Do+not+disable+bytecode+verification)."); //$NON-NLS-1$
	}

	@Test
	public void noVerify_javaArguments() {
		TestEvent[] testEvents = new TestEvent[] {new VMInfoTestEvent("", "-noverify")};
		testVerifyNoneRule(testEvents,
				"The application ran with bytecode verification disabled with '-noverify' argument. Disabling bytecode verification is unsafe and should not be done in a production system. If it is not necessary for the application, then dont use -Xverify:none or -noverify on the command line. See the [Secure Coding Standard for Java](https://www.securecoding.cert.org/confluence/display/java/ENV04-J.+Do+not+disable+bytecode+verification)."); //$NON-NLS-1$
	}

	private void testVerifyNoneRule(TestEvent[] testEvents, String descriptionExpected) {
		IItemCollection events = new MockEventCollection(testEvents);
		VerifyNoneRule verifyNoneRule = new VerifyNoneRule();
		RunnableFuture<IResult> future = verifyNoneRule.createEvaluation(events,
				IPreferenceValueProvider.DEFAULT_VALUES, new ResultProvider());
		try {
			future.run();
			IResult res = future.get();
			String longDesc = ResultToolkit.populateMessage(res, res.getExplanation(), false);
			Assert.assertEquals(descriptionExpected, longDesc);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

	}
}
