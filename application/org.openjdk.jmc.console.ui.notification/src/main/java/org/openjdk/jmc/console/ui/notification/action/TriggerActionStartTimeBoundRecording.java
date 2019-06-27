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
package org.openjdk.jmc.console.ui.notification.action;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.alert.AlertObject;
import org.openjdk.jmc.alert.AlertPlugin;
import org.openjdk.jmc.alert.NotificationUIToolkit;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor.RecordingState;
import org.openjdk.jmc.rjmx.triggers.IActivatableTriggerAction;
import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.common.util.StatusFactory;

/**
 * NotificationAction that starts a time bound recording.
 */
public class TriggerActionStartTimeBoundRecording extends TriggerAction implements IActivatableTriggerAction {

	public static final int MAX_CONTINUOUS_ERROR_COUNT = 10;
	private int recordingNumber = 0;

	/**
	 * Constructor.
	 */
	public TriggerActionStartTimeBoundRecording() {
	}

	/**
	 * @throws FlightRecorderException
	 * @see ITriggerAction#handleNotificationEvent(TriggerEvent)
	 */
	@Override
	public void handleNotificationEvent(final TriggerEvent event) throws FlightRecorderException {
		final IFlightRecorderService service = event.getSource().getServiceOrNull(IFlightRecorderService.class);
		if (service == null) {
			NotificationPlugin.getDefault().getLogger().severe("There is no flight recorder available on the " //$NON-NLS-1$
					+ event.getSource().getServerDescriptor().getDisplayName() + " JVM"); //$NON-NLS-1$
			return;
		}
		startTimeBoundRecording(service, event);
	}

	private void startTimeBoundRecording(final IFlightRecorderService service, TriggerEvent event)
			throws FlightRecorderException {
		IQuantity duration = getSetting("timerange").getQuantity(); //$NON-NLS-1$
		try {
			RecordingOptionsBuilder b = new RecordingOptionsBuilder(service);
			b.duration(duration);
			String name = getSetting("name").getString(); //$NON-NLS-1$
			if (++recordingNumber > 1) {
				name += " " + recordingNumber; //$NON-NLS-1$
			}
			b.name(name);

			MCFile path = IDESupportToolkit.createFileResource(getSetting("file").getFileName()); //$NON-NLS-1$
			IRecordingDescriptor descriptor = service.start(b.build(),
					TriggerActionRecordingToolkit.getTemplate("Profiling", service)); //$NON-NLS-1$
			boolean open = getSetting("open").getBoolean(); //$NON-NLS-1$

			if (!descriptor.isContinuous()) {
				new WaitAndOpenJob(service, descriptor, event, path, open)
						.schedule(duration.clampedLongValueIn(UnitLookup.MILLISECOND));
			}
		} catch (QuantityConversionException e) {
			// This shouldn't happen for JFR versions known at this point.
			throw new FlightRecorderException(null, e);
		}
	}

	private class WaitAndOpenJob extends Job {
		private final IFlightRecorderService m_service;
		private IRecordingDescriptor m_descriptor;
		private final MCFile m_path;
		private int m_updateErrorCount = 0;
		private final boolean m_open;
		private final TriggerEvent m_event;

		public WaitAndOpenJob(IFlightRecorderService service, IRecordingDescriptor descriptor, TriggerEvent event,
				MCFile path, boolean open) {
			super(descriptor.getName());
			m_service = service;
			m_descriptor = descriptor;
			m_event = event;
			m_path = path;
			m_open = open;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				m_descriptor = m_service.getUpdatedRecordingDescription(m_descriptor);
				m_updateErrorCount = 0;
			} catch (FlightRecorderException e) {
				NotificationPlugin.getDefault().getLogger()
						.severe("Problem updating a flight recording on the " + m_descriptor.getName() + " JVM"); //$NON-NLS-1$ //$NON-NLS-2$
				if (++m_updateErrorCount > MAX_CONTINUOUS_ERROR_COUNT) {
					return StatusFactory
							.createErr(NLS.bind(Messages.TriggerActionStartTimeBoundRecording_UPDATE_STATUS_ERROR_MSG,
									m_descriptor.getName()), e, false);
				}
			}
			if (m_descriptor.getState() != RecordingState.STOPPED) {
				this.schedule(1000);
				return Status.OK_STATUS;
			}
			try {
				File writtenFile = dumpFile(monitor, m_service, m_descriptor, m_path);
				if (m_open) {
					WorkbenchToolkit.asyncOpenEditor(new MCPathEditorInput(writtenFile, false));
				} else {
					showAlert(writtenFile, m_event);
				}
				return StatusFactory.createOk(
						NLS.bind(Messages.WriteAndOpenRecordingJob_MESSAGE_SUCCESSFUL_DUMP, m_descriptor.getName()));
			} catch (FlightRecorderException e) {
				return StatusFactory.createErr(NLS.bind(Messages.TriggerActionStartTimeBoundRecording_SERVICE_ERROR_MSG,
						m_descriptor.getName()), e, false);
			} catch (IOException e) {
				return StatusFactory.createErr(
						NLS.bind(Messages.TriggerActionStartTimeBoundRecording_IO_ERROR_MSG, m_descriptor.getName()), e,
						false);
			}
		}
	}

	private File dumpFile(
		IProgressMonitor monitor, IFlightRecorderService service, IRecordingDescriptor descriptor, MCFile path)
			throws IOException, FlightRecorderException {
		InputStream stream = service.openStream(descriptor, false);
		try {
			return IDESupportToolkit.writeToUniqueFile(path, stream, monitor);
		} finally {
			IOToolkit.closeSilently(stream);
		}
	}

	private void showAlert(File file, TriggerEvent event) {
		AlertObject ao = new AlertObject(event.getCreationTime(),
				event.getSource().getServerDescriptor().getDisplayName(), event.getRule(),
				NotificationUIToolkit.prettyPrint(event,
						NLS.bind(Messages.TriggerActionStartTimeBoundRecording_MESSAGE_RECORDING_SUCCESSFUL_PATH,
								file.getPath())),
				null);
		AlertPlugin.getDefault().addAlertObject(ao);
	}

	@Override
	public boolean supportsAction(IConnectionHandle handle) {
		return TriggerActionRecordingToolkit.supportsJfrAction(handle);
	}

	@Override
	public boolean isActivatable(IConnectionHandle handle) {
		return TriggerActionRecordingToolkit.isActivatableJfrAction(handle);
	}
}
