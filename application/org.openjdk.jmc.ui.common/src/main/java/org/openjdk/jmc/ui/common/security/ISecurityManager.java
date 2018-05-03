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

import java.util.Set;

/**
 * This is the interface for the security manager used by Mission Control. It may seem like a
 * security breach to use Strings for passwords (some may be concerned that they remain in the
 * system, since they are immutable and can not be cleared in the same manner as for instance a char
 * array). However, since the credentials must be made into strings to be passed into the JMX
 * environment when establishing a connection I may as well use strings to make the API simpler to
 * use.
 */
public interface ISecurityManager {

	/**
	 * Returns a stored object and removes it from the security manager
	 *
	 * @param key
	 *            the key of the stored object
	 * @return the stored value, or null if the store is locked. Will be a String[] or a byte[]
	 *         depending on which store method it was inserted with.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	Object withdraw(String key) throws SecurityException;

	/**
	 * When the store is accessible, all objects belonging to {@code family} that is not identified
	 * by {@code keys} will be removed
	 *
	 * @param family
	 *            the family to clear
	 * @param keys
	 *            the keys of the objects to keep
	 */
	void clearFamily(String family, Set<String> keys) throws FailedToSaveException;

	/**
	 * Returns a stored object
	 *
	 * @param key
	 *            the key of the stored object
	 * @return the stored value. Will be a String[] or a byte[] depending on which store method it
	 *         was inserted with.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	Object get(String key) throws SecurityException;

	/**
	 * @param key
	 *            the key of the object
	 * @param value
	 *            the value to store secure.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	void storeWithKey(String key, byte ... value) throws SecurityException;

	/**
	 * @param key
	 *            the key of the object
	 * @param value
	 *            the value to store secure.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	void storeWithKey(String key, String ... value) throws SecurityException;

	/**
	 * @param family
	 *            the family to which the object belongs.
	 * @param value
	 *            the value to store secure.
	 * @return the key of the stored object.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	String storeInFamily(String family, byte ... value) throws SecurityException;

	/**
	 * @param family
	 *            the family to which the object belongs.
	 * @param value
	 *            the value to store secure.
	 * @return the key of the stored object.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	String storeInFamily(String family, String ... value) throws SecurityException;

	/**
	 * @param value
	 *            the value to store secure.
	 * @return the key of the stored object.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	String store(byte ... value) throws SecurityException;

	/**
	 * @param value
	 *            the value to store secure.
	 * @return the key of the stored object.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	String store(String ... value) throws SecurityException;

	/**
	 * Unlocks the storage if it is locked
	 *
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 */
	void unlock() throws ActionNotGrantedException;

	/**
	 * @return true if the store is locked, false otherwise.
	 */
	boolean isLocked();

	/**
	 * @param key
	 *            the key of the stored object
	 * @return true if the key exists, false otherwise.
	 */
	boolean hasKey(String key);

	/**
	 * @return the supported encryption ciphers.
	 */
	Set<String> getEncryptionCiphers();

	/**
	 * @return the currently used encryption cipher.
	 */
	String getEncryptionCipher();

	/**
	 * Sets the encryption ciphers to use.
	 *
	 * @param encryptionCipher
	 *            the encryption cipher to use.
	 * @throws CipherNotAvailableException
	 *             if the provided cipher is not available.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	void setEncryptionCipher(String encryptionCipher) throws SecurityException;

	/**
	 * Triggers a change of the master password.
	 *
	 * @throws CipherNotAvailableException
	 *             if no cipher is available.
	 * @throws ActionNotGrantedException
	 *             if the master password was not given.
	 * @throws SecurityException
	 *             for other security problems.
	 */
	void changeMasterPassword() throws SecurityException;

}
