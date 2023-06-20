/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.rjmx.internal;

import java.util.Properties;
import java.util.logging.Level;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.JMXRMIPreferences;

public final class JMXRMISystemPropertiesProvider {

	public static void setup() {
		try {
			Properties jmxRmiProperties = JMXRMIPreferences.getInstance().getProperties();
			if (jmxRmiProperties != null) {
				for (String prop : jmxRmiProperties.stringPropertyNames()) {
					Object val = jmxRmiProperties.get(prop);
					if (val != null && !val.toString().isEmpty()) {
						System.setProperty(prop, val.toString());
					}
				}
			}
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.FINE, "Did not load jmxRmiProperties", e); //$NON-NLS-1$
		}
	}

	public static void clearJMXRMISystemProperties() {
		try {
			Properties jmxRmiProperties = JMXRMIPreferences.getInstance().getProperties();
			if (jmxRmiProperties != null) {
				for (String prop : jmxRmiProperties.stringPropertyNames()) {
					System.clearProperty(prop);
				}
			}
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.FINE, "Failed to remove JMXRMI SystemProperties", e); //$NON-NLS-1$
		}
	}

	public static boolean isKeyStoreConfigured() {
		try {
			Properties jmxRmiProperties = JMXRMIPreferences.getInstance().getProperties();
			if (jmxRmiProperties != null) {
				int totalPrefCnt = 0;
				for (String prop : jmxRmiProperties.stringPropertyNames()) {
					Object val = jmxRmiProperties.get(prop);
					if (val != null && !val.toString().isEmpty()) {
						++totalPrefCnt;
					}
				}
				if (totalPrefCnt == 4) {
					return true;
				}
			}
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.FINE, "Did not load jmxRmiProperties", e); //$NON-NLS-1$
		}
		return false;
	}
}
