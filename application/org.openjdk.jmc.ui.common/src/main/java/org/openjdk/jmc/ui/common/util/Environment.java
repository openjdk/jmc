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
package org.openjdk.jmc.ui.common.util;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that returns information about the OS and other things related to the local environment
 */
public class Environment {
	/**
	 * Known OS types.
	 */
	public enum OSType {
		WINDOWS, MAC, SOLARIS, LINUX, UNKNOWN_UNIX, UNKNOWN;

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private static final String OS_NAME = System.getProperty("os.name"); //$NON-NLS-1$
	private static final OSType OS_TYPE = determineOSType(getFullOSName());
	private static final boolean IS_DEBUG = Boolean.getBoolean("org.openjdk.jmc.debug"); //$NON-NLS-1$
	private static final int LOCAL_PID; // The PID of the JVM running this mission control.

	static {
		LOCAL_PID = initPID();
		if (LOCAL_PID != 0) {
			Logger.getLogger("org.openjdk.jmc.common.mbean").log(Level.FINE, //$NON-NLS-1$
					"Acquired local PID. PID resolved to " + LOCAL_PID); //$NON-NLS-1$
		}
	}

	/**
	 * Tell if this Mission Control instance is in debug mode, i.e. was started up with the debug
	 * flag {@code -Dmcdebug=true}.
	 *
	 * @return {@code true} if in debug mode
	 */
	public static boolean isDebug() {
		return IS_DEBUG;
	}

	/**
	 * @return the full OS name found from the system property "os.name"
	 */
	public static String getFullOSName() {
		return OS_NAME;
	}

	/**
	 * @return the OS type
	 */
	public static OSType getOSType() {
		return OS_TYPE;
	}

	/**
	 * @return the "normal" 100% scaled DPI setting of the OS, 96 for Windows & Linux and 72 for
	 *         MacOS.
	 */
	public static double getNormalDPI() {
		return OS_TYPE.equals(OSType.MAC) ? 72d : 96d;
	}

	/**
	 * @return the OS type
	 */
	private static OSType determineOSType(String os) {
		if (os == null) {
			return OSType.UNKNOWN;
		}
		os = os.toLowerCase();

		if (os.contains("win")) { //$NON-NLS-1$
			return OSType.WINDOWS;
		}
		if (os.contains("mac")) { //$NON-NLS-1$
			return OSType.MAC;
		}
		if (os.contains("sunos")) { //$NON-NLS-1$
			return OSType.SOLARIS;
		}
		if (os.contains("linux")) { //$NON-NLS-1$
			return OSType.LINUX;
		}
		if (os.contains("nix") || os.contains("nux")) { //$NON-NLS-1$ //$NON-NLS-2$
			return OSType.UNKNOWN_UNIX;
		}
		return OSType.UNKNOWN;
	}

	/**
	 * @return the PID of this JVM, or 0 if no PID could be determined
	 */
	public static int getThisPID() {
		return LOCAL_PID;
	}

	private static int initPID() {
		try {
			String name = ManagementFactory.getRuntimeMXBean().getName();
			if (name != null) {
				String s = name.split("@")[0]; //$NON-NLS-1$
				return Integer.parseInt(s);
			}
		} catch (Exception e) {
			Logger.getLogger("org.openjdk.jmc.common.mbean").log(Level.FINE, //$NON-NLS-1$
					"Could not retrieve PID of this running jvm instance", e); //$NON-NLS-1$
		}
		return 0;
	}
}
