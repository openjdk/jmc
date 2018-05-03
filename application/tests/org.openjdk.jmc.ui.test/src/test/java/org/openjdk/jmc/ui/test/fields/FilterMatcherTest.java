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
		assertTrue(match("", "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(match("foo", "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(match("", "foo")); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(match("foo", "foo")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(match("foo", "bar")); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(match("foo", "f?o")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("foo", "???")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("f?o", "???")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(match("???", "foo")); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(match("", "*")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("", "**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("foo", "*")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("foo", "**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("foo", "*foo*")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("foo", "**f**o**o**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("barfoobar", "*foo*")); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(matchCaseUnsensitive("fOo", "foO")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRegexpMatch() throws Exception {
		assertTrue(match("foo", "regexp:.*")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("foo", "regexp:fo.")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(match("foo", "regexp:f.")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("foo", "regexp:.o.")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(match("foo", "regexp:bo.")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(match("foo", "regexp: .o.")); //$NON-NLS-1$ //$NON-NLS-2$
//		assertFalse(match("foo", "regexp:*"));
	}
}
