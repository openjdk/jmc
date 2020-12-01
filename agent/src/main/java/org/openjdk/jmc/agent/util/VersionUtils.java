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
package org.openjdk.jmc.agent.util;

import java.lang.reflect.Method;
import java.util.OptionalInt;

public final class VersionUtils {
	private static final JFRVersion AVAILABLE_JFR_VERSION = determineJFRVersion();
	private static final int FEATURE_VERSION = determineFeatureVersion();

	public enum JFRVersion {
		JFR, JFRNEXT, NONE
	}

	private VersionUtils() {
	}

	private static JFRVersion determineJFRVersion() {
		JFRVersion type = null;
		try {
			Class.forName("com.oracle.jrockit.jfr.InstantEvent"); //$NON-NLS-1$
			type = JFRVersion.JFR;
		} catch (ClassNotFoundException e) {
			try {
				Class.forName("jdk.jfr.ValueDescriptor"); //$NON-NLS-1$
				type = JFRVersion.JFRNEXT;
			} catch (ClassNotFoundException e2) {
				type = JFRVersion.NONE;
			}
		}
		return type;
	}

	/**
	 * Returns the current JVM's JFR version.
	 */
	public static JFRVersion getAvailableJFRVersion() {
		return AVAILABLE_JFR_VERSION;
	}

	/**
	 * Returns the current JVM's feature (major) version, e.g. 8, 11, or 15.
	 */
	public static OptionalInt getFeatureVersion() {
		return FEATURE_VERSION == 0 ? OptionalInt.empty() : OptionalInt.of(FEATURE_VERSION);
	}

	private static int determineFeatureVersion() {
		try {
			Method versionMethod = getMethod(Runtime.class, "version");

			// pre Java 9
			if (versionMethod == null) {
				String version = System.getProperty("java.version");
				return Integer.valueOf(version.substring(2, 3));
			}

			Object version = versionMethod.invoke(null);

			Method featureMethod = getMethod(version.getClass(), "feature");

			// Java 10+
			if (featureMethod != null) {
				return (int) featureMethod.invoke(version);
			} else {
				// Java 9
				Method majorMethod = getMethod(version.getClass(), "major");
				return (int) majorMethod.invoke(version);
			}

		} catch (Exception e) {
			return 0;
		}
	}

	private static Method getMethod(Class<?> clazz, String methodName) throws Exception {
		try {
			return clazz.getDeclaredMethod(methodName);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
}
