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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.RecordingProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.common.util.StatusFactory;

/**
 * Dumps a recording to file and then subsequently opens the file in an editor.
 */
public final class DumpRecordingJob extends Job {
	private final RecordingProvider m_recording;
	private final MCFile m_savePath;
	private IQuantity m_startTime;
	private IQuantity m_endTime;
	private IQuantity lastPartDuration;

	public DumpRecordingJob(RecordingProvider recording, MCFile savePath, IQuantity lastPartDuration) {
		this(recording, savePath);
		this.lastPartDuration = lastPartDuration;
	}

	public DumpRecordingJob(RecordingProvider recording, MCFile savePath, IQuantity startTime, IQuantity endTime) {
		this(recording, savePath);
		m_startTime = startTime;
		m_endTime = endTime;
	}

	public DumpRecordingJob(RecordingProvider recording, MCFile savePath) {
		super(NLS.bind(Messages.DUMP_RECORDING_JOB_NAME, recording.getRecordingDescriptor().getName()));
		m_recording = recording;
		m_savePath = savePath;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IConnectionHandle connection = null;
		InputStream is = null;
		try {
			m_recording.setIsOpening(true);
			connection = m_recording.getServerHandle().connect(getName());

			boolean removeDone = FlightRecorderUI.getDefault().removeFinishedRecordings();
			IFlightRecorderService flrService = connection.getServiceOrThrow(IFlightRecorderService.class);
			if (lastPartDuration != null) {
				if (m_recording.isStopped()) {
					IQuantity endTime = m_recording.getEndTime();
					IQuantity startTime = endTime.subtract(lastPartDuration);
					is = flrService.openStream(m_recording.getRecordingDescriptor(), startTime, endTime, removeDone);
				} else {
					is = flrService.openStream(m_recording.getRecordingDescriptor(), lastPartDuration, removeDone);
				}
			} else if (m_startTime != null && m_endTime != null) {
				is = flrService.openStream(m_recording.getRecordingDescriptor(), m_startTime, m_endTime, removeDone);
			} else {
				is = flrService.openStream(m_recording.getRecordingDescriptor(), removeDone);
			}
			File actualSavePath = IDESupportToolkit.writeToUniqueFile(m_savePath, is, monitor);
			WorkbenchToolkit.asyncOpenEditor(new MCPathEditorInput(actualSavePath, false));
			m_recording.setIsOpening(false);
			return StatusFactory.createOk(Messages.DUMP_RECORDING_JOB_SUCCESS_MSG);
		} catch (ServiceNotAvailableException e) {
			return StatusFactory.createErr(NLS.bind(Messages.DUMP_RECORDING_JOB_SERVICE_ERROR_MSG,
					m_recording.getRecordingDescriptor().getName()), e, false);
		} catch (FlightRecorderException e) {
			return StatusFactory.createErr(NLS.bind(Messages.DUMP_RECORDING_JOB_SERVICE_ERROR_MSG,
					m_recording.getRecordingDescriptor().getName()), e, false);
		} catch (IOException e) {
			return StatusFactory.createErr(
					NLS.bind(Messages.DUMP_RECORDING_JOB_IO_ERROR_MSG, m_recording.getRecordingDescriptor().getName()),
					e, false);
		} finally {
			IOToolkit.closeSilently(is);
			IOToolkit.closeSilently(connection);
		}
	}
}
