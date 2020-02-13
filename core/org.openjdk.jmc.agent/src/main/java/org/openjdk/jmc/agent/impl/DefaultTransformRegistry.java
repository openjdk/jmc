/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.impl;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openjdk.jmc.agent.Method;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.ReturnValue;
import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.Field;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;

public class DefaultTransformRegistry implements TransformRegistry {
	private static final String XML_ATTRIBUTE_NAME_ID = "id"; //$NON-NLS-1$
	private static final String XML_ELEMENT_NAME_EVENT = "event"; //$NON-NLS-1$
	private static final String XML_ELEMENT_METHOD_NAME = "method"; //$NON-NLS-1$
	private static final String XML_ELEMENT_FIELD_NAME = "field"; //$NON-NLS-1$
	private static final String XML_ELEMENT_PARAMETER_NAME = "parameter"; //$NON-NLS-1$
	private static final String XML_ELEMENT_RETURN_VALUE_NAME = "returnvalue"; //$NON-NLS-1$

	// Global override section
	private static final String XML_ELEMENT_CONFIGURATION = "config"; //$NON-NLS-1$
	
	// Logging
	private static final Logger logger = Logger.getLogger("DefaultTransformRegistry");

	// Maps class name -> Transform Descriptors
	// First step in update should be to check if we even have transformations for the given class
	private final HashMap<String, List<TransformDescriptor>> transformData = new HashMap<>();

	private volatile boolean revertInstrumentation = false;

	@Override
	public boolean hasPendingTransforms(String className) {
		List<TransformDescriptor> transforms = transformData.get(className);
		if (transforms == null || !isPendingTransforms(transforms)) {
			return false;
		}
		return true;
	}

	public static TransformRegistry from(InputStream in) throws XMLStreamException {
		HashMap<String, String> globalDefaults = new HashMap<>();
		DefaultTransformRegistry registry = new DefaultTransformRegistry();
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);
		while (streamReader.hasNext()) {
			if (streamReader.isStartElement()) {
				QName element = streamReader.getName();
				if (XML_ELEMENT_NAME_EVENT.equals(element.getLocalPart())) {
					TransformDescriptor td = parseTransformData(streamReader, globalDefaults);
					if (validate(registry,td)) {
						add(registry, td);
					}
					continue;
				} else if (XML_ELEMENT_CONFIGURATION.equals(element.getLocalPart())) {
					// These are the global defaults.
					streamReader.next();
					readGlobalConfig(streamReader, globalDefaults);
				}
			}
			streamReader.next();
		}
		return registry;
	}

	private static void add(DefaultTransformRegistry registry, TransformDescriptor td) {
		List<TransformDescriptor> transformDataList = registry.getTransformData(td.getClassName());
		if (transformDataList == null) {
			transformDataList = new ArrayList<>();
			registry.transformData.put(td.getClassName(), transformDataList);
		}
		transformDataList.add(td);
	}

	private static boolean validate(DefaultTransformRegistry registry, TransformDescriptor td) {
		if (td.getClassName() == null) {
			System.err.println("Encountered probe without associated class! Check probe definitions!"); //$NON-NLS-1$
			return false;
		}
		if (td.getId() == null) {
			System.err.println("Encountered probe without associated id! Check probe definitions!"); //$NON-NLS-1$
			return false;
		}

		List<TransformDescriptor> transformDataList = registry.getTransformData(td.getClassName());
		if (transformDataList != null) {
			String tdEventClassName = ((JFRTransformDescriptor)td).getEventClassName();
			for (TransformDescriptor tdListEntry : transformDataList) {
				String existingName = ((JFRTransformDescriptor) tdListEntry).getEventClassName();
				if (existingName.equals(tdEventClassName)) {
					System.err.println("Encountered probe with an event class name that already exists. "
							+ "Check probe definitions!"); //$NON-NLS-1$
					return false;
				}
			}
		}

		return true;
	}

	private static TransformDescriptor parseTransformData(
		XMLStreamReader streamReader, HashMap<String, String> globalDefaults) throws XMLStreamException {
		String id = streamReader.getAttributeValue("", XML_ATTRIBUTE_NAME_ID); //$NON-NLS-1$
		streamReader.next();
		Map<String, String> values = new HashMap<>();
		List<Parameter> parameters = new LinkedList<>();
		List<Field> fields = new LinkedList<>();
		Method method = null;
		ReturnValue[] returnValue = new ReturnValue[1];
		while (streamReader.hasNext()) {
			if (streamReader.isStartElement()) {
				String name = streamReader.getName().getLocalPart();
				if (XML_ELEMENT_METHOD_NAME.equals(name)) {
					method = parseMethod(streamReader, parameters, returnValue);
					continue;
				}
				if (XML_ELEMENT_FIELD_NAME.equals(name)) {
					fields.add(parseField(streamReader));
					continue;
				}
				streamReader.next();
				if (streamReader.hasText()) {
					String value = streamReader.getText();
					if (value != null) {
						value = value.trim();
					}
					values.put(name, value);
				}
			} else if (streamReader.isEndElement()) {
				String name = streamReader.getName().getLocalPart();
				if (XML_ELEMENT_NAME_EVENT.equals(name)) {
					break;
				}
			}
			streamReader.next();
		}
		transfer(globalDefaults, values);
		return TransformDescriptor.create(id, TypeUtils.getInternalName(values.get("class")), method, values, parameters, returnValue[0], fields); //$NON-NLS-1$
	}

	private static void transfer(HashMap<String, String> globalDefaults, Map<String, String> values) {
		for (Entry<String, String> entry : globalDefaults.entrySet()) {
			if (!values.containsKey(entry.getKey())) {
				values.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private static void readGlobalConfig(XMLStreamReader streamReader, HashMap<String, String> globalDefaults) {
		addDefaults(globalDefaults);
		try {
			while (streamReader.hasNext()) {
				if (streamReader.isStartElement()) {
					String key = streamReader.getName().getLocalPart();
					streamReader.next();
					if (streamReader.hasText()) {
						String value = streamReader.getText();
						globalDefaults.put(key, value);
					}
				} else if (streamReader.isEndElement()) {
					String name = streamReader.getName().getLocalPart();
					if (XML_ELEMENT_CONFIGURATION.equals(name)) {
						break;
					}
				}
				streamReader.next();
			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	private static void addDefaults(HashMap<String, String> globalDefaults) {
		globalDefaults.put(TransformDescriptor.ATTRIBUTE_CLASS_PREFIX, "__JFREvent"); //$NON-NLS-1$
		// For safety reasons, allowing toString is opt-in
		globalDefaults.put(TransformDescriptor.ATTRIBUTE_ALLOW_TO_STRING, "false"); //$NON-NLS-1$
		// For safety reasons, allowing converters is opt-in
		globalDefaults.put(TransformDescriptor.ATTRIBUTE_ALLOW_CONVERTER, "false"); //$NON-NLS-1$
	}

	private static Parameter parseParameter(int index, XMLStreamReader streamReader) throws XMLStreamException {
		streamReader.next();
		String name = null;
		String description = null;
		String contentType = null;
		String relationKey = null;
		String converterClassName = null;

		while (streamReader.hasNext()) {
			if (streamReader.isStartElement()) {
				String key = streamReader.getName().getLocalPart();
				streamReader.next();
				if (streamReader.hasText()) {
					String value = streamReader.getText();
					if (value != null) {
						value = value.trim();
					}
					if ("name".equals(key)) { //$NON-NLS-1$
						name = value;
					} else if ("description".equals(key)) { //$NON-NLS-1$
						description = value;
					} else if ("contenttype".equals(key)) { //$NON-NLS-1$
						contentType = value;
					} else if ("relationkey".equals(key)) { //$NON-NLS-1$
						relationKey = value;
					} else if ("converter".equals(key)) { //$NON-NLS-1$
						converterClassName = value;
					}
				}
			} else if (streamReader.isEndElement()) {
				if (XML_ELEMENT_PARAMETER_NAME.equals(streamReader.getName().getLocalPart())) {
					break;
				}
			}
			streamReader.next();
		}
		return new Parameter(index, name, description, contentType, relationKey, converterClassName);
	}

	private static Field parseField(XMLStreamReader streamReader) throws XMLStreamException {
		streamReader.next();
		String name = null;
		String expression = null;
		String description = null;
		String contentType = null;
		String relationKey = null;
		String converterClassName = null;

		while (streamReader.hasNext()) {
			if (streamReader.isStartElement()) {
				String key = streamReader.getName().getLocalPart();
				streamReader.next();
				if (streamReader.hasText()) {
					String value = streamReader.getText();
					if (value != null) {
						value = value.trim();
					}
					if ("name".equals(key)) { //$NON-NLS-1$
						name = value;
					} else if ("expression".equals(key)) {
						expression = value;
					} else if ("description".equals(key)) { //$NON-NLS-1$
						description = value;
					} else if ("contenttype".equals(key)) { //$NON-NLS-1$
						contentType = value;
					} else if ("relationkey".equals(key)) { //$NON-NLS-1$
						relationKey = value;
					} else if ("converter".equals(key)) { //$NON-NLS-1$
						converterClassName = value;
					}
				}
			} else if (streamReader.isEndElement()) {
				if (XML_ELEMENT_FIELD_NAME.equals(streamReader.getName().getLocalPart())) {
					break;
				}
			}
			streamReader.next();
		}
		return new Field(name, expression, description, contentType, relationKey, converterClassName);
	}

	private static ReturnValue parseReturnValue(XMLStreamReader streamReader) throws XMLStreamException {
		streamReader.next();
		String name = null;
		String description = null;
		String contentType = null;
		String relationKey = null;
		String converterClassName = null;

		while (streamReader.hasNext()) {
			if (streamReader.isStartElement()) {
				String key = streamReader.getName().getLocalPart();
				streamReader.next();
				if (streamReader.hasText()) {
					String value = streamReader.getText();
					if (value != null) {
						value = value.trim();
					}
					if ("name".equals(key)) { //$NON-NLS-1$
						name = value;
					} else if ("description".equals(key)) { //$NON-NLS-1$
						description = value;
					} else if ("contenttype".equals(key)) { //$NON-NLS-1$
						contentType = value;
					} else if ("relationkey".equals(key)) { //$NON-NLS-1$
						relationKey = value;
					} else if ("converter".equals(key)) { //$NON-NLS-1$
						converterClassName = value;
					}
				}
			} else if (streamReader.isEndElement()) {
				if (XML_ELEMENT_RETURN_VALUE_NAME.equals(streamReader.getName().getLocalPart())) {
					break;
				}
			}
			streamReader.next();
		}
		return new ReturnValue(name, description, contentType, relationKey, converterClassName);
	}

	private static Method parseMethod(XMLStreamReader streamReader, List<Parameter> parameters, ReturnValue[] returnValue)
			throws XMLStreamException {
		streamReader.next();
		String name = null;
		String descriptor = null;
		while (streamReader.hasNext()) {
			if (streamReader.isStartElement()) {
				String key = streamReader.getName().getLocalPart();
				if (XML_ELEMENT_PARAMETER_NAME.equals(key)) {
					if (streamReader.getAttributeCount() > 0) {
						String indexAttribute = streamReader.getAttributeValue(0);
						parameters.add(parseParameter(Integer.parseInt(indexAttribute), streamReader));
					}
					continue;
				}
				if (XML_ELEMENT_RETURN_VALUE_NAME.equals(key)) {
					returnValue[0] = parseReturnValue(streamReader);
					continue;
				}
				streamReader.next();
				if (streamReader.hasText()) {
					String value = streamReader.getText();
					if ("name".equals(key)) { //$NON-NLS-1$
						name = value;
					} else if ("descriptor".equals(key)) { //$NON-NLS-1$
						descriptor = value;
					}
				}
			} else if (streamReader.isEndElement()) {
				if (XML_ELEMENT_METHOD_NAME.equals(streamReader.getName().getLocalPart())) {
					break;
				}
			}
			streamReader.next();
		}
		return new Method(name, descriptor);
	}

	@Override
	public List<TransformDescriptor> getTransformData(String className) {
		return transformData.get(className);
	}

	private boolean isPendingTransforms(List<TransformDescriptor> transforms) {
		for (TransformDescriptor td : transforms) {
			if (td.isPendingTransforms()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, List<TransformDescriptor>> entry : transformData.entrySet()) {
			builder.append("Transformations for class: "); //$NON-NLS-1$
			builder.append(entry.getKey());
			builder.append("\n"); //$NON-NLS-1$
			for (TransformDescriptor td : entry.getValue()) {
				builder.append("\t"); //$NON-NLS-1$
				builder.append(td.toString());
				builder.append("\n"); //$NON-NLS-1$
			}
		}
		return builder.toString();
	}

	public List<TransformDescriptor> modify(String xmlDescription) {
		try  {
			List<TransformDescriptor> tds = new ArrayList<TransformDescriptor>();
			StringReader reader = new StringReader(xmlDescription);
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			XMLStreamReader streamReader = inputFactory.createXMLStreamReader(reader);
			HashMap<String, String> globalDefaults = new HashMap<String, String>();
			List<String> removedOldClasses = new ArrayList<String>();
			logger.info(xmlDescription);
			while (streamReader.hasNext()) {
				if (streamReader.isStartElement()) {
					QName element = streamReader.getName();
					if (XML_ELEMENT_NAME_EVENT.equals(element.getLocalPart())) {
						TransformDescriptor td = parseTransformData(streamReader, globalDefaults);
						if(!removedOldClasses.contains(td.getClassName())) {
							transformData.remove(td.getClassName());
							removedOldClasses.add(td.getClassName());
						}
						if (validate(this,td)) {
							add(this, td);
							tds.add(td);
						}
						continue;
					} else if (XML_ELEMENT_CONFIGURATION.equals(element.getLocalPart())) {
						readGlobalConfig(streamReader, globalDefaults);
					}
				}
				streamReader.next();
			}
			return tds;
		} catch (XMLStreamException xse) {
			logger.log(Level.SEVERE, "Failed to create XML Stream Reader", xse);
			return null;
		}
	}

	public List<String> clearAllTransformData() {
		List<String> classNames = new ArrayList<>(transformData.keySet());
		transformData.clear();
		return classNames;
	}

	public void setRevertInstrumentation(boolean shouldRevert) {
		this.revertInstrumentation = shouldRevert;
	}

	public boolean isRevertIntrumentation() {
		return revertInstrumentation;
	}

}
