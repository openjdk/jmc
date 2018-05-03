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
package org.openjdk.jmc.rjmx.triggers.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The notification handler handles the notification framework and provides a simple way to register
 * and unregister rules.
 */
// FIXME: Must be made thread safe!
public class NotificationRegistry {
	private static final Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.triggers"); //$NON-NLS-1$
	private static final String XML_ROOT_ELEMENT = "triggers"; //$NON-NLS-1$
	private static final String XML_ELEMENT_NOTIFICATION_RULES = "notification_rules"; //$NON-NLS-1$
	public static final String XML_RULE_COMPONENT_NAME = "notification_rule"; //$NON-NLS-1$
	public static final String XML_RULE_ELEMENT_NOTIFICATION_RULE_LINK = "notification_rule_link"; //$NON-NLS-1$
	public static final String XML_RULE_ELEMENT_RULE_NAME = "rule_name"; //$NON-NLS-1$
	public static final String XML_RULE_ELEMENT_RULE_PATH = "rule_path"; //$NON-NLS-1$
	public static final String XML_RULE_ELEMENT_RULE_ID = "rule_id"; //$NON-NLS-1$
	public static final String DEFAULT_RULE_NAME = "<new rule>"; //$NON-NLS-1$

	private INotificationFactory m_factory;
	// <RegistryEntry>
	private final List<RegistryEntry> m_availableConstraints = new LinkedList<>();
	// <RegistryEntry>
	private final List<RegistryEntry> m_availableActions = new LinkedList<>();
	// <Class, RegistryEntry>
	private final Map<Class<?>, RegistryEntry> m_classToRegisterEntryMap = new HashMap<>();
	// <String, NotificationRule>
	private final Set<TriggerRule> m_availableRules = new HashSet<>();
	// <RJMXConnectorModel, NotificationRuleBag>
	protected HashMap<String, NotificationRuleBag> ruleBagMap = new HashMap<>();

	/**
	 * Unregisters a notification rule.
	 *
	 * @param rule
	 *            Rule to remove.
	 * @param serverGuid
	 *            the uid to unbind the rule from.
	 */
	public void unregisterRule(TriggerRule rule, String serverGuid) {
		getRuleBagForUID(serverGuid).removeRule(rule);
	}

	/**
	 * Returns a list of all the Notification rules that have been registered on the specified
	 * ConnectionDescriptor uid.
	 *
	 * @param uid
	 *            The id on which the rules are registered.
	 * @return A List of all rules registered on the model.
	 */
	public Collection<TriggerRule> getRegisteredRules(String uid) {
		return getRuleBagForUID(uid).getAllRegisteredRules();
	}

	/**
	 * Registers a new notification rule.
	 *
	 * @param rule
	 *            the rule to register
	 * @param serverGuid
	 *            the uid to bind this rule to.
	 * @return
	 */
	public boolean registerRule(TriggerRule rule, String serverGuid) {
		return getRuleBagForUID(serverGuid).addRule(rule);
	}

	private NotificationRuleBag getRuleBagForUID(String uid) {
		NotificationRuleBag ruleBag = ruleBagMap.get(uid);
		if (ruleBag == null) {
			ruleBag = new NotificationRuleBag(uid);
			ruleBagMap.put(uid, ruleBag);
		}
		return ruleBag;
	}

	/**
	 * Gets the availableConstraints.
	 *
	 * @return Returns a List of ConstraintEntries <RegistryEntry>
	 */
	public List<RegistryEntry> getAvailableConstraints() {
		return m_availableConstraints;
	}

	/**
	 * Registers a constraint.
	 *
	 * @param constraintEntry
	 *            the constraint to register.
	 */
	public void registerConstraint(RegistryEntry constraintEntry) {
		m_availableConstraints.add(constraintEntry);
		m_classToRegisterEntryMap.put(constraintEntry.getRegisteredClass(), constraintEntry);
	}

	/**
	 * Registers an action.
	 *
	 * @param actionEntry
	 *            The action to register.
	 */
	public void registerAction(RegistryEntry actionEntry) {
		m_availableActions.add(actionEntry);
		m_classToRegisterEntryMap.put(actionEntry.getRegisteredClass(), actionEntry);
	}

	/**
	 * Gets the availableActions.
	 *
	 * @return Returns a List of RegisterEntry <RegistryEntry>
	 */
	public List<RegistryEntry> getAvailableActions() {
		return m_availableActions;
	}

	/**
	 * Return the RegisterEntry for a certain class, or null if no such exists.
	 *
	 * @param myClass
	 *            the class to check for
	 * @return See above.
	 */
	public synchronized RegistryEntry getEntryForClass(Class<?> myClass) {
		return m_classToRegisterEntryMap.get(myClass);
	}

	/**
	 * Gets the availableRules.
	 *
	 * @return Returns a Collection containing all available rules.
	 */
	public Collection<TriggerRule> getAvailableRules() {
		return m_availableRules;
	}

	/**
	 * Adds a global notification rule. This rule will later have to be activated for connector
	 * model that wants it.
	 *
	 * @param rule
	 *            The rule to add.
	 */
	public void addNotificationRule(TriggerRule rule) {
		if (isNameAvailable(rule.getName())) {
			m_availableRules.add(rule);
		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 * @param rule
	 *            the notification rule to perform the critical operation on.
	 * @param runnable
	 *            a runnable with the critical operation.
	 */
	public void performCriticalRuleChange(TriggerRule rule, Runnable runnable) {
		ArrayList<NotificationRuleBag> bags = new ArrayList<>();
		for (NotificationRuleBag bag : ruleBagMap.values()) {
			if (bag.removeRule(rule)) {
				bags.add(bag);
			}
		}
		runnable.run();
		for (NotificationRuleBag bag : bags) {
			bag.addRule(rule);
		}
	}

	/**
	 * Removes the notification rule and unregisters it in all ConnectorModels that use it.
	 *
	 * @param rule
	 *            The rule to remove and unregister.
	 */
	public void removeNotificationRule(TriggerRule rule) {
		for (NotificationRuleBag bag : ruleBagMap.values()) {
			bag.removeRule(rule);
		}
		m_availableRules.remove(rule);
	}

	/**
	 * Returns true if there is no rule with the same name as the argument rule.
	 *
	 * @param name
	 *            The rule name to be checked.
	 * @return true if the rule name is unique
	 */
	public boolean isNameAvailable(String name) {
		return getRuleByName(name) == null;
	}

	/**
	 * Returns the rule identified by the name ruleName.
	 *
	 * @param ruleName
	 *            Name of the rule to retrieve.
	 * @return NotificationRule The rule, if one exist. Null otherwise
	 */
	private TriggerRule getRuleByName(String ruleName) {
		for (TriggerRule rule : m_availableRules) {
			if (rule.getName().equals(ruleName)) {
				return rule;
			}
		}
		return null;
//		return (NotificationRule) m_availableRules.get(ruleName);
	}

	/**
	 * @param selected
	 * @param exportLinks
	 * @throws IOException
	 */
	public Document exportToXml(Collection<TriggerRule> selected, boolean exportLinks) throws IOException {
		Document doc = XmlToolkit.createNewDocument(XML_ROOT_ELEMENT);
		Element repoRoot = doc.getDocumentElement();
		// Export the rules
		Element rulez = XmlToolkit.createElement(repoRoot, XML_ELEMENT_NOTIFICATION_RULES);
		for (TriggerRule rule : getAvailableRules()) {
			if (selected == null || selected.contains(rule)) {
				rule.exportToXml(rulez);
			}
		}

		if (exportLinks) {
			// Export the links between rules and connections
			for (String uid : ruleBagMap.keySet()) {
				for (TriggerRule rule : getRegisteredRules(uid)) {
					Element ruleLink = XmlToolkit.createElement(repoRoot,
							NotificationRegistry.XML_RULE_ELEMENT_NOTIFICATION_RULE_LINK);
					XmlToolkit.setSetting(ruleLink, NotificationRegistry.XML_RULE_ELEMENT_RULE_ID, uid);
					XmlToolkit.setSetting(ruleLink, NotificationRegistry.XML_RULE_ELEMENT_RULE_NAME, (rule).getName());
				}
			}
		}
		return doc;
	}

	public List<TriggerRule> importFromXML(Element node) throws IOException {
		if (!node.getTagName().equals(XML_ROOT_ELEMENT)) {
			throw new IOException();
		}
		Element rules = XmlToolkit.getOrCreateElement(node, NotificationRegistry.XML_ELEMENT_NOTIFICATION_RULES);

		List<TriggerRule> badRules = new ArrayList<>();
		// Initializes the rules.
		List<Element> ruleList = XmlToolkit.getChildElementsByTag(rules, NotificationRegistry.XML_RULE_COMPONENT_NAME);
		for (Element ruleElem : ruleList) {
			TriggerRule rule = TriggerRule.buildFromXml(ruleElem, getFactory());
			if (rule.isComplete()) {
				if (!isNameAvailable(rule.getName())) {
					badRules.add(rule);
				} else {
					addNotificationRule(rule);
				}
			} else {
				badRules.add(rule);
			}
		}

		// Register the rules on the models
		List<Element> ruleLinks = XmlToolkit.getChildElementsByTag(node,
				NotificationRegistry.XML_RULE_ELEMENT_NOTIFICATION_RULE_LINK);
		for (Element ruleLink : ruleLinks) {
			String uid = XmlToolkit.getSetting(ruleLink, NotificationRegistry.XML_RULE_ELEMENT_RULE_ID, null);
			if (uid == null) {
				LOGGER.log(Level.WARNING,
						"Could not find the UID for connection descriptor associated with the rule. Skipping rule."); //$NON-NLS-1$
				continue;
			}

			String ruleName = XmlToolkit.getSetting(ruleLink, NotificationRegistry.XML_RULE_ELEMENT_RULE_NAME, null);
			TriggerRule rule = getRuleByName(ruleName);
			if (rule != null) {
				getRuleBagForUID(uid).addRule(rule);
			}
		}
		return badRules;
	}

	public void activateTriggersFor(IConnectionHandle connectionHandle) {
		getRuleBagForUID(connectionHandle.getServerDescriptor().getGUID()).activate(connectionHandle);
	}

	public void deactivateTriggersFor(String serverGuid) {
		getRuleBagForUID(serverGuid).deactivate();
	}

	/**
	 * Gets the factory for creating constraints and actions
	 *
	 * @return a factory
	 */
	public INotificationFactory getFactory() {
		return m_factory;
	}

	/**
	 * Sets the factory to use when creating constraints and notifications
	 *
	 * @param factory
	 *            the factory
	 */
	public void setFactory(INotificationFactory factory) {
		m_factory = factory;
	}
}
