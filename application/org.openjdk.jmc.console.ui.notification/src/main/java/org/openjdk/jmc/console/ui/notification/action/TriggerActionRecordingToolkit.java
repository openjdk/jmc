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

import java.io.IOException;
import java.text.ParseException;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;

public class TriggerActionRecordingToolkit {

	private TriggerActionRecordingToolkit() {
		throw new Error("Don't instantiate"); //$NON-NLS-1$
	}

	protected static IConstrainedMap<EventOptionID> getTemplate(String name, IFlightRecorderService service)
			throws FlightRecorderException {
		for (String templateXML : service.getServerTemplates()) {
			EventConfiguration template;
			try {
				template = new EventConfiguration(EventConfiguration.createModel(templateXML));
			} catch (ParseException e) {
				continue;
			} catch (IOException e) {
				continue;
			}
			if (template.getName().equals(name)) {
				IDescribedMap<EventOptionID> options = service.getDefaultEventOptions();
				return template.getEventOptions(options.emptyWithSameConstraints());
			}
		}
		return null;
	}

	public static boolean supportsJfrAction(IConnectionHandle handle) {
		return handle.getServiceOrNull(IFlightRecorderService.class) != null;
	}

	public static boolean isActivatableJfrAction(IConnectionHandle handle) {
		IFlightRecorderService flrService = handle.getServiceOrNull(IFlightRecorderService.class);
		try {
			return flrService != null && (flrService.isEnabled() || ControlPanel.askUserForEnable(flrService,
					Messages.TriggerActionRecordingToolkit_COMMERCIAL_FEATURES_QUESTION_TRIGGERS));
		} catch (FlightRecorderException e) {
			// FIXME: Consider reporting the exception to the user
			return false;
		}
	}
}
