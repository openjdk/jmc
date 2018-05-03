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

import org.openjdk.jmc.common.IDescribable;

/**
 * An identifier with a name and a description. The identifier value is composed of a string and a
 * long value, but the actual values of these should normally not be that interesting. The important
 * part is that it can be compared using {@link #equals(Object)}.
 */
// FIXME: This is almost only used while reading flight recordings. Can we move it there?
public class LabeledIdentifier implements IDescribable {

	private final String interfaceId;
	private final long implId;
	private final String name;
	private final String description;

	/**
	 * Create a new instance.
	 * 
	 * @param interfaceId
	 *            interface part of the identifier value
	 * @param implId
	 *            implementation part of the identifier value
	 * @param name
	 *            identifier name
	 * @param description
	 *            identifier description
	 */
	public LabeledIdentifier(String interfaceId, long implId, String name, String description) {
		this.interfaceId = interfaceId;
		this.implId = implId;
		this.name = name;
		this.description = description;
	}

	/**
	 * @return interface part of the identifier value
	 */
	public String getInterfaceId() {
		return interfaceId;
	}

	/**
	 * @return implementation part of the identifier value
	 */
	public long getImplementationId() {
		return implId;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * The {@link #getDescription()} method gives a generated description. Use this method to get
	 * the description passed into the constructor.
	 * 
	 * @return identifier description
	 */
	public String getDeclaredDescription() {
		return description;
	}

	@Override
	public String getDescription() {
		return interfaceId + " (" + implId + ")" + (description == null ? "" : " " + description); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public int hashCode() {
		return (int) ((implId >>> 32) ^ implId) ^ interfaceId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof LabeledIdentifier) && ((LabeledIdentifier) obj).implId == implId
				&& ((LabeledIdentifier) obj).interfaceId.equals(interfaceId);
	}

	@Override
	public String toString() {
		return getName();
	};
}
