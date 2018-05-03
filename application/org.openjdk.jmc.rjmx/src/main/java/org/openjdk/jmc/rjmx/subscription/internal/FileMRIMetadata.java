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

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataProvider;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;

/**
 * This class is used to read the default metadata from the mrimetadata.xml file.
 */
class FileMRIMetadata {
	private static final String ELEMENT_METADATA_COLLECTION = "metadatacollection"; //$NON-NLS-1$
	private static final String ELEMENT_METADATA = "metadata"; //$NON-NLS-1$
	private static final String ELEMENT_MRI_DATA_PATH = "mri.dataPath"; //$NON-NLS-1$
	private static final String ELEMENT_MRI_OBJECT_NAME = "mri.objectName"; //$NON-NLS-1$
	private static final String ELEMENT_MRI_TYPE = "mri.type"; //$NON-NLS-1$
	private static final String ELEMENT_MRI_QUALIFIED_NAME = "mri.qualifiedName"; //$NON-NLS-1$
	private static final String ELEMENT_DISPLAY_NAME = "displayname"; //$NON-NLS-1$
	private static final String ELEMENT_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String ELEMENT_UPDATE_TIME = "updatetime"; //$NON-NLS-1$
	private static final String ELEMENT_UNIT_STRING = "unitstring"; //$NON-NLS-1$
	private static final String ELEMENT_COMPOSITE = "composite"; //$NON-NLS-1$
	private static final String ELEMENT_TYPE = "type"; //$NON-NLS-1$
	private static final String ELEMENT_ARGUMENTS = "arguments"; //$NON-NLS-1$
	private static final String DEFAULT_DISPLAY_NAME = "No name"; //$NON-NLS-1$
	private static final String DEFAULT_DESCRIPTION = "This attribute has no extended description"; //$NON-NLS-1$

	// The logger.
	private static final Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.subscription"); //$NON-NLS-1$

	private final Map<MRI, Map<String, Object>> metadataMap = new HashMap<>();

	static Map<MRI, Map<String, Object>> readDefaultsFromFile() {
		FileMRIMetadata metadataLoader = new FileMRIMetadata();
		InputStream is = null;
		try {
			is = FileMRIMetadata.class.getResourceAsStream("mrimetadata.xml"); //$NON-NLS-1$
			Document doc = XmlToolkit.loadDocumentFromStream(is);
			List<Element> elems = XmlToolkit.getChildElementsByTag(doc.getDocumentElement(),
					ELEMENT_METADATA_COLLECTION);
			if (elems.size() != 1 || elems.get(0) == null) {
				throw new Exception("Could not find the attributes element!"); //$NON-NLS-1$
			}
			for (Element e : XmlToolkit.getChildElementsByTag(elems.get(0), FileMRIMetadata.ELEMENT_METADATA)) {
				try {
					metadataLoader.loadMetadataElement(e);
				} catch (Exception e1) {
					LOGGER.log(Level.WARNING, "Malformed descriptor in mrimetadata.xml, skipping metadata", e1); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Tried reading mrimetadata.xml, but an exception occurred: " + e.getMessage() //$NON-NLS-1$
					+ "Extended information about attributes may not be available, " //$NON-NLS-1$
					+ "and the console will not operate optimally.", e); //$NON-NLS-1$
		} finally {
			IOToolkit.closeSilently(is);
		}
		return metadataLoader.metadataMap;
	}

	private void loadMetadataElement(Element e) {
		String mriType = XmlToolkit.getSetting(e, ELEMENT_MRI_TYPE, Type.ATTRIBUTE.getTypeName());
		String mriDataPath = XmlToolkit.getSetting(e, ELEMENT_MRI_DATA_PATH, null);
		String mriObjectName = XmlToolkit.getSetting(e, ELEMENT_MRI_OBJECT_NAME, null);
		String mriQualifiedName = XmlToolkit.getSetting(e, ELEMENT_MRI_QUALIFIED_NAME, null);

		if ((mriDataPath == null || mriObjectName == null) && mriQualifiedName == null) {
			LOGGER.warning("Could not read metadata information properly. [dataPath=" + mriDataPath + ",objectName=" //$NON-NLS-1$ //$NON-NLS-2$
					+ mriObjectName + "|qualifiedName=null] will not be properly configured."); //$NON-NLS-1$
			return;
		}
		MRI mri = mriQualifiedName != null ? MRI.createFromQualifiedName(mriQualifiedName)
				: new MRI(Type.fromString(mriType), mriObjectName, mriDataPath);
		putMetadataForElement(mri, e);
		String updateTime = XmlToolkit.getSetting(e, ELEMENT_UPDATE_TIME, null);
		putValue(mri, IMRIMetadataProvider.KEY_UPDATE_TIME, updateTime);

		boolean hasCompositeTag = XmlToolkit.getChildElementOrNull(e, ELEMENT_COMPOSITE) != null;
		if (hasCompositeTag) {
			String rootName = mri.getDataPath() + MRI.VALUE_COMPOSITE_DELIMITER_STRING;
			for (Element childElement : XmlToolkit.getChildElementsByTag(e, FileMRIMetadata.ELEMENT_METADATA)) {
				String childDataPath = XmlToolkit.getSetting(childElement, FileMRIMetadata.ELEMENT_MRI_DATA_PATH, ""); //$NON-NLS-1$
				MRI childMri = new MRI(mri.getType(), mri.getObjectName(), rootName + childDataPath);
				putMetadataForElement(childMri, childElement);
				putValue(childMri, IMRIMetadataProvider.KEY_UPDATE_TIME, updateTime);
			}
		}
	}

	private void putMetadataForElement(MRI mri, Element e) {
		putValue(mri, IMRIMetadataProvider.KEY_DISPLAY_NAME,
				XmlToolkit.getSetting(e, ELEMENT_DISPLAY_NAME, DEFAULT_DISPLAY_NAME));
		putValue(mri, IMRIMetadataProvider.KEY_DESCRIPTION,
				XmlToolkit.getSetting(e, ELEMENT_DESCRIPTION, DEFAULT_DESCRIPTION));
		String unitString = XmlToolkit.getSetting(e, ELEMENT_UNIT_STRING, null);
		putValue(mri, IMRIMetadataProvider.KEY_UNIT_STRING, unitString);
		String valueType = XmlToolkit.getSetting(e, ELEMENT_TYPE, null);
		putValue(mri, IMRIMetadataProvider.KEY_VALUE_TYPE, valueType);
		if (MRIMetadataToolkit.isNumerical(valueType) && unitString == null) {
			LOGGER.warning("Unit is missing for " + mri); //$NON-NLS-1$
		}
		List<Element> propsList = XmlToolkit.getChildElementsByTag(e, ELEMENT_ARGUMENTS);
		if (propsList.size() != 0) {
			if (propsList.size() != 1) {
				LOGGER.warning("Warning: Found several arguments listings for attribute " + mri.toString() //$NON-NLS-1$
						+ ". Will use only first."); //$NON-NLS-1$
			}
			NodeList arguments = propsList.get(0).getChildNodes();
			for (int i = 0, length = arguments.getLength(); i < length; i++) {
				if (arguments.item(i) instanceof Element) {
					Element argument = (Element) arguments.item(i);
					putValue(mri, argument.getNodeName(), XmlToolkit.getStringValue(argument));
				}
			}
		}
	}

	private void putValue(MRI mri, String key, Object value) {
		metadataMap.computeIfAbsent(mri, k -> new HashMap<>()).put(key, value);
	}
}
