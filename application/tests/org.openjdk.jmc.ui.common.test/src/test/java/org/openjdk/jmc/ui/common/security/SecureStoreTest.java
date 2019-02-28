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
package org.openjdk.jmc.ui.common.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.prefs.Preferences;

import org.openjdk.jmc.ui.common.security.SecureStore;
import org.openjdk.jmc.ui.common.security.SecurityException;
import org.openjdk.jmc.ui.common.util.Environment;

@SuppressWarnings("nls")
public class SecureStoreTest {

	private final String pwd = "test";
	private final String family = "family";
	private final String family2 = "family2";
	private final String value1 = "1";
	private final String value2 = "2";

	@BeforeClass
	public static void beforeClass() {
		Assume.assumeFalse("Skipping testing on Linux due to possible lack of entropy",
				Environment.getOSType() == Environment.OSType.LINUX);
	}

	@Test
	public void testEncryptDecrypt() throws Exception {
		Preferences prefs = new DummyPreferences();
		SecureStore store = new SecureStore(prefs);
		store.initialize();
		store.setPassword(pwd);
		SecureStore decryptedStore = new SecureStore(prefs);
		assertTrue(!decryptedStore.isInitialized());
		decryptedStore.initialize(pwd);
		assertTrue(decryptedStore.isInitialized());
		assertEquals(store.insert(null, true, value2), decryptedStore.insert(null, true, value2));
		assertEquals(store.insert(family, true, value1), decryptedStore.insert(family, true, value1));
		assertEquals(store.insert(family, false, value1), family);
		assertFalse(store.insert(family, true, value1).equals(decryptedStore.insert(null, true, value1)));

	}

	@Test(expected = SecurityException.class)
	public void testInvalidCipher() throws Exception {
		Preferences prefs = new DummyPreferences();
		SecureStore store = new SecureStore(prefs);
		store.initialize();
		store.setPassword(pwd);
		store.setEncryptionCipher("UnknownCipher");
	}

	@Test
	public void testInsert() throws Exception {
		Preferences prefs = new DummyPreferences();
		SecureStore store = new SecureStore(prefs);
		store.initialize();
		store.setPassword(pwd);
		String key = store.insert(family, true, value1);
		String key2 = store.insert(family, true, value1);
		String key3 = store.insert(family, true, new String[] {value1, value2});
		assertFalse(key.equals(key2));
		SecureStore decryptedStore = new SecureStore(prefs);
		decryptedStore.initialize(pwd);
		assertEquals(value1, decryptedStore.get(key));
		assertEquals(2, ((String[]) decryptedStore.get(key3)).length);
	}

	@Test
	public void testRemove() throws Exception {
		Preferences prefs = new DummyPreferences();
		SecureStore store = new SecureStore(prefs);
		store.initialize();
		store.setPassword(pwd);
		String key = store.insert(family, true, value1);
		String key2 = store.insert(family, true, value1);
		SecureStore decryptedStore = new SecureStore(prefs);
		assertTrue(decryptedStore.hasKey(key2));
		assertNull(value1, decryptedStore.remove(key2));
		assertFalse(decryptedStore.hasKey(key2));
		decryptedStore.initialize(pwd);
		assertEquals(value1, decryptedStore.get(key));
		assertEquals(value1, decryptedStore.remove(key));
		assertNull(decryptedStore.get(key));
		assertNull(decryptedStore.get(key2));
	}

	@Test
	public void testInvalidPassword() throws Exception {
		Preferences prefs = new DummyPreferences();
		SecureStore store = new SecureStore(prefs);
		store.initialize();
		store.setPassword(pwd);
		store.insert(null, true, value1);
		store = new SecureStore(prefs);
		try {
			store.initialize("other");
			fail();
		} catch (Exception e) {
		}
	}

	@Test
	public void testAllCiphers() throws Exception {
		Preferences prefs = new DummyPreferences();
		SecureStore store = new SecureStore(prefs);
		store.initialize();
		store.setPassword(pwd);
		String key = store.insert(null, true, value1);
		for (String cipher : SecureStore.ENCRYPTION_CIPHERS) {
			store.setEncryptionCipher(cipher);
			store = new SecureStore(prefs);
			store.initialize(pwd);
			assertEquals(cipher, store.getEncryptionCipher());
			assertEquals(value1, store.get(key));
			System.out.println(cipher + " tested ok");
		}
	}

	@Test
	public void testChangePassword() throws Exception {
		Preferences prefs = new DummyPreferences();
		SecureStore store = new SecureStore(prefs);
		store.initialize();
		store.setPassword(pwd);
		String key = store.insert(null, true, value1);
		store = new SecureStore(prefs);
		store.initialize(pwd);
		store.setPassword("newPwd");
		store = new SecureStore(prefs);
		store.initialize("newPwd");
		assertEquals(value1, store.get(key));
	}

	@Test
	public void testClearFamily() throws Exception {
		Preferences prefs = new DummyPreferences();
		SecureStore store = new SecureStore(prefs);
		store.initialize();
		store.setPassword(pwd);
		String key = store.insert(family, true, value1);
		String key2 = store.insert(family2, true, value1);
		store = new SecureStore(prefs);
		assertTrue(store.hasKey(key));
		assertTrue(store.hasKey(key2));
		store.clearFamily(family, new HashSet<>(Arrays.asList(key)));
		assertTrue(store.hasKey(key));
		store.clearFamily(family, Collections.<String> emptySet());
		assertFalse(store.hasKey(key));
		store.initialize(pwd);
		assertTrue(store.hasKey(key2));
		assertNull(store.get(key));
		assertEquals(value1, store.get(key2));
		store.clearFamily(family2, Collections.<String> emptySet());
		assertNull(value1, store.get(key2));
	}
}
