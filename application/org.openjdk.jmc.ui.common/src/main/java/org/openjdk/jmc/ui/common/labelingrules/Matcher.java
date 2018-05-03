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

import java.text.MessageFormat;
import java.util.Locale;

/**
 * Helper class for doing the matching.
 */
final class Matcher {
	private final String matchPart;
	private MessageFormat matchMessageFormat;

	/**
	 * Constructor
	 */
	public Matcher(String matchPart) {
		this.matchPart = matchPart.replace('=', '\0');
		initialize(this.matchPart);
	}

	private void initialize(String matchPart) {
		matchMessageFormat = new MessageFormat(matchPart, new Locale("sv", "SE")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param vals
	 *            the array of values to check.
	 * @return true if the set of values matches the matching part of the rule.
	 */
	public boolean matches(Object[] vals) {
		// Resolving all variables...
		String resolved = matchMessageFormat.format(vals).replace('\n', ' ');
		String[] parts = resolved.split("\0"); //$NON-NLS-1$
		String left = parts[0].trim();
		String right = parts[1].trim();
		return left.equals(right) || left.matches(right);
	}

	@Override
	public String toString() {
		return "Matcher [" + matchPart + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
