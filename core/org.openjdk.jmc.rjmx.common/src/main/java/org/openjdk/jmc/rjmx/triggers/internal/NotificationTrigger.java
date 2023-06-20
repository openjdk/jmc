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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Element;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.internal.ExtendedMRIMetadataToolkit;
import org.openjdk.jmc.rjmx.triggers.IExceptionHandler;
import org.openjdk.jmc.rjmx.triggers.ITrigger;
import org.openjdk.jmc.rjmx.triggers.IValueEvaluator;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.extension.internal.ExtensionLoader;

/**
 * This is the combination of a notification type and the actual evaluator instance (with
 * instantiated thresholds) that triggers a rule.
 */
// FIXME: This class would definitely benefit from a cleanup/refactoring
public class NotificationTrigger implements ITrigger {
	/** Bitmask for trigger. */
	public static final int TRIGGER_ON_RULE_TRIGGERED = 1;

	/** Bitmask for recover. */
	public static final int TRIGGER_ON_RULE_RECOVERED = 2;

	/** Bitmask for trigger or recover. */
	public static final int TRIGGER_ON_RULE_TRIGGERED_AND_RECOVERED = TRIGGER_ON_RULE_TRIGGERED
			| TRIGGER_ON_RULE_RECOVERED;

	/** Trigger state - newly initialized */
	private static final int STATE_START = 0;
	/** Trigger is high - condition has been met and it has triggered. */
	private static final int STATE_TRIGGER_HIGH = 1;
	/**
	 * Trigger is low - condition for recovery has been met and it has triggered.
	 */
	private static final int STATE_TRIGGER_LOW = 2;
	/**
	 * Trigger is high - condition has switched to true and it has not yet triggered.
	 */
	private static final int STATE_WAITING_SUSTAIN_HIGH = 3;
	/**
	 * Trigger is low - condition has switched to false and it has not yet triggered.
	 */
	private static final int STATE_WAITING_SUSTAIN_LOW = 4;

	private static final int TRIGGER_ERROR_HANDLING_LIMIT_TIME_MS = 60000;

	// XML elements
	private static final String XML_ELEMENT_LIMIT_PERIOD = "trigger_limit_period"; //$NON-NLS-1$
	private static final String XML_ELEMENT_SUSTAIN_TIME = "trigger_sustain_time"; //$NON-NLS-1$
	private static final String XML_ELEMENT_ON_WHAT = "trigger_on_what"; //$NON-NLS-1$
	private static final String XML_VALUE_EVALUATOR_COMPONENT_TAG = "value_evaluator"; //$NON-NLS-1$
	private static final String XML_VALUE_EVALUATOR_TYPE_ATTRIBUTE_NAME = "type"; //$NON-NLS-1$
	public static final String XML_ELEMENT_ATTRIBUTE_NAME = "notification_attribute_name"; //$NON-NLS-1$

	private volatile MRI m_notificationAttributeDescriptor;
	private volatile IValueEvaluator m_valueEvaluator;
	private volatile int m_limitTime;
	private volatile int m_sustainTime;
	private volatile int m_triggerOn = TRIGGER_ON_RULE_TRIGGERED_AND_RECOVERED;

	/*
	 * Must NOT be static since triggers are only shared by the same rule on different and making
	 * this static will share their state on different rules.
	 */
	private final HashMap<String, StateStore> uidToStateStoreMap = new HashMap<>();

	/*
	 * Exception handlers that takes care of the exceptions that can occur when invoking an action.
	 */
	private static final List<IExceptionHandler> EXCEPTION_HANDLERS = new LinkedList<>();

	/**
	 * This class and the uidToStateStoreMap is here to work around a bug where each rule instance
	 * is accessed by several connections. This class associates the state of each trigger with a
	 * connection uid.
	 */
	static class StateStore {
		int m_lastTriggeredState = STATE_START;
		// Flag to keep track on what kind of event was triggered last.
		int m_triggerState = STATE_START;

		// Trigger flags
		Long m_lastSwitchEventTimestamp;
		Long m_lastTriggerEventTimestamp;
		Long m_lastTriggerErrorTimestamp;
	}

	/**
	 * Constructor.
	 * <p>
	 * The empty constructor is the interesting one since this class may very well be instantiated
	 * by reflection.
	 */
	public NotificationTrigger() {
		// Instantiated through reflection.
	}

	/**
	 * Constructor.
	 *
	 * @param descriptor
	 *            the attribute of the trigger.
	 * @param evaluator
	 *            the evaluator to use.
	 */
	public NotificationTrigger(MRI descriptor, IValueEvaluator evaluator) {
		setAttributeDescriptor(descriptor);
		setValueEvaluator(evaluator);
	}

	/**
	 * Gets the notificationType.
	 *
	 * @return Returns a NotificationType
	 */
	@Override
	public MRI getAttributeDescriptor() {
		return m_notificationAttributeDescriptor;
	}

	/**
	 * Sets the notification attribute.
	 *
	 * @param notificationAttributeDescriptor
	 *            The notification attribute to set
	 */
	public void setAttributeDescriptor(MRI notificationAttributeDescriptor) {
		m_notificationAttributeDescriptor = notificationAttributeDescriptor;
	}

	/**
	 * Gets the valueEvaluator.
	 *
	 * @return Returns a ValueEvaluator
	 */
	@Override
	public IValueEvaluator getValueEvaluator() {
		return m_valueEvaluator;
	}

	/**
	 * Sets the valueEvaluator.
	 *
	 * @param valueEvaluator
	 *            The valueEvaluator to set
	 */
	public void setValueEvaluator(IValueEvaluator valueEvaluator) {
		m_valueEvaluator = valueEvaluator;
	}

	private StateStore getStateStoreForUID(String uid) {
		StateStore store = uidToStateStoreMap.get(uid);
		if (store == null) {
			store = new StateStore();
			uidToStateStoreMap.put(uid, store);
		}
		return store;
	}

	/**
	 * Delegates the call to the internal ValueEvaluator and triggers if the state changes from the
	 * current state (depending on the trigger settings).
	 *
	 * @param connectionHandle
	 *            the connection handle
	 * @param rule
	 *            the rule that we're checking
	 * @param aspectEvent
	 *            event with value that we may, or may not, trigger on
	 * @see IValueEvaluator#triggerOn(Object)
	 */
	@Override
	public void triggerOn(IConnectionHandle connectionHandle, TriggerRule rule, MRIValueEvent aspectEvent) {
		if (aspectEvent.getValue() == null) {
			// if we have no value we shouldn't trigger.
			return;
		}

		Object eventValue = aspectEvent.getValue();
		StateStore stateStore = getStateStoreForUID(connectionHandle.getServerDescriptor().getGUID());

		if (eventValue instanceof Number) {
			MRI mri = aspectEvent.getMRI();
			IUnit unit = ExtendedMRIMetadataToolkit.getUnit(connectionHandle, mri);
			if (unit != null) {
				eventValue = unit.quantity((Number) eventValue);
			}
		}

		boolean triggered;
		try {
			triggered = getValueEvaluator().triggerOn(eventValue);
		} catch (Exception e) {
			if (stateStore.m_lastTriggerErrorTimestamp == null || (aspectEvent.getTimestamp()
					- stateStore.m_lastTriggerErrorTimestamp >= TRIGGER_ERROR_HANDLING_LIMIT_TIME_MS)) {
				stateStore.m_lastTriggerErrorTimestamp = aspectEvent.getTimestamp();
				handleException(connectionHandle, rule, e);
			}
			return;
		}

		if (stateStore.m_triggerState == STATE_START || getSustainTimeMillis() == 0) {
			if (triggered) {
				stateStore.m_triggerState = STATE_WAITING_SUSTAIN_HIGH;
			} else {
				stateStore.m_triggerState = STATE_WAITING_SUSTAIN_LOW;
			}
			stateStore.m_lastSwitchEventTimestamp = aspectEvent.getTimestamp();
		}

		switch (stateStore.m_triggerState) {
		case STATE_WAITING_SUSTAIN_HIGH:
			if (triggered) {
				if ((stateStore.m_lastTriggeredState == STATE_TRIGGER_LOW
						|| stateStore.m_lastTriggeredState == STATE_START)
						&& aspectEvent.getTimestamp()
								- stateStore.m_lastSwitchEventTimestamp.longValue() >= getSustainTimeMillis()) {
					if (stateStore.m_lastTriggerEventTimestamp == null || (aspectEvent.getTimestamp()
							- stateStore.m_lastTriggerEventTimestamp.longValue() >= getLimitTimeMillis())) {
						doTrigger(STATE_TRIGGER_HIGH, connectionHandle, rule, aspectEvent,
								(getTriggerOn() & TRIGGER_ON_RULE_TRIGGERED) > 0);
					}
				}
			} else {
				stateStore.m_lastSwitchEventTimestamp = aspectEvent.getTimestamp();
				stateStore.m_triggerState = STATE_WAITING_SUSTAIN_LOW;
			}
			break;
		case STATE_TRIGGER_HIGH:
			if (!triggered) {
				stateStore.m_lastSwitchEventTimestamp = aspectEvent.getTimestamp();
				stateStore.m_triggerState = STATE_WAITING_SUSTAIN_LOW;
			}
			break;
		case STATE_TRIGGER_LOW:
			if (triggered) {
				stateStore.m_lastSwitchEventTimestamp = aspectEvent.getTimestamp();
				stateStore.m_triggerState = STATE_WAITING_SUSTAIN_HIGH;
			}
			break;
		case STATE_WAITING_SUSTAIN_LOW:
			if (!triggered) {
				if (stateStore.m_lastTriggeredState == STATE_TRIGGER_HIGH && (aspectEvent.getTimestamp()
						- stateStore.m_lastSwitchEventTimestamp.longValue() >= getSustainTimeMillis())) {
					if (stateStore.m_lastTriggerEventTimestamp == null || (aspectEvent.getTimestamp()
							- stateStore.m_lastTriggerEventTimestamp.longValue() >= getLimitTimeMillis())) {
						doTrigger(STATE_TRIGGER_LOW, connectionHandle, rule, aspectEvent,
								(getTriggerOn() & TRIGGER_ON_RULE_RECOVERED) > 0);
					}
				}
			} else {
				stateStore.m_lastSwitchEventTimestamp = aspectEvent.getTimestamp();
				stateStore.m_triggerState = STATE_WAITING_SUSTAIN_HIGH;
			}
			break;
		default:
			throw new IllegalArgumentException("Trigger entered illegal state!"); //$NON-NLS-1$
		}
	}

	/**
	 * Note: This method will only trigger an action if the constraints checks out.
	 *
	 * @param triggState
	 * @param connectionHandle
	 * @param rule
	 * @param aspectEvent
	 */
	private void doTrigger(
		int triggState, IConnectionHandle connectionHandle, TriggerRule rule, MRIValueEvent aspectEvent,
		boolean notificationEnabled) {
		StateStore stateStore = getStateStoreForUID(connectionHandle.getServerDescriptor().getGUID());
		TriggerEvent event = new TriggerEvent(connectionHandle, rule, aspectEvent.getValue(),
				triggState == STATE_TRIGGER_HIGH,
				(int) (aspectEvent.getTimestamp() - stateStore.m_lastSwitchEventTimestamp.longValue()));
		if (!checkConstraints(rule, event)) {
			return;
		}
		stateStore.m_lastTriggeredState = triggState;
		stateStore.m_triggerState = triggState;
		if (notificationEnabled) {
			stateStore.m_lastTriggerEventTimestamp = aspectEvent.getTimestamp();
			// FIXME: The invocation of the actions should be added to a queue and dispatched in a separate thread
			try {
				rule.getAction().handleNotificationEvent(event);
			} catch (Throwable e) {
				handleException(connectionHandle, rule, e);
			}
		}
	}

	private void handleException(IConnectionHandle connectionHandle, TriggerRule rule, Throwable e) {
		IExceptionHandler[] handlers = getExceptionHandlers();
		for (IExceptionHandler handler : handlers) {
			handler.handleException(connectionHandle, rule, e);
		}
	}

	/**
	 * These are currently only initialized when needed, and only once. A better idea would probably
	 * be to initialize them in the service instance. This works for now.
	 */
	private synchronized IExceptionHandler[] getExceptionHandlers() {
		if (EXCEPTION_HANDLERS.size() == 0) {
			initializeExceptionHandlers();
		}
		return EXCEPTION_HANDLERS.toArray(new IExceptionHandler[EXCEPTION_HANDLERS.size()]);
	}

	private void initializeExceptionHandlers() {
		ExtensionLoader<IExceptionHandler> loader = new ExtensionLoader<>(
				"org.openjdk.jmc.rjmx.triggerActionExceptionHandlers", "exceptionHandler"); //$NON-NLS-1$ //$NON-NLS-2$
		EXCEPTION_HANDLERS.addAll(loader.getPrototypes());
	}

	/**
	 * Method checkConstraints.
	 *
	 * @param e
	 *            The event to check.
	 * @return boolean Returns true if all constraints are fulfilled.
	 */
	private static boolean checkConstraints(TriggerRule rule, TriggerEvent e) {
		if (!rule.hasConstraints()) {
			return true;
		}
		return rule.getConstraintHolder().isValid(e);
	}

	/**
	 * Gets the waitTime in seconds
	 *
	 * @return Returns a int
	 */
	public int getLimitTime() {
		return m_limitTime;
	}

	/**
	 * Gets the waitTime in milliseconds.
	 *
	 * @return Returns a int
	 */
	public int getLimitTimeMillis() {
		return m_limitTime * 1000;
	}

	/**
	 * Sets the waitTime. This regulates the maximum frequency at which the rule will fire. It will
	 * only fire the rule if at least this amount of seconds have passed. Default is zero, which
	 * means the trigger will always fire.
	 *
	 * @param waitTime
	 *            The time to wait before starting to fire the rule again.
	 */
	public void setLimitTime(int waitTime) {
		m_limitTime = waitTime;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NotificationTrigger type = " + getAttributeDescriptor().toString() + "  eval = " //$NON-NLS-1$ //$NON-NLS-2$
				+ getValueEvaluator().toString();
	}

	/**
	 * Gets the triggerOn. This value is represented by TRIGGER_ON_X and determines on which trigger
	 * flank to trigger. (Either on trigger, recovery or both).
	 *
	 * @return an int according to the explanation above.
	 */
	public int getTriggerOn() {
		return m_triggerOn;
	}

	/**
	 * Sets the triggerOn. Set this to adjust on which trigger flank to trigger. (Either on trigger,
	 * recovery or both).
	 *
	 * @param triggerOn
	 *            The triggerOn to set
	 */
	public void setTriggerOn(int triggerOn) {
		if (triggerOn < 0 || triggerOn > TRIGGER_ON_RULE_TRIGGERED_AND_RECOVERED) {
			throw new IllegalArgumentException(triggerOn + " is not a valid trigger flank!"); //$NON-NLS-1$
		}
		m_triggerOn = triggerOn;
	}

	public void initializeFromXml(Element node, INotificationFactory factory) {
		IValueEvaluator evaluator = null;

		int triggerOn = Integer.parseInt(XmlToolkit.getSetting(node, XML_ELEMENT_ON_WHAT,
				String.valueOf(TRIGGER_ON_RULE_TRIGGERED_AND_RECOVERED)));
		int waitTime = Integer.parseInt(XmlToolkit.getSetting(node, XML_ELEMENT_LIMIT_PERIOD, "0")); //$NON-NLS-1$

		int sustainTime = Integer.parseInt(XmlToolkit.getSetting(node, XML_ELEMENT_SUSTAIN_TIME, "0")); //$NON-NLS-1$

		String typeName = XmlToolkit.getSetting(node, XML_ELEMENT_ATTRIBUTE_NAME, null);
		assert (typeName != null);
		MRI descriptor = MRI.createFromQualifiedName(typeName);

		Element evalNode = XmlToolkit.getOrCreateElement(node, XML_VALUE_EVALUATOR_COMPONENT_TAG);
		String evaluatorClass = evalNode.getAttribute(XML_VALUE_EVALUATOR_TYPE_ATTRIBUTE_NAME);

		// Support old style XML for people upgrading
		if ("".equals(evaluatorClass)) { //$NON-NLS-1$
			evaluatorClass = XmlToolkit.getSetting(node, "value_evaluator_class", null); //$NON-NLS-1$
			if (evaluatorClass == null) {
				RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
						"No element specifying the value evaluator class!"); //$NON-NLS-1$
				return;
			}
		}
		assert (evaluatorClass != null);
		try {
			evaluator = factory.createEvaluator(evaluatorClass);
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Error instantiating value evaluator", e); //$NON-NLS-1$
			return;
		}
		evaluator.initializeEvaluatorFromXml(evalNode);

		setAttributeDescriptor(descriptor);
		setTriggerOn(triggerOn);
		setValueEvaluator(evaluator);
		setSustainTime(sustainTime);
		setLimitTime(waitTime);
	}

	public void exportToXml(Element triggerNode) {
		XmlToolkit.setSetting(triggerNode, XML_ELEMENT_LIMIT_PERIOD, Integer.toString(getLimitTime()));
		XmlToolkit.setSetting(triggerNode, XML_ELEMENT_SUSTAIN_TIME, Integer.toString(getSustainTime()));
		XmlToolkit.setSetting(triggerNode, XML_ELEMENT_ON_WHAT, Integer.toString(getTriggerOn()));
		XmlToolkit.setSetting(triggerNode, XML_ELEMENT_ATTRIBUTE_NAME, getAttributeDescriptor().getQualifiedName());
		Element evalNode = XmlToolkit.createElement(triggerNode, XML_VALUE_EVALUATOR_COMPONENT_TAG);
		evalNode.setAttribute(XML_VALUE_EVALUATOR_TYPE_ATTRIBUTE_NAME, getValueEvaluator().getClass().getName());
		getValueEvaluator().exportEvaluatorToXml(evalNode);
	}

	/**
	 * @return the sustain time in seconds.
	 */
	@Override
	public int getSustainTime() {
		return m_sustainTime;
	}

	/**
	 * Returns the sustain time, i.e. the time a signal has to remain high/low before we trigger in
	 * milliseconds.
	 *
	 * @return the sustain time, i.e. the time a signal has to remain high/low before we trigger.
	 */
	public int getSustainTimeMillis() {
		return m_sustainTime * 1000;
	}

	/**
	 * Sets the sustain time, in seconds.
	 *
	 * @param sustainTime
	 *            the time a signal has to remain high/low before triggering.
	 */
	public void setSustainTime(int sustainTime) {
		m_sustainTime = sustainTime;
	}

	/**
	 * Clears the state information for this trigger
	 *
	 * @param uid
	 */
	public void clearState(String uid) {
		uidToStateStoreMap.remove(uid);
	}

}
