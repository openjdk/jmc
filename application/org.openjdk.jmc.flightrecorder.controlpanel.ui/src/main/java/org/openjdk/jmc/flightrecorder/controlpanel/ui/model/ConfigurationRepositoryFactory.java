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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.model;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.spi.IConfigurationStorageDelegate;
import org.openjdk.jmc.flightrecorder.configuration.spi.IStorageProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;

/**
 * Factory to create a {@link EventConfigurationRepository} with local templates as well as
 * contributed templates.
 */
public class ConfigurationRepositoryFactory {
	private final static String EXTENSION_POINT = "org.openjdk.jmc.flightrecorder.configuration.storageProvider"; //$NON-NLS-1$
	private final static String EXTENSION_ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private final static String LOWER_CASE_FILE_EXTENSION = IEventConfiguration.JFC_FILE_EXTENSION.toLowerCase();
	private final static String LAST_STARTED_CONFIGURATION_FILE_NAME = "last_started_configuration" //$NON-NLS-1$
			+ LOWER_CASE_FILE_EXTENSION;
	static final File CONFIGURATION_STORAGE_DIR = ControlPanel.getDefault().getStateLocation().append(".rectemplates") //$NON-NLS-1$
			.toFile();

	public static EventConfigurationRepository create() {
		EventConfigurationRepository repository = new EventConfigurationRepository();
		initiate(repository);
		return repository;
	}

	protected static void initiate(EventConfigurationRepository repository) {
		addLocalTemplatesTo(repository);
		addContributedTemplatesTo(repository);

		// Force sort and changes to be cleared. No one is actually listening.
		repository.notifyObservers();
	}

	private static void addLocalTemplatesTo(EventConfigurationRepository repository) {
		File localDir = CONFIGURATION_STORAGE_DIR;
		if (!localDir.isDirectory()) {
			return;
		}

		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase(Locale.ENGLISH).endsWith(LOWER_CASE_FILE_EXTENSION);
			}
		};

		File[] files = localDir.listFiles(filter);
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.length() == 0) {
				// FIXME: Leftover from earlier storage delegates. Log and delete?
			} else {
				try {
					IEventConfiguration template;
					if (file.getName().equalsIgnoreCase(LAST_STARTED_CONFIGURATION_FILE_NAME)) {
						XMLModel model = EventConfiguration.createModel(file);
						template = new EventConfiguration(model, VolatileStorageDelegate.getLastStartedDelegate());
					} else {
						PrivateStorageDelegate delegate = new PrivateStorageDelegate(file);
						template = new EventConfiguration(delegate);
					}
					repository.add(template);
				} catch (IOException e) {
					// FIXME: Better exception handling
					e.printStackTrace();
				} catch (ParseException e) {
					// FIXME: Better exception handling
					e.printStackTrace();
				}
			}
		}
	}

	private static void addContributedTemplatesTo(EventConfigurationRepository repository) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_POINT)) {
			IStorageProvider provider = null;
			try {
				provider = (IStorageProvider) element.createExecutableExtension(EXTENSION_ATTRIBUTE_CLASS);
			} catch (CoreException e) {
				ControlPanel.getDefault().getLogger().log(Level.WARNING,
						"Problems when creating IStorageProvider " + element.getName(), e); //$NON-NLS-1$
				continue;
			}
			for (IConfigurationStorageDelegate delegate : provider.getStorageDelegates(LOWER_CASE_FILE_EXTENSION)) {
				try {
					IEventConfiguration template = new EventConfiguration(delegate);
					repository.add(template);
				} catch (IOException | ParseException | IllegalArgumentException e) {
					// FIXME: Can there be some exception cases where we actually want the stack trace?
					ControlPanel.getDefault().getLogger().log(Level.WARNING,
							"Problems when reading reading from ITemplateStorageDelegate " + delegate.getLocationInfo() //$NON-NLS-1$
									+ ": " + e.getMessage()); //$NON-NLS-1$
				}
			}
		}
	}

	public static File getCreatedStorageDir() throws IOException {
		File dir = CONFIGURATION_STORAGE_DIR;
		if (!dir.isDirectory()) {
			// Since the parent directory should exist, we explicitly avoid "mkdirs()".
			if (!dir.mkdir()) {
				throw new IOException("Could not create the directory " + dir.toString()); //$NON-NLS-1$
			}
		}
		return dir;
	}

	public static void saveAsLastStarted(IEventConfiguration configuration) {
		try {
			File file = new File(getCreatedStorageDir(), LAST_STARTED_CONFIGURATION_FILE_NAME);
			configuration.exportToFile(file);
		} catch (IOException e) {
			// Not fatal. We just won't get the last used configuration next time.
			ControlPanel.getDefault().getLogger().log(Level.WARNING, "Cannod save last used configuration.", e); //$NON-NLS-1$
		}
	}
}
