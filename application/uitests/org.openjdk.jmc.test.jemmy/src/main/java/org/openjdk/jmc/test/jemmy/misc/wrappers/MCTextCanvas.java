/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;
import org.openjdk.jmc.ui.misc.ChartTextCanvas;
import org.jemmy.Point;
import org.jemmy.control.Wrap;
import org.jemmy.interfaces.Mouse.MouseButtons;
import org.jemmy.interfaces.Parent;

/**
 * The Jemmy wrapper for the Mission Control Text Canvas.
 */
public class MCTextCanvas extends MCJemmyBase {
	private MCTextCanvas(Wrap<? extends ChartTextCanvas> textCanvasWrap) {
		this.control = textCanvasWrap;
	}

	/**
	 * Returns all visible {@link MCtextCanvas} objects underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start the search for the ChartTextCanvas object
	 * @return a {@link List} of {@link MCtextCanvas} objects
	 */
	@SuppressWarnings("unchecked")
	public static List<MCTextCanvas> getAll(Wrap<? extends Shell> shell) {
		List<Wrap<? extends ChartTextCanvas>> list = getVisible(
				shell.as(Parent.class, ChartTextCanvas.class).lookup(ChartTextCanvas.class));
		List<MCTextCanvas> canvases = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			canvases.add(new MCTextCanvas(list.get(i)));
		}
		return canvases;
	}

	/**
	 * Returns the first visible {@link MCtextCanvas} object underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start the search for the ChartTextCanvas object
	 * @return a {@link MCtextCanvas} object
	 */
	public static MCTextCanvas getFirst(Wrap<? extends Shell> shell) {
		return getAll(shell).get(0);
	}

	/**
	 * Returns the first visible {@link MCTextCanvas} object underneath the Mission Control main
	 * shell
	 *
	 * @return a {@link MCTextCanvas} object
	 */
	public static MCTextCanvas getTextCanvas() {
		return getFirst(getShell());
	}

	/**
	 * Sets a selection listener for the Text Canvas
	 *
	 * @param listener
	 *            the selection listener to be set
	 */
	public void setSelectionListener(Runnable listener) {
		ChartTextCanvas.class.cast(control.getControl()).setSelectionListener(listener);
	}

	/**
	 * Click the middle thread listed in the Text Canvas
	 */
	public void clickTextCanvas() {
		Display.getDefault().syncExec(() -> {
			control.mouse().click(1, getRelativeClickPoint(), MouseButtons.BUTTON1);
		});
	}

	/**
	 * Calculates the click point of the Text Canvas
	 *
	 * @return the Point of the Text Canvas
	 */
	private Point getRelativeClickPoint() {
		Fetcher<Point> fetcher = new Fetcher<Point>() {
			@Override
			public void run() {
				Rectangle clientArea = ChartTextCanvas.class.cast(control.getControl()).getClientArea();
				setOutput(new Point(clientArea.width / 2, clientArea.height / 2));
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

}
