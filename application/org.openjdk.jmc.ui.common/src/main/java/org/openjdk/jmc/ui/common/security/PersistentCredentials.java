/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ICredentials} stored in the {@link ISecurityManager}. The username and password are lazy
 * loaded on demand.
 */
public class PersistentCredentials implements ICredentials {

	private final String id;
	private String[] wrapped;

	private static final Pattern PASSWORD_PATTERN = Pattern
			.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#(&)[{-}]:;',?/*~$^+=<>]).{8,20}$"); //$NON-NLS-1$

	public PersistentCredentials(String id) {
		this.id = id;
	}

	public PersistentCredentials(String username, String password) throws SecurityException {
		this(username, password, null);
	}

	public PersistentCredentials(String username, String password, String family) throws SecurityException {
		wrapped = new String[] {username, password};
		id = SecurityManagerFactory.getSecurityManager().storeInFamily(family, wrapped);
	}

	@Override
	public String getUsername() throws SecurityException {
		return getCredentials()[0];
	}

	@Override
	public String getPassword() throws SecurityException {
		return getCredentials()[1];
	}

	private String[] getCredentials() throws SecurityException {
		if (wrapped == null) {
			wrapped = (String[]) SecurityManagerFactory.getSecurityManager().get(id);
		}
		if (wrapped == null || wrapped.length != 2) {
			throw new CredentialsNotAvailableException();
		}
		return wrapped;
	}

	@Override
	public String getExportedId() {
		return id;
	}

	public static boolean isPasswordValid(final String password) {
		Matcher matcher = PASSWORD_PATTERN.matcher(password);
		return matcher.matches();
	}
}
