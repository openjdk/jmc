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
package org.openjdk.jmc.rjmx.triggers.extension.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;

/**
 * Helper class for loading extensions
 */
public class ExtensionLoader<T> {
	private final String m_extensionPoint;
	private final String m_extensionName;
	private final HashMap<String, IConfigurationElement> m_extensions = new HashMap<>();
	private final ArrayList<T> m_prototypes = new ArrayList<>();

	public ExtensionLoader(String extensionPoint, String extensionName) {
		m_extensionPoint = extensionPoint;
		m_extensionName = extensionName;

		load();
	}

	public String getExtensionName() {
		return m_extensionName;
	}

	public String getExtensionPointId() {
		return m_extensionPoint;
	}

	protected void load() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint exPoint = registry.getExtensionPoint(getExtensionPointId());
		if (exPoint == null) {
			return;
		}
		try {
			IExtension[] ext = exPoint.getExtensions();
			for (IExtension element2 : ext) {
				IConfigurationElement[] element = element2.getConfigurationElements();
				for (IConfigurationElement element3 : element) {
					if (element3.getName().equalsIgnoreCase(getExtensionName())) {
						initExtension((element3));
					}
				}
			}
		} catch (InvalidRegistryObjectException iroe) {
			// FIXME: Better error handling
			System.err.println("Extension point not valid."); //$NON-NLS-1$
			System.err.println(iroe.getMessage());
		}
	}

	public IConfigurationElement getConfigElement(String className) {
		return m_extensions.get(className);
	}

	public void initExtension(IConfigurationElement element) {
		try {
			@SuppressWarnings("unchecked")
			T prototype = (T) element.createExecutableExtension("class"); //$NON-NLS-1$
			String className = element.getAttribute("class"); //$NON-NLS-1$

			m_extensions.put(className, element);
			m_prototypes.add(prototype);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public Collection<T> getPrototypes() {
		return m_prototypes;
	}

}
