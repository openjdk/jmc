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

import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.triggers.IActivatableTriggerAction;
import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;

/**
 * NotificationAction that starts a continuous recording.
 */
public class TriggerActionStartContinuousRecording extends TriggerAction implements IActivatableTriggerAction {

	private int recordingNumber = 0;

	/**
	 * Constructor.
	 */
	public TriggerActionStartContinuousRecording() {
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
		try {
			RecordingOptionsBuilder b = new RecordingOptionsBuilder(service);
			b.duration(0);
			String name = getSetting("name").getString(); //$NON-NLS-1$
			if (++recordingNumber > 1) {
				name += " " + recordingNumber; //$NON-NLS-1$
			}
			b.name(name);
			service.start(b.build(), TriggerActionRecordingToolkit.getTemplate("Continuous", service)); //$NON-NLS-1$
		} catch (QuantityConversionException e) {
			// This shouldn't happen as long as the recording options support a name and a duration.
			throw new FlightRecorderException(null, e);
		}
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
