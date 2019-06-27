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
import java.util.logging.Level;

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
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderToolkit;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;

public class WriteAndOpenRecordingJob extends Job {
	private final String serverName;
	private final IFlightRecorderService service;
	private final MCFile path;
	private final IQuantity timerange;
	private final boolean open;
	private TriggerEvent event;

	public WriteAndOpenRecordingJob(String jobName, String serverName, IFlightRecorderService service, MCFile path,
			IQuantity timerange, boolean open, TriggerEvent event) {
		super(jobName);
		this.service = service;
		this.serverName = serverName;
		this.path = path;
		this.timerange = timerange;
		this.open = open;
		this.event = event;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		String recordingName;
		try {
			IRecordingDescriptor descriptor = findRecording();
			if (descriptor == null) {
				RJMXPlugin.getDefault().getLogger().severe("There is no usable recording running at " + serverName); //$NON-NLS-1$
				return new Status(IStatus.ERROR, NotificationPlugin.PLUGIN_ID,
						Messages.WriteAndOpenRecordingJob_ERROR_MESSAGE_COULD_NOT_FIND_RECORDING);
			}
			recordingName = descriptor.getName();
			File writtenFile = writeFile(monitor, descriptor, timerange);
			if (open) {
				WorkbenchToolkit.asyncOpenEditor(new MCPathEditorInput(writtenFile, false));
			} else {
				showAlert(writtenFile, event);
			}
		} catch (Exception e) {
			// Want non-localized message in the log!
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not dump recording. Faulty rule in console?", //$NON-NLS-1$
					e);
			return new Status(IStatus.ERROR, NotificationPlugin.PLUGIN_ID,
					NLS.bind(Messages.WriteAndOpenRecordingJob_ERROR_MESSAGE_DUMPING_RECORDING, serverName), e);
		}
		return new Status(IStatus.OK, NotificationPlugin.PLUGIN_ID,
				NLS.bind(Messages.WriteAndOpenRecordingJob_MESSAGE_SUCCESSFUL_DUMP, recordingName));
	}

	private void showAlert(File file, TriggerEvent event) {
		String serverName = event.getSource().getServerDescriptor().getDisplayName();
		String message = NotificationUIToolkit.prettyPrint(event,
				NLS.bind(Messages.WriteAndOpenRecordingJob_MESSAGE_DUMP_SUCCESSFUL_PATH, file.getAbsolutePath()));
		AlertObject ao = new AlertObject(event.getCreationTime(), serverName, event.getRule(), message, null);
		AlertPlugin.getDefault().addAlertObject(ao);
	}

	private IRecordingDescriptor findRecording() throws FlightRecorderException {
		return FlightRecorderToolkit.getDescriptorByTimerange(service.getAvailableRecordings(), timerange);
	}

	private File writeFile(IProgressMonitor monitor, IRecordingDescriptor descriptor, IQuantity duration)
			throws FlightRecorderException, IOException {
		InputStream stream = service.openStream(descriptor, duration, false);
		try {
			return IDESupportToolkit.writeToUniqueFile(path, stream, monitor);
		} finally {
			IOToolkit.closeSilently(stream);
		}
	}
}
