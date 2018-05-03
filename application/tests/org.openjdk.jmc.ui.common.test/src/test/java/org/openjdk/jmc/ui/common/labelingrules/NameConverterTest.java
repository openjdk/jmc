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
package org.openjdk.jmc.ui.common.labelingrules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;

import org.junit.Test;

import org.openjdk.jmc.ui.common.labelingrules.NameConverter;
import org.openjdk.jmc.ui.common.labelingrules.NamingRule;
import org.openjdk.jmc.ui.common.util.Environment;

@SuppressWarnings("nls")
public class NameConverterTest {
	private final static Object[] EXAMPLE_VALUES1 = new Object[] {"1.5", "[Unknown]",
			"C:\\Java\\eclipse3.3.1.1\\plugins\\org.eclipse.equinox.launcher_1.0.1.R33x_v20070828.jar",
			Integer.valueOf(4711)};
	private final static Object[] EXAMPLE_VALUES2 = new Object[] {"1.6", "", "weblogic.Server",
			Integer.valueOf(Environment.getThisPID())};
	private final static Object[] EXAMPLE_VALUES3 = new Object[] {"1.5", "", "org.jboss.Main", Integer.valueOf(666)};

	@Test
	public void testFormatJBoss() {
		NamingRule rule = new NamingRule("Test rule", "{2}=org.jboss.Main=>[{0}] JBoss ({3})", 2000, null);
		String result = rule.format(EXAMPLE_VALUES3);
		assertEquals("[1.5] JBoss (" + MessageFormat.format("{0}", new Object[] {(Integer) EXAMPLE_VALUES3[3]}) + ")",
				result);
	}

	@Test
	public void testFormatWLS() {
		NamingRule rule = new NamingRule("Test rule", "{2}=weblogic.Server=>[{0}] WebLogic Server ({3})", 2000, null);
		String result = rule.format(EXAMPLE_VALUES2);
		assertEquals(
				"[1.6] WebLogic Server (" + MessageFormat.format("{0}", new Object[] {Environment.getThisPID()}) + ")",
				result);
	}

	@Test
	public void testConvertEclipse() {
		NamingRule rule = new NamingRule("Test rule",
				"{2}=.*org.eclipse.equinox.launcher_1.0.1.R33x.*=>[{0}] Eclipse 3.3 ({3})", 2000, null);
		String result = rule.format(EXAMPLE_VALUES1);
		assertEquals(
				"[1.5] Eclipse 3.3 (" + MessageFormat.format("{0}", new Object[] {(Integer) EXAMPLE_VALUES1[3]}) + ")",
				result);
	}

	@Test
	public void testConvertJBoss() {
		NamingRule rule = new NamingRule("Test rule", "{2}=org.jboss.Main=>[{0}] JBoss ({3})", 2000, null);
		String result = rule.format(EXAMPLE_VALUES3);
		assertEquals("[1.5] JBoss (" + MessageFormat.format("{0}", new Object[] {(Integer) EXAMPLE_VALUES3[3]}) + ")",
				result);
	}

	@Test
	public void testAddNamingRule() {
		NameConverter nc = new NameConverter();
		NamingRule rule = new NamingRule("Test rule", "{2} = .*MyApp.* => MyCoolApp ({3})", 2000, null);
		nc.addNamingRule(rule);
		assertTrue(nc.getRules().contains(rule));
	}

	@Test
	public void testDefaultRules() {
		NameConverter nc = new NameConverter();
		assertTrue(nc.getRules().size() > 0);
		int lastPrio = Integer.MAX_VALUE;
		for (NamingRule rule : nc.getRules()) {
			assertNotNull(rule.getName());
			assertNotNull(rule.getNormalizedExpression());
			assertTrue(rule.getPriority() <= lastPrio);
			lastPrio = rule.getPriority();
		}
	}
}
