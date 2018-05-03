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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.triggers.internal.INotificationFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Holds a list of NotificationConstraints and has a name. Might be used later if we wish to allow
 * the user to save a collection of constraints under a name and reuse those constraints in several
 * rules.
 */
public final class TriggerConstraintHolder {
	private final static String XML_CONSTRAINT_TAG = "constraint"; //$NON-NLS-1$
	private final static String XML_ELEMENT_CONSTRAINT_CLASS = "constraint_class"; //$NON-NLS-1$

	private final List<ITriggerConstraint> m_constraintList = Collections
			.synchronizedList(new ArrayList<ITriggerConstraint>(1));

	TriggerConstraintHolder() {

	}

	/**
	 * Gets the constraintList.
	 *
	 * @return Returns a List of constraints
	 */
	public List<ITriggerConstraint> getConstraintList() {
		return m_constraintList;
	}

	/**
	 * Adds a constraint to the holder.
	 *
	 * @param constraint
	 *            the constraint to add to this collection of constraints.
	 */
	public void addConstraint(ITriggerConstraint constraint) {
		List<ITriggerConstraint> list = getConstraintList();
		if (!list.contains(constraint)) {
			getConstraintList().add(constraint);
		}
	}

	/**
	 * Removes a constraint from the holder.
	 *
	 * @param constraint
	 *            the constraint to remove.
	 */
	public void removeConstraint(ITriggerConstraint constraint) {
		getConstraintList().remove(constraint);
	}

	/**
	 * Returns true if all its NotificationConstraints evaluates to true.
	 *
	 * @see ITriggerConstraint#isValid(TriggerEvent)
	 */
	public boolean isValid(TriggerEvent e) {
		List<ITriggerConstraint> list = getConstraintList();
		synchronized (list) {
			Iterator<ITriggerConstraint> iter = getConstraintList().iterator();
			while (iter.hasNext()) {
				ITriggerConstraint constraint = iter.next();
				if (!(constraint.isValid(e))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		List<ITriggerConstraint> list = getConstraintList();
		synchronized (list) {
			Iterator<ITriggerConstraint> iter = getConstraintList().iterator();
			while (iter.hasNext()) {
				ITriggerConstraint constraint = iter.next();
				buf.append('\t');
				buf.append(constraint.getName());
				buf.append('\n');
			}
		}
		return buf.toString();
	}

	static TriggerConstraintHolder buildFromXml(Element node, INotificationFactory factory) {
		TriggerConstraintHolder holder = new TriggerConstraintHolder();
		NodeList constraints = node.getElementsByTagName(XML_CONSTRAINT_TAG);

		for (int i = 0; i < constraints.getLength(); i++) {
			Element constraintNode = (Element) constraints.item(i);
			String className = XmlToolkit.getSetting(constraintNode, XML_ELEMENT_CONSTRAINT_CLASS, null);
			assert (className != null);
			// without a class name we're in trouble
			ITriggerConstraint constraint = null;
			try {
				constraint = factory.createConstraint(className);
				constraint.initializeFromXml(constraintNode);
//				constraint = (NotificationConstraint) Class.forName(className).newInstance();
			} catch (Exception e) {
				// We need to notify the user of this.
				RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Error instantiating NotificationConstraint.", e); //$NON-NLS-1$
				return holder;
			}
			holder.addConstraint(constraint);
		}
		return holder;
	}

	void exportToXml(Element constraintsNode) {
		for (int i = 0; i < getConstraintList().size(); i++) {
			ITriggerConstraint constraint = getConstraintList().get(i);
			Element constraintElement = XmlToolkit.createElement(constraintsNode, XML_CONSTRAINT_TAG);
			XmlToolkit.setSetting(constraintElement, XML_ELEMENT_CONSTRAINT_CLASS, constraint.getClass().getName());
			constraint.exportToXml(constraintElement);
		}
	}
}
