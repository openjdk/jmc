/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2020, Red Hat Inc. All rights reserved.
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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.fetchers.Fetcher;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.jemmy.Point;
import org.jemmy.control.Wrap;
import org.jemmy.input.StringPopupOwner;
import org.jemmy.interfaces.Keyboard.KeyboardButtons;
import org.jemmy.interfaces.Mouse.MouseButtons;
import org.jemmy.interfaces.Parent;
import org.jemmy.resources.StringComparePolicy;

/**
 * The Jemmy wrapper for the Mission Control Chart Canvas.
 */
public class MCChartCanvas extends MCJemmyBase {

	private MCChartCanvas(Wrap<? extends ChartCanvas> ChartCanvasWrap) {
		this.control = ChartCanvasWrap;
	}

	/**
	 * Returns all visible {@link MCChartCanvas} objects underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start the search for the ChartCanvas object
	 * @return a {@link List} of {@link MCChartCanvas} objects
	 */
	@SuppressWarnings("unchecked")
	public static List<MCChartCanvas> getAll(Wrap<? extends Shell> shell) {
		List<Wrap<? extends ChartCanvas>> list = getVisible(
				shell.as(Parent.class, ChartCanvas.class).lookup(ChartCanvas.class));
		List<MCChartCanvas> canvases = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			canvases.add(new MCChartCanvas(list.get(i)));
		}
		return canvases;
	}

	/**
	 * Returns the first visible {@link MCChartCanvas} object underneath the supplied shell
	 *
	 * @param shell
	 *            the shell from where to start the search for the ChartCanvas object
	 * @return a {@link MCChartCanvas} object
	 */
	public static MCChartCanvas getFirst(Wrap<? extends Shell> shell) {
		return getAll(shell).get(0);
	}

	/**
	 * Returns the first visible {@link MCChartCanvas} object underneath the Mission Control main
	 * shell
	 *
	 * @return a {@link MCChartCanvas} object
	 */
	public static MCChartCanvas getChartCanvas() {
		return getFirst(getShell());
	}

	/**
	 * Clicks a specific menu item in the context menu
	 * 
	 * @param menuItemText
	 *            the menu item of interest
	 */
	@SuppressWarnings("unchecked")
	public void clickContextMenuItem(String menuItemText) {
		focusMc();
		StringPopupOwner<Shell> contextMenu = control.as(StringPopupOwner.class);
		contextMenu.setPolicy(StringComparePolicy.SUBSTRING);
		contextMenu.push(getRelativeClickPoint(), new String[] {menuItemText});
	}

	/**
	 * Click the center of the chart in the ChartCanvas
	 */
	public void clickChart() {
		Display.getDefault().syncExec(() -> {
			control.mouse().click(1, getRelativeClickPoint(), MouseButtons.BUTTON1);
		});
	}

	/**
	 * Zoom in the chart with keyboard controls
	 */
	public void keyboardZoomIn() {
		control.keyboard().pushKey(KeyboardButtons.UP);
		waitForIdle();
	}

	/**
	 * Zoom out the chart with keyboard controls
	 */
	public void keyboardZoomOut() {
		control.keyboard().pushKey(KeyboardButtons.DOWN);
		waitForIdle();
	}

	/**
	 * Checks the isEnabled value for a menu item in the context menu
	 *
	 * @param menuItemText
	 *            the menu item of interest
	 * @return the isEnabled value for the menu item of interest
	 */
	public boolean isContextMenuItemEnabled(String menuItemText) {
		return this.isContextMenuItemEnabled(getRelativeClickPoint(), menuItemText);
	}

	/**
	 * Fetches the height of the Chart Canvas
	 *
	 * @return the height of the Chart Canvas
	 */
	public int getHeight() {
		Fetcher<Integer> fetcher = new Fetcher<Integer>() {
			@Override
			public void run() {
				setOutput(control.getControl().getParent().getSize().y);
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}

	/**
	 * Calculates the click point of the Chart Canvas
	 *
	 * @return the Point of the Chart Canvas
	 */
	private Point getRelativeClickPoint() {
		Fetcher<Point> fetcher = new Fetcher<Point>() {
			@Override
			public void run() {
				setOutput(new Point(control.getControl().getParent().getSize().x / 2,
						control.getControl().getParent().getSize().y / 2));
			}
		};
		Display.getDefault().syncExec(fetcher);
		return fetcher.getOutput();
	}
}
