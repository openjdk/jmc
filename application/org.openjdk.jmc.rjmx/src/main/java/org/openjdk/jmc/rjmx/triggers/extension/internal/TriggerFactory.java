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
package org.openjdk.jmc.rjmx.triggers.extension.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.ITriggerConstraint;
import org.openjdk.jmc.rjmx.triggers.IValueEvaluator;
import org.openjdk.jmc.rjmx.triggers.condition.internal.TriggerCondition;
import org.openjdk.jmc.rjmx.triggers.internal.INotificationFactory;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationTrigger;
import org.openjdk.jmc.rjmx.triggers.internal.RegistryEntry;

/**
 * This class is responsible for activating action and constraint extensions. It will also register
 * the classes with the {@link NotificationRegistry}.
 */
public class TriggerFactory implements INotificationFactory {
	// FIXME: Important, we need to add better error handling.

	private static final String XML_CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$

	private static final String XML_TRIGGER_ACTIONS = "org.openjdk.jmc.rjmx.triggerActions"; //$NON-NLS-1$
	private static final String XML_TRIGGER_ACTION = "triggerAction"; //$NON-NLS-1$

	private static final String XML_TRIGGER_CONSTRAINTS = "org.openjdk.jmc.rjmx.triggerConstraints"; //$NON-NLS-1$
	private static final String XML_TRIGGER_CONSTRAINT = "triggerConstraint"; //$NON-NLS-1$

	private static final String XML_TRIGGER_EVALUATORS = "org.openjdk.jmc.rjmx.triggerEvaluators"; //$NON-NLS-1$
	private static final String XML_TRIGGER_EVALUATOR = "triggerEvaluator"; //$NON-NLS-1$

	private final NotificationRegistry notificationRegistry;
	private ExtensionLoader<TriggerComponent> m_constraints;
	private ExtensionLoader<TriggerComponent> m_actions;
	private ExtensionLoader<IValueEvaluator> m_evaluators;

	/**
	 * Create a trigger factory using the given notification registry. (Passed here to avoid
	 * dependency on external singleton lookup, which loosens initialization order and improve
	 * testability.)
	 *
	 * @param notificationRegistry
	 *            where actions/constraints are registered
	 */
	public TriggerFactory(NotificationRegistry notificationRegistry) {
		this.notificationRegistry = notificationRegistry;
	}

	public void initializeFactory() {
		getActionExtensions();
		getConstraintExtensions();
		getEvaluatorExtensions();
	}

	@Override
	public ITriggerConstraint createConstraint(String className) throws Exception {
		return (ITriggerConstraint) createObject(getConstraintExtensions().getConfigElement(className));
	}

	@Override
	public ITriggerAction createAction(String className) throws Exception {
		return (ITriggerAction) createObject(getActionExtensions().getConfigElement(className));
	}

	@Override
	public IValueEvaluator createEvaluator(String className) throws Exception {
		return (IValueEvaluator) createObject(getEvaluatorExtensions().getConfigElement(className));
	}

	protected Object createObject(IConfigurationElement element) {
		try {
			return element.createExecutableExtension(XML_CLASS_ATTRIBUTE);
		} catch (CoreException e) {
			// FIXME: We must have error handling here
			e.printStackTrace();
		}
		return null;
	}

	public synchronized ExtensionLoader<TriggerComponent> getActionExtensions() {
		if (m_actions == null) {
			m_actions = new ExtensionLoader<>(XML_TRIGGER_ACTIONS, XML_TRIGGER_ACTION);
			for (TriggerComponent prototype : m_actions.getPrototypes()) {
				RegistryEntry entry = createRegistryEntry(prototype);
				notificationRegistry.registerAction(entry);
			}
		}
		return m_actions;
	}

	public synchronized ExtensionLoader<TriggerComponent> getConstraintExtensions() {
		if (m_constraints == null) {
			m_constraints = new ExtensionLoader<>(XML_TRIGGER_CONSTRAINTS, XML_TRIGGER_CONSTRAINT);
			for (TriggerComponent prototype : m_constraints.getPrototypes()) {
				RegistryEntry entry = createRegistryEntry(prototype);
				notificationRegistry.registerConstraint(entry);
			}
		}
		return m_constraints;
	}

	public synchronized ExtensionLoader<IValueEvaluator> getEvaluatorExtensions() {
		if (m_evaluators == null) {
			m_evaluators = new ExtensionLoader<>(XML_TRIGGER_EVALUATORS, XML_TRIGGER_EVALUATOR);
		}
		return m_evaluators;
	}

	private RegistryEntry createRegistryEntry(TriggerComponent extension) {
		String description = extension.getDescription();
		String listName = extension.getName();
		Class<? extends TriggerComponent> registeredClass = extension.getClass();
		Class<? extends TriggerComponent> editorClass = extension.getClass();

		return new RegistryEntry(registeredClass, listName, description, editorClass);
	}

	@Override
	public NotificationTrigger createTrigger() {
		return new TriggerCondition();
	}
}
