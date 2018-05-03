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
package org.openjdk.jmc.ui.misc;

import java.util.concurrent.Executor;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * Toolkit for interacting with the {@link Display} in a safe way.
 */
public class DisplayToolkit {

	private static final Executor IN_DISPLAY_THREAD_EXECUTOR = DisplayToolkit::safeAsyncExec;

	public static abstract class SafeRunnable implements Runnable {
		final protected Runnable m_runnable;

		public SafeRunnable(Runnable runnable) {
			m_runnable = runnable;
		}

		@Override
		public void run() {
			if (isSafe()) {
				m_runnable.run();
			}
		}

		protected abstract boolean isSafe();

		protected abstract Display getDisplay();

		protected void asyncExec() {
			if (isSafe()) {
				getDisplay().asyncExec(this);
			}
		}

		protected void syncExec() {
			if (isSafe()) {
				getDisplay().syncExec(this);
			}
		}

		protected void timerExec(int millis) {
			if (isSafe()) {
				getDisplay().timerExec(millis, this);
			}
		}
	}

	public static class SafeDisplayRunnable extends SafeRunnable {
		final private Display m_display;

		public SafeDisplayRunnable(Runnable runnable, Display display) {
			super(runnable);
			m_display = display;
		}

		public static boolean isSafe(Display display) {
			return display != null && !display.isDisposed();
		}

		@Override
		protected Display getDisplay() {
			return m_display;
		}

		@Override
		protected boolean isSafe() {
			return isSafe(m_display);
		}
	}

	public static class SafeWidgetRunnable extends SafeRunnable {
		final private Widget m_widget;

		public SafeWidgetRunnable(Runnable runnable, Widget widget) {
			super(runnable);
			m_widget = widget;
		}

		@Override
		protected boolean isSafe() {
			if (DisplayToolkit.isSafe(m_widget)) {
				return SafeDisplayRunnable.isSafe(m_widget.getDisplay());
			}
			return false;
		}

		@Override
		protected Display getDisplay() {
			return m_widget.getDisplay();
		}
	}

	/**
	 * Place a shell, e.g a dialog, in the center of another shell, e.g the application window.
	 *
	 * @param parent
	 *            the parent shell
	 * @param shell
	 *            the shell to place
	 */
	public static void placeDialogInCenter(Shell parent, Shell shell) {
		Rectangle parentSize = parent.getBounds();
		Rectangle mySize = shell.getBounds();

		int locationX, locationY;
		locationX = (parentSize.width - mySize.width) / 2 + parentSize.x;
		locationY = (parentSize.height - mySize.height) / 2 + parentSize.y;

		shell.setLocation(new Point(locationX, locationY));
	}

	/**
	 * Dispose the widget if it is not null or already disposed.
	 *
	 * @param widget
	 *            the widget to dispose
	 */
	public static void dispose(Widget widget) {
		if (DisplayToolkit.isSafe(widget)) {
			widget.dispose();
		}
	}

	public static void dispose(Resource resource) {
		if (DisplayToolkit.isSafe(resource)) {
			resource.dispose();
		}
	}

	public static void safeAsyncExec(final Widget widget, Runnable runnable) {
		new SafeWidgetRunnable(runnable, widget).asyncExec();
	}

	public static void safeSyncExec(Widget widget, Runnable runnable) {
		new SafeWidgetRunnable(runnable, widget).syncExec();
	}

	public static void safeAsyncExec(Runnable runnable) {
		new SafeDisplayRunnable(runnable, Display.getDefault()).asyncExec();
	}

	public static void safeSyncExec(Runnable runnable) {
		new SafeDisplayRunnable(runnable, Display.getDefault()).syncExec();
	}

	public static void safeAsyncExec(Display display, Runnable runnable) {
		new SafeDisplayRunnable(runnable, display).asyncExec();
	}

	public static void safeSyncExec(Display display, Runnable runnable) {
		new SafeDisplayRunnable(runnable, display).syncExec();
	}

	public static void safeTimerExec(final Display display, final int milliseconds, final Runnable runnable) {
		// Display.timerExec must run in ui-thread.
		safeSyncExec(display, new Runnable() {
			@Override
			public void run() {
				new SafeDisplayRunnable(runnable, display).timerExec(milliseconds);
			}
		});
	}

	public static boolean isSafe(Widget widget) {
		return widget != null && !widget.isDisposed();
	}

	public static boolean isSafe(Resource resource) {
		return resource != null && !resource.isDisposed();
	}

	public static Executor inDisplayThread() {
		return IN_DISPLAY_THREAD_EXECUTOR;
	}
}
