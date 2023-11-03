/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.rjmx.common.subscription.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.openjdk.jmc.rjmx.common.RJMXCorePlugin;
import org.openjdk.jmc.rjmx.common.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.common.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.common.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.common.subscription.IMRITransformation;
import org.openjdk.jmc.rjmx.common.subscription.IMRITransformationFactory;
import org.openjdk.jmc.rjmx.common.subscription.MRI;

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
		initialize();
	}

	private static void initialize() {
		IMRITransformationFactory transformationFactory = new SingleMRITransformationFactory();
		String transformationName = "difference";
		Properties props = new Properties();
		props.put("visualizeLabel", "Visualize difference...");
		props.put("transformationClass", "org.openjdk.jmc.rjmx.common.subscription.internal.DifferenceTransformation");
		Properties transProps = new Properties();
		transProps.put("displayName", "%s (difference)");
		props.put(TRANSFORMATION_NAME_ATTRIBUTE, transformationName);
		transformationFactory.setFactoryProperties(props, transProps);
		TRANSFORMATION_FACTORIES.put(transformationName, transformationFactory);

		transformationFactory = new SingleMRITransformationFactory();
		transformationName = "rate";
		props = new Properties();
		props.put("visualizeLabel", "Visualize rate per second...");
		props.put("transformationClass", "org.openjdk.jmc.rjmx.common.subscription.internal.DifferenceTransformation");
		transProps = new Properties();
		transProps.put("displayName", "%s (rate per second)");
		transProps.put("rate", "1000");
		props.put(TRANSFORMATION_NAME_ATTRIBUTE, transformationName);
		transformationFactory.setFactoryProperties(props, transProps);
		TRANSFORMATION_FACTORIES.put(transformationName, transformationFactory);

		transformationFactory = new SingleMRITransformationFactory();
		transformationName = "average";
		props = new Properties();
		props.put("visualizeLabel", "Visualize average...");
		props.put("transformationClass", "org.openjdk.jmc.rjmx.common.subscription.internal.AverageTransformation");
		transProps = new Properties();
		transProps.put("terms", "30");
		transProps.put("displayName", "%%s (average over %s samples)");
		props.put(TRANSFORMATION_NAME_ATTRIBUTE, transformationName);
		transformationFactory.setFactoryProperties(props, transProps);
		TRANSFORMATION_FACTORIES.put(transformationName, transformationFactory);

		transformationFactory = new SingleMRITransformationFactory();
		transformationName = "delta";
		props = new Properties();
		props.put("visualizeLabel", "Visualize delta...");
		props.put("transformationClass", "org.openjdk.jmc.rjmx.common.subscription.internal.DeltaTransformation");
		transProps = new Properties();
		transProps.put("displayName", "%s (delta)");
		props.put(TRANSFORMATION_NAME_ATTRIBUTE, transformationName);
		transformationFactory.setFactoryProperties(props, transProps);
		TRANSFORMATION_FACTORIES.put(transformationName, transformationFactory);
	}

	public void initializeFromExtensions(Map<String, IMRITransformationFactory> transformationFactories) {
		TRANSFORMATION_FACTORIES.clear();
		for (Map.Entry<String, IMRITransformationFactory> factory : transformationFactories.entrySet()) {
			TRANSFORMATION_FACTORIES.put(factory.getKey(), factory.getValue());
		}
	}

	public static void addTransformationsFactory(
		String transformationName, IMRITransformationFactory transformationFactory) {
		TRANSFORMATION_FACTORIES.put(transformationName, transformationFactory);
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
		RJMXCorePlugin.getDefault().getLogger().log(Level.SEVERE,
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
				String.format(textPattern, attributeMetadata.getMetadata(IMRIMetadataProvider.KEY_DISPLAY_NAME)));
		metadataService.setMetadata(mri, IMRIMetadataProvider.KEY_DESCRIPTION,
				String.format(textPattern, attributeMetadata.getMetadata(IMRIMetadataProvider.KEY_DESCRIPTION)));
		metadataService.setMetadata(mri, IMRIMetadataProvider.KEY_UPDATE_TIME,
				(String) attributeMetadata.getMetadata(IMRIMetadataProvider.KEY_UPDATE_TIME));
		metadataService.setMetadata(mri, IMRIMetadataProvider.KEY_UNIT_STRING,
				(String) attributeMetadata.getMetadata(IMRIMetadataProvider.KEY_UNIT_STRING));
	}
}
