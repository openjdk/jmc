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
package org.openjdk.jmc.rcp.application.scripting;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.openjdk.jmc.rcp.application.ApplicationPlugin;
import org.openjdk.jmc.rcp.application.scripting.model.Line;
import org.openjdk.jmc.rcp.application.scripting.model.OperatingSystem;
import org.openjdk.jmc.rcp.application.scripting.model.Process;
import org.openjdk.jmc.rcp.application.scripting.model.Program;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Updates the markers (e.g. compilation errors) for the view,
 */
final class MarkerPainter implements PaintListener, Observer {
	final static int MARKER_WIDTH = 15;

	private final OperatingSystem m_os;
	private final StyledText m_control;
	private final Canvas m_markerArea;
	private final Image m_markerImage;
	private final Image m_instructionPointerImage;

	MarkerPainter(Canvas markerArea, OperatingSystem os, StyledText control) {
		m_os = os;
		m_control = control;
		m_markerArea = markerArea;
		m_markerArea.addPaintListener(this);
		m_markerImage = ApplicationPlugin.getDefault().getImageRegistry()
				.getDescriptor(ApplicationPlugin.ICON_ERROR_MARKER).createImage();
		m_instructionPointerImage = ApplicationPlugin.getDefault().getImageRegistry()
				.getDescriptor(ApplicationPlugin.ICON_INSTRUCTION_POINTER).createImage();

		hookMarkersListeners();
		hookImageDisposeListener(m_markerImage, m_instructionPointerImage);
	}

	private void hookImageDisposeListener(final Image ... images) {
		m_markerArea.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				for (Image image : images) {
					image.dispose();
				}
			}
		});
	}

	private void hookMarkersListeners() {
		// Update when the user changes the code
		m_os.getProcessInFocus().getProgram().addObserver(this);

		// Update when the current instruction pointer moves.
		m_os.getProcessInFocus().addObserver(this);

		// Update when the user scrolls vertically
		m_control.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				update();
			}
		});

		// Update when the user scrolls vertically
		m_control.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				update(null, null);
			}
		});

		// Update when the user move the caret
		m_control.addCaretListener(new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				// update after caret has changed the visible content
				DisplayToolkit.safeAsyncExec(event.display, new Runnable() {
					@Override
					public void run() {
						update();
					}
				});
			}
		});
	}

	@Override
	public void paintControl(PaintEvent e) {
		Program program = m_os.getProcessInFocus().getProgram();
		synchronized (program) {
			drawProgramCounter(e.gc, m_os.getProcessInFocus());
			drawMarkers(e.gc, m_os.getProcessInFocus());
		}
	}

	private int drawMarkers(GC gc, Process p) {
		int lineNumber = 0;
		for (Line line : p.getProgram().getSourceCode()) {
			if (line.getErrorMessage() != null) {
				draw(gc, lineNumber, m_markerImage);
			}
			lineNumber++;
		}
		return lineNumber;
	}

	private void drawProgramCounter(GC gc, Process p) {
		if (m_os.getProcessInFocus().isRunning()) {
			draw(gc, p.getInstructionPointer(), m_instructionPointerImage);
		}
	}

	private void draw(GC gc, int line, Image image) {
		int startHeight = m_control.getLinePixel(line);
		if (startHeight >= 0) {
			gc.drawImage(image, 0, startHeight + 2);
		}
	}

	void update() {
		m_markerArea.redraw();
		/*
		 * Explicit calls to update() should be avoided unless absolutely necessary. They may have a
		 * negative performance impact and may cause issues on Mac OS X Cocoa (SWT 3.6). If it is
		 * required here, there must be a justifying comment.
		 * 
		 * Possibly justified? But doubtful. Removing for now.
		 */
//		m_markerArea.update();
	}

	@Override
	public void update(Observable o, Object arg) {
		update();
	}
}
