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
package org.openjdk.jmc.rjmx.actionprovider.internal;

import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.CLASS_ATTRIBUTE;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.EXTENSION_ELEMENT_ACTION;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.EXTENSION_ELEMENT_ADDON;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.EXTENSION_ELEMENT_PROVIDER;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.EXTENSION_ELEMENT_PROVIDER_FACTORY;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.EXTENSION_POINT;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.LOCATION_ATTRIBUTE;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.PRIORITY_ATTRIBUTE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.actionprovider.IActionProviderFactory;
import org.openjdk.jmc.ui.common.action.IActionProvider;

public class ActionProviderRepository {

	public static IActionProvider buildActionProvider(IServerHandle handle) {
		ActionProvider ap = new ActionProvider();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		List<IConfigurationElement> addOnElements = new ArrayList<>();
		Map<String, ActionProviderDescriptor> declaredProviders = new HashMap<>();
		List<IConfigurationElement> configurationElements = Arrays
				.asList(registry.getConfigurationElementsFor(EXTENSION_POINT));

		Comparator<IConfigurationElement> comparator = new Comparator<IConfigurationElement>() {

			public int getPrio(IConfigurationElement e) {
				String prio = e.getAttribute(PRIORITY_ATTRIBUTE);
				return prio == null ? 0 : Integer.parseInt(prio);
			}

			@Override
			public int compare(IConfigurationElement o1, IConfigurationElement o2) {
				return getPrio(o1) - getPrio(o2);
			}
		};

		Collections.sort(configurationElements, comparator);
		for (IConfigurationElement element : configurationElements) {
			try {
				loadElement(element, handle, ap, declaredProviders);
				if (element.getName().equals(EXTENSION_ELEMENT_ADDON)) {
					addOnElements.add(element);
				}
			} catch (Exception e) {
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
						"Could not load extension for " + EXTENSION_POINT, e); //$NON-NLS-1$
			}
		}

		Collections.sort(addOnElements, comparator);
		for (IConfigurationElement element : addOnElements) {
			String id = element.getAttribute(LOCATION_ATTRIBUTE);
			ActionProviderDescriptor apd = declaredProviders.get(id);
			if (apd != null) {
				try {
					for (IConfigurationElement e : element.getChildren()) {
						loadElement(e, handle, apd, null);
					}
				} catch (Exception e) {
					RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
							"Could not load extension for " + EXTENSION_POINT, e); //$NON-NLS-1$
				}
			}
		}
		return ap;
	}

	private static IActionProvider buildProvider(
		IConfigurationElement providerElement, IServerHandle handle,
		Map<String, ActionProviderDescriptor> declaredProviders) throws Exception {
		ActionProviderDescriptor adp = new ActionProviderDescriptor(providerElement);
		for (IConfigurationElement e : providerElement.getChildren()) {
			loadElement(e, handle, adp, declaredProviders);
		}
		if (adp.getId() != null && declaredProviders != null) {
			declaredProviders.put(adp.getId(), adp);
		}
		return adp;
	}

	private static void loadElement(
		IConfigurationElement element, IServerHandle handle, ActionProvider into,
		Map<String, ActionProviderDescriptor> declaredProviders) throws Exception {
		if (element.getName().equals(EXTENSION_ELEMENT_ACTION)) {
			into.getActions().add(new ActionDescriptor(element, handle));
		} else if (element.getName().equals(EXTENSION_ELEMENT_PROVIDER)) {
			into.getProviders().add(buildProvider(element, handle, declaredProviders));
		} else if (element.getName().equals(EXTENSION_ELEMENT_PROVIDER_FACTORY)) {
			IActionProviderFactory factory = ((IActionProviderFactory) element
					.createExecutableExtension(CLASS_ATTRIBUTE));
			factory.initialize(handle);
			into.getProviders().addAll(factory.getActionProviders());
			into.getActions().addAll(factory.getActions());
		}
	}
}
