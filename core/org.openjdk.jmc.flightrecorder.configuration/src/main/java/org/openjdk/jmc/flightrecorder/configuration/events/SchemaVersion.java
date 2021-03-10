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
package org.openjdk.jmc.flightrecorder.configuration.events;

import java.io.InputStream;

import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.configuration.internal.Messages;

public enum SchemaVersion {
	V1("1.0", "jfc_v1.xsd", Messages.getString(Messages.SchemaVersion_JDK_7_OR_8)), //$NON-NLS-1$ //$NON-NLS-2$
	V2("2.0", "jfc_v2.xsd", Messages.getString(Messages.SchemaVersion_JDK_9_AND_ABOVE)); //$NON-NLS-1$ //$NON-NLS-2$

	public static SchemaVersion fromBeanVersion(String beanVersion) {
		for (SchemaVersion version : values()) {
			if (version.version.equals(beanVersion)) {
				return version;
			}
		}
		return null;
	}

	public static SchemaVersion fromJavaVersion(JavaVersion javaVersion) {
		return javaVersion.isGreaterOrEqualThan(JavaVersionSupport.JDK_9) ? V2
				: javaVersion.isGreaterOrEqualThan(JavaVersionSupport.JDK_7_U_4) ? V1 : null;
	}

	private final String version;
	private final String xsdFile;
	private final String description;

	private SchemaVersion(String version, String xsdFile, String description) {
		this.version = version;
		this.xsdFile = xsdFile;
		this.description = description;
	}

	public String attributeValue() {
		return version;
	}

	public InputStream createSchemaStream() {
		return SchemaVersion.class.getResourceAsStream(xsdFile);
	}

	@Override
	public String toString() {
		return version + " (" + xsdFile + ')'; //$NON-NLS-1$
	}

	public String getDescription() {
		return description;
	}
}
