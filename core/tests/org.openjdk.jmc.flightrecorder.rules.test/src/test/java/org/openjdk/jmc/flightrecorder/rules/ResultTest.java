/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;
import java.util.concurrent.RunnableFuture;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemQueryBuilder;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;

@SuppressWarnings("nls")
public class ResultTest {

	private static class TestRule implements IRule {

		@Override
		public RunnableFuture<Result> evaluate(IItemCollection items, IPreferenceValueProvider valueProvider) {
			return null;
		}

		@Override
		public Collection<TypedPreference<?>> getConfigurationAttributes() {
			return null;
		}

		@Override
		public String getId() {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getTopic() {
			return null;
		}

	}

	private static final IRule RULE = new TestRule();
	private static final String MSG = "testString";

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testNormal() {
		new Result(RULE, 10, MSG, MSG + MSG, ItemQueryBuilder.fromWhere(ItemFilters.type("test")).build());
		new Result(RULE, 100.0, MSG, null, null);
		new Result(RULE, 0.000000, MSG, null, null);
		new Result(RULE, 0, MSG);
		new Result(RULE, Result.NOT_APPLICABLE, MSG, null, null);
		new Result(RULE, -2, MSG);
		new Result(RULE, -3, MSG);
		new Result(RULE, -200, MSG); // undeclared in-progress magic number
	}

	@Test
	public void testLessThanScore() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("greater than or equal to 0");
		new Result(RULE, -0.5, MSG);
	}

	@Test
	public void testGreaterThanScore() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("less than or equal to 100");
		new Result(RULE, 101, MSG);
	}

	@Test
	public void testNaNscore() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("NaN");
		new Result(RULE, Double.NaN, MSG);
	}

	@Test
	public void testInfinity() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("infinite");
		new Result(RULE, Double.NEGATIVE_INFINITY, MSG);
	}

	@Test
	public void testStringParameters() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("description");
		new Result(new TestRule(), 10, null);
	}

	@Test
	public void testNullRule() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Rule parameter cannot be null");
		new Result(null, 10, MSG);
	}

}
