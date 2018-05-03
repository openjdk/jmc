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
package org.openjdk.jmc.common.util;

import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCType;

/**
 * Base implementation of the {@link IMCType} interface.
 */
// FIXME: Move MC* classes and related toolkits to a separate package?
public class MCType implements IMCType {

	/**
	 * Constant indicating an unknown Java type. Only used to avoid returning null types.
	 */
	static final IMCType UNKNOWN = new MCType(""); //$NON-NLS-1$

	private static final char PACKAGE_SEPARATOR = '.';
	private static final char INNER_CLASS_SEPARATOR = '$';

	private final String fullName;
	private final String typeName;
	private final IMCPackage _package;

	/**
	 * Create an instance from a binary Java type name according to <i>The Java Language
	 * Specification</i>, Section 13.1.
	 * 
	 * @param jlsTypeName
	 *            JLS type name
	 */
	MCType(String jlsTypeName) {
		fullName = jlsTypeName;
		int point = findPackageNameLength(jlsTypeName);
		String packageName;
		if (point >= 0) {
			packageName = jlsTypeName.substring(0, point);
			typeName = jlsTypeName.substring(point + 1);
		} else {
			packageName = ""; //$NON-NLS-1$
			typeName = jlsTypeName;
		}
		_package = new MCPackage(packageName, null, null);
	}

	private static int findPackageNameLength(String jlsTypeName) {
		int point = -1;
		for (int i = 0; i < jlsTypeName.length(); i++) {
			switch (jlsTypeName.charAt(i)) {
			case PACKAGE_SEPARATOR:
				point = i;
				break;
			case INNER_CLASS_SEPARATOR:
				return point;
			default:
			}
		}
		return point;
	}

	@Override
	public IMCPackage getPackage() {
		return _package;
	}

	@Override
	final public String getTypeName() {
		return typeName;
	}

	@Override
	final public String getFullName() {
		return fullName;
	}

	@Override
	public String toString() {
		return fullName;
	}

	@Override
	public int hashCode() {
		return fullName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof MCType && ((MCType) obj).fullName.equals(fullName);
	}

}
