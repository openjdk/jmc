/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.ui.test.security;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.openjdk.jmc.common.security.PersistentCredentials;

@SuppressWarnings("nls")
public class MasterPasswordTest {

	private final String invalidPwdValue = "Jmc20"; // Invalid Length, Missing special character
	private final String validPwdValue = "Jmc@2022"; // Following all standards
	private final String invalidPwdValue1 = "Jmcu2022"; // Missing special character
	private final String invalidPwdValue2 = "JMC@2022"; // Missing lowercase
	private final String invalidPwdValue3 = "jmc!2022"; // Missing uppercase
	private final String invalidPwdValue4 = "Jmc#xyzs"; // Missing digit

	@Test
	public void testInvalidMasterPassword() throws Exception {
		boolean result = PersistentCredentials.isPasswordValid(invalidPwdValue);
		assertFalse(result);
	}

	@Test
	public void testValidMasterPassword() throws Exception {
		boolean result = PersistentCredentials.isPasswordValid(validPwdValue);
		assertTrue(result);
	}

	@Test
	public void testInvalidPwdMissingSpecialChar() throws Exception {
		boolean result = PersistentCredentials.isPasswordValid(invalidPwdValue1);
		assertFalse(result);
	}

	@Test
	public void testInvalidPwdMissingLowerCase() throws Exception {
		boolean result = PersistentCredentials.isPasswordValid(invalidPwdValue2);
		assertFalse(result);
	}

	@Test
	public void testInvalidPwdMissingUpperCase() throws Exception {
		boolean result = PersistentCredentials.isPasswordValid(invalidPwdValue3);
		assertFalse(result);
	}

	@Test
	public void testInvalidPwdMissingDigit() throws Exception {
		boolean result = PersistentCredentials.isPasswordValid(invalidPwdValue4);
		assertFalse(result);
	}
}
