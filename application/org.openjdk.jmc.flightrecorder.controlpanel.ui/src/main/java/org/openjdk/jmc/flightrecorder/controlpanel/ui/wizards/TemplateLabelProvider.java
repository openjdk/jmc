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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ImageConstants;

/**
 * LabelPovider for recording templates
 */
final class TemplateLabelProvider extends LabelProvider implements IStyledLabelProvider {
	private boolean displayVersion;

	public TemplateLabelProvider(boolean displayVersion) {
		this.displayVersion = displayVersion;
	}

	@Override
	public String getText(Object element) {
		IEventConfiguration rt = ((IEventConfiguration) element);
		String text = rt.getName();
		String location = rt.getLocationInfo();
		if (location != null) {
			text = text + " - " + location; //$NON-NLS-1$
		}
		if (displayVersion) {
			String version = rt.getVersion().getDescription();
			text = text + " (" + version + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return text;
	}

	// FIXME: Use in combobox in RecordingWizardPage
	@Override
	public StyledString getStyledText(Object element) {
		IEventConfiguration rt = ((IEventConfiguration) element);
		StyledString styled = new StyledString(rt.getName());
		String location = rt.getLocationInfo();
		if (location != null) {
			styled.append(" - " + location, StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
		}
		if (displayVersion) {
			String version = rt.getVersion().getDescription();
			styled.append(" (" + version + ")", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return styled;
	}

	@Override
	public Image getImage(Object element) {
		return ControlPanel.getDefault().getImage(ImageConstants.ICON_FLIGHT_RECORDING_CONFIGURATION_TEMPLATE);
	}
}
