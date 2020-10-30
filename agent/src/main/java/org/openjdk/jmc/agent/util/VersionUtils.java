/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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

public class VersionUtils {

	private static final int FEATURE_VERSION = determineFeatureVersion();

	public static int getFeatureVersion() {
		return FEATURE_VERSION;
	}

	private static int determineFeatureVersion() {
		try {
			Method versionMethod = getMethod(Runtime.class, "version");

			if (versionMethod == null) {
				String version = System.getProperty("java.version");
				return Integer.valueOf(version.substring(2, 3));
			}

			Object version = versionMethod.invoke(null);

			Method featureMethod = getMethod(version.getClass(), "feature");
			if (featureMethod != null) {
				return (int) featureMethod.invoke(version);
			} else {
				Method majorMethod = getMethod(version.getClass(), "major");
				return (int) majorMethod.invoke(version);
			}

		} catch (Exception e) {
			System.out.println(
					"Could not identify Java version. The agent will not work. If on JDK 11, try adding  --add-exports java.base/jdk.internal.misc=ALL-UNNAMED"); //$NON-NLS-1$
			e.printStackTrace();
			System.out.flush();
			System.exit(3);

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
