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
package org.openjdk.jmc.rjmx;

import org.openjdk.jmc.rjmx.internal.ServerHandle;

/**
 * A handle that is used to setup connections to a server.
 * <p>
 * Handles can be closed and disposed. Closing a handle closes all its current existing connections.
 * Disposing a handle also prevents it from opening new connections.
 */
public interface IServerHandle {

	/**
	 * Describes the different connection states a server can handle be in. A handle is
	 * {@link #DISPOSED} if it is invalidated by a graceful dispose action, and {@link #FAILED} if
	 * it is invalidated for some other reason. If the handle is not disposed it is either
	 * {@link #CONNECTED} if there is an active connection to the server, or {@link #DISCONNECTED}
	 * if there is not.
	 */
	enum State {
		DISCONNECTED, CONNECTED, DISPOSED, FAILED;
	}

	/**
	 * @param usage
	 *            A localized string that may be shown to the user, describing why this connection
	 *            was established
	 * @return A handle representing an open connection to the server
	 * @throws ConnectionException
	 *             If the connection failed, for example since the server was no longer reachable,
	 *             or this handle was closed.
	 */
	IConnectionHandle connect(String usage) throws ConnectionException;

	/**
	 * @param usage
	 *            A localized string that may be shown to the user, describing why this connection
	 *            was established
	 * @param listener
	 *            A listener that is notified when the status of the connection changes
	 * @return A handle representing an open connection to the server
	 * @throws ConnectionException
	 *             If the the connection failed, for example since the server was no longer
	 *             reachable, or this handle was closed.
	 */
	IConnectionHandle connect(String usage, IConnectionListener listener) throws ConnectionException;

	/**
	 * Closes all existing connections for this server handle but leaves it in a state which enables
	 * new invokes of connect.
	 */
	void close();

	/**
	 * Disposes this server handle, closes all connection handles and makes it invalid to use for
	 * creating new connections.
	 */
	void dispose();

	/**
	 * @return An object describing the server this instance is a handle for
	 */
	IServerDescriptor getServerDescriptor();

	/**
	 * @return The state of this handle
	 */
	State getState();

	/**
	 * Creates a server handle for a possible connection. This descriptor might or might not be
	 * valid and actually point to a real RJMX server. Used when one wants to connect to an explicit
	 * server.
	 *
	 * @param descriptor
	 *            the descriptor for the handle
	 * @return the server handle
	 */
	static IServerHandle create(IConnectionDescriptor descriptor) {
		return new ServerHandle(descriptor);
	}
}
