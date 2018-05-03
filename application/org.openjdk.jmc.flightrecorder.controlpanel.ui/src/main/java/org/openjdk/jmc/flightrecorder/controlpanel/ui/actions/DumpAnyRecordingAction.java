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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.actions;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.FlightRecorderProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ImageConstants;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.RecordingProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.DumpRecordingWizardModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards.DumpRecordingWizardPage;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.ui.wizards.AbstractWizardUserAction;
import org.openjdk.jmc.ui.wizards.OnePageWizard;

/**
 */
public class DumpAnyRecordingAction extends AbstractWizardUserAction {

	private final FlightRecorderProvider flightRecorder;

	public DumpAnyRecordingAction(FlightRecorderProvider flightRecorderProvider) {
		super(Messages.ACTION_DUMP_ANY_RECORDING_LABEL, Messages.ACTION_DUMP_ANY_RECORDING_TOOLTIP,
				ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.ICON_FLIGHT_RECORDING_DUMP));
		flightRecorder = flightRecorderProvider;
	}

	@Override
	public IWizard doCreateWizard() throws Exception {
		IConnectionHandle handle = null;
		try {
			handle = flightRecorder.getServerHandle().connect(Messages.ACTION_DUMP_ANY_RECORDING_LABEL);
			RecordingProvider recording = flightRecorder.getSnapshotRecording(handle);
			if (recording != null) {
				flightRecorder.resetWarning();
				return new OnePageWizard(new DumpRecordingWizardPage(new DumpRecordingWizardModel(recording)));
			} else {
				// FIXME: Can we make different error message depending on version?
				throw new FlightRecorderException(NLS.bind(Messages.DUMP_ANY_RECORDING_ERROR_MSG,
						flightRecorder.getServerHandle().getServerDescriptor().getDisplayName()));
			}
		} catch (Exception e) {
			flightRecorder.setWarning(e.getLocalizedMessage());
			throw e;
		} finally {
			IOToolkit.closeSilently(handle);
		}
	}

}
