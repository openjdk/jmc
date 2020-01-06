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
package org.openjdk.jmc.ui.charts;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.ui.misc.ChartCanvas;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;
import org.openjdk.jmc.ui.misc.TimeFilter;

public class ChartFilterControlBar extends Composite {

	private static final String THREADS_LABEL = "Threads";

	private TimeFilter timeFilter;

	public ChartFilterControlBar(Composite parent, Listener resetListener, IRange<IQuantity> recordingRange) {
		super(parent, SWT.NONE);
		this.setLayout(new GridLayout(3, false));
		this.setBackground(Palette.getThreadsPageBackgroundColor());
		Label nameLabel = new Label(this, SWT.CENTER | SWT.HORIZONTAL);
		nameLabel.setText(THREADS_LABEL);
		nameLabel.setBackground(Palette.getThreadsPageBackgroundColor());
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, true);
		gd.widthHint = 180;
		nameLabel.setLayoutData(gd);
		nameLabel.setFont(JFaceResources.getFontRegistry().get(JFaceResources.BANNER_FONT));

		timeFilter = new TimeFilter(this, recordingRange, resetListener);
		timeFilter.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
	}

	public void setChart(XYChart chart) {
		timeFilter.setChart(chart);
	}

	public void setChartCanvas(ChartCanvas canvas) {
		timeFilter.setChartCanvas(canvas);
	}

	public void setStartTime(IQuantity startTime) {
		timeFilter.setStartTime(startTime);
	}

	public void setEndTime(IQuantity endTime) {
		timeFilter.setEndTime(endTime);
	}
}
