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

import java.text.MessageFormat;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCModule;
import org.openjdk.jmc.common.IMCPackage;

/**
 * Base implementation of the {@link IMCPackage} interface.
 */
// FIXME: Move MC* classes and related toolkits to a separate package?
public class MCPackage implements IMCPackage, IDescribable {
	private final String name;
	private final IMCModule module;
	private final Boolean isExported;

	/**
	 * Create a new package instance.
	 *
	 * @param name
	 *            package name
	 * @param module
	 *            module that the package resides, or {@code null} if it is in a pre-modules
	 *            environment
	 * @param isExported
	 *            If the package is exported by the module or not. Use {@code true} if it is in a
	 *            pre-modules environment.
	 */
	// FIXME: Should null be recommended for unknown export status?
	MCPackage(String name, IMCModule module, Boolean isExported) {
		this.name = name;
		this.module = module;
		this.isExported = isExported;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IMCModule getModule() {
		return module;
	}

	@Override
	public Boolean isExported() {
		return isExported;
	}

	@Override
	public String toString() {
		return "Package: " + getName(); //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return MessageFormat.format("{0} (module={1}, exported={2})", getName(), //$NON-NLS-1$
				getModule() != null ? getModule().getName() : null, isExported());
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj instanceof MCPackage && ((MCPackage) obj).name.equals(name);
	}
}
