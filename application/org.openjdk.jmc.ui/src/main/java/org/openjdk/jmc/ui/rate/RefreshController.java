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
package org.openjdk.jmc.ui.rate;

import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.openjdk.jmc.ui.dial.DialViewer;
import org.openjdk.jmc.ui.misc.IRefreshable;

/**
 * Class that refreshes object a at a regular interval in the user interface thread.
 */
public abstract class RefreshController {
	final private static int UPDATE_INTERVAL = 50;

	private static WeakHashMap<Display, Master> MASTERS = new WeakHashMap<>();

	protected final Vector<IRefreshable> m_refreshables = new Vector<>();

	/**
	 * Create a new refresh group that can collectively be {@link #start() started} and
	 * {@link #stop() stopped}. All refreshes will be performed in the event thread of the given
	 * {@link Display display}.
	 *
	 * @param parent
	 * @return
	 */
	public static RefreshController createGroup(Display display) {
		/*
		 * To fully support multiple displays seems to be the simplest way to silence SpotBugs while
		 * also ensuring correctness.
		 */
		if (display.getThread() != Thread.currentThread()) {
			SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);
		}

		// Keep at most one master per display.
		Master master;
		synchronized (RefreshController.class) {
			master = MASTERS.get(display);
			if (master == null) {
				master = new Master(display);
				MASTERS.put(display, master);
			}
		}
		return new Group(master);
	}

	/**
	 * Create a new refresh group that can collectively be {@link #start() started} and
	 * {@link #stop() stopped}, but also stops whenever the given {@link RefreshController parent}
	 * stops.
	 *
	 * @param parent
	 * @return
	 */
	public static RefreshController createGroup(RefreshController parent) {
		return new Group(parent);
	}

	private static class Master extends RefreshController {
		final private Display m_display;
		final private AtomicBoolean m_keepAlive = new AtomicBoolean();

		/**
		 * Constructs a {@link RefreshController.Master}
		 *
		 * @param display
		 *            the display to update the viewers
		 */
		public Master(Display display) {
			m_display = display;
		}

		@Override
		public void start() {
			if (!m_keepAlive.getAndSet(true)) {
				scheduleUpdate();
			}
		}

		@Override
		public void stop() {
			m_keepAlive.set(false);
		}

		private void scheduleUpdate() {
			m_display.timerExec(UPDATE_INTERVAL, new Runnable() {
				@Override
				public void run() {
					if (m_keepAlive.get()) {
						update();
						scheduleUpdate();
					}
				}
			});
			;
		}

		@Override
		public void add(IRefreshable refeshable) {
			super.add(refeshable);
			start();
		}

		@Override
		public void remove(IRefreshable refeshable) {
			super.remove(refeshable);
			if (m_refreshables.isEmpty()) {
				stop();
			}
		}
	}

	private static class Group extends RefreshController implements IRefreshable {
		private final RefreshController master;
		private boolean enabled;

		public Group(RefreshController master) {
			this.master = master;
		}

		@Override
		public synchronized void stop() {
			if (enabled) {
				master.remove(this);
				enabled = false;
			}
		}

		@Override
		public synchronized void start() {
			if (!enabled) {
				master.add(this);
				enabled = true;
			}
		}

		@Override
		public boolean refresh() {
			update();
			return true;
		}
	}

	/**
	 * Adds a {@link IRefreshable} to the list of objects that should be refreshed.
	 *
	 * @param viewer
	 */
	public void add(IRefreshable refeshable) {
		m_refreshables.add(refeshable);
	}

	/**
	 * Removes a {@link DialViewer} from this list of viewers that should be updated.
	 *
	 * @param viewer
	 */
	public void remove(IRefreshable refeshable) {
		m_refreshables.remove(refeshable);
	}

	/**
	 * Stops updating the {@link DialViewer}s
	 */
	public abstract void stop();

	/**
	 * Starts updating the {@link DialViewer}s
	 */
	public abstract void start();

	/**
	 * Forces an update of the refreshable objects.
	 */
	public void update() {
		IRefreshable[] refreshables;
		synchronized (m_refreshables) {
			refreshables = m_refreshables.toArray(new IRefreshable[m_refreshables.size()]);
		}
		for (IRefreshable refreshable : refreshables) {
			refreshable.refresh();
		}
	}

	/**
	 * Remove all objects scheduled for refresh.
	 */
	public void clear() {
		m_refreshables.clear();
	}
}
