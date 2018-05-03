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

import java.util.ArrayList;
import java.util.List;

/**
 * Node in the evaluation graph to apply control changes to JFR configuration settings. It uses the
 * transmitter/receiver terminology instead of producer/consumer to lessen the confusion with JFR
 * event producers.
 */
abstract class Node {
	private final List<Node> m_receivers = new ArrayList<>();
	private final List<Node> m_transmitters = new ArrayList<>();

	/**
	 * Add node that will be notified of changes in this node.
	 *
	 * @param receiver
	 *            the node that will be notified
	 */
	public final void addReceiver(Node receiver) {
		m_receivers.add(receiver);
		receiver.addTransmitter(this);
	}

	protected void addTransmitter(Node transmitter) {
		m_transmitters.add(transmitter);
	}

	/**
	 * Notifies all receivers listening to this node.
	 */
	protected void fireChange() {
		onChange();
	}

	/**
	 * Subclasses should subclass if they wants be to notified when a transmitter value has changed.
	 */
	protected void onChange() {
		for (Node node : m_receivers) {
			node.onChange();
		}
	}

	/**
	 * Returns a list of all nodes that produces (transmits) values for this node.
	 */
	protected final List<Node> getTransmitters() {
		return m_transmitters;
	}

	/**
	 * Return the current value of this node
	 *
	 * @return the value
	 */
	abstract Value getValue();
}
