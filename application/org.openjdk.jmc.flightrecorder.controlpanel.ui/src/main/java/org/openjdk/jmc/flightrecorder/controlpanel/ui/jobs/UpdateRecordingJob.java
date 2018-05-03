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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.jobs;

import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.ui.common.util.StatusFactory;

/**
 * Tries to update an existing recording with a new recording settings.
 */
public class UpdateRecordingJob extends Job {
	private final IRecordingDescriptor m_recordingDescriptor;
	private final IConstrainedMap<String> m_recordingOptions;
	private final IConstrainedMap<EventOptionID> m_recordingSettings;
	private final IServerHandle m_server;

	public UpdateRecordingJob(IServerHandle server, IRecordingDescriptor recordingDescriptor,
			IConstrainedMap<String> recordingOptions, IConstrainedMap<EventOptionID> recordingSettings) {
		super(NLS.bind(Messages.UPDATE_RECORDING_JOB_NAME, recordingDescriptor.getName()));
		this.m_server = server;
		this.m_recordingDescriptor = recordingDescriptor;
		m_recordingOptions = recordingOptions;
		m_recordingSettings = recordingSettings;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IConnectionHandle connection = null;
		try {
			connection = m_server.connect(getName());
			IFlightRecorderService flightRecorderService = connection.getServiceOrThrow(IFlightRecorderService.class);
			flightRecorderService.updateRecordingOptions(m_recordingDescriptor, m_recordingOptions);
			flightRecorderService.updateEventOptions(m_recordingDescriptor, m_recordingSettings);
			return StatusFactory.createOk(Messages.UPDATE_RECORDING_JOB_SUCCESS_MSG);
		} catch (Exception e) {
			ControlPanel.getDefault().getLogger().log(Level.WARNING, "Could not update recording", e); //$NON-NLS-1$
			return StatusFactory.createErr(
					NLS.bind(Messages.UPDATE_RECORDING_JOB_SERVICE_ERROR_MSG, m_recordingDescriptor.getName()));
		} finally {
			IOToolkit.closeSilently(connection);
		}
	}
}
