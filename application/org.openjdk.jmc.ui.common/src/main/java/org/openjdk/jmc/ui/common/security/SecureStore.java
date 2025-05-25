/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.ui.common.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Assert;
import org.osgi.service.prefs.Preferences;

import org.openjdk.jmc.common.io.ValidatingObjectInputStream;
import org.openjdk.jmc.common.security.FailedToSaveException;
import org.openjdk.jmc.common.security.SecurityException;
import org.openjdk.jmc.ui.common.CorePlugin;

/**
 * Key-value-store that can be stored encrypted
 */
public class SecureStore {

	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.ui.common"); //$NON-NLS-1$

	public static final Set<String> ENCRYPTION_CIPHERS;
	public static final String DEFAULT_CIPHER;
	private static final String XML_ELEMENT_SECURE_STORE = "secureStore"; //$NON-NLS-1$
	private static final String XML_ELEMENT_KEYS = "secureStoreKeys"; //$NON-NLS-1$
	private static final String XML_ELEMENT_CIPHER = "secureStoreCipher"; //$NON-NLS-1$
	private static final String[] PREFERRED_CIPHERS = new String[] {"PBEWithHmacSHA512AndAES_256", //$NON-NLS-1$
			"PBEWithHmacSHA512AndAES_128"}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String SEP = "_"; //$NON-NLS-1$
	private DecryptedStorage storage;
	private String pwd;
	private String cipher;
	private Preferences prefs;
	private Set<String> keys;

	private static final String CIPHER_PREFERENCES = "jmc.cipherPref"; //$NON-NLS-1$
	private static final String DEFAULT_CIPHER_PREFERENCE = "PBEWithHmacSHA512AndAES_256"; //$NON-NLS-1$
	private final static String CIPHER_PREFERENCES_LIST;

	static {
		Properties cipherPrefProperties = getCipherPreferenceList();
		CIPHER_PREFERENCES_LIST = getCipherPrefProperty(cipherPrefProperties, CIPHER_PREFERENCES,
				DEFAULT_CIPHER_PREFERENCE);
		Set<String> ciphers = new HashSet<>();

		for (String cipher : populateCipherList()) {
			ciphers.add(cipher);
		}

		ENCRYPTION_CIPHERS = Collections.unmodifiableSet(ciphers);
		DEFAULT_CIPHER = findDefaultCipher();
	}

	private static String findDefaultCipher() {
		for (String pc : PREFERRED_CIPHERS) {
			if (ENCRYPTION_CIPHERS.contains(pc)) {
				return pc;
			}
		}
		String lastCipher = null;
		for (String c : ENCRYPTION_CIPHERS) {
			lastCipher = c;
		}
		return lastCipher;
	}

	/**
	 * Create a new instance based on a specific preferences object. Normally you should use
	 * {@link SecureStore#createDefault()} to get the default secure store.
	 * 
	 * @param prefs
	 *            preferences object containing secure store data
	 */
	public SecureStore(Preferences prefs) {
		byte[] keysData = prefs.getByteArray(XML_ELEMENT_KEYS, null);
		if (keysData == null) {
			keys = new HashSet<>();
		} else {
			try (ValidatingObjectInputStream ois = ValidatingObjectInputStream
					.build(new ByteArrayInputStream(keysData))) {
				@SuppressWarnings("unchecked")
				HashSet<String> k = ois.safeReadObject(HashSet.class, Arrays.asList(String.class), 100000, 1000000);
				keys = k;
			} catch (Exception e) {
				CorePlugin.getDefault().getLogger().log(Level.WARNING, "Could not load SecureStore keys", e); //$NON-NLS-1$
				keys = new HashSet<>();
			}
		}
		if (!ENCRYPTION_CIPHERS.isEmpty()) {
			this.prefs = prefs;
			cipher = prefs.get(XML_ELEMENT_CIPHER, DEFAULT_CIPHER);
		} else {
			doInitialize(new DecryptedStorage());
		}
	}

	/**
	 * @return the default secure store
	 */
	public static SecureStore createDefault() {
		return new SecureStore(CorePlugin.getDefault().getPreferences().node(SecureStore.class.getName()));
	}

	// Any classes that we support storing of must be cleared for deserialization in DecryptedStorage
	private synchronized String insertInternal(String key, boolean keyFamily, Object value)
			throws FailedToSaveException {
		Assert.isTrue(isInitialized());
		key = keyFamily || key == null ? generateKey(key) : key;
		storage.objects.put(key, value);
		keys.add(key);
		save();
		return key;
	}

	public String insert(String key, boolean keyFamily, String value) throws FailedToSaveException {
		return insertInternal(key, keyFamily, value);
	}

	public String insert(String key, boolean keyFamily, String[] value) throws FailedToSaveException {
		return insertInternal(key, keyFamily, value);
	}

	public String insert(String key, boolean keyFamily, byte[] value) throws FailedToSaveException {
		return insertInternal(key, keyFamily, value);
	}

	private String generateKey(String family) {
		return (storage.nextId++) + SEP + storage.storeId.toString() + (family == null ? "" : SEP + family); //$NON-NLS-1$
	}

	public synchronized Object get(String key) {
		Assert.isTrue(isInitialized());
		return storage.objects.get(key);
	}

	public synchronized void clearFamily(String family, Set<String> keepKeys) throws FailedToSaveException {
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String key = it.next();
			String[] keyParts = key.split(SEP);
			if (keyParts.length == 3 && keyParts[2].equals(family) && !keepKeys.contains(key)) {
				it.remove();
			}
		}
		if (isInitialized()) {
			cleanupStorage();
		}
		save();
	}

	public synchronized boolean hasKey(String key) {
		return keys.contains(key);
	}

	private void cleanupStorage() {
		Set<String> removeKeys = new HashSet<>(storage.objects.keySet());
		removeKeys.removeAll(keys);
		for (String key : removeKeys) {
			storage.objects.remove(key);
		}
	}

	public synchronized Object remove(String key) throws FailedToSaveException {
		Object o = null;
		keys.remove(key);
		if (isInitialized()) {
			o = storage.objects.remove(key);
		}
		save();
		return o;
	}

	public synchronized String getEncryptionCipher() {
		return cipher;
	}

	public synchronized void setEncryptionCipher(String cipher) throws SecurityException {
		Assert.isTrue(isInitialized());
		if (cipher == null || !ENCRYPTION_CIPHERS.contains(cipher)) {
			throw new SecurityException("Cipher " + cipher + " is not available"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.cipher = cipher;
		save();
	}

	public synchronized boolean isInitialized() {
		return storage != null;
	}

	public synchronized boolean isEncrypted() {
		return !isInitialized() && prefs.get(XML_ELEMENT_SECURE_STORE, null) != null;
	}

	public synchronized boolean isPersistable() {
		return prefs != null;
	}

	public synchronized void initialize() {
		if (!isInitialized()) {
			doInitialize(new DecryptedStorage());
			cipher = DEFAULT_CIPHER;
			keys = new HashSet<>();
		}
	}

	public synchronized void initialize(String pwd) throws Exception {
		Assert.isTrue(isEncrypted());
		doInitialize(new DecryptedStorage(prefs.get(XML_ELEMENT_SECURE_STORE, null), cipher, pwd));
		this.pwd = pwd;
		cleanupStorage();
		save();
	}

	public synchronized void setPassword(String pwd) throws FailedToSaveException {
		Assert.isTrue(isInitialized());
		this.pwd = pwd;
		save();
	}

	private synchronized void doInitialize(DecryptedStorage storage) {
		this.storage = storage;
	}

	private void save() throws FailedToSaveException {
		try {
			if (isPersistable()) {
				if (isInitialized()) {
					String encrypted = storage.getEncrypted(cipher, pwd);
					prefs.put(XML_ELEMENT_CIPHER, cipher);
					prefs.put(XML_ELEMENT_SECURE_STORE, encrypted);
				}
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(keys);
				oos.flush();
				prefs.putByteArray(XML_ELEMENT_KEYS, bos.toByteArray());
				prefs.flush();
			}
		} catch (Exception e) {
			prefs = null;
			CorePlugin.getDefault().getLogger().log(Level.SEVERE, "Could not save secure store", e); //$NON-NLS-1$
			throw new FailedToSaveException(e);
		}
	}

	private static String[] populateCipherList() {
		String[] ciphers = CIPHER_PREFERENCES_LIST.split(",");
		return ciphers;
	}

	private static String getCipherPrefProperty(
		Properties cipherPrefProperties, String propertyName, String defaultValue) {
		if (cipherPrefProperties != null) {
			String propertyValue = cipherPrefProperties.getProperty(propertyName);
			if (propertyValue != null && !propertyValue.startsWith("@")) { //$NON-NLS-1$
				return propertyValue;
			}
		}
		return defaultValue;
	}

	private static Properties getCipherPreferenceList() {
		Properties cipherPrefProperties = new Properties();
		try (InputStream is = SecureStore.class.getResourceAsStream("/preferences.properties")) { //$NON-NLS-1$
			if (is == null) {
				LOGGER.log(Level.SEVERE, "Could not open preferences.properties file."); //$NON-NLS-1$
				return null;
			}
			cipherPrefProperties.load(is);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error loading preferences.properties file.", e); //$NON-NLS-1$
			return null;
		}
		return cipherPrefProperties;

	}

}
