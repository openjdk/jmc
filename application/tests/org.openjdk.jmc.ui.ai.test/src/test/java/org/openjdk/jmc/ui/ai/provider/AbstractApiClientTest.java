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
package org.openjdk.jmc.ui.ai.provider;

import org.junit.Assert;
import org.junit.Test;

public class AbstractApiClientTest {

	/** Minimal concrete subclass that exposes extractJsonString for testing. */
	private static final class TestClient extends AbstractApiClient {
		String extract(String key, String json) {
			return extractJsonString(key, json);
		}
	}

	private static final TestClient CLIENT = new TestClient();

	private static String extract(String key, String json) {
		return CLIENT.extract("\"" + key + "\"", json);
	}

	@Test
	public void testSimpleExtraction() {
		Assert.assertEquals("abc123", extract("id", "{\"id\": \"abc123\"}"));
	}

	@Test
	public void testKeyAbsent() {
		Assert.assertNull(extract("id", "{\"foo\": \"bar\"}"));
	}

	@Test
	public void testNullValue() {
		Assert.assertNull(extract("id", "{\"id\": null}"));
	}

	@Test
	public void testNonStringValue() {
		Assert.assertNull(extract("id", "{\"id\": 42}"));
	}

	@Test
	public void testWhitespaceAroundColon() {
		Assert.assertEquals("spaced", extract("id", "{ \"id\" : \"spaced\" }"));
	}

	@Test
	public void testEscapedCharsInValue() {
		Assert.assertEquals("a\nb", extract("id", "{\"id\": \"a\\nb\"}"));
	}

	@Test
	public void testUnterminatedStringReturnsNull() {
		Assert.assertNull(extract("id", "{\"id\": \"unclosed"));
	}

	@Test
	public void testValueCollisionSkipped() {
		// The string "id" appears as a VALUE before the actual "id" key.
		// With multiple intervening keys the original indexOf would return the wrong value.
		Assert.assertEquals("correct", extract("id", "{\"type\": \"id\", \"foo\": \"bar\", \"id\": \"correct\"}"));
	}

	@Test
	public void testOnlyValueNoKey() {
		// "id" appears only as a value — there is no key named "id".
		Assert.assertNull(extract("id", "{\"type\": \"id\"}"));
	}

	@Test
	public void testSecondKeyChosen() {
		// Two keys; make sure we return the value of the correct one.
		Assert.assertEquals("second", extract("id", "{\"other\": \"first\", \"id\": \"second\"}"));
	}

	@Test
	public void testEmptyStringValue() {
		Assert.assertEquals("", extract("id", "{\"id\": \"\"}"));
	}
}
