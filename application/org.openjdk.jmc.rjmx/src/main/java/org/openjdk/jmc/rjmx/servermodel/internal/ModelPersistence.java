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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.management.remote.JMXServiceURL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerDescriptor;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.ui.common.security.ICredentials;
import org.openjdk.jmc.ui.common.security.PersistentCredentials;
import org.openjdk.jmc.ui.common.security.SecurityException;
import org.openjdk.jmc.ui.common.security.SecurityManagerFactory;

class ModelPersistence {

	final static String SERVER_CREDENTIALS_FAMILY = "org.openjdk.jmc.rjmx.servermodel.ModelPersistence"; //$NON-NLS-1$
	private final static String XML_COMPONENT_TAG = "server"; //$NON-NLS-1$
	private final static String XML_ELEMENT_SERVER_NAME = "server_name"; //$NON-NLS-1$
	private final static String XML_ELEMENT_SERVER_PATH = "server_path"; //$NON-NLS-1$
	private final static String XML_ELEMENT_JMX_SERVICE_URL = "jmx_service_url"; //$NON-NLS-1$
	private final static String XML_ELEMENT_CREDENTIALS_ID = "credentials_id"; //$NON-NLS-1$
	private final static String XML_ELEMENT_UID = "uid"; //$NON-NLS-1$
	private final static String XML_ROOT_TAG = "server_model"; //$NON-NLS-1$
	private final static Server[] EMPTY = new Server[] {};

	/**
	 * Stores the current settings to persistent storage.
	 *
	 * @throws IOException
	 * @throws SecurityException
	 */
	synchronized static Document export(boolean exportForTransfer, Server ... servers) throws Exception {
		Document doc = XmlToolkit.createNewDocument(XML_ROOT_TAG);
		Element repoRoot = doc.getDocumentElement();
		Set<String> credentialIds = new HashSet<>();
		for (Server s : servers) {
			exportToXml(s, repoRoot, exportForTransfer, credentialIds);
		}
		if (!exportForTransfer) {
			SecurityManagerFactory.getSecurityManager().clearFamily(SERVER_CREDENTIALS_FAMILY, credentialIds);
		}
		return doc;
	}

	private static void exportToXml(Server me, Element parentNode, boolean exportForTransfer, Set<String> credentialIds)
			throws SecurityException {
		if (me != null && me.getDiscoveryInfo() == null) {
			Element connectionNode = XmlToolkit.createElement(parentNode, XML_COMPONENT_TAG);
			IServerDescriptor descriptor = me.getServerHandle().getServerDescriptor();
			if (!exportForTransfer) {
				XmlToolkit.setSetting(connectionNode, XML_ELEMENT_UID, descriptor.getGUID());
			}
			XmlToolkit.setSetting(connectionNode, XML_ELEMENT_JMX_SERVICE_URL, me.getConnectionUrl().toString());
			XmlToolkit.setSetting(connectionNode, XML_ELEMENT_SERVER_NAME, descriptor.getDisplayName());
			XmlToolkit.setSetting(connectionNode, XML_ELEMENT_SERVER_PATH, me.getPath());
			if (!exportForTransfer && me.getCredentials() != null) {
				String storedCredentialsId = me.getCredentials().getExportedId();
				if (storedCredentialsId != null) {
					XmlToolkit.setSetting(connectionNode, XML_ELEMENT_CREDENTIALS_ID, storedCredentialsId);
					credentialIds.add(storedCredentialsId);
				}
			}
		}
	}

	/**
	 * Loads the current settings from persistent storage.
	 */
	synchronized static Server[] loadSettings(Document doc) throws IOException {
		try {
			Element root = doc.getDocumentElement();
			if (root.getTagName().equals(XML_ROOT_TAG)) {
				return loadServersFromXml(root);
			} else {
				throw new IOException();
			}
		} catch (Exception e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Problem initializing from browser settings", e); //$NON-NLS-1$
		}
		return EMPTY;
	}

	private static Server loadServerFromXml(Element node) throws MalformedURLException {
		JMXServiceURL url = new JMXServiceURL(XmlToolkit.getSetting(node, XML_ELEMENT_JMX_SERVICE_URL, null));
		String guid = XmlToolkit.getSetting(node, XML_ELEMENT_UID, null);
		String credentialsId = XmlToolkit.getSetting(node, XML_ELEMENT_CREDENTIALS_ID, null);
		ICredentials c = credentialsId == null ? null : new PersistentCredentials(credentialsId);
		String name = XmlToolkit.getSetting(node, XML_ELEMENT_SERVER_NAME, null);
		String path = XmlToolkit.getSetting(node, XML_ELEMENT_SERVER_PATH, null);
		IServerDescriptor sd = new ServerDescriptor(guid, name, null);
		IConnectionDescriptor cd = new ConnectionDescriptorBuilder().url(url).credentials(c).build();
		return new Server(path, url, c, null, sd, cd);
	}

	private static Server[] loadServersFromXml(Element node) {
		List<Server> servers = new ArrayList<>();
		for (Element e : XmlToolkit.getChildElementsByTag(node, XML_COMPONENT_TAG)) {
			try {
				servers.add(loadServerFromXml(e));
			} catch (MalformedURLException e1) {
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Problem loading server from element " + e, e1); //$NON-NLS-1$
			}
		}
		return servers.toArray(new Server[servers.size()]);
	}
}
