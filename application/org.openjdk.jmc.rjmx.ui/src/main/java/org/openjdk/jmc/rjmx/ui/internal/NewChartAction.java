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
package org.openjdk.jmc.rjmx.ui.internal;

import org.eclipse.jface.action.Action;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.MCSectionPart;
import org.openjdk.jmc.ui.preferences.PreferenceConstants;

public class NewChartAction extends Action {

	private final SectionPartManager spm;
	private final IConnectionHandle ch;
	private final String defaultTitle;

	public NewChartAction(SectionPartManager spm, IConnectionHandle ch) {
		this(spm, ch, Messages.NewVisualizerAction_MY_CHART_X_TEXT);
	}

	public NewChartAction(SectionPartManager spm, IConnectionHandle ch, String defaultTitle) {
		super(null, RJMXUIPlugin.getDefault().getMCImageDescriptor(IconConstants.ICON_ADD_OBJECT));

		if (UIPlugin.getDefault().getPreferenceStore()
				.getBoolean(PreferenceConstants.P_ACCESSIBILITY_BUTTONS_AS_TEXT)) {
			super.setText(Messages.NewVisualizerAction_ADD_CHART_TEXT);
		}
		setToolTipText(Messages.NewVisualizerAction_ADD_CHART_TEXT);
		this.spm = spm;
		this.ch = ch;
		this.defaultTitle = defaultTitle;
	}

	@Override
	public void run() {
		String title = defaultTitle;
		if (spm.hasSectionPartTitle(title)) {
			title = spm.createUniqueSectionPartTitle(title);
		}
		CombinedChartSectionPart csp = new CombinedChartSectionPart(spm.getContainer(), spm.getFormToolkit(),
				MCSectionPart.DEFAULT_TWISTIE_STYLE, ch);
		spm.add(csp, true, true);
		csp.getChart().getChartModel().setChartTitle(title);
		csp.getChart().getChartModel().notifyObservers();
	}
}
