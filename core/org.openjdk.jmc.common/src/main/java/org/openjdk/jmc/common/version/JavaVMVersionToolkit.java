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
package org.openjdk.jmc.common.version;

/**
 * A toolkit to handle JVM versions.
 */
public class JavaVMVersionToolkit {

	/**
	 * Since Sun decided not to have java.specification.version as a performance counter, we need to
	 * try to decode the specification version from the VM version.
	 *
	 * @param vmVersion
	 *            the full VM version.
	 * @return the java specification version, or something close to it.
	 */
	public static String decodeJavaVersion(String vmVersion) {
		String specVersion = vmVersion;
		if (vmVersion.startsWith("R") || vmVersion.startsWith("P") || vmVersion.startsWith("DEBUG-")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			// JRockit version...
			if (vmVersion.startsWith("DEBUG-")) { //$NON-NLS-1$
				specVersion = vmVersion.split("-")[4]; //$NON-NLS-1$
			} else {
				specVersion = vmVersion.split("-")[3]; //$NON-NLS-1$
			}
		}
		return parseJavaVersion(specVersion);
	}

	/**
	 * Parses version string to find java version, such as 1.6.
	 *
	 * @param version
	 *            A string that may contain a java version
	 * @return Short java version, or null
	 */
	public static String parseJavaVersion(String version) {
		int onePointIndex = version.indexOf("1."); //$NON-NLS-1$
		if (onePointIndex >= 0) {
			int nextPointIndex = version.indexOf('.', onePointIndex + 2);
			if (nextPointIndex >= 0 && isNumber(version.substring(onePointIndex + 2, nextPointIndex))) {
				return version.substring(onePointIndex, nextPointIndex);
			}
			return version.substring(onePointIndex);
		}
		return null;
	}

	private static boolean isNumber(String string) {
		try {
			Integer.parseInt(string);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Returns whether this is a JRockit JVM or not.
	 *
	 * @param vmName
	 *            the JVM name to check.
	 * @return <tt>true</tt> of it is a JRockit, <tt>false</tt> if it isn't or if was not possible
	 *         to tell.
	 */
	public static boolean isJRockitJVMName(String vmName) {
		if (vmName == null) {
			return false;
		}
		return vmName.startsWith("BEA JRockit") || vmName.startsWith("Oracle JRockit"); //$NON-NLS-1$ //$NON-NLS-2$;
	}

	/**
	 * Returns whether this is a HotSpot JVM or not.
	 *
	 * @param vmName
	 *            the JVM name to check.
	 * @return <tt>true</tt> if it is a HotSpot, <tt>false</tt> if it isn't or if was not possible
	 *         to tell.
	 */
	public static boolean isHotspotJVMName(String vmName) {
		if (vmName == null) {
			return false;
		}
		return vmName.startsWith("Java HotSpot") || vmName.startsWith("OpenJDK"); //$NON-NLS-1$ //$NON-NLS-2$;
	}

}
