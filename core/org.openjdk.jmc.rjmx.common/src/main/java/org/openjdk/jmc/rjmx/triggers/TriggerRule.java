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
package org.openjdk.jmc.rjmx.triggers;

import java.util.logging.Level;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.triggers.internal.INotificationFactory;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationToolkit;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationTrigger;
import org.w3c.dom.Element;

/**
 * A notification rule is the 3-tuple (NotificationTrigger, NotificationConstraint and
 * NotificationAction) with a name.
 * <p>
 * A rule is an {@link IMRIValueListener} that will trigger whenever a value that fulfills the
 * conditions of the rule is intercepted.
 * <p>
 * Note that this API may change in 6.0.
 */
public final class TriggerRule implements Comparable<Object>, IDescribable {
	private static final String DEFAULT_RULE_PATH = "<new group>"; //$NON-NLS-1$
	private static final String XML_ELEMENT_ACTION_CLASS = "notification_action_class"; //$NON-NLS-1$
	private static final String XML_ELEMENT_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String XML_ACTION_TAG = "notification_action"; //$NON-NLS-1$
	private final static String XML_CONSTRAINT_TAG = "constraint"; //$NON-NLS-1$
	private static final String XML_TRIGGER_TAG = "trigger"; //$NON-NLS-1$
	private static final String DEFAULT_DESCRIPTION = null;

	private volatile String m_name;
	private volatile NotificationTrigger m_trigger;
	private final TriggerConstraintHolder m_constraintHolder;
	private volatile ITriggerAction m_action;

	private String m_rulePath;
	private String m_description;

	public TriggerRule() {
		this(NotificationRegistry.DEFAULT_RULE_NAME, null, null);
	}

	public TriggerRule(String name, NotificationTrigger trigger, ITriggerAction action) {
		this(name, trigger, new TriggerConstraintHolder(), action);
	}

	public TriggerRule(String name, NotificationTrigger trigger, TriggerConstraintHolder constraintHolder,
			ITriggerAction action) {
		this(name, null, "Default Group", trigger, constraintHolder, action); //$NON-NLS-1$
	}

	public TriggerRule(String name, String description, String rulePath, NotificationTrigger trigger,
			TriggerConstraintHolder constraintHolder, ITriggerAction action) {
		setName(name);
		setDescription(description);
		setRulePath(rulePath);
		setTrigger(trigger);
		m_constraintHolder = constraintHolder;
		setAction(action);
	}

	/**
	 * Gets the action.
	 *
	 * @return Returns a NotificationAction
	 */
	public ITriggerAction getAction() {
		return m_action;
	}

	/**
	 * A path to the rule. A grouping.
	 *
	 * @return the path to the rule.
	 */
	public String getRulePath() {
		return m_rulePath;
	}

	/**
	 * Sets the path to the rule.
	 *
	 * @param rulePath
	 *            the path to the rule.
	 */
	public void setRulePath(String rulePath) {
		m_rulePath = rulePath;
	}

	/**
	 * Sets the action.
	 *
	 * @param action
	 *            The action to set
	 */
	public void setAction(ITriggerAction action) {
		m_action = action;
	}

	/**
	 * Gets the name.
	 *
	 * @return Returns a String
	 */
	@Override
	public String getName() {
		return m_name;
	}

	/**
	 * Sets the name.
	 *
	 * @param name
	 *            The name to set
	 */
	public void setName(String name) {
		m_name = name;
	}

	/**
	 * Gets the trigger.
	 *
	 * @return Returns a NotificationTrigger
	 */
	public NotificationTrigger getTrigger() {
		return m_trigger;
	}

	/**
	 * Sets the trigger.
	 *
	 * @param trigger
	 *            the trigger to set
	 */
	public void setTrigger(NotificationTrigger trigger) {
		m_trigger = trigger;
	}

	/**
	 * Gets the constraintHolder.
	 *
	 * @return Returns a NotificationConstraintHolder
	 */
	public TriggerConstraintHolder getConstraintHolder() {
		return m_constraintHolder;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Only for internal debugging purposes.
	 *
	 * @return String verbose information about this rule.
	 */
	protected String getVerboseInformation() {
		StringBuffer sb = new StringBuffer();
		sb.append("----Notification rule----"); //$NON-NLS-1$
		sb.append("\nName: "); //$NON-NLS-1$
		sb.append(getName());
		if (hasTrigger()) {
			sb.append("\nTrigger: "); //$NON-NLS-1$
			sb.append(getTrigger().toString());
		} else {
			sb.append("\nNo trigger!"); //$NON-NLS-1$
		}
		if (hasConstraints()) {
			sb.append("\nConstraints:\n"); //$NON-NLS-1$
			sb.append(getConstraintHolder().toString());
		} else {
			sb.append("\nNoConstraints!\n"); //$NON-NLS-1$
		}
		if (hasAction()) {
			sb.append("Action: "); //$NON-NLS-1$
			sb.append(getAction().toString());
		} else {
			sb.append("No action!"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * @return boolean true if this rule has constraints.
	 */
	public boolean hasConstraints() {
		return getConstraintHolder() != null && getConstraintHolder().getConstraintList().size() > 0;
	}

	/**
	 * @return true if this rule contains a complete trigger.
	 */
	public boolean hasTrigger() {
		return getTrigger() != null && NotificationToolkit.isComplete(getTrigger());
	}

	/**
	 * @return boolean true if this rule has an action.
	 */
	public boolean hasAction() {
		return getAction() != null;
	}

	/**
	 * To be able to sort it in lists...
	 *
	 * @see Comparable#compareTo(Object)
	 */
	@Override
	public int compareTo(Object o) {
		return toString().compareTo(o.toString());
	}

	public static TriggerRule buildFromXml(Element node, INotificationFactory factory) {
		String ruleName = XmlToolkit.getSetting(node, NotificationRegistry.XML_RULE_ELEMENT_RULE_NAME,
				NotificationRegistry.DEFAULT_RULE_NAME);
		String rulePath = XmlToolkit.getSetting(node, NotificationRegistry.XML_RULE_ELEMENT_RULE_PATH,
				DEFAULT_RULE_PATH);
		String description = XmlToolkit.getSetting(node, XML_ELEMENT_DESCRIPTION, DEFAULT_DESCRIPTION);
//		NotificationTrigger trigger = new NotificationTrigger();
		NotificationTrigger trigger = factory.createTrigger();
		Element triggerNode = XmlToolkit.getOrCreateElement(node, XML_TRIGGER_TAG);
		trigger.initializeFromXml(triggerNode, factory);

		Element constraintsNode = XmlToolkit.getOrCreateElement(node, XML_CONSTRAINT_TAG);
		TriggerConstraintHolder constraintHolder = TriggerConstraintHolder.buildFromXml(constraintsNode, factory);

		Element actionNode = XmlToolkit.getOrCreateElement(node, XML_ACTION_TAG);
		String className = XmlToolkit.getSetting(actionNode, XML_ELEMENT_ACTION_CLASS, null);
		assert (className != null);
		// If the type is missing we're in trouble...
		ITriggerAction action = null;
		try {
			action = factory.createAction(className);
			action.initializeFromXml(actionNode);
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Error while initializing the rule " + ruleName, e); //$NON-NLS-1$
			return new TriggerRule();
		}
		return new TriggerRule(ruleName, description, rulePath, trigger, constraintHolder, action);
	}

	/**
	 * Creates and inserts an XML node for this object that becomes a subnode to the specified
	 * parent node.
	 *
	 * @param parentNode
	 *            the XML node to become a subnode to
	 */
	public void exportToXml(Element parentNode) {
		Element ruleNode = XmlToolkit.createElement(parentNode, getComponentTag());
		XmlToolkit.setSetting(ruleNode, NotificationRegistry.XML_RULE_ELEMENT_RULE_NAME, getName());
		XmlToolkit.setSetting(ruleNode, NotificationRegistry.XML_RULE_ELEMENT_RULE_PATH, getRulePath());
		XmlToolkit.setSetting(ruleNode, XML_ELEMENT_DESCRIPTION, getDescription());

		Element triggerNode = XmlToolkit.createElement(ruleNode, XML_TRIGGER_TAG);
		getTrigger().exportToXml(triggerNode);
		// Export the action
		Element actionNode = XmlToolkit.createElement(ruleNode, XML_ACTION_TAG);
		XmlToolkit.setSetting(actionNode, XML_ELEMENT_ACTION_CLASS, getAction().getClass().getName());
		getAction().exportToXml(actionNode);
		Element constraintsNode = XmlToolkit.createElement(ruleNode, XML_CONSTRAINT_TAG);
		m_constraintHolder.exportToXml(constraintsNode);
	}

	/**
	 * This method returns true if the rule has a name, a complete trigger and an action.
	 *
	 * @return boolean
	 */
	public boolean isComplete() {
		return hasTrigger() && NotificationToolkit.isComplete(getTrigger()) && hasAction();
	}

	private String getComponentTag() {
		return NotificationRegistry.XML_RULE_COMPONENT_NAME;
	}

	/**
	 * Sets the description of the rule
	 *
	 * @param description
	 *            the description, or null if not available
	 */
	public void setDescription(String description) {
		m_description = description;
	}

	/**
	 * Returns the description of the rule
	 *
	 * @return the description, or null if not available
	 */
	@Override
	public String getDescription() {
		return m_description;
	}

	/**
	 * Return whether this rule is ready to handle events. Implementations may perform checks and
	 * operations within this method.
	 *
	 * @return <tt>true</tt> if rule is ready to handle events, <tt>false</tt> otherwise
	 */
	public boolean isReady() {
		if (hasAction()) {
			return getAction().isReady();
		}
		return false;
	}
}
