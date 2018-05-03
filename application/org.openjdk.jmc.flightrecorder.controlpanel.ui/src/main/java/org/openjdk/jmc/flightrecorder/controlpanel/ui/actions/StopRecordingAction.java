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

import java.util.logging.Level;

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ImageConstants;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.RecordingProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.ui.misc.AbstractWarningAction;
import org.openjdk.jmc.ui.misc.DialogToolkit;

/**
 * Stop recording.
 */
public class StopRecordingAction extends AbstractWarningAction {

	private final RecordingProvider recording;

	public StopRecordingAction(RecordingProvider recording) {
		super(Messages.ACTION_STOP_RECORDING_LABEL, Messages.ACTION_STOP_RECORDING_TOOLTIP,
				ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.ICON_FLIGHT_RECORDING_STOP));
		this.recording = recording;
	}

	private boolean okToStopDefaultRecording() {
		return DialogToolkit.openQuestionOnUiThread(Messages.STOP_RECORDING_TITLE,
				NLS.bind(Messages.STOP_RECORDING_MSG, recording.getRecordingDescriptor().getName()));
	}

	@Override
	public void doExecute() {
		IRecordingDescriptor recordingDescriptor = recording.getRecordingDescriptor();
		if (recordingDescriptor.getId() != 0 || okToStopDefaultRecording()) {
			IConnectionHandle connection = null;
			try {
				connection = recording.getServerHandle().connect(Messages.ACTION_STOP_RECORDING_TOOLTIP);
				connection.getServiceOrThrow(IFlightRecorderService.class).stop(recordingDescriptor);
			} catch (Exception e) {
				ControlPanel.getDefault().getLogger().log(Level.SEVERE, "Could not stop recording."); //$NON-NLS-1$
			} finally {
				IOToolkit.closeSilently(connection);
			}
		}
	}

}
