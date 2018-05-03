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
package org.openjdk.jmc.rjmx.internal;

import org.openjdk.jmc.rjmx.ISyntheticAttribute;
import org.openjdk.jmc.rjmx.subscription.MRI;

public class SyntheticAttributeEntry {
	private final ISyntheticAttribute attribute;
	private final MRI descriptor;
	private final String description;
	private final String type;
	private final boolean readable;
	private final boolean writeable;
	private final boolean isIs;

	public SyntheticAttributeEntry(ISyntheticAttribute attribute, MRI descriptor, String description, String type,
			boolean readable, boolean writeable, boolean isIs) {
		this.attribute = attribute;
		this.descriptor = descriptor;
		this.description = description;
		this.type = type;
		this.readable = readable;
		this.writeable = writeable;
		this.isIs = isIs;
	}

	public ISyntheticAttribute getAttribute() {
		return attribute;
	}

	public String getName() {
		return descriptor.getQualifiedName();
	}

	public String getDescription() {
		return description;
	}

	public String getType() {
		return type;
	}

	public boolean isReadable() {
		return readable;
	}

	public boolean isWriteable() {
		return writeable;
	}

	public MRI getAttributeDescriptor() {
		return descriptor;
	}

	public boolean isIs() {
		return isIs;
	}

	@Override
	public String toString() {
		return getName() + '[' + type + ", " + flags() + ']'; //$NON-NLS-1$
	}

	private String flags() {
		String flags = ""; //$NON-NLS-1$
		if (isReadable()) {
			flags += 'R';
		}
		if (isWriteable()) {
			flags += 'W';
		}
		if (isIs()) {
			flags += 'I';
		}
		return flags;
	}

	@Override
	public int hashCode() {
		return descriptor.hashCode();
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
		SyntheticAttributeEntry other = (SyntheticAttributeEntry) obj;
		if (descriptor == null) {
			if (other.descriptor != null) {
				return false;
			}
		} else if (!descriptor.equals(other.descriptor)) {
			return false;
		}
		return true;
	}
}
