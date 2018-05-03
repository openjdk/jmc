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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.gui;

import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_CONTENT_TYPE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_CONTROL_REFERENCE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_DESCRIPTION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_FALSE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_LABEL;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_LABEL_MANDATORY;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_MAXIMUM;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_MINIMUM;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_NAME_DEFINITION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_NAME_REFERENCE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_OPERATOR;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_TRUE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_URI;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_VALUE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.ATTRIBUTE_VERSION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_AND;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_CATEGORY;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_CONDITION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_CONTROL;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_EVENTTYPE_V1;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_EVENTTYPE_V2;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_FLAG;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_NOT;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_OPTION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_OR;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_PRODUCER;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_SELECTION;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_SETTING;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_TEST;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.JFCGrammar.TAG_TEXT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.logging.Level;

import org.openjdk.jmc.common.unit.ComparableConstraint;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IPersister;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLAttribute;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTag;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;
import org.openjdk.jmc.ui.UIPlugin;

/**
 * Model building and holding an evaluation graph to apply control changes to JFR configuration
 * settings. It can also build the corresponding GUI controls. It uses the transmitter/receiver
 * terminology for the node relationships, rather than producer/consumer to lessen the confusion
 * with JFR event producers.
 */
public final class GUIModel extends Observable {
	private static final IPersister<String> PLAIN_TEXT_PERSISTER = PLAIN_TEXT.getPersister();
	private final List<ProducerEnvironment> m_producerEnvs;
	private final HashMap<String, Node> m_transmitterNodes;
	private final HashMap<String, List<Node>> m_receiverNodeLists;
	private final XMLModel m_model;
	private SchemaVersion m_schemaVersion;

	public GUIModel(XMLModel model) {
		m_model = model;
		m_producerEnvs = new ArrayList<>();
		m_transmitterNodes = new HashMap<>();
		m_receiverNodeLists = new HashMap<>();
		m_schemaVersion = SchemaVersion.fromBeanVersion(m_model.getRoot().getValue(ATTRIBUTE_VERSION));

		// JFC V1.0
		for (XMLTagInstance producerElement : model.getRoot().getTagsInstances(TAG_PRODUCER)) {
			createProducer(producerElement);
		}

		// JFC V2.0
		createGlobalProducer(model.getRoot());

		hookupTransmittersAndReceivers();
	}

	private void hookupTransmittersAndReceivers() {
		for (Entry<String, List<Node>> entry : m_receiverNodeLists.entrySet()) {
			Node transmitter = m_transmitterNodes.get(entry.getKey());
			if (transmitter != null) {
				for (Node receiver : entry.getValue()) {
					transmitter.addReceiver(receiver);
				}
			}
		}
	}

	/**
	 * Perform initial evaluation of the node graph, to make it consistent. Otherwise, it might not
	 * be consistent until all top nodes has been modified.
	 */
	public void evaluate() {
		for (ProducerEnvironment producer : m_producerEnvs) {
			for (WidgetNode topNode : producer.getWidgets()) {
				topNode.fireChange();
			}
		}
	}

	private void createProducer(XMLTagInstance producerElement) {
		ProducerEnvironment producerEnv = createProducerEnvironment(producerElement);
		createControls(producerElement.getTagsInstances(TAG_CONTROL), producerEnv);
		createEventSettings(producerElement.getTagsInstances(TAG_EVENTTYPE_V1), producerEnv);
	}

	private ProducerEnvironment createProducerEnvironment(XMLTagInstance producerElement) {
		ProducerEnvironment environment;
		environment = new ProducerEnvironment();
		environment.setName(producerElement.getValue(ATTRIBUTE_LABEL));
		environment.setDescription(producerElement.getValue(ATTRIBUTE_DESCRIPTION));
		environment.setURI(producerElement.getValue(ATTRIBUTE_URI));
		m_producerEnvs.add(environment);
		return environment;
	}

	private void createGlobalProducer(XMLTagInstance rootElement) {
		List<XMLTagInstance> controlElements = rootElement.getTagsInstances(TAG_CONTROL);
		if (!controlElements.isEmpty()) {
			ProducerEnvironment environment = new ProducerEnvironment();
			environment.setDescription(rootElement.getValue(ATTRIBUTE_DESCRIPTION));
			m_producerEnvs.add(environment);
			createControls(controlElements, environment);
			createEventSettings(findNestedEventsV2(rootElement), environment);
		}
	}

	private List<XMLTagInstance> findNestedEventsV2(XMLTagInstance rootElement) {
		List<XMLTagInstance> categories = rootElement.getTagsInstances(TAG_CATEGORY);
		List<XMLTagInstance> events = rootElement.getTagsInstances(TAG_EVENTTYPE_V2);
		for (int i = 0; i < categories.size(); i++) {
			XMLTagInstance category = categories.get(i);
			events.addAll(category.getTagsInstances(TAG_EVENTTYPE_V2));
			for (XMLTagInstance subcategory : category.getTagsInstances(TAG_CATEGORY)) {
				categories.add(subcategory);
			}
		}
		return events;
	}

	private void createControls(Iterable<XMLTagInstance> controlElements, ProducerEnvironment producerEnv) {
		for (XMLTagInstance controlElement : controlElements) {
			createInputs(controlElement, producerEnv);
			createConditionals(controlElement, producerEnv);
		}
	}

	public String addProducerPrefix(
		ProducerEnvironment producerEnv, XMLTagInstance tagInstance, XMLAttribute attribute) {
		String value = tagInstance.getValue(attribute);
		if (isURI(value)) {
			return value;
		}
		return producerEnv.getURI() + value;
	}

	private static boolean isURI(String value) {
		return value.contains(":"); //$NON-NLS-1$
	}

	private void createConditionals(XMLTagInstance controlElement, ProducerEnvironment producerEnv) {
		for (XMLTagInstance conditionElement : controlElement.getTagsInstances(TAG_CONDITION)) {
			ConditionNodeItem item = buildConditionItem(producerEnv, conditionElement);
			String name = addProducerPrefix(producerEnv, conditionElement, ATTRIBUTE_NAME_DEFINITION);

			// FIXME: How do we know it is a ConditionNode? What if it isn't?
			ConditionNode node = (ConditionNode) m_transmitterNodes.get(name);
			if (node == null) {
				node = new ConditionNode();
				addTransmitterNode(producerEnv, name, node);
			}
			// Set up the receiver for the internal condition items immediately, since order matters.
			item.addReceiver(node);
		}
	}

	private void createEventSettings(Iterable<XMLTagInstance> eventElements, ProducerEnvironment producerEnv) {
		for (XMLTagInstance eventElement : eventElements) {
			for (XMLTagInstance settingElement : eventElement.getTagsInstances(TAG_SETTING)) {
				createSetting(producerEnv, settingElement);
			}
		}
	}

	private void createSetting(ProducerEnvironment producerEnv, final XMLTagInstance settingElement) {
		String variableName = settingElement.getValue(ATTRIBUTE_CONTROL_REFERENCE);
		if (!variableName.trim().isEmpty()) {
			variableName = addProducerPrefix(producerEnv, settingElement, ATTRIBUTE_CONTROL_REFERENCE);
			addReceiverNode(producerEnv, variableName, new SettingNode(m_model, settingElement));
		}
	}

	private void addReceiverNode(ProducerEnvironment producerEnv, String variableName, Node receiverNode) {
		List<Node> receiverList = m_receiverNodeLists.get(variableName);
		if (receiverList == null) {
			receiverList = new ArrayList<>();
			m_receiverNodeLists.put(variableName, receiverList);
		}
		receiverList.add(receiverNode);
	}

	private void addTransmitterNode(ProducerEnvironment producerEnv, String variableName, Node transmitterNode) {
		producerEnv.addNode(transmitterNode);
		m_transmitterNodes.put(variableName, transmitterNode);
	}

	private void createInputs(XMLTagInstance controlElement, ProducerEnvironment producerEnv) {
		for (XMLTagInstance inputElement : controlElement.getTagsInstances()) {
			if (inputElement.getTag() == TAG_TEXT) {
				String typeStr = inputElement.getExplicitValue(ATTRIBUTE_CONTENT_TYPE);
				String minPersisted = inputElement.getExplicitValue(ATTRIBUTE_MINIMUM);
				String maxPersisted = inputElement.getExplicitValue(ATTRIBUTE_MAXIMUM);
				// FIXME: Must simplify to not fall back on V1 once the JDK is properly updated. We cannot release with the fallback.
				IConstraint<?> constraint = PLAIN_TEXT_PERSISTER;
				if (m_schemaVersion != SchemaVersion.V1) {
					constraint = CommonConstraints.forContentTypeV2(typeStr);
				}
				if (constraint == PLAIN_TEXT_PERSISTER) {
					constraint = CommonConstraints.forContentTypeV1(typeStr);
				}
				if (constraint != PLAIN_TEXT_PERSISTER) {
					try {
						constraint = ComparableConstraint.constrain(constraint, minPersisted, maxPersisted);
					} catch (QuantityConversionException e) {
						// FIXME: Report in GUI?
						UIPlugin.getDefault().getLogger().log(Level.WARNING, "Control element limit out of range.", e); //$NON-NLS-1$
					}
				}
				TextNode<?> node = new TextNode<>(m_model, inputElement, constraint);
				node.setLabel(inputElement.getValue(ATTRIBUTE_LABEL_MANDATORY));
				node.setDescription(inputElement.getValue(ATTRIBUTE_DESCRIPTION));
				String name = addProducerPrefix(producerEnv, inputElement, ATTRIBUTE_NAME_DEFINITION);
				addTransmitterNode(producerEnv, name, node);
			}

			if (inputElement.getTag() == TAG_FLAG) {
				FlagNode node = new FlagNode(m_model, inputElement);
				node.setLabel(inputElement.getValue(ATTRIBUTE_LABEL_MANDATORY));
				node.setDescription(inputElement.getValue(ATTRIBUTE_DESCRIPTION));
				String name = addProducerPrefix(producerEnv, inputElement, ATTRIBUTE_NAME_DEFINITION);
				addTransmitterNode(producerEnv, name, node);
			}

			if (inputElement.getTag() == TAG_SELECTION) {
				SelectionNode node = new SelectionNode(m_model, inputElement);
				node.setLabel(inputElement.getValue(ATTRIBUTE_LABEL_MANDATORY));
				node.setDescription(inputElement.getValue(ATTRIBUTE_DESCRIPTION));
				String name = addProducerPrefix(producerEnv, inputElement, ATTRIBUTE_NAME_DEFINITION);

				for (XMLTagInstance optionElement : inputElement.getTagsInstances(TAG_OPTION)) {
					node.addItem(optionElement);
				}
				addTransmitterNode(producerEnv, name, node);
			}
		}
	}

	private ConditionNodeItem buildConditionItem(ProducerEnvironment producerEnv, XMLTagInstance conditionElement) {
		ConditionNodeItem cv = new ConditionNodeItem(conditionElement.getValue(ATTRIBUTE_TRUE),
				conditionElement.getValue(ATTRIBUTE_FALSE));
		List<XMLTagInstance> children = conditionElement.getTagsInstances();
		if (children.size() == 1) {
			createLogicNode(producerEnv, children.get(0)).addReceiver(cv);
			return cv;
		}
		throw new IllegalArgumentException("Condition must have exactly one direct child"); //$NON-NLS-1$
	}

	private Node createLogicNode(ProducerEnvironment producerEnv, XMLTagInstance logicElement) {
		XMLTag tag = logicElement.getTag();
		List<XMLTagInstance> children = logicElement.getTagsInstances();

		if (tag == TAG_TEST) {
			if (children.isEmpty()) {
				String variableName = addProducerPrefix(producerEnv, logicElement, ATTRIBUTE_NAME_REFERENCE);
				String operator = logicElement.getValue(ATTRIBUTE_OPERATOR);
				String value = logicElement.getValue(ATTRIBUTE_VALUE);

				Node test = new TestNode(value, operator);
				addReceiverNode(producerEnv, variableName, test);
				return test;
			}
			throw new IllegalArgumentException(TAG_TEST + " mustn't have any child elements"); //$NON-NLS-1$
		}

		if (tag == TAG_NOT) {
			if (children.size() == 1) {
				NotNode notNode = new NotNode();
				Node childNode = createLogicNode(producerEnv, children.get(0));
				childNode.addReceiver(notNode);
				return notNode;
			}
			throw new IllegalArgumentException(TAG_NOT + " must have a single child element"); //$NON-NLS-1$
		}

		if (tag == TAG_AND) {
			Node and = new AndNode();
			hookOperatorChildren(producerEnv, and, children);
			return and;
		}

		if (tag == TAG_OR) {
			Node or = new OrNode();
			hookOperatorChildren(producerEnv, or, children);
			return or;
		}
		throw new IllegalArgumentException("Unknown tag " + tag); //$NON-NLS-1$
	}

	private void hookOperatorChildren(
		ProducerEnvironment producerEnv, Node operatorNode, List<XMLTagInstance> operandElements) {
		for (XMLTagInstance childElement : operandElements) {
			createLogicNode(producerEnv, childElement).addReceiver(operatorNode);
		}
	}

	public List<ProducerEnvironment> getProducers() {
		return m_producerEnvs;
	}
}
