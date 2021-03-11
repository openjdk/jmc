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
package org.openjdk.jmc.rjmx.preferences;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import org.openjdk.jmc.common.security.SecurelyStoredByteArray;

public class JMXRMIPreferences extends SecurelyStoredByteArray {

	public static final String PROPERTY_KEY_KEYSTORE = "javax.net.ssl.keyStore"; //$NON-NLS-1$
	public static final String PROPERTY_KEY_TRUSTSTORE = "javax.net.ssl.trustStore"; //$NON-NLS-1$
	public static final String PROPERTY_KEY_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword"; //$NON-NLS-1$
	public static final String PROPERTY_KEY_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword"; //$NON-NLS-1$

	private static final JMXRMIPreferences INSTANCE = new JMXRMIPreferences();

	private Properties cache;

	private JMXRMIPreferences() {
		super("org.openjdk.jmc.rjmx.preferences.SslPreferences"); //$NON-NLS-1$
	}

	public static JMXRMIPreferences getInstance() {
		return INSTANCE;
	}

	@Override
	public void set(byte ... value) throws Exception {
		super.set(value);
		buildCache(value);
	}

	public Properties getProperties() throws Exception {
		synchronized (this) {
			if (cache != null) {
				return cache;
			}
		}
		byte[] bytes = get();
		if (bytes != null) {
			return buildCache(bytes);
		}
		return null;
	}

	@Override
	public void remove() throws Exception {
		super.remove();
		setCache(null);
	}

	private Properties buildCache(byte[] value) throws IOException {
		Properties props = new Properties();
		props.load(new ByteArrayInputStream(value));
		return setCache(props);
	}

	private synchronized Properties setCache(Properties props) {
		cache = props;
		return props;
	}
}
