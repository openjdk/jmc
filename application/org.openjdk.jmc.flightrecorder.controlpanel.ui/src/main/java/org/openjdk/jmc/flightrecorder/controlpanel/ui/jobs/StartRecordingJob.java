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

import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;

import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.FlightRecorderProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.RecordingProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.IServerHandle.State;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.common.util.StatusFactory;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Starts a flight recording based on the recording options and the recording template.
 */
public final class StartRecordingJob extends Job {
	private final IConstrainedMap<String> m_recordingOptions;
	private final IConstrainedMap<EventOptionID> m_eventOptions;
	private final MCFile m_path;
	private final FlightRecorderProvider m_recorder;
	private volatile boolean m_canceling;

	public StartRecordingJob(FlightRecorderProvider recorder, String name, IConstrainedMap<String> recordingOptions,
			IConstrainedMap<EventOptionID> eventOptions, MCFile path) {
		super(NLS.bind(Messages.RECORDING_JOB_NAME, name));
		m_recordingOptions = recordingOptions;
		m_eventOptions = eventOptions;
		m_path = path;
		m_recorder = recorder;
		setUser(true);
	}

	@Override
	protected void canceling() {
		m_canceling = true;
		// Must interrupt thread so it wakes up and shuts down right away,
		// otherwise the thread will wake up later after JMC has shut down, and we get
		// a warning in the log; "Job found still running after platform shutdown".
		getThread().interrupt();
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IConnectionHandle connection = null;
		String connectionName = null;
		IRecordingDescriptor startedRecording = null;
		try {
			connectionName = m_recorder.getServerHandle().getServerDescriptor().getDisplayName();
			connection = m_recorder.getServerHandle().connect(getName());
			IFlightRecorderService flightRecorderService = connection.getServiceOrThrow(IFlightRecorderService.class);
			startedRecording = flightRecorderService.start(m_recordingOptions, m_eventOptions);
			RecordingProvider recording = m_recorder.getRecording(startedRecording);
			recording.setDumpToFile(m_path);
			if (!startedRecording.isContinuous()) {
				return monitorRecording(recording, monitor, connection, flightRecorderService);
			}
			return Status.OK_STATUS;
		} catch (Exception e) {
			// FIXME: Can sometime get CONNECTED state since the connection might not have had time to close yet
			IServerHandle.State state = m_recorder.getServerHandle().getState();
			if (state == State.DISPOSED) {
				return Status.OK_STATUS;
			} else if (startedRecording != null) {
				showMonitoringWarning(startedRecording, connectionName, e);
				return Status.OK_STATUS;
			} else {
				showStartingWarning(connectionName, e);
				return Status.OK_STATUS;
			}
		} finally {
			IOToolkit.closeSilently(connection);
		}
	}

	private void showStartingWarning(final String connectionName, final Exception exception) {
		DisplayToolkit.safeAsyncExec(Display.getDefault(), new Runnable() {
			@Override
			public void run() {
				DialogToolkit.showException(Display.getCurrent().getActiveShell(),
						Messages.START_RECORDING_JOB_SERVICE_ERROR_TITLE,
						NLS.bind(Messages.START_RECORDING_JOB_SERVICE_ERROR_MESSAGE, connectionName), exception);
			}
		});
	}

	private void showMonitoringWarning(
		final IRecordingDescriptor startedRecording, final String connectionName, final Exception e) {
		final String detailedMessage = NLS.bind(Messages.START_RECORDING_JOB_ERROR_WHILE_MONITORING_RECORDING_MESSAGE,
				startedRecording.getName(), connectionName);
		if (FlightRecorderUI.getDefault().getShowMonitoringWarning()) {
			ControlPanel.getDefault().getLogger().log(Level.WARNING, detailedMessage, e);
			DisplayToolkit.safeAsyncExec(Display.getDefault(), new Runnable() {
				@Override
				public void run() {
					MessageDialogWithToggle warnDialog = MessageDialogWithToggle.openWarning(
							Display.getCurrent().getActiveShell(),
							Messages.START_RECORDING_JOB_ERROR_WHILE_MONITORING_RECORDING_TITLE, detailedMessage,
							org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages.PREFERENCES_SHOW_MONITORING_WARNING_TEXT,
							true, null, null);
					if (warnDialog.getReturnCode() == IDialogConstants.OK_ID) {
						if (!warnDialog.getToggleState()) {
							FlightRecorderUI.getDefault().setShowMonitoringWarning(false);
						}
					}
				}
			});
		} else {
			ControlPanel.getDefault().getLogger().log(Level.FINER, detailedMessage, e);
		}
	}

	private IStatus monitorRecording(
		RecordingProvider rec, IProgressMonitor monitor, IConnectionHandle connection,
		IFlightRecorderService flightRecorderService) throws Exception {
		String recordingName = rec.getRecordingDescriptor().getName();
		int totalWork = rec.getRecordingDescriptor().getDuration().clampedIntFloorIn(SECOND);
		monitor.beginTask(recordingName, totalWork);

		int progress = 0;
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			if (rec.wasClosed()) {
				monitor.done();
				break;
			}

			rec = m_recorder.findRecording(rec.getRecordingDescriptor().getId(), connection);
			if (rec != null) {
				if (rec.isStopped() || m_canceling) {
					if (rec.isAlive()) {
						flightRecorderService.stop(rec.getRecordingDescriptor());
					}
					new DumpRecordingJob(rec, rec.getDumpToFile()).schedule();
					monitor.done();
					break;
				}
				IQuantity remaining = rec.getTimeRemaining();
				IQuantity duration = rec.getRecordingDescriptor().getDuration();
				float partOfWorkDone = (duration != null) && (remaining != null)
						? 1 - (float) remaining.ratioTo(duration) : 0;
				// FIXME: Make sure we never make negative progress even if server time is skewed due to hibernation.
				// A problem is that there is no definite way to retrieve current time of a server through JMX
				int newProgress = Math.max(Math.round(totalWork * partOfWorkDone), progress);
				int worked = newProgress - progress;
				progress = newProgress;
				try {
					// FIXME: Setting negative values violates the contract of IProgressMonitor.worked()
					// It works for org.eclipse.ui.internal.progress.ProgressManager.JobMonitor though,
					// so for now we do it anyway to support changing the recording duration.
					monitor.worked(worked);
				} catch (RuntimeException e) {
					// Catch potential exceptions since we use illegal arguments
					ControlPanel.getDefault().getLogger().log(Level.WARNING, "Could not update progress", e); //$NON-NLS-1$
				}
			} else {
				monitor.done();
				return StatusFactory
						.createErr(NLS.bind(Messages.START_FLIGHT_RECORDING_JOB_MISSING_RECORDING, recordingName));
			}
		}
		return Status.OK_STATUS;
	}
}
