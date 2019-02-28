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
package org.openjdk.jmc.ui.test.fields;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openjdk.jmc.ui.common.util.FilterMatcher;

public class FilterMatcherTest {

	private boolean match(String stringToFilter, String filterString) {
		return FilterMatcher.getInstance().match(stringToFilter, filterString, false);
	}

	private boolean matchCaseUnsensitive(String stringToFilter, String filterString) {
		return FilterMatcher.getInstance().match(stringToFilter, filterString, true);
	}

	@Test
	public void testMatch() throws Exception {
		assertTrue(match("", ""));
		assertFalse(match("foo", ""));
		assertFalse(match("", "foo"));

		assertTrue(match("foo", "foo"));
		assertFalse(match("foo", "bar"));

		assertTrue(match("foo", "f?o"));
		assertTrue(match("foo", "???"));
		assertTrue(match("f?o", "???"));
		assertFalse(match("???", "foo"));

		assertTrue(match("", "*"));
		assertTrue(match("", "**"));
		assertTrue(match("foo", "*"));
		assertTrue(match("foo", "**"));
		assertTrue(match("foo", "*foo*"));
		assertTrue(match("foo", "**f**o**o**"));
		assertTrue(match("barfoobar", "*foo*"));

		assertTrue(matchCaseUnsensitive("fOo", "foO"));
	}

	@Test
	public void testRegexpMatch() throws Exception {
		assertTrue(match("foo", "regexp:.*"));
		assertTrue(match("foo", "regexp:fo."));
		assertFalse(match("foo", "regexp:f."));
		assertTrue(match("foo", "regexp:.o."));
		assertFalse(match("foo", "regexp:bo."));
		assertTrue(match("foo", "regexp: .o."));
//		assertFalse(match("foo", "regexp:*"));
	}
}
