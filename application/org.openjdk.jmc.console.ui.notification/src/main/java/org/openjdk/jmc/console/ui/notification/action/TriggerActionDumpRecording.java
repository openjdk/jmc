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

import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;

import java.util.logging.Level;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderToolkit;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;
import org.openjdk.jmc.ui.common.idesupport.IDESupportToolkit;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * NotificationAction that dumps what is available from the continuous recording when triggered.
 */
public class TriggerActionDumpRecording extends TriggerAction {

	/**
	 * Constructor.
	 */
	public TriggerActionDumpRecording() {
	}

	/**
	 * @see ITriggerAction#handleNotificationEvent(TriggerEvent)
	 */
	@Override
	public void handleNotificationEvent(final TriggerEvent event) {
		final IFlightRecorderService service = event.getSource().getServiceOrNull(IFlightRecorderService.class);
		if (service == null) {
			RJMXPlugin.getDefault().getLogger()
					.severe("There is no flight recorder available on the " + event.getSource() + " JVM"); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		// can only look up window in UI thread and can not let job be asynchronous
		DisplayToolkit.safeAsyncExec(new Runnable() {
			@Override
			public void run() {
				Job job = createDumpFlightRecordingJob(event, service);
				job.schedule();
			}
		});
	}

	protected Job createDumpFlightRecordingJob(final TriggerEvent event, final IFlightRecorderService service) {
		MCFile path = IDESupportToolkit.createFileResource(getSetting("file").getFileName()); //$NON-NLS-1$
		IQuantity timerange = getSetting("timerange").getQuantity(); //$NON-NLS-1$
		Boolean open = getSetting("open").getBoolean(); //$NON-NLS-1$
		return new WriteAndOpenRecordingJob(
				NLS.bind(Messages.TriggerActionDumpRecording_DUMPING_JOB_NAME, event.getRule().getName()),
				event.getSource().getServerDescriptor().getDisplayName(), service, path, timerange, open, event);
	}

	@Override
	public boolean supportsAction(IConnectionHandle handle) {
		IFlightRecorderService jfrService = handle.getServiceOrNull(IFlightRecorderService.class);
		try {
			return jfrService != null && jfrService.isEnabled() && FlightRecorderToolkit
					.getDescriptorByTimerange(jfrService.getAvailableRecordings(), MILLISECOND.quantity(0)) != null;
		} catch (FlightRecorderException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
					"Got exception when checking for available recordings in JVM", e); //$NON-NLS-1$

		}
		return false;
	}
}
