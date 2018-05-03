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
package org.openjdk.jmc.rjmx.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IConnectionListener;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.IServerHandle;

public final class ServerHandle implements IServerHandle {

	private final List<DefaultConnectionHandle> connectionHandles = new ArrayList<>();
	private final RJMXConnection connection;
	private final Runnable observer;
	private Boolean disposedGracefully; // null if not yet disposed
	private final Runnable connectionListener = new Runnable() {

		@Override
		public void run() {
			disconnect();
		}
	};
	private final IConnectionListener connectionHandleListener = new IConnectionListener() {

		@Override
		public void onConnectionChange(IConnectionHandle handle) {
			if (!handle.isConnected() && removeConnectionHandle(handle)) {
				nofifyObserver();
			}
		}
	};

	public ServerHandle(IConnectionDescriptor descriptor) {
		this(new ServerDescriptor(), descriptor, null);
	}

	public ServerHandle(IServerDescriptor server, IConnectionDescriptor descriptor, Runnable observer) {
		this.observer = observer;
		connection = new RJMXConnection(descriptor, server, connectionListener);
	}

	public IConnectionDescriptor getConnectionDescriptor() {
		return connection.getConnectionDescriptor();
	}

	@Override
	public IServerDescriptor getServerDescriptor() {
		return connection.getServerDescriptor();
	}

	public synchronized IConnectionHandle[] getConnectionHandles() {
		IConnectionHandle[] handles = new IConnectionHandle[connectionHandles.size()];
		Iterator<DefaultConnectionHandle> it = connectionHandles.iterator();
		for (int i = 0; i < handles.length; i++) {
			handles[i] = it.next();
		}
		return handles;
	}

	@Override
	public IConnectionHandle connect(String usage) throws ConnectionException {
		IConnectionListener[] listeners = new IConnectionListener[] {connectionHandleListener};
		return doConnect(usage, listeners);
	}

	@Override
	public IConnectionHandle connect(String usage, IConnectionListener listener) throws ConnectionException {
		IConnectionListener[] listeners = new IConnectionListener[] {listener, connectionHandleListener};
		return doConnect(usage, listeners);
	}

	private IConnectionHandle doConnect(String usage, IConnectionListener[] listeners) throws ConnectionException {
		boolean performedConnect;
		DefaultConnectionHandle newConnectionHandle;
		synchronized (this) {
			if (isDisposed()) {
				throw new ConnectionException("Server handle is disposed"); //$NON-NLS-1$
			}
			performedConnect = connection.connect();
			newConnectionHandle = new DefaultConnectionHandle(connection, usage, listeners);
			connectionHandles.add(newConnectionHandle);
		}
		if (performedConnect) {
			nofifyObserver();
		}
		return newConnectionHandle;
	}

	public void dispose(boolean gracefully) {
		synchronized (this) {
			if (!isDisposed()) {
				disposedGracefully = gracefully;
			}
		}
		disconnect();
	}

	@Override
	public void dispose() {
		dispose(true);
	}

	private synchronized boolean isDisposed() {
		return disposedGracefully != null;
	}

	private synchronized boolean removeConnectionHandle(IConnectionHandle handle) {
		connectionHandles.remove(handle);
		if (connectionHandles.size() == 0) {
			connection.close();
			return true;
		}
		return false;
	}

	@Override
	public void close() {
		disconnect();
	}

	private void disconnect() {
		for (IConnectionHandle handle : getConnectionHandles()) {
			IOToolkit.closeSilently(handle);
		}
	}

	@Override
	public String toString() {
		return connection.toString();
	}

	private void nofifyObserver() {
		if (observer != null) {
			observer.run();
		}
	}

	@Override
	public synchronized State getState() {
		if (isDisposed()) {
			return disposedGracefully ? State.DISPOSED : State.FAILED;
		} else {
			return connection.isConnected() ? State.CONNECTED : State.DISCONNECTED;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		disconnect();
		super.finalize();
	}
}
