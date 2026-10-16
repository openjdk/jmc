/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.openjdk.jmc.agent.Agent;
import org.openjdk.jmc.agent.Field;
import org.openjdk.jmc.agent.Method;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.ReturnValue;
import org.openjdk.jmc.agent.TransformDescriptor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.XMLValidationException;
import org.openjdk.jmc.agent.collections.CollectionResizeEmitter;
import org.openjdk.jmc.agent.collections.CollectionTrackingSettings;
import org.openjdk.jmc.agent.collections.CollectionTransformDescriptor;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.util.IOToolkit;
import org.openjdk.jmc.agent.util.TypeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DefaultTransformRegistry implements TransformRegistry {
	private static final String XML_ATTRIBUTE_NAME_ID = "id"; //$NON-NLS-1$
	private static final String XML_ELEMENT_NAME_EVENT = "event"; //$NON-NLS-1$
	private static final String XML_ELEMENT_METHOD_NAME = "method"; //$NON-NLS-1$
	private static final String XML_ELEMENT_FIELD_NAME = "field"; //$NON-NLS-1$
	private static final String XML_ELEMENT_PARAMETER_NAME = "parameter"; //$NON-NLS-1$
	private static final String XML_ELEMENT_RETURN_VALUE_NAME = "returnvalue"; //$NON-NLS-1$

	// Global override section
	private static final String XML_ELEMENT_CONFIGURATION = "config"; //$NON-NLS-1$

	private static final String XML_ELEMENT_COLLECTION_TRACKING = "collectiontracking"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_NAME_MINSIZE = "minsize"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_NAME_ENABLED = "enabled"; //$NON-NLS-1$

	private static final String EMPTY_ROOT_DOCUMENT = "<jfragent/>"; //$NON-NLS-1$
	private static final String XML_PARSER_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl"; //$NON-NLS-1$

	// Logging
	private static final Logger logger = Logger.getLogger("DefaultTransformRegistry");

	// Maps class name -> Transform Descriptors
	// First step in update should be to check if we even have transformations for the given class.
	// The map is concurrent and the lists are copy-on-write (never mutated once published): the
	// Transformer iterates them on class-load/retransform threads while JMX pushes mutate the
	// registry.
	private final ConcurrentHashMap<String, List<TransformDescriptor>> transformData = new ConcurrentHashMap<>();

	private volatile boolean revertInstrumentation;

	private volatile CollectionTrackingSettings collectionTrackingSettings = CollectionTrackingSettings.disabled();

	private String currentConfiguration = "";

	private static final String PROBE_SCHEMA_XSD = "jfrprobes_schema.xsd"; //$NON-NLS-1$
	private static final Schema PROBE_SCHEMA;

	static {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			PROBE_SCHEMA = factory
					.newSchema(new StreamSource(DefaultTransformRegistry.class.getResourceAsStream(PROBE_SCHEMA_XSD)));
		} catch (SAXException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Override
	public boolean hasPendingTransforms(String className) {
		List<TransformDescriptor> transforms = transformData.get(className);
		if (transforms == null || !isPendingTransforms(transforms)) {
			return false;
		}
		return true;
	}

	public static TransformRegistry empty() {
		return new DefaultTransformRegistry();
	}

	public static void validateProbeDefinition(InputStream in) throws XMLValidationException {
		try {
			Validator validator = PROBE_SCHEMA.newValidator();
			validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			validator.validate(new StreamSource(in));
		} catch (IOException | SAXException e) {
			throw new XMLValidationException(e.getMessage(), e);
		}
	}

	public static void validateProbeDefinition(String configuration) throws XMLValidationException {
		validateProbeDefinition(new ByteArrayInputStream(configuration.getBytes()));
	}

	public static TransformRegistry from(InputStream in) throws XMLStreamException, XMLValidationException {
		byte[] buf;
		InputStream configuration;
		try {
			buf = IOToolkit.readFully(in, -1, true);
			configuration = new ByteArrayInputStream(buf);
			configuration.mark(0);
			validateProbeDefinition(configuration);
			configuration.reset();
		} catch (IOException e) {
			throw new XMLStreamException(e);
		} catch (XMLValidationException xve) {
			throw xve;
		}

		HashMap<String, String> globalDefaults = new HashMap<>();
		DefaultTransformRegistry registry = new DefaultTransformRegistry();
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		disableExternalEntityProcessing(inputFactory);
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(configuration);
		while (streamReader.hasNext()) {
			if (streamReader.isStartElement()) {
				QName element = streamReader.getName();
				if (XML_ELEMENT_NAME_EVENT.equals(element.getLocalPart())) {
					TransformDescriptor td = parseTransformData(streamReader, globalDefaults);
					if (validate(registry, td)) {
						registry.add(td);
					}
					continue;
				} else if (XML_ELEMENT_CONFIGURATION.equals(element.getLocalPart())) {
					// These are the global defaults.
					streamReader.next();
					readGlobalConfig(streamReader, globalDefaults);
				} else if (XML_ELEMENT_COLLECTION_TRACKING.equals(element.getLocalPart())) {
					registry.collectionTrackingSettings = parseCollectionTracking(streamReader);
				}
			}
			streamReader.next();
		}
		if (registry.collectionTrackingSettings.isEnabled()) {
			registry.addCollectionTrackingTransforms(null, null);
		}
		try {
			configuration.reset();
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
		registry.setCurrentConfiguration(getXmlAsString(configuration));
		return registry;
	}

	private void add(TransformDescriptor td) {
		// Copy-on-write: a list published in transformData is never mutated, since the Transformer
		// may be iterating it concurrently.
		List<TransformDescriptor> existing = transformData.get(td.getClassName());
		List<TransformDescriptor> updated = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
		updated.add(td);
		transformData.put(td.getClassName(), updated);
	}

	/**
	 * Registers the fixed, hidden collection resize instrumentation points (idempotently). These
	 * bypass the JFR-specific {@link #validate} step since they are not user-defined event probes.
	 * When the sets are non-null, {@code retainedClasses} receives all tracked class names (so they
	 * survive {@link #clearAllOtherTransformData}), while {@code modifiedClasses} only receives
	 * classes whose woven state actually changes, so re-enabling active tracking does not
	 * retransform anything.
	 */
	private void addCollectionTrackingTransforms(Set<String> modifiedClasses, Set<String> retainedClasses) {
		for (CollectionTransformDescriptor descriptor : CollectionTransformDescriptor.all()) {
			if (retainedClasses != null) {
				retainedClasses.add(descriptor.getClassName());
			}
			CollectionTransformDescriptor present = findCollectionDescriptor(descriptor.getClassName());
			if (present == null) {
				add(descriptor);
				if (modifiedClasses != null) {
					modifiedClasses.add(descriptor.getClassName());
				}
			} else if (present.isPendingTransforms() && modifiedClasses != null) {
				// The descriptor is registered but its weave never succeeded (the bridge was not
				// installed at transform time, or the weave threw). Retransform to retry rather
				// than leaving the class silently uninstrumented on an identical re-push.
				modifiedClasses.add(descriptor.getClassName());
			}
		}
	}

	private CollectionTransformDescriptor findCollectionDescriptor(String className) {
		List<TransformDescriptor> existing = transformData.get(className);
		if (existing != null) {
			for (TransformDescriptor td : existing) {
				if (td instanceof CollectionTransformDescriptor) {
					return (CollectionTransformDescriptor) td;
				}
			}
		}
		return null;
	}

	/**
	 * Removes only the descriptors of one kind from a class's transform list, leaving descriptors
	 * of the other kind in place - collection tracking descriptors and user-defined JFR probes
	 * share the class-keyed {@code transformData} map and must never clobber each other.
	 *
	 * @param className
	 *            the internal class name whose list to prune.
	 * @param collectionDescriptors
	 *            {@code true} to remove {@link CollectionTransformDescriptor}s, {@code false} to
	 *            remove the (JFR probe) descriptors that are not.
	 * @return {@code true} if at least one descriptor was removed.
	 */
	private boolean removeTransformDescriptors(String className, boolean collectionDescriptors) {
		List<TransformDescriptor> existing = transformData.get(className);
		if (existing == null) {
			return false;
		}
		// Copy-on-write, see add(): the published list must not be mutated.
		List<TransformDescriptor> kept = new ArrayList<>(existing);
		boolean removed = kept.removeIf(td -> (td instanceof CollectionTransformDescriptor) == collectionDescriptors);
		if (kept.isEmpty()) {
			transformData.remove(className);
		} else if (removed) {
			transformData.put(className, kept);
		}
		return removed;
	}

	@Override
	public void disableCollectionTracking() {
		collectionTrackingSettings = CollectionTrackingSettings.disabled();
		for (CollectionTransformDescriptor descriptor : CollectionTransformDescriptor.all()) {
			removeTransformDescriptors(descriptor.getClassName(), true);
		}
		// Keep the read-back configuration truthful, e.g. when called on an init failure after the
		// startup configuration (with tracking enabled) was already stored.
		currentConfiguration = renderEffectiveConfiguration(currentConfiguration);
	}

	/**
	 * Enables (or retunes) collection resize tracking from a JMX {@code modify}. Enabling it for
	 * the first time defines the bootstrap bridge and retransforms hot core JDK collection classes,
	 * which can cause a lot of deoptimization. Prefer enabling at startup, and prefer disabling the
	 * JFR event in the recording over unweaving, which retransforms the classes again.
	 */
	private void enableCollectionTracking(
		XMLStreamReader streamReader, Set<String> modifiedClasses, Set<String> retainedClasses) {
		String minSizeValue = streamReader.getAttributeValue("", XML_ATTRIBUTE_NAME_MINSIZE); //$NON-NLS-1$
		// Only an explicit minsize retunes the live threshold; a bare element keeps it.
		Integer explicitMinSize = (minSizeValue == null || minSizeValue.trim().isEmpty()) ? null
				: Integer.valueOf(parseMinSize(minSizeValue));
		try {
			CollectionResizeEmitter.installOrRetune(explicitMinSize);
		} catch (Throwable t) {
			// Throwable: a missing jdk.jfr module or inaccessible Unsafe surfaces as an Error and
			// must not escape into the JMX call.
			logger.log(Level.SEVERE, "Failed to initialize collection resize tracking; it will not be enabled", t); //$NON-NLS-1$
		}
		if (CollectionResizeEmitter.isInstalled()) {
			collectionTrackingSettings = CollectionTrackingSettings.enabled(CollectionResizeEmitter.getMinSize());
			addCollectionTrackingTransforms(modifiedClasses, retainedClasses);
		}
	}

	private static int parseMinSize(String minSizeValue) {
		if (minSizeValue == null || minSizeValue.trim().isEmpty()) {
			return CollectionTrackingSettings.DEFAULT_MIN_SIZE;
		}
		try {
			long parsed = Long.parseLong(minSizeValue.trim());
			return parsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) parsed;
		} catch (NumberFormatException e) {
			// Larger than a long (or malformed despite validation) - suppress rather than fall back to
			// the noisiest default.
			logger.log(Level.WARNING, "Could not parse collection tracking minsize '" + minSizeValue //$NON-NLS-1$
					+ "'; suppressing events (using Integer.MAX_VALUE)", e); //$NON-NLS-1$
			return Integer.MAX_VALUE;
		}
	}

	private static CollectionTrackingSettings parseCollectionTracking(XMLStreamReader streamReader) {
		if (!parseEnabled(streamReader)) {
			return CollectionTrackingSettings.disabled();
		}
		return CollectionTrackingSettings
				.enabled(parseMinSize(streamReader.getAttributeValue("", XML_ATTRIBUTE_NAME_MINSIZE))); //$NON-NLS-1$
	}

	/**
	 * @return the value of the {@code enabled} attribute on the current element; an absent
	 *         attribute means enabled. The xs:boolean lexical space is {@code true|false|1|0}.
	 */
	private static boolean parseEnabled(XMLStreamReader streamReader) {
		String value = streamReader.getAttributeValue("", XML_ATTRIBUTE_NAME_ENABLED); //$NON-NLS-1$
		if (value == null) {
			return true;
		}
		String trimmed = value.trim();
		return !("false".equals(trimmed) || "0".equals(trimmed)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static boolean validate(DefaultTransformRegistry registry, TransformDescriptor td) {
		if (td.getClassName() == null) {
			Agent.getLogger().warning("Encountered probe without associated class! Check probe definitions!"); //$NON-NLS-1$
			return false;
		}
		if (td.getId() == null) {
			Agent.getLogger().warning("Encountered probe without associated id! Check probe definitions!"); //$NON-NLS-1$
			return false;
		}

		List<TransformDescriptor> transformDataList = registry.getTransformData(td.getClassName());
		if (transformDataList != null && td instanceof JFRTransformDescriptor) {
			String tdEventClassName = ((JFRTransformDescriptor) td).getEventClassName();
			for (TransformDescriptor tdListEntry : transformDataList) {
				// The list may hold non-JFR descriptors (collection tracking); only JFR probes have
				// event class names to dedupe against.
				if (!(tdListEntry instanceof JFRTransformDescriptor)) {
					continue;
				}
				String existingName = ((JFRTransformDescriptor) tdListEntry).getEventClassName();
				if (existingName.equals(tdEventClassName)) {
					Agent.getLogger().warning("Encountered probe with an event class name that already exists: "
							+ tdEventClassName + "Check probe definitions!"); //$NON-NLS-1$
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
		return TransformDescriptor.create(id, TypeUtils.getInternalName(values.get("class")), method, values, //$NON-NLS-1$
				parameters, returnValue[0], fields);
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
			logger.log(Level.SEVERE, "Failed to parse global config", e);
		}
	}

	private static void addDefaults(HashMap<String, String> globalDefaults) {
		globalDefaults.put(TransformDescriptor.ATTRIBUTE_CLASS_PREFIX, "__JFREvent"); //$NON-NLS-1$
		// For safety reasons, allowing toString is opt-in
		globalDefaults.put(TransformDescriptor.ATTRIBUTE_ALLOW_TO_STRING, "false"); //$NON-NLS-1$
		// For safety reasons, allowing converters is opt-in
		globalDefaults.put(TransformDescriptor.ATTRIBUTE_ALLOW_CONVERTER, "false"); //$NON-NLS-1$
		globalDefaults.put(TransformDescriptor.ATTRIBUTE_EMIT_ON_EXCEPTION, "false"); //$NON-NLS-1$
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

	private static Method parseMethod(
		XMLStreamReader streamReader, List<Parameter> parameters, ReturnValue[] returnValue) throws XMLStreamException {
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
						descriptor = value != null ? value.trim() : null;
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
		List<TransformDescriptor> data = transformData.get(className);
		return data != null ? data : Collections.emptyList();
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

	@Override
	public Set<String> modify(String xmlDescription) throws XMLValidationException {
		try {
			validateProbeDefinition(xmlDescription);

			StringReader reader = new StringReader(xmlDescription);
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			disableExternalEntityProcessing(inputFactory);
			XMLStreamReader streamReader = inputFactory.createXMLStreamReader(reader);
			HashMap<String, String> globalDefaults = new HashMap<String, String>();
			// Classes for the caller to retransform.
			Set<String> modifiedClasses = new HashSet<>();
			// Tracked collection classes to keep, without retransforming.
			Set<String> retainedClasses = new HashSet<>();
			// Classes with an <event> probe in this push.
			Set<String> declaredEventClasses = new HashSet<>();
			boolean collectionTrackingDisabled = false;
			logger.info(xmlDescription);
			while (streamReader.hasNext()) {
				if (streamReader.isStartElement()) {
					QName element = streamReader.getName();
					if (XML_ELEMENT_NAME_EVENT.equals(element.getLocalPart())) {
						TransformDescriptor td = parseTransformData(streamReader, globalDefaults);
						if (declaredEventClasses.add(td.getClassName())) {
							// Purge only the JFR probes from the previous configuration; a collection
							// tracking descriptor on the same class must survive.
							removeTransformDescriptors(td.getClassName(), false);
						}
						modifiedClasses.add(td.getClassName());
						if (validate(this, td)) {
							add(td);
						}
						continue;
					} else if (XML_ELEMENT_CONFIGURATION.equals(element.getLocalPart())) {
						readGlobalConfig(streamReader, globalDefaults);
					} else if (XML_ELEMENT_COLLECTION_TRACKING.equals(element.getLocalPart())) {
						if (parseEnabled(streamReader)) {
							enableCollectionTracking(streamReader, modifiedClasses, retainedClasses);
						} else {
							collectionTrackingDisabled = true;
						}
					}
				}
				streamReader.next();
			}
			if (collectionTrackingDisabled) {
				// The JMX caller retransforms the dropped classes back to their unwoven bytecode.
				// An omitted element leaves tracking untouched, see TransformRegistry#modify.
				disableCollectionTracking();
			} else if (collectionTrackingSettings.isEnabled()) {
				addCollectionTrackingTransforms(null, retainedClasses);
			}
			// Tracked collection classes survive clearAllOtherTransformData, so stale JFR probes on
			// them from the previous configuration must be purged here (and the class rewoven) unless
			// this push re-declared probes for the class.
			for (String className : retainedClasses) {
				if (!declaredEventClasses.contains(className) && removeTransformDescriptors(className, false)) {
					modifiedClasses.add(className);
				}
			}
			setCurrentConfiguration(xmlDescription);
			Set<String> classesToKeep = new HashSet<>(modifiedClasses);
			classesToKeep.addAll(retainedClasses);
			clearAllOtherTransformData(classesToKeep);
			return modifiedClasses;
		} catch (XMLStreamException xse) {
			logger.log(Level.SEVERE, "Failed to create XML Stream Reader", xse);
			throw new XMLValidationException(xse.getMessage(), xse);
		}
	}

	private void clearAllOtherTransformData(Set<String> classesToKeep) {
		Set<String> classNames = new HashSet<>(getClassNames());
		for (String className : classNames) {
			if (!classesToKeep.contains(className)) {
				transformData.remove(className);
			}
		}
	}

	@Override
	public Set<String> clearAllTransformData() {
		Set<String> classNames = new HashSet<>(getClassNames());
		transformData.clear();
		// Revert-all disables collection tracking too; the returned class names still include the
		// collection classes, so they are retransformed back to their original (unwoven) bytecode.
		collectionTrackingSettings = CollectionTrackingSettings.disabled();
		return classNames;
	}

	private static String getXmlAsString(InputStream in) {
		return new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
	}

	@Override
	public Set<String> getClassNames() {
		return Collections.unmodifiableSet(transformData.keySet());
	}

	@Override
	public CollectionTrackingSettings getCollectionTrackingSettings() {
		return collectionTrackingSettings;
	}

	@Override
	public String getCurrentConfiguration() {
		return currentConfiguration;
	}

	@Override
	public void setCurrentConfiguration(String configuration) {
		currentConfiguration = renderEffectiveConfiguration(configuration);
	}

	/**
	 * Renders the effective collection tracking state into a configuration document: any
	 * {@code collectiontracking} element in the input is replaced by
	 * {@code <collectiontracking enabled="true|false" minsize="N"/>} reflecting the live state
	 * ({@code minsize} only when enabled), and the element is added if absent, since the pushed
	 * document alone does not describe the tracking state (see {@link #modify}).
	 *
	 * @param xmlConfiguration
	 *            the pushed (schema-validated) configuration, possibly empty.
	 * @return the configuration with the effective tracking state rendered in, or the input
	 *         verbatim if it could not be parsed.
	 */
	private String renderEffectiveConfiguration(String xmlConfiguration) {
		String base = xmlConfiguration == null || xmlConfiguration.trim().isEmpty() ? EMPTY_ROOT_DOCUMENT
				: xmlConfiguration;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XML_PARSER_DISALLOW_DOCTYPE, true);
			Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(base)));
			Element root = document.getDocumentElement();
			Node child = root.getFirstChild();
			while (child != null) {
				Node next = child.getNextSibling();
				if (child.getNodeType() == Node.ELEMENT_NODE
						&& XML_ELEMENT_COLLECTION_TRACKING.equals(child.getNodeName())) {
					root.removeChild(child);
				}
				child = next;
			}
			CollectionTrackingSettings settings = collectionTrackingSettings;
			Element tracking = document.createElement(XML_ELEMENT_COLLECTION_TRACKING);
			tracking.setAttribute(XML_ATTRIBUTE_NAME_ENABLED, Boolean.toString(settings.isEnabled()));
			if (settings.isEnabled()) {
				tracking.setAttribute(XML_ATTRIBUTE_NAME_MINSIZE, Integer.toString(settings.getMinSize()));
			}
			root.appendChild(tracking);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			javax.xml.transform.Transformer serializer = transformerFactory.newTransformer();
			serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
			StringWriter writer = new StringWriter();
			serializer.transform(new DOMSource(document), new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not render the effective collection tracking state into the read-back" //$NON-NLS-1$
					+ " configuration; storing the pushed configuration verbatim", e); //$NON-NLS-1$
			return xmlConfiguration;
		}
	}

	@Override
	public void setRevertInstrumentation(boolean shouldRevert) {
		this.revertInstrumentation = shouldRevert;
	}

	@Override
	public boolean isRevertIntrumentation() {
		return revertInstrumentation;
	}

	private static void disableExternalEntityProcessing(XMLInputFactory inputFactory) {
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		inputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
	}

}
