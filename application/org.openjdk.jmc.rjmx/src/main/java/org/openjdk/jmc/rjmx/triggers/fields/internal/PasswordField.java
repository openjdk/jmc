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
package org.openjdk.jmc.rjmx.triggers.fields.internal;

import java.util.logging.Level;

import org.w3c.dom.Element;

import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.ui.common.security.SecurityException;
import org.openjdk.jmc.ui.common.security.SecurityManagerFactory;

/**
 * A field holding a password. Actually what is stored in the field is a key to the password stored
 * in the security manager unless the password is <tt>null</tt> or the empty string in which case
 * the empty string is stored.
 */
final public class PasswordField extends Field {

	public PasswordField(String id, String label, String defaultValue, String description) throws Exception {
		super(id, label, defaultValue, description);
	}

	@Override
	public void initializeFromXml(Element node) {
		// only store key in field, not create new password
		putValueAndUpdateListener(XmlToolkit.getSetting(node, getId(), "")); //$NON-NLS-1$
	}

	@Override
	String parsedValue(String password) throws SecurityException {
		return password;
	}

	@Override
	protected void putValue(String newValue) {
		String newPasswordKey = createPasswordKey(newValue);
		releasePassword();
		super.putValue(newPasswordKey);
	}

	private String createPasswordKey(String password) {
		String key = ""; //$NON-NLS-1$
		if (!isEmptyPassword(password)) {
			try {
				key = SecurityManagerFactory.getSecurityManager().store(password);
			} catch (SecurityException e) {
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
						"Could not store password for field " + getId() + '!', e); //$NON-NLS-1$
				throw new RuntimeException(e);
			}
		}
		return key;
	}

	@Override
	protected boolean equalsValue(String value) {
		String currentKey = getValue();
		if (isEmptyPassword(value)) {
			return isEmptyPassword(currentKey);
		}
		return value.equals(lookupPassword(currentKey));
	}

	private String lookupPassword(String key) {
		String password = ""; //$NON-NLS-1$
		try {
			if (!isEmptyPassword(key)) {
				password = ((String[]) SecurityManagerFactory.getSecurityManager().get(key))[0];
			}
		} catch (SecurityException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"Could not retrieve password for key " + key + " and field " + getId() + '!', e); //$NON-NLS-1$ //$NON-NLS-2$
			throw new RuntimeException(e);
		}
		return password;
	}

	/**
	 * Returns whether given string needs to be stored as a password. <tt>Null</tt> and the empty
	 * string does not and can be stored as themselves. This also means that it can be used to test
	 * a key if possible points to a password or not.
	 *
	 * @param password
	 *            the string to test
	 * @return <tt>true</tt> if <tt>null</tt> or empty, <tt>false</tt> otherwise
	 */
	private boolean isEmptyPassword(String password) {
		return password == null || password.length() == 0;
	}

	@Override
	public String getPassword() {
		return lookupPassword(getValue());
	}

	@Override
	public int getType() {
		return PASSWORD;
	}

	@Override
	void initDefaultValue(String defaultValue) {
		setValue(defaultValue);
	}

	@Override
	public void initDefaultPreferenceValue() {
		if (getId() != null) {
			String prefDefaultKey = RJMXPlugin.getDefault().getRJMXPreferences().get(getId(), null);
			if (prefDefaultKey != null && prefDefaultKey.trim().length() != 0) {
				putValueAndUpdateListener(prefDefaultKey);
			}
		}
	}

	private void putValueAndUpdateListener(String prefDefaultKey) {
		super.putValue(prefDefaultKey);
		updateListener();
	}

	/**
	 * Used to clear out the current password key and corresponding saved password. Ought to be
	 * invoked every time the fields password goes out of scope.
	 */
	/*
	 * FIXME: Currently it is only invoked when the field's value changes.
	 * 
	 * We have no knowledge when the password field is GC:ed. This leads to the possibility that up
	 * to one password can be leaked for each field in the UI. On the other hand, one typically uses
	 * password from preferences...
	 */
	private void releasePassword() {
		String key = getValue();
		if (!isEmptyPassword(key)) {
			try {
				String prefDefaultKey = null;
				if (getId() != null) {
					prefDefaultKey = RJMXPlugin.getDefault().getRJMXPreferences().get(getId(), null);
				}
				if (prefDefaultKey == null || !prefDefaultKey.equals(key)) {
					SecurityManagerFactory.getSecurityManager().withdraw(key);
				}
				super.putValue(""); //$NON-NLS-1$
			} catch (SecurityException e) {
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
						"Could not release password for field " + getId() + " on dispose!", e); //$NON-NLS-1$ //$NON-NLS-2$
				// Do not get too upset if we leak one. Will not notify end-user. If all else works, this _should_ work
				// as well.
			}
		}
	}

}
