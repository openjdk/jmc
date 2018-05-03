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
package org.openjdk.jmc.rjmx.servermodel.internal;

import java.util.function.Consumer;
import java.util.logging.Level;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderRepository;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.internal.ServerHandle;
import org.openjdk.jmc.rjmx.servermodel.IDiscoveryInfo;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.ui.common.action.IActionProvider;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.ui.common.labelingrules.NameConverter;
import org.openjdk.jmc.ui.common.resource.IImageResource;
import org.openjdk.jmc.ui.common.resource.Resource;
import org.openjdk.jmc.ui.common.security.ICredentials;
import org.openjdk.jmc.ui.common.util.ICopyable;

public class Server implements IServer, ICopyable, IImageResource {
	private final ICredentials credentials;
	private final IDiscoveryInfo discoveryInfo;
	private final JMXServiceURL url;
	private final Resource imageResource;
	private IActionProvider actionProvider;
	private String path;
	private ServerHandle serverHandle;
	private Consumer<? super Server> observer;
	private final Runnable listener = new Runnable() {

		@Override
		public void run() {
			Consumer<? super Server> o = getObserver();
			if (o != null) {
				try {
					o.accept(Server.this);
				} catch (Exception e) {
					RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Notify Server observer " + o + " failed", //$NON-NLS-1$ //$NON-NLS-2$
							e);
				}
			}
		}
	};

	public Server(String path, JMXServiceURL url, ICredentials credentials, IDiscoveryInfo discoveryInfo,
			IServerDescriptor serverDesc, IConnectionDescriptor connector) {
		this.discoveryInfo = discoveryInfo;
		this.credentials = credentials;
		this.path = path;
		this.url = url;
		JVMDescriptor jvmInfo = serverDesc.getJvmInfo();
		imageResource = jvmInfo != null ? NameConverter.getInstance().getImageResource(jvmInfo) : null;
		serverHandle = new ServerHandle(serverDesc, connector, listener);
		actionProvider = ActionProviderRepository.buildActionProvider(serverHandle);
	}

	synchronized void setObserver(Consumer<? super Server> observer) {
		this.observer = observer;
	}

	private synchronized Consumer<? super Server> getObserver() {
		return observer;
	}

	@Override
	public IDiscoveryInfo getDiscoveryInfo() {
		return discoveryInfo;
	}

	public ICredentials getCredentials() {
		return credentials;
	}

	public synchronized void setPath(String path) {
		this.path = path;
	}

	public synchronized String getPath() {
		return path;
	}

	@Override
	public synchronized ServerHandle getServerHandle() {
		return serverHandle;
	}

	@Override
	public synchronized IActionProvider getActionProvider() {
		return actionProvider;
	}

	@Override
	public synchronized IConnectionHandle[] getConnectionHandles() {
		return serverHandle.getConnectionHandles();
	}

	@Override
	public boolean isCopyable() {
		return url != null;
	}

	public JMXServiceURL getConnectionUrl() {
		return url;
	}

	@Override
	public synchronized Object copy() {
		IServerDescriptor originalSd = serverHandle.getServerDescriptor();
		IServerDescriptor desc = new ServerDescriptor(null, originalSd.getDisplayName(), originalSd.getJvmInfo());
		return new Server(path, url, credentials, null, desc, serverHandle.getConnectionDescriptor());
	}

	@Override
	public void reset() {
		ServerHandle oldHandle;
		synchronized (this) {
			oldHandle = serverHandle;
			serverHandle = new ServerHandle(oldHandle.getServerDescriptor(), oldHandle.getConnectionDescriptor(),
					listener);
			actionProvider = ActionProviderRepository.buildActionProvider(serverHandle);
		}
		oldHandle.dispose(true);
	}

	@Override
	public Resource getImageResource() {
		return imageResource;
	}

}
