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
package org.openjdk.jmc.ui.polling;

import java.util.Vector;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

// import org.openjdk.jmc.console.ui.ConsolePlugin;
// import org.openjdk.jmc.console.ui.preferences.ConsoleConstants;

/**
 * A Class that keeps polling
 */
public class PollManager implements Runnable, IPropertyChangeListener {
	public interface Pollable {
		/**
		 * @return false if the Pollable no longer wishes to be polled.
		 */
		public boolean poll();
	}

	private final Vector<Pollable> m_pollableObjects = new Vector<>();
	volatile private int m_pollingInterval = 0;
	volatile private boolean m_keepAlive = false;
	private final String m_propertyName;

	public PollManager(int pollingInterval, String propertyName) {
		m_pollingInterval = pollingInterval;
		m_propertyName = propertyName;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(m_propertyName)) {
			m_pollingInterval = ((Integer) event.getNewValue()).intValue();
		}
	}

	/**
	 * Adds a pollable object to the PollManager. This object will be polled constantly with the
	 * interval specicified in the constructor
	 *
	 * @param pollee
	 */
	public synchronized void addPollee(Pollable pollee) {
		if (!m_pollableObjects.contains(pollee)) {
			m_pollableObjects.add(pollee);
			updatePollThread();
			// refresh data if a new pollable object is added
			notifyAll();
		}
	}

	/**
	 * Removes a pollable object
	 *
	 * @param pollee
	 */
	public synchronized void removePollee(Pollable pollee) {
		if (m_pollableObjects.remove(pollee)) {
			updatePollThread();
		}
	}

	/**
	 * Resumes polling after the PollManager has been paused.
	 */
	public synchronized void resume() {
		updatePollThread();
	}

	/**
	 * Pauses the PollManager. The polling can be resumed by calling resume()
	 */
	public synchronized void pause() {
		stopIfNotStopped();
	}

	/**
	 * Removes all pollableObject from the PollManager.
	 * <p>
	 * This method is typically called when you want to clean up the PollManager If you temporarily
	 * would like the polling stop you should call pause.
	 */
	public synchronized void stop() {
		m_pollableObjects.clear();
		updatePollThread();
	}

	private void updatePollThread() {
		if (m_pollableObjects.size() > 0) {
			startIfNotStarted();
		} else {
			stopIfNotStopped();
		}
	}

	private void startIfNotStarted() {
		if (!m_keepAlive) {
			m_keepAlive = true;
			new Thread(this, "Polling Thread").start(); //$NON-NLS-1$
		}
	}

	private void stopIfNotStopped() {
		if (m_keepAlive) {
			m_keepAlive = false;
			notifyAll();
		}
	}

	@Override
	public void run() {
		while (m_keepAlive) {
			try {
				pollAllPollees();
				synchronized (this) {
					this.wait(m_pollingInterval);
				}
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	private void pollAllPollees() {
		Object[] pollees = m_pollableObjects.toArray();
		for (Object pollee : pollees) {
			Pollable pollableObject = (Pollable) pollee;
			if (!pollableObject.poll()) {
				m_pollableObjects.remove(pollableObject);
			}
		}
	}

	public synchronized void poll() {
		notifyAll();
	}

}
