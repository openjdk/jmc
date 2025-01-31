/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.rjmx.subscription.internal;

import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.common.subscription.IMRITransformationFactory;
import org.openjdk.jmc.rjmx.common.subscription.internal.MRITransformationBaseToolkit;

/**
 * An MRI transformation toolkit responsible for creating transformations from MRI, finding
 * attributes they depend on, etc. Will read available transformation factories from the extension
 * "org.openjdk.jmc.rjmx.attributeTransformation".
 */
public class MRITransformationToolkit extends MRITransformationBaseToolkit {

	private MRITransformationToolkit() {
		throw new AssertionError("Not to be instantiated!"); //$NON-NLS-1$
	}

	static {
		initializeFromExtensions();
	}

	private static void initializeFromExtensions() {
		IExtensionRegistry er = Platform.getExtensionRegistry();
		IExtensionPoint ep = er.getExtensionPoint(TRANSFORMATION_EXTENSION_NAME);
		IExtension[] extensions = ep.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] configs = extension.getConfigurationElements();
			for (IConfigurationElement config : configs) {
				if (config.getName().equals(TRANSFORMATION_ELEMENT)) {
					try {
						IMRITransformationFactory transformationFactory = (IMRITransformationFactory) config
								.createExecutableExtension("class"); //$NON-NLS-1$
						String transformationName = config.getAttribute(TRANSFORMATION_NAME_ATTRIBUTE);
						Properties props = new Properties();
						Properties transProps = new Properties();
						props.put(TRANSFORMATION_NAME_ATTRIBUTE, transformationName);
						for (IConfigurationElement prop : config.getChildren()) {
							if (prop.getName().equals(TRANSFORMATION_PROPERTY_ELEMENT)) {
								props.put(prop.getAttribute(TRANSFORMATION_PROPERTY_NAME),
										prop.getAttribute(TRANSFORMATION_PROPERTY_VALUE));
							} else if (prop.getName().equals(TRANSFORMATION_PROPERTIES_ELEMENT)) {
								for (IConfigurationElement transProp : prop.getChildren()) {
									if (transProp.getName().equals(TRANSFORMATION_PROPERTY_ELEMENT)) {
										transProps.put(transProp.getAttribute(TRANSFORMATION_PROPERTY_NAME),
												transProp.getAttribute(TRANSFORMATION_PROPERTY_VALUE));
									}
								}
							}
						}
						transformationFactory.setFactoryProperties(props, transProps);
						TRANSFORMATION_FACTORIES.put(transformationName, transformationFactory);
					} catch (CoreException e) {
						RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
								"Could not instantiate attribute transformation factory!", e); //$NON-NLS-1$
					}
				}
			}
		}
	}

}
