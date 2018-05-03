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
import org.openjdk.jmc.rjmx.triggers.extension.internal.TriggerComponent;

/**
 * Base class for a trigger actions.
 * <p>
 * See extension point org.openjdk.jmc.rjmx.triggerActions
 */
public abstract class TriggerAction extends TriggerComponent implements ITriggerAction {
	/**
	 * The method to be invoked when a rule triggers.
	 *
	 * @param e
	 *            the event to take action on.
	 */
	@Override
	public abstract void handleNotificationEvent(TriggerEvent e) throws Throwable;

	/**
	 * Returns a setting specified in the extension point for the trigger action.
	 *
	 * @param identifier
	 *            the identifier to look up the setting for.
	 * @return an {@link ISetting} or null if there is no setting for the given identifier.
	 */
	final public ISetting getSetting(String identifier) {
		return getFieldHolder().getField(identifier);
	}

	@Override
	public boolean supportsAction(IConnectionHandle handle) {
		return true;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void exportToXml(Element parentNode) {
		getFieldHolder().exportToXml(parentNode);
	}

	@Override
	public void initializeFromXml(Element parentNode) {
		getFieldHolder().initializeFromXml(parentNode);
	}
}
