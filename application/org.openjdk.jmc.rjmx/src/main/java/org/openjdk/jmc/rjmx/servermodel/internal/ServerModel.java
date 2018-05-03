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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;

import javax.management.remote.JMXServiceURL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.w3c.dom.Document;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorProvider;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.rjmx.servermodel.IServerModel;

public final class ServerModel extends Observable implements IServerModel {
	private static final String EXTENSIONPOINT_DESCRIPTORPROVIDER = "descriptorProvider"; //$NON-NLS-1$
	private final Map<String, Server> elements = new HashMap<>();

	private final IDescriptorListener descriptorListener = new IDescriptorListener() {

		@Override
		public void onDescriptorRemoved(String descriptorId) {
			Server removedEntry = doRemove(descriptorId);
			if (removedEntry != null) {
				removedEntry.getServerHandle().dispose(false);
				modelChanged(null);
			}
		}

		@Override
		public void onDescriptorDetected(
			IServerDescriptor sd, String path, JMXServiceURL url, IConnectionDescriptor cd, IDescribable provider) {
			cd = cd != null ? cd : new ConnectionDescriptorBuilder().url(url).build();
			insert(new Server(path, url, null, new DiscoveryInfo(provider), sd, cd));
		}

	};

	public ServerModel() {
		setUpDiscoveryListeners();
	}

	@Override
	public synchronized boolean isEmpty() {
		return elements.size() > 0;
	}

	@Override
	public synchronized Server[] elements() {
		Collection<Server> serverCollection = elements.values();
		Server[] servers = new Server[serverCollection.size()];
		Iterator<Server> it = serverCollection.iterator();
		for (int i = 0; i < servers.length; i++) {
			servers[i] = it.next();
		}
		return servers;
	}

	public void remove(IServer ... servers) {
		for (IServer is : servers) {
			Server server = doRemove(is.getServerHandle().getServerDescriptor().getGUID());
			if (server != null) {
				server.getServerHandle().dispose(true);
			}
		}
		modelChanged(null);
	}

	private synchronized Server doRemove(String guid) {
		return elements.remove(guid);
	}

	private synchronized Server doAdd(Server server) {
		return elements.put(server.getServerHandle().getServerDescriptor().getGUID(), server);
	}

	public void insert(Server ... servers) {
		for (Server server : servers) {
			Server oldEntry = doAdd(server);
			if (oldEntry != server) {
				server.setObserver(this::modelChanged);
				if (oldEntry != null) {
					oldEntry.getServerHandle().dispose(true);
				}
			}
		}
		modelChanged(null);
	}

	void modelChanged(Server element) {
		setChanged();
		notifyObservers(element);
	}

	public Document exportServers(Server ... servers) throws Exception {
		if (servers.length == 0) {
			return ModelPersistence.export(false, elements());
		} else {
			return ModelPersistence.export(true, servers);
		}

	}

	public void importServers(Document doc) throws IOException {
		insert(ModelPersistence.loadSettings(doc));
	}

	private void setUpDiscoveryListeners() {
		IExtensionRegistry er = Platform.getExtensionRegistry();

		IConfigurationElement[] configs = er.getConfigurationElementsFor(RJMXPlugin.PLUGIN_ID,
				EXTENSIONPOINT_DESCRIPTORPROVIDER);
		for (IConfigurationElement config : configs) {
			try {
				if (config.getName().equals("provider")) { //$NON-NLS-1$
					// Only one provider per descriptorProvider.
					IDescriptorProvider provider = (IDescriptorProvider) config.createExecutableExtension("class"); //$NON-NLS-1$
					provider.addDescriptorListener(descriptorListener);
				}
			} catch (CoreException e) {
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Failed to start up a IDescriptorProvider!", e); //$NON-NLS-1$
			}
		}
	}

}
