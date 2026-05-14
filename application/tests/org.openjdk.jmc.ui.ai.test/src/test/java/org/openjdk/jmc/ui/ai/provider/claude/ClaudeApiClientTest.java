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
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.openjdk.jmc.ui.ai.provider.claude;

import org.junit.Assert;
import org.junit.Test;

public class ClaudeApiClientTest {

	private static void assertRoundTrip(String original) {
		String escaped = ClaudeApiClient.escapeJson(original);
		String restored = ClaudeApiClient.unescapeJson(escaped);
		Assert.assertEquals("round-trip failed for: " + original, original, restored);
	}

	@Test
	public void testEscapeNewline() {
		Assert.assertEquals("line1\\nline2", ClaudeApiClient.escapeJson("line1\nline2"));
	}

	@Test
	public void testEscapeTab() {
		Assert.assertEquals("a\\tb", ClaudeApiClient.escapeJson("a\tb"));
	}

	@Test
	public void testEscapeBackslash() {
		Assert.assertEquals("a\\\\b", ClaudeApiClient.escapeJson("a\\b"));
	}

	@Test
	public void testEscapeQuote() {
		Assert.assertEquals("say \\\"hello\\\"", ClaudeApiClient.escapeJson("say \"hello\""));
	}

	@Test
	public void testRoundTripPlainText() {
		assertRoundTrip("hello world");
	}

	@Test
	public void testRoundTripNewline() {
		assertRoundTrip("line1\nline2");
	}

	@Test
	public void testRoundTripTab() {
		assertRoundTrip("col1\tcol2");
	}

	@Test
	public void testRoundTripBackslash() {
		assertRoundTrip("C:\\Users\\jmc");
	}

	@Test
	public void testRoundTripQuote() {
		assertRoundTrip("say \"hello\"");
	}

	@Test
	public void testRoundTripBackslashBeforeN() {
		// Literal backslash followed by 'n' — must not become a newline after round-trip.
		assertRoundTrip("path\\new_folder");
	}

	@Test
	public void testRoundTripBackslashBeforeT() {
		// Literal backslash followed by 't' — must not become a tab after round-trip.
		assertRoundTrip("path\\to\\thing");
	}

	@Test
	public void testRoundTripBackslashQuote() {
		// Backslash immediately followed by a quote.
		assertRoundTrip("value\\\"quoted");
	}

	@Test
	public void testRoundTripMultipleBackslashes() {
		assertRoundTrip("a\\\\b");
	}

	@Test
	public void testRoundTripEmpty() {
		assertRoundTrip("");
	}

	@Test
	public void testUnescapeNull() {
		Assert.assertNull(ClaudeApiClient.unescapeJson(null));
	}

	@Test
	public void testRoundTripMixed() {
		assertRoundTrip("Hello\tWorld\nPath: C:\\Users\\test \"quoted\"");
	}
}
