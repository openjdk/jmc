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
package org.openjdk.jmc.rjmx.persistence.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.internal.IDisposableService;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.rjmx.services.IPersistenceService;
import org.openjdk.jmc.rjmx.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.subscription.MRI;

public class PersistenceWriter implements IPersistenceService, IDisposableService, IPreferenceChangeListener {

	private final Map<MRI, AttributeWriter> attributes = new HashMap<>();
	private final String uid;
	private boolean running;
	private final ISubscriptionService service;

	public PersistenceWriter(String uid, ISubscriptionService service) {
		this.service = service;
		this.uid = uid;
		RJMXPlugin.getDefault().getRJMXPreferences().addPreferenceChangeListener(this);
		loadState();
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (event.getKey().equals(PreferencesKeys.PROPERTY_PERSISTENCE_DIRECTORY)
				&& (event.getNewValue() instanceof String)) {
			File uidPersistenceDirectory = calculatePersistenceDirectory((String) event.getNewValue());
			for (AttributeWriter writer : attributes.values()) {
				writer.setPersistenceDir(uidPersistenceDirectory);
			}
		} else if (event.getKey().equals(PreferencesKeys.PROPERTY_PERSISTENCE_LOG_ROTATION_LIMIT_KB)
				&& (event.getNewValue() instanceof String)) {
			try {
				long maxFileSize = calculateMaxFileSize(Long.parseLong((String) event.getNewValue()));
				for (AttributeWriter writer : attributes.values()) {
					writer.setMaxFileSize(maxFileSize);
				}
			} catch (NumberFormatException e) {
				// Ignore invalid value
			}
		}
	}

	@Override
	public synchronized void start() throws IOException {
		for (Entry<MRI, AttributeWriter> e : attributes.entrySet()) {
			if (e.getValue().isEnabled()) {
				e.getValue().start();
				service.addMRIValueListener(e.getKey(), e.getValue());
			}
		}
		running = true;
	}

	@Override
	public synchronized void stop() {
		running = false;
		for (AttributeWriter writer : attributes.values()) {
			service.removeMRIValueListener(writer);
			writer.stop();
		}
	}

	@Override
	public synchronized void add(MRI mri) {
		AttributeWriter writer = getWriter(mri);
		writer.setEnabled(true);
		if (isRunning()) {
			writer.start();
			service.addMRIValueListener(mri, writer);
		}
	}

	@Override
	public synchronized void remove(MRI mri) {
		AttributeWriter e = getWriter(mri);
		e.stop();
		e.setEnabled(false);
		service.removeMRIValueListener(mri, e);
	}

	@Override
	public synchronized MRI[] getAttributes() {
		ArrayList<MRI> active = new ArrayList<>();
		for (Entry<MRI, AttributeWriter> e : attributes.entrySet()) {
			if (e.getValue().isEnabled()) {
				active.add(e.getKey());
			}
		}
		return active.toArray(new MRI[active.size()]);
	}

	@Override
	public synchronized boolean isRunning() {
		return running;
	}

	private AttributeWriter getWriter(MRI mri) {
		AttributeWriter writer = attributes.get(mri);
		if (writer == null) {
			File persistenceDirectory = calculatePersistenceDirectory(RJMXPlugin.getDefault().getRJMXPreferences().get(
					PreferencesKeys.PROPERTY_PERSISTENCE_DIRECTORY, PreferencesKeys.DEFAULT_PERSISTENCE_DIRECTORY));
			long maxFileSize = calculateMaxFileSize(RJMXPlugin.getDefault().getRJMXPreferences().getLong(
					PreferencesKeys.PROPERTY_PERSISTENCE_LOG_ROTATION_LIMIT_KB,
					PreferencesKeys.DEFAULT_PERSISTENCE_LOG_ROTATION_LIMIT_KB));
			writer = new AttributeWriter(mri, persistenceDirectory, maxFileSize);
			attributes.put(mri, writer);
		}
		return writer;
	}

	private void storeState() {
		StringBuilder allAttributes = new StringBuilder();
		for (Entry<MRI, AttributeWriter> e : attributes.entrySet()) {
			if (e.getValue().isEnabled()) {
				if (allAttributes.length() > 0) {
					allAttributes.append("|"); //$NON-NLS-1$
				}
				allAttributes.append(e.getKey().getQualifiedName());
			}
		}
		if (allAttributes.length() > 0) {
			RJMXPlugin.getDefault().getServerPreferences(uid).put(getClass().getName(), allAttributes.toString());
		} else {
			RJMXPlugin.getDefault().getServerPreferences(uid).remove(getClass().getName());
		}

	}

	private void loadState() {
		String state = RJMXPlugin.getDefault().getServerPreferences(uid).get(getClass().getName(), null);
		if (state != null) {
			for (String line : state.split("\\|")) { //$NON-NLS-1$
				add(MRI.createFromQualifiedName(line.trim()));
			}
		} else {
			add(MRI.createFromQualifiedName("attribute://java.lang:type=OperatingSystem/ProcessCpuLoad")); //$NON-NLS-1$
			add(MRI.createFromQualifiedName("attribute://java.lang:type=OperatingSystem/SystemCpuLoad")); //$NON-NLS-1$
			add(MRI.createFromQualifiedName("attribute://java.lang:type=OperatingSystem/UsedPhysicalMemorySize")); //$NON-NLS-1$
			add(MRI.createFromQualifiedName("attribute://java.lang:type=Threading/ThreadCount")); //$NON-NLS-1$
			add(MRI.createFromQualifiedName("attribute://java.lang:type=Memory/HeapMemoryUsage/committed")); //$NON-NLS-1$
			add(MRI.createFromQualifiedName("attribute://java.lang:type=Memory/HeapMemoryUsage/used")); //$NON-NLS-1$
			add(MRI.createFromQualifiedName("attribute://java.lang:type=Memory/NonHeapMemoryUsage/committed")); //$NON-NLS-1$
			add(MRI.createFromQualifiedName("attribute://java.lang:type=Memory/NonHeapMemoryUsage/used")); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized void dispose() {
		RJMXPlugin.getDefault().getRJMXPreferences().removePreferenceChangeListener(this);
		stop();
		storeState();
	}

	private long calculateMaxFileSize(long preferenceValue) {
		return preferenceValue * 1024;
	}

	private File calculatePersistenceDirectory(String preferenceValue) {
		File persistenceDirectory = new File(preferenceValue);
		return new File(persistenceDirectory, StringToolkit.encodeFilename(uid));
	}

}
