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
package org.openjdk.jmc.console.jconsole;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

import com.sun.tools.jconsole.JConsolePlugin;

/**
 * Looks up the JConsolePlugins.
 */
public class JConsolePluginLoader {
	private static final String EXTENSION_POINT = "org.openjdk.jmc.console.jconsole.jconsolePlugin"; //$NON-NLS-1$
	private static final String EXTENSION_ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

	private static ServiceLoader<JConsolePlugin> pluginService = null;

	/**
	 * @return a list of the plugins found at the specified path.
	 * @throws IOException
	 */
	public static synchronized List<JConsolePlugin> getPlugins(File file) throws IOException {
		initPluginService(file);
		List<JConsolePlugin> plugins = new ArrayList<>();
		if (pluginService != null) {
			for (JConsolePlugin p : pluginService) {
				plugins.add(p);
			}
		}
		return plugins;
	}

	public static List<JConsolePlugin> getExtensionPlugins() {
		List<JConsolePlugin> plugins = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement config : registry.getConfigurationElementsFor(EXTENSION_POINT)) {
			JConsolePlugin plugin;
			try {
				plugin = (JConsolePlugin) config.createExecutableExtension(EXTENSION_ATTRIBUTE_CLASS);
				plugins.add(plugin);
			} catch (CoreException e) {
				Activator.getLogger().log(Level.SEVERE, "Could not load JConsole plugin " + config.getName(), e); //$NON-NLS-1$
			}
		}
		return plugins;
	}

	private static void initPluginService(File file) throws IOException {
		String[] files = file.list();
		if (files != null && files.length > 0) {
			pluginService = ServiceLoader.load(JConsolePlugin.class,
					new URLClassLoader(getURLs(file), Activator.class.getClassLoader()));
		}
	}

	/**
	 * Retrieves the URL's to all the directories plus all the jar's in the provided directory path.
	 *
	 * @param file
	 * @return
	 */
	private static URL[] getURLs(File rootPath) throws IOException {
		if (!rootPath.isDirectory() || !rootPath.exists()) {
			throw new IOException(
					NLS.bind(Messages.getString(Messages.JConsolePluginLoader_MESSAGE_NOT_VALID_PLUGIN_PATH),
							rootPath.toString()));
		}
		File[] files = rootPath.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return true;
				}
				if (pathname.getName().endsWith(".jar")) { //$NON-NLS-1$
					return true;
				}
				return false;
			}
		});

		if (files == null) {
			return new URL[0];
		}

		URL[] urls = new URL[files.length];
		for (int i = 0; i < files.length; i++) {
			urls[i] = new URL("file", "", files[i].getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return urls;
	}

}
