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

import org.w3c.dom.Element;

import org.openjdk.jmc.rjmx.IConnectionHandle;

/**
 * Interface for actions taken when a NotificationRule triggers.
 */
public interface ITriggerAction {
	/**
	 * Returns the name of this object.
	 *
	 * @return the name of this object.
	 */
	String getName();

	/**
	 * Returns the description of this object.
	 *
	 * @return the description of this object.
	 */
	String getDescription();

	/**
	 * The method to be invoked when a rule triggers.
	 *
	 * @param e
	 *            the event to take action on.
	 */
	void handleNotificationEvent(TriggerEvent e) throws Throwable;

	/**
	 * Returns whether this action is supported and can be handled by the connection.
	 *
	 * @param connection
	 *            the connection to test
	 * @return <tt>true</tt> if action is supported, <tt>false</tt> otherwise
	 */
	boolean supportsAction(IConnectionHandle connection);

	/**
	 * Return whether this action is ready to handle events. Implementations may perform checks and
	 * operations within this method.
	 *
	 * @return <tt>true</tt> if action is ready to handle events, <tt>false</tt> otherwise
	 */
	boolean isReady();

	/**
	 * Initializes the instance according to the specified node representing the object.
	 *
	 * @param node
	 *            the XML node representing the object
	 * @throws Exception
	 *             if an exception occured during initialization.
	 */
	void initializeFromXml(Element node) throws Exception;

	/**
	 * Creates and inserts an XML node for this object that becomes a subnode to the specified
	 * parent node.
	 *
	 * @param parentNode
	 *            the XML node to become a subnode to
	 */
	void exportToXml(Element parentNode);
}
