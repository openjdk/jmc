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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml;

import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_LABEL_MANDATORY;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_NAME;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_CONFIGURATION_V1;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.openjdk.jmc.ui.UIPlugin;

/**
 * A stateless validator of {@link XMLModel}s used for Flight Recording Configurations. Should
 * perhaps factor out aspects that can be expressed using {@link JFCGrammar}.
 */
public final class JFCXMLValidator implements IXMLValidator {
	private static final JFCXMLValidator SHARED = new JFCXMLValidator();

	interface IXMLNodeValidator {
		XMLValidationResult validate(Object parentNode, Object node);
	}

	private static class DanglingReference implements IXMLNodeValidator {
		private final Set<String> m_variableNames;
		private final URI m_baseURI;

		DanglingReference(Set<String> variableNames, URI baseURI) {
			m_variableNames = variableNames;
			m_baseURI = baseURI;
		}

		@Override
		public XMLValidationResult validate(Object parentNode, Object node) {
			if (node instanceof XMLAttributeInstance) {
				XMLAttributeInstance i = (XMLAttributeInstance) node;
				if (i.getAttribute().getType() == XMLNodeType.REFERENCE) {
					String variableName = i.getValue();
					if (!variableName.isEmpty()) {
						variableName = m_baseURI.resolve(variableName).toString();
						if (!m_variableNames.contains(variableName)) {
							return new XMLValidationResult(i, "Variable '" + variableName + "' can't be found.", true); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}
			return XMLValidationResult.OK;
		}
	};

	private static class DuplicateDefinition implements IXMLNodeValidator {
		private final Set<String> m_variables = new HashSet<>();

		@Override
		public XMLValidationResult validate(Object parentNode, Object node) {
			if (node instanceof XMLAttributeInstance) {
				XMLAttributeInstance i = (XMLAttributeInstance) node;
				if (i.getAttribute().getType() == XMLNodeType.DEFINITION) {
					if (parentNode instanceof XMLTagInstance) {
						// Only conditions are allowed to define duplicates.
						XMLTagInstance parent = (XMLTagInstance) parentNode;
						if (parent.getTag() == JFCGrammar.TAG_CONDITION) {
							return XMLValidationResult.OK;
						}
					}
					String name = i.getValue();
					if (m_variables.contains(name)) {
						return new XMLValidationResult(i, "Variable '" + name + "' is defined more than once.", true); //$NON-NLS-1$ //$NON-NLS-2$
					}
					m_variables.add(i.getValue());
				}
			}
			return XMLValidationResult.OK;
		}
	};

	private static class MissingChild implements IXMLNodeValidator {
		@Override
		public XMLValidationResult validate(Object parentNode, Object node) {
			if (node instanceof XMLTagInstance) {
				XMLTagInstance i = (XMLTagInstance) node;
				if (i.getTag().getType() == XMLNodeType.ELEMENT_WITH_AT_LEAST_ONE_CHILD
						&& i.getTagsInstances().isEmpty()) {
					return new XMLValidationResult(i, i.getTag().getName() + " must have at least on child element.", //$NON-NLS-1$
							true);
				}
			}
			return XMLValidationResult.OK;
		}
	};

	private static class InvalidAttribute implements IXMLNodeValidator {
		@Override
		public XMLValidationResult validate(Object parentNode, Object node) {
			if (node instanceof XMLAttributeInstance) {
				XMLAttributeInstance instance = (XMLAttributeInstance) node;
				XMLAttribute attribute = instance.getAttribute();
				if (attribute.isRequired() && instance.isImplicitDefault()) {
					return new XMLValidationResult(instance,
							"'" + instance.getAttribute().getName() + "' is missing content.", true); //$NON-NLS-1$ //$NON-NLS-2$
				}
				Collection<String> validValues = attribute.getValidValues();
				if (!validValues.isEmpty() && !validValues.contains(instance.getValue().toLowerCase())) {
					StringBuilder textBuf = new StringBuilder("Attribute '"); //$NON-NLS-1$
					textBuf.append(instance.getAttribute().getName());
					textBuf.append("' must have one of these values: \""); //$NON-NLS-1$
					Iterator<String> values = validValues.iterator();
					while (values.hasNext()) {
						textBuf.append(values.next());
						if (values.hasNext()) {
							textBuf.append("\", \""); //$NON-NLS-1$
						}
					}
					textBuf.append("\"."); //$NON-NLS-1$

					return new XMLValidationResult(instance, textBuf.toString(), true);
				}
			}
			return XMLValidationResult.OK;
		}
	}

	public static IXMLValidator getValidator() {
		return SHARED;
	}

	private JFCXMLValidator() {
	}

	@Override
	public List<XMLValidationResult> validate(XMLModel model) {
		String label;
		if (model.getRoot().getTag() == TAG_CONFIGURATION_V1) {
			label = model.getRoot().getValue(ATTRIBUTE_NAME);
		} else {
			label = model.getRoot().getValue(ATTRIBUTE_LABEL_MANDATORY);
		}
		UIPlugin.getDefault().getLogger().log(Level.FINE, "Running JFC validation on " + label); //$NON-NLS-1$
		List<XMLValidationResult> errors = new ArrayList<>();
		XMLTagInstance configuration = model.getRoot();
		List<XMLTagInstance> producers = configuration.getTagsInstances(JFCGrammar.TAG_PRODUCER);
		// General stuff
		collectErrors(null, configuration, new MissingChild(), errors);
		collectErrors(null, configuration, new InvalidAttribute(), errors);

		/*
		 * Check variables for each producer Variables can span multiple producers, if the are
		 * prefixed with producer uri. Example: http://www.oracle.com/hotspot/jvm/file-io-threshold
		 */
		Set<String> qualifiedVariableNames = createQualifiedVariableSet(producers, JFCGrammar.ATTRIBUTE_URI);
		for (XMLTagInstance producer : producers) {
			URI producerURI = createTrailingSlashURI(producer.getValue(JFCGrammar.ATTRIBUTE_URI));
			collectErrors(null, producer, new DuplicateDefinition(), errors);
			collectErrors(null, producer, new DanglingReference(qualifiedVariableNames, producerURI), errors);
		}
		return errors;
	}

	private void collectErrors(Object parent, Object o, IXMLNodeValidator validator, List<XMLValidationResult> result) {
		result.add(validator.validate(parent, o));
		if (o instanceof XMLTagInstance) {
			XMLTagInstance t = (XMLTagInstance) o;
			for (XMLTagInstance i : t.getTagsInstances()) {
				collectErrors(o, i, validator, result);
			}
			for (XMLAttributeInstance i : t.getAttributeInstances()) {
				collectErrors(o, i, validator, result);
			}
		}
	}

	// FIXME: Generalize so that an overriding base URI can be applied at any element level?
	// Consider xml:base (http://en.wikipedia.org/wiki/XML_Base).
	public static Set<String> createQualifiedVariableSet(
		List<XMLTagInstance> rootElements, XMLAttribute baseUriAttribute) {
		List<String> variables = new ArrayList<>();
		for (XMLTagInstance rootElement : rootElements) {
			URI baseURI = createTrailingSlashURI(rootElement.getValue(baseUriAttribute));
			Queue<Object> q = new LinkedList<>();
			q.add(rootElement);
			while (!q.isEmpty()) {
				Object o = q.poll();
				if (o instanceof XMLTagInstance) {
					for (XMLTagInstance tagInstance : ((XMLTagInstance) o).getTagsInstances()) {
						q.add(tagInstance);
					}
					for (XMLAttributeInstance attributeInstance : ((XMLTagInstance) o).getAttributeInstances()) {
						q.add(attributeInstance);
					}
				}

				if (o instanceof XMLAttributeInstance) {
					XMLAttributeInstance ai = (XMLAttributeInstance) o;
					if (ai.getAttribute().getType() == XMLNodeType.DEFINITION) {
						String value = ai.getValue();
						if (!value.trim().isEmpty()) {
							String variableName = baseURI.resolve(value).toString();
							variables.add(variableName);
						}
					}
				}
			}
		}
		Collections.sort(variables);
		return new LinkedHashSet<>(variables);
	}

	@Override
	public XMLTag getRootElementType() {
		return JFCGrammar.ROOT;
	}

	@Override
	public Set<XMLTag> getElementsTooKeepOnOneLine() {
		return JFCGrammar.ONE_LINE_ELEMENTS;
	}

	private static URI createTrailingSlashURI(String uri) {
		if (uri.endsWith("/")) { //$NON-NLS-1$
			return URI.create(uri);
		}
		return URI.create(uri + '/');
	}
}
