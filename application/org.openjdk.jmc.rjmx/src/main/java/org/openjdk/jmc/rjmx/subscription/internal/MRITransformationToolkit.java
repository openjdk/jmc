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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRITransformation;
import org.openjdk.jmc.rjmx.subscription.IMRITransformationFactory;
import org.openjdk.jmc.rjmx.subscription.MRI;

/**
 * An MRI transformation toolkit responsible for creating transformations from MRI, finding
 * attributes they depend on, etc. Will read available transformation factories from the extension
 * "org.openjdk.jmc.rjmx.attributeTransformation".
 */
public class MRITransformationToolkit {

	static final String TRANSFORMATION_EXTENSION_NAME = "org.openjdk.jmc.rjmx.attributeTransformation"; //$NON-NLS-1$
	static final String TRANSFORMATION_ELEMENT = "attributeTransformation"; //$NON-NLS-1$
	public static final String TRANSFORMATION_NAME_ATTRIBUTE = "transformationName"; //$NON-NLS-1$
	static final String TRANSFORMATION_PROPERTY_ELEMENT = "property"; //$NON-NLS-1$
	static final String TRANSFORMATION_PROPERTY_NAME = "name"; //$NON-NLS-1$
	static final String TRANSFORMATION_PROPERTY_VALUE = "value"; //$NON-NLS-1$
	static final String TRANSFORMATION_PROPERTIES_ELEMENT = "transformationProperties"; //$NON-NLS-1$

	private static final Map<String, IMRITransformationFactory> TRANSFORMATION_FACTORIES = new HashMap<>();

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

	/**
	 * Creates a new transformation with given transformation MRI for given connection.
	 *
	 * @param mri
	 *            the transformation MRI
	 * @return the corresponding transformation object
	 */
	public static IMRITransformation createTransformation(MRI mri) {
		String transformationName = getTransformationName(mri);
		if (TRANSFORMATION_FACTORIES.containsKey(transformationName)) {
			Properties properties = createProperties(mri);
			return TRANSFORMATION_FACTORIES.get(transformationName).createTransformation(properties);
		}
		RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
				"Could not instantiate unknown transformation type " + transformationName + "!"); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	/**
	 * Returns the different available transformation factories.
	 *
	 * @return the set of transformation factories
	 */
	public static Iterable<IMRITransformationFactory> getFactories() {
		return Collections.unmodifiableCollection(TRANSFORMATION_FACTORIES.values());
	}

	private static String getTransformationName(MRI mri) {
		String path = mri.getDataPath();
		int partitionIndex = path.indexOf('?');
		if (partitionIndex >= 0) {
			return path.substring(0, partitionIndex);
		}
		return path;
	}

	private static Properties createProperties(MRI mri) {
		Properties properties = new Properties();
		String path = mri.getDataPath();
		int partitionIndex = path.indexOf('?');
		if (partitionIndex >= 0) {
			path = path.substring(partitionIndex + 1);
			for (String property : path.split("&")) { //$NON-NLS-1$
				int equalIndex = property.indexOf('=');
				properties.put(property.substring(0, equalIndex), property.substring(equalIndex + 1));
			}
		}
		return properties;
	}

	public static void forwardMetadata(
		IMRIMetadataService metadataService, MRI mri, IMRIMetadata attributeMetadata, String textPattern) {
		metadataService.setMetadata(mri, IMRIMetadataProvider.KEY_DISPLAY_NAME,
				NLS.bind(textPattern, attributeMetadata.getMetadata(IMRIMetadataProvider.KEY_DISPLAY_NAME)));
		metadataService.setMetadata(mri, IMRIMetadataProvider.KEY_DESCRIPTION,
				NLS.bind(textPattern, attributeMetadata.getMetadata(IMRIMetadataProvider.KEY_DESCRIPTION)));
		metadataService.setMetadata(mri, IMRIMetadataProvider.KEY_UPDATE_TIME,
				(String) attributeMetadata.getMetadata(IMRIMetadataProvider.KEY_UPDATE_TIME));
		metadataService.setMetadata(mri, IMRIMetadataProvider.KEY_UNIT_STRING,
				(String) attributeMetadata.getMetadata(IMRIMetadataProvider.KEY_UNIT_STRING));
	}
}
