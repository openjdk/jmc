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
 * Parses the {@code java.version} property and extracts the components to able to compare versions.
 * Handles Java 9 version {@code 9.1.3.0-ea}
 * (<i>{@code spec.feature.security.patch[-prebuildinfo]}</i>). Handles old Java versions
 * {@code 1.8.0_40-ea} (<i>{@code major.minor.micro_update[-prebuildinfo]}</i>). Does not do any
 * interpretation of the numbers. Does not handle comparison between {@code 9.1.0} and
 * {@code 1.9.2.0} (a version which should never exist).
 */
public class JavaVersion {

	private final int[] versionNumbers;
	private final boolean isEarlyAccess;
	/**
	 * Constant denoting an unparsable number.
	 */
	private static final int UNKNOWN = -1;

	/**
	 * Create an instance based on a Java version string as reported by a Java runtime.
	 *
	 * @param version
	 *            Java version string
	 */
	public JavaVersion(String version) {
		String[] numbers = version.split("[\\._]"); //$NON-NLS-1$
		int offset = 0;
		int versionNumbersLength = numbers.length;
		if (numbers.length > 0 && parseNumber(0, numbers) == 1) {
			offset = 1;
			versionNumbersLength = numbers.length - 1;
		}
		versionNumbers = new int[versionNumbersLength];
		for (int i = 0; i < versionNumbersLength; i++) {
			versionNumbers[i] = parseNumber(i + offset, numbers);
		}
		isEarlyAccess = version.contains("ea"); //$NON-NLS-1$
	}

	/**
	 * Create an instance based on version numbers.
	 *
	 * @param versionNumbers
	 *            One or more numbers denoting a Java version. The first number is the major
	 *            version, the second number is the minor version, the third number is the micro
	 *            version, and the fourth number is the update version. If one or more numbers are
	 *            omitted, then zeroes will be used for them.
	 */
	public JavaVersion(int ... versionNumbers) {
		this(false, versionNumbers);
	}

	/**
	 * Create an instance based on version numbers.
	 *
	 * @param isEarlyAccess
	 *            {@code true} if this version should indicate an early access build, {@code false}
	 *            otherwise
	 * @param versionNumbers
	 *            One or more numbers denoting a Java version. The first number is the major
	 *            version, the second number is the minor version, the third number is the micro
	 *            version, and the fourth number is the update version. If one or more numbers are
	 *            omitted, then zeroes will be used for them.
	 */
	public JavaVersion(boolean isEarlyAccess, int ... versionNumbers) {
		this.versionNumbers = versionNumbers;
		this.isEarlyAccess = isEarlyAccess;
	}

	private int parseNumber(int index, String[] numbers) {
		if (index + 1 > numbers.length) {
			return UNKNOWN;
		}
		StringBuilder numberStringBuilder = new StringBuilder();
		for (int i = 0; i < numbers[index].length(); i++) {
			char c = numbers[index].charAt(i);
			if (Character.isDigit(c)) {
				numberStringBuilder.append(c);
			} else {
				break;
			}
		}
		try {
			return Integer.parseInt(numberStringBuilder.toString());
		} catch (NumberFormatException nfe) {
			return UNKNOWN;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (int versionNumber : versionNumbers) {
			result = prime * result + versionNumber;
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JavaVersion other = (JavaVersion) obj;
		return isGreaterOrEqualThan(other) && other.isGreaterOrEqualThan(this);
	}

	/**
	 * Compare another version instance with this instance.
	 *
	 * @param otherVersion
	 *            version to compare with
	 * @return {@code true} if this instance is greater than or equal to {@code otherVersion}
	 */
	public boolean isGreaterOrEqualThan(JavaVersion otherVersion) {
		int maxLength = Math.max(versionNumbers.length, otherVersion.versionNumbers.length);
		for (int i = 0; i < maxLength; i++) {
			int thisNumber = versionNumbers.length > i ? versionNumbers[i] : 0;
			int otherNumber = otherVersion.versionNumbers.length > i ? otherVersion.versionNumbers[i] : 0;
			if (thisNumber != otherNumber) {
				return thisNumber > otherNumber;
			}
		}
		return !this.isEarlyAccess || otherVersion.isEarlyAccess;
	}

	/**
	 * Check if another version instance has the same major version as this instance.
	 *
	 * @param otherVersion
	 *            version to compare with
	 * @return {@code true} if this instance has the same major version number as
	 *         {@code otherVersion}
	 */
	public boolean isSameMajorVersion(JavaVersion otherVersion) {
		return otherVersion != null && versionNumbers.length > 0 && otherVersion.versionNumbers.length > 0
				&& versionNumbers[0] == otherVersion.versionNumbers[0];
	}

	/**
	 * @return the major version number
	 */
	public int getMajorVersion() {
		return versionNumbers[0];
	}

	@Override
	public String toString() {
		if (versionNumbers.length == 0) {
			return "[]"; //$NON-NLS-1$
		}
		StringBuilder b = new StringBuilder();
		b.append(versionNumbers[0]);
		for (int i = 1; i < versionNumbers.length; i++) {
			b.append('.');
			b.append(versionNumbers[i]);
		}
		return b.toString();
	}

	/**
	 * @return {@code true} if this instance indicates an early access release
	 */
	public boolean isEarlyAccess() {
		return isEarlyAccess;
	}
}
