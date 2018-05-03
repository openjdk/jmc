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
package org.openjdk.jmc.ui.accessibility;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * A {@link FocusTracker} ensure that a Composite tracks the focus. That is,
 * <ul>
 * <li>a dashed line is drawn around the Composite when it receives the focus, either by the use of
 * tab key or by click at the composite with the mouse.
 * <li>that the {@link Composite} is traversed when using the keyboard.
 * </ul>
 * The {@link FocusTracker} automatically removes itself when the widget is disposed. No need to
 * worry about resource handling or memory leaks. Usage: <blockquote>
 *
 * <pre>
 * final Composite c = new Composite(parent, SWT.NONE)
 * Button a = new Button(c, SWT.NONE);
 * a.setText(&quot;Enable focus tracking&quot;);
 * a.addSelectionListener( new SelectionAdapter()
 * {
 * 	FocusTracker.enable(c);
 * });
 * Button b = new Button(c, SWT.NONE);
 * b.setText(&quot;Disable focus tracking&quot;);
 * b.addSelectionListener( new SelectionAdapter()
 * {
 * 	FocusTracker.disable(c);
 * });
 * </pre>
 *
 * </blockquote>
 */
public final class FocusTracker {
	private static final String TRACKER_KEY = FocusTracker.class.getName();

	private final Composite m_composite;
	private final KeyListener m_keyListener;
	private final FocusListener m_focusListener;
	private final PaintListener m_paintListener;
	private final DisposeListener m_disposeListener;
	private final SimpleTraverseListener m_traverseListener;

	private static class EmptyKeyListener extends KeyAdapter {
		// at least one key listener is necessary for the focus to work properly
	};

	/**
	 * Create a {@link FocusTracker} for a {@link Composite}
	 *
	 * @param composite
	 *            the {@link Composite} to track
	 */
	private FocusTracker(Composite composite) {
		m_composite = composite;
		m_keyListener = new EmptyKeyListener();

		m_focusListener = new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				scheduleRepaint();
			}

			@Override
			public void focusLost(FocusEvent e) {
				scheduleRepaint();
			}
		};
		m_paintListener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (m_composite.isFocusControl()) {
					drawFocusOn(m_composite, e.gc);
				}
			}
		};
		m_disposeListener = new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				disable();
			}
		};
		m_traverseListener = new SimpleTraverseListener(true);
	}

	/**
	 * Draw focus for the given {@link Composite composite} on the supplied {@link GC graphics
	 * context}. Note that this method does not check that the {@code composite} is in focus, so it
	 * should only be called if {@link Composite#isFocusControl() composite.isFocusControl()}
	 * returns {@code true}.
	 *
	 * @param composite
	 *            the {@link Composite} to draw focus for
	 * @param gc
	 *            a {@link GC} using the native coordinate system of {@code composite}
	 */
	public static void drawFocusOn(Composite composite, GC gc) {
		Rectangle clientArea = composite.getClientArea();
		gc.drawFocus(0, 0, clientArea.width, clientArea.height);
	}

	/**
	 * Enable focus tracking for the {@link Composite}.
	 *
	 * @param composite
	 *            the {@link Composite} to track
	 */
	public static void enableFocusTracking(final Composite composite) {
		if (!FocusTracker.hasFocusTracker(composite)) {
			new FocusTracker(composite).enable();
		}
	}

	/**
	 * Disable focus tracking for the {@link Composite}
	 *
	 * @param composite
	 *            the {@link Composite} to track
	 */
	public static void disableFocusTracking(final Composite composite) {
		if (FocusTracker.hasFocusTracker(composite)) {
			FocusTracker.getFocusTracker(composite).disable();
		}
	}

	/**
	 * Checks if the {@link Composite} has a {@link FocusTracker}. See
	 * {@link #enableFocusTracking(Composite)} and {@link #disableFocusTracking(Composite)}
	 *
	 * @param composite
	 *            the {@link Composite} to check
	 * @return true if the {@link Composite} has a tracker
	 */
	public static boolean hasFocusTracker(Composite composite) {
		return getFocusTracker(composite) != null;
	}

	/**
	 * Return the {@link FocusTracker} for a {@link Composite}, or null if the there is no tracker
	 * for the {@link Composite}
	 *
	 * @param composite
	 *            the {@link Composite} to inspect
	 * @return a Focus
	 */
	public static FocusTracker getFocusTracker(Composite composite) {
		Object object = composite.getData(TRACKER_KEY);
		if (object instanceof FocusTracker) {
			return (FocusTracker) object;
		}
		return null;
	}

	/**
	 * Schedule a repaint job.
	 */
	private void scheduleRepaint() {
		DisplayToolkit.safeAsyncExec(m_composite, new Runnable() {
			@Override
			public void run() {
				m_composite.redraw();
				/*
				 * Explicit calls to update() should be avoided unless absolutely necessary. They
				 * may have a negative performance impact and may cause issues on Mac OS X Cocoa
				 * (SWT 3.6). If it is required here, there must be a justifying comment.
				 */
//				m_composite.update();
			}
		});
	}

	/**
	 * Enable focus tracking for the {@link Composite}
	 */
	private void enable() {
		m_composite.addKeyListener(m_keyListener);
		m_composite.addFocusListener(m_focusListener);
		m_composite.addPaintListener(m_paintListener);
		m_composite.addDisposeListener(m_disposeListener);
		m_composite.addTraverseListener(m_traverseListener);
		m_composite.setData(TRACKER_KEY, this);
	}

	/**
	 * Disable focus tracking for the {@link Composite}
	 */
	private void disable() {
		if (!m_composite.isDisposed()) {
			m_composite.removeKeyListener(m_keyListener);
			m_composite.removeFocusListener(m_focusListener);
			m_composite.removePaintListener(m_paintListener);
			m_composite.removeDisposeListener(m_disposeListener);
			m_composite.removeTraverseListener(m_traverseListener);
			// Object is a dummy, can't set data to null
			m_composite.setData(TRACKER_KEY, FocusTracker.TRACKER_KEY);
		}
	}
}
