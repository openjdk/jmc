/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;

public class ChartLaneHeightControls extends Composite {
	private Button incHeightBtn;
	private Button decHeightBtn;
	private ChartCanvas chartCanvas;
	private ChartTextCanvas textCanvas;

	public ChartLaneHeightControls(Composite parent, ChartCanvas chartCanvas, ChartTextCanvas textCanvas) {
		super(parent, SWT.NONE);
		this.setLayout(new GridLayout(2, true));
		this.chartCanvas = chartCanvas;
		this.textCanvas = textCanvas;
		this.setBackground(Palette.PF_BLACK_100.getSWTColor());

		decHeightBtn = new Button(this, SWT.PUSH);
		decHeightBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		decHeightBtn.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_MINUS));
		decHeightBtn.setToolTipText(Messages.ChartLaneHeightControls_LANE_HEIGHT_DECREASE_TOOLTIP);
		decHeightBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				adjustLaneHeight(-1);
			}
		});

		incHeightBtn = new Button(this, SWT.PUSH);
		incHeightBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		incHeightBtn.setToolTipText(Messages.ChartLaneHeightControls_LANE_HEIGHT_INCREASE_TOOLTIP);
		incHeightBtn.setImage(UIPlugin.getDefault().getImage(UIPlugin.ICON_FA_PLUS));
		incHeightBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				adjustLaneHeight(1);
			}
		});
	}

	private void adjustLaneHeight(int amount) {
		chartCanvas.adjustLaneHeight(amount);
		chartCanvas.redrawChart();
		if (textCanvas != null) {
			textCanvas.adjustLaneHeight(amount);
			textCanvas.redrawChartText();
		}
	}

}
