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
package org.openjdk.jmc.browser.attach;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.attach.AttachToolkit;
import org.openjdk.jmc.browser.attach.LocalJVMToolkit.DiscoveryEntry;
import org.openjdk.jmc.rjmx.descriptorprovider.AbstractDescriptorProvider;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;

/**
 * Provides descriptors for the local JVMs. Note that this is instantiated as an executable
 * extension from Eclipse. It only makes sense to have a single one active at any given time, thus a
 * proxy for the singleton is returned as the executable extension.
 */
public class LocalDescriptorProvider extends AbstractDescriptorProvider {
	private static final long LOCAL_REFRESH_INTERVAL = 5000;
	private LocalScanner scanner;
	private Thread scannerThread;
	private static final LocalDescriptorProvider instance = new LocalDescriptorProvider();

	/**
	 * Map<PID, ConnectionDescriptor>
	 */
	private final Map<Integer, DiscoveryEntry> lastDescriptors = new HashMap<>();

	/**
	 * This is where we periodically scan the local machines and report deltas to the listeners.
	 */
	private class LocalScanner implements Runnable {
		boolean isRunning;

		@Override
		public void run() {
			isRunning = true;
			while (isRunning) {
				try {
					scan();
					Thread.sleep(LOCAL_REFRESH_INTERVAL);
				} catch (InterruptedException e) {
					// Don't mind being interrupted.
				}
			}
		}

		/**
		 * Marks this scanner as terminated.
		 */
		public void shutdown() {
			isRunning = false;
		}

		private void scan() {
			HashMap<Integer, DiscoveryEntry> newOnes = new HashMap<>();
			DiscoveryEntry[] props = LocalJVMToolkit.getAttachableJVMs();
			for (DiscoveryEntry prop : props) {
				newOnes.put(prop.getServerDescriptor().getJvmInfo().getPid(), prop);
			}

			synchronized (lastDescriptors) {
				// Remove stale ones...
				for (Iterator<Entry<Integer, DiscoveryEntry>> entryIterator = lastDescriptors.entrySet()
						.iterator(); entryIterator.hasNext();) {
					Entry<Integer, DiscoveryEntry> entry = entryIterator.next();
					if (newOnes.containsKey(entry.getKey())) {
						continue;
					}
					DiscoveryEntry d = entry.getValue();
					entryIterator.remove();
					onDescriptorRemoved(d.getServerDescriptor().getGUID());
				}

				// Add new ones...
				for (Entry<Integer, DiscoveryEntry> entry : newOnes.entrySet()) {
					if (lastDescriptors.containsKey(entry.getKey())) {
						continue;
					}
					DiscoveryEntry d = entry.getValue();
					onDescriptorDetected(d.getServerDescriptor(), null, null, d.getConnectionDescriptor());
				}
				lastDescriptors.clear();
				lastDescriptors.putAll(newOnes);
			}
		}
	}

	/**
	 * Constructor.
	 */
	private LocalDescriptorProvider() {
	}

	@Override
	public String getName() {
		return Messages.LocalDescriptorProvider_PROVIDER_NAME;
	}

	@Override
	public String getDescription() {
		return Messages.LocalDescriptorProvider_PROVIDER_DESCRIPTION;
	}

	/**
	 * Sets up the thread.
	 */
	private void initialize() {
		if (!AttachToolkit.isLocalAttachAvailable()) {
			BrowserAttachPlugin.getPluginLogger().warning(
					"Could not find the classes needed to support attach. Attaching to local JVMs will be disabled! This will happen if you're not running Mission Control with a JDK, because the JDK tools.jar is needed for attach. In JDK 9 the java.management and jdk.attach modules are required. Run with logging set to FINE for more information."); //$NON-NLS-1$
			return;
		}

		scanner = new LocalScanner();
		scannerThread = new Thread(scanner, "Local Descriptor Scanner"); //$NON-NLS-1$
		scannerThread.start();
	}

	@Override
	public void addDescriptorListener(IDescriptorListener l) {
		synchronized (m_descriptorListeners) {
			if (m_descriptorListeners.size() == 0) {
				super.addDescriptorListener(l);
				initialize();
			} else {
				super.addDescriptorListener(l);
			}
		}
	}

	@Override
	public void removeDescriptorListener(IDescriptorListener l) {
		synchronized (m_descriptorListeners) {
			super.removeDescriptorListener(l);
			if (m_descriptorListeners.size() == 0 && scanner != null) {
				scanner.shutdown();
				scannerThread.interrupt();
			}
		}
	}

	/**
	 * Shuts down the scanner thread.
	 */
	public void shutdown() {
		scanner.shutdown();
	}

	/**
	 * @return the LocalDescriptorProvider.
	 */
	public static LocalDescriptorProvider getInstance() {
		return instance;
	}
}
