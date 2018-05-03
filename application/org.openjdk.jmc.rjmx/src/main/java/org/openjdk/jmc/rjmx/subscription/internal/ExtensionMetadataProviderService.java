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
package org.openjdk.jmc.rjmx.subscription.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProviderService;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * The holder of the metadata providers. Should typically not return metadata until all plug-ins are
 * loaded...
 */
public class ExtensionMetadataProviderService implements IMRIMetadataProviderService {

	private boolean m_hasInitializedExtensions = false;
	private final List<IMRIMetadataProviderService> m_providers = new ArrayList<>();

	private synchronized void checkExtenstionsInitialized() {
		if (!m_hasInitializedExtensions) {
			initializeFromExtensions();
			m_hasInitializedExtensions = true;
		}
	}

	private void initializeFromExtensions() {
		IExtensionRegistry er = Platform.getExtensionRegistry();
		IExtensionPoint ep = er.getExtensionPoint("org.openjdk.jmc.rjmx.metadataprovider"); //$NON-NLS-1$
		IExtension[] extensions = ep.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] configs = extension.getConfigurationElements();
			for (IConfigurationElement config : configs) {
				if (config.getName().equals("metadataProvider")) { //$NON-NLS-1$
					try {
						IMRIMetadataProviderService provider = (IMRIMetadataProviderService) config
								.createExecutableExtension("class"); //$NON-NLS-1$
						m_providers.add(provider);
					} catch (CoreException e) {
						RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
								"Could not instantiate metadata provider '" + config.getAttribute("class") + "'!", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
		}
	}

	@Override
	public Object getMetadata(IMRIMetadataService metadataService, MRI mri, String dataKey) {
		checkExtenstionsInitialized();
		for (IMRIMetadataProviderService providerService : m_providers) {
			Object value = providerService.getMetadata(metadataService, mri, dataKey);
			if (value != null) {
				return value;
			}
		}
		return null;
	}
}
