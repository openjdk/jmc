/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules;

import org.junit.Assert;
import org.junit.Test;

public class SeverityTest {

	/**
	 * The severities in ascending order of severity. This is the order the rest of these tests, and
	 * the severity filtering throughout the code base, are written against.
	 */
	private static final Severity[] ASCENDING = {Severity.IGNORE, Severity.NA, Severity.OK, Severity.INFO,
			Severity.WARNING};

	/**
	 * The declaration order of the enum must agree with the severity scores, so that the natural
	 * ordering of the enum is also the severity ordering. IGNORE used to be declared last, which
	 * made compareTo rank it as the most severe value and let ignored results through every
	 * severity filter.
	 */
	@Test
	public void declarationOrderMatchesScoreOrder() {
		Assert.assertArrayEquals("Severity constants are no longer declared in ascending severity order", ASCENDING,
				Severity.values());
		for (int i = 1; i < ASCENDING.length; i++) {
			Assert.assertTrue(ASCENDING[i - 1] + " should score lower than " + ASCENDING[i],
					ASCENDING[i - 1].getLimit() < ASCENDING[i].getLimit());
		}
	}

	@Test
	public void ignoreIsTheLeastSevere() {
		for (Severity severity : Severity.values()) {
			if (severity != Severity.IGNORE) {
				Assert.assertFalse("IGNORE must not satisfy a minimum of " + severity,
						Severity.IGNORE.isAtLeast(severity));
				Assert.assertTrue(severity + " must satisfy a minimum of IGNORE", severity.isAtLeast(Severity.IGNORE));
			}
		}
	}

	@Test
	public void isAtLeastMatchesScoreOrder() {
		for (int i = 0; i < ASCENDING.length; i++) {
			for (int j = 0; j < ASCENDING.length; j++) {
				Severity severity = ASCENDING[i];
				Severity minimum = ASCENDING[j];
				Assert.assertEquals(severity + ".isAtLeast(" + minimum + ")", i >= j, severity.isAtLeast(minimum));
			}
		}
	}

	@Test
	public void isAtLeastIsReflexive() {
		for (Severity severity : Severity.values()) {
			Assert.assertTrue(severity + " should be at least itself", severity.isAtLeast(severity));
		}
	}

	@Test
	public void getMapsScoresToTheHighestMatchingSeverity() {
		Assert.assertEquals(Severity.WARNING, Severity.get(100));
		Assert.assertEquals(Severity.WARNING, Severity.get(75));
		Assert.assertEquals(Severity.INFO, Severity.get(74));
		Assert.assertEquals(Severity.INFO, Severity.get(25));
		Assert.assertEquals(Severity.OK, Severity.get(24));
		Assert.assertEquals(Severity.OK, Severity.get(0));
		Assert.assertEquals(Severity.NA, Severity.get(-1));
		Assert.assertEquals(Severity.IGNORE, Severity.get(-3));
	}

	/**
	 * Every constant must be reachable from {@link Severity#get(double)}, which iterates a separate
	 * array that has to be kept in descending score order.
	 */
	@Test
	public void getReturnsEverySeverity() {
		for (Severity severity : Severity.values()) {
			Assert.assertEquals("Severity.get should return " + severity + " for its own score", severity,
					Severity.get(severity.getLimit()));
		}
	}
}
