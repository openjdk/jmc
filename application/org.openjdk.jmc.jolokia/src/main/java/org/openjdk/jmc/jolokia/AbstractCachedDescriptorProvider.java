/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Kantega AS. All rights reserved. 
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
package org.openjdk.jmc.jolokia;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.rjmx.descriptorprovider.AbstractDescriptorProvider;
import org.openjdk.jmc.rjmx.descriptorprovider.IDescriptorListener;

/**
 * I cache a list if identified JVMs that can be refreshed in the background by some means of
 * discovering JVMs and notify changes of any changes
 */
@SuppressWarnings("restriction")
public abstract class AbstractCachedDescriptorProvider extends AbstractDescriptorProvider {

	private static final long LOCAL_REFRESH_INTERVAL = 20000;
	private Scanner scanner;
	private Thread scannerThread;
	/**
	 * Map<UUID, IServerDescriptor>
	 */
	private final Map<String, ServerConnectionDescriptor> knownDescriptors = new HashMap<>();

	/**
	 * This is where we periodically scan and report deltas to the listeners.
	 */
	private class Scanner implements Runnable {
		boolean isRunning;

		@Override
		public void run() {
			isRunning = true;
			while (isRunning) {
				try {
					scan();
					Thread.sleep(LOCAL_REFRESH_INTERVAL);
				} catch (InterruptedException ignore) {
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

		protected void scan() {
			Map<String, ServerConnectionDescriptor> newOnes = discoverJvms();

			synchronized (knownDescriptors) {
				// Remove stale ones...
				for (Iterator<Entry<String, ServerConnectionDescriptor>> entryIterator = knownDescriptors.entrySet()
						.iterator(); entryIterator.hasNext();) {
					Entry<String, ServerConnectionDescriptor> entry = entryIterator.next();
					if (newOnes.containsKey(entry.getKey())) {
						continue;
					}
					entryIterator.remove();
					onDescriptorRemoved(entry.getKey());
				}

				// Add new ones...
				for (Entry<String, ServerConnectionDescriptor> entry : newOnes.entrySet()) {
					if (knownDescriptors.containsKey(entry.getKey())) {
						continue;
					}
					onDescriptorDetected(entry.getValue(), entry.getValue().getPath(), entry.getValue().serviceUrl(),
							entry.getValue());
				}
				knownDescriptors.clear();
				knownDescriptors.putAll(newOnes);
			}
		}
	}

	/**
	 * Sets up the thread.
	 */
	private void initialize() {

		scanner = new Scanner();
		scannerThread = new Thread(scanner, getName()); // $NON-NLS-1$
		scannerThread.start();
	}

	protected abstract boolean isEnabled();

	protected abstract Map<String, ServerConnectionDescriptor> discoverJvms();

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

}
