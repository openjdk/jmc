/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Red Hat Inc. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.common.services.IServiceFactory;
import org.openjdk.jmc.rjmx.common.services.internal.ServiceEntry;

public class ServiceFactoryInitializer {
	private final static String EXTENSION_POINT = "org.openjdk.jmc.rjmx.service";
	private final static String EXTENSION_ELEMENT_SERVICE = "service";
	private final static String EXTENSION_ATTRIBUTE_FACTORY = "factory";
	private final static String EXTENSION_ATTRIBUTE_NAME = "name";
	private final static String EXTENSION_ATTRIBUTE_DESCRIPTION = "description";

	public static List<ServiceEntry<?>> initializeFromExtensions() {
		List<ServiceEntry<?>> serviceEntries = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement config : registry.getConfigurationElementsFor(EXTENSION_POINT)) {
			if (config.getName().equals(EXTENSION_ELEMENT_SERVICE)) {
				try {
					IServiceFactory<?> factory = (IServiceFactory<?>) config
							.createExecutableExtension(EXTENSION_ATTRIBUTE_FACTORY);
					serviceEntries.add(createServiceEntry(factory, config));
				} catch (CoreException e) {
					RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not instantiate service factory!", e);
				}
			}
		}
		return serviceEntries;
	}

	private static <T> ServiceEntry<T> createServiceEntry(IServiceFactory<T> factory, IConfigurationElement config) {
		String name = config.getAttribute(EXTENSION_ATTRIBUTE_NAME);
		String description = config.getAttribute(EXTENSION_ATTRIBUTE_DESCRIPTION);
		return new ServiceEntry<>(factory, name, description);
	}
}
