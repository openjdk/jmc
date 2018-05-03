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
package org.openjdk.jmc.jdp.client;

/**
 * A discovery event. These are triggered from the {@link JDPClient} when a {@link Discoverable} is
 * FOUND, LOST or CHANGED.
 */
public final class DiscoveryEvent {
	public enum Kind {
		FOUND, LOST, CHANGED
	}

	private final Kind kind;
	private final Discoverable discoverable;

	/**
	 * Constructor.
	 *
	 * @param kind
	 *            the type of discovery that was made.
	 * @param discoverable
	 *            what was discovered.
	 */
	public DiscoveryEvent(Kind kind, Discoverable discoverable) {
		this.kind = kind;
		this.discoverable = discoverable;
	}

	/**
	 * The kind of discovery. The discoverable was either FOUND, LOST or CHANGED.
	 *
	 * @return kind of discovery. The discoverable was either FOUND, LOST or CHANGED.
	 */
	public Kind getKind() {
		return kind;
	}

	/**
	 * @return the discoverable for which the status was updated.
	 */
	public Discoverable getDiscoverable() {
		return discoverable;
	}

	@Override
	public String toString() {
		return kind + " " + discoverable.getSessionId(); //$NON-NLS-1$
	}
}
