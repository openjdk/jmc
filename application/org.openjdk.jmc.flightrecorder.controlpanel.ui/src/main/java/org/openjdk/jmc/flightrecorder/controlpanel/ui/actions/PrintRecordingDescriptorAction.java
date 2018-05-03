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

import static org.openjdk.jmc.common.IDisplayable.AUTO;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.RecordingProvider;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.ui.common.action.IUserAction;
import org.openjdk.jmc.ui.misc.IGraphical;

/**
 * Prints information about a recording descriptor. Used for debugging purposes. Not to be
 * translated.
 */
@SuppressWarnings("nls")
public class PrintRecordingDescriptorAction implements IUserAction, IGraphical {
	private final IRecordingDescriptor recording;
	private IFlightRecorderService flightRecorderService;
	private final RecordingProvider rec;

	public PrintRecordingDescriptorAction(RecordingProvider recording) {
		rec = recording;
		this.recording = recording.getRecordingDescriptor();
	}

	private PrintStream createNewOutputStream() {
		PrintStream newOut = null;
		try {
			String fileName = System
					.getProperty("org.openjdk.jmc.flightrecorder.controlpanel.ui.printRecordingFileName");
			if (fileName != null) {
				newOut = new PrintStream(new FileOutputStream(fileName));
			}
		} catch (IOException e) {
		}
		return newOut;
	}

	private void printRecordingInfo(PrintStream stream) {
		printRecordingInfo(new PrintWriter(stream));
	}

	private void printRecordingInfo(PrintWriter writer) {
		try {
			printDivider(writer);
			printRecordingDescriptor(writer);
			printRecordingOptions(writer);
			// FIXME: Restore after fixing printEventSettings
//			printEventSettings(writer);
			printAvailableRecordingOptions(writer);
			printAvailableEventTypes(writer);
			printCurrentEventTypeSettings(writer);
			printDivider(writer);
			writer.flush();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void printDivider(PrintWriter writer) {
		writer.println("============================================================");
	}

	private void printRecordingDescriptor(PrintWriter writer) {
		writer.printf("id=%d%n", recording.getId());
		writer.printf("name=%s%n", recording.getName());
		writer.printf("state=%s%n", recording.getState());
		writer.printf("options=%n");
		for (Entry<?, ?> entry : ((Map<?, ?>) recording.getOptions()).entrySet()) {
			writer.printf("\t%s=%s%s%n", entry.getKey(), entry.getValue(),
					((entry.getValue() != null) ? NLS.bind(" ({0})", entry.getValue().getClass()) : ""));
		}
		writer.printf("objectName=%s%n", recording.getObjectName());
		writer.printf("dataStartTime=%s%n", display(recording.getDataStartTime()));
		writer.printf("dataEndTime=%s%n", display(recording.getDataEndTime()));
	}

	private String display(IDisplayable displayable) {
		return (displayable != null) ? displayable.displayUsing(AUTO) : null;
	}

	private void printRecordingOptions(PrintWriter writer)
			throws FlightRecorderException, IllegalArgumentException, ConnectionException {
		IConstrainedMap<String> options = flightRecorderService.getRecordingOptions(recording);
		writer.printf("getRecordingOptions(recordingDescriptor)=%n");
		for (String key : options.keySet()) {
			writer.printf("\t%s=%n\t\t%s, %s%n", key, options.getPersistableString(key), options.getConstraint(key));
		}
	}

	/*
	 * FIXME: printEventSettings causes a ClassCastException because EventOptionID is no longer
	 * Comparable. Reimplement this method.
	 */
//	private void printEventSettings(PrintWriter writer) throws FlightRecorderException {
//		IConstrainedMap<EventOptionID> settings = flightRecorderService.getEventSettings(recording);
//		SortedSet<EventOptionID> options = new TreeSet<>(settings.keySet());
//		writer.printf("getEventSettings(recordingDescriptor)=%n");
//		for (EventOptionID optionID : options) {
//			writer.printf("\t%s=%n\t\t%s%n", optionID, settings.getPersistableString(optionID));
//		}
//	}

	private void printAvailableRecordingOptions(PrintWriter writer) throws FlightRecorderException {
		Map<String, IOptionDescriptor<?>> optionDescriptors = flightRecorderService.getAvailableRecordingOptions();
		writer.printf("getAvailableRecordingOptions()=%n");
		for (Entry<String, IOptionDescriptor<?>> entry : optionDescriptors.entrySet()) {
			writer.printf("\t%s=\t%s%n", entry.getKey(), entry.getValue());
		}
	}

	private void printAvailableEventTypes(PrintWriter writer) throws FlightRecorderException {
		writer.printf("getAvailableEventTypes()=%n");
		int i = 0;
		for (IEventTypeInfo eventType : flightRecorderService.getAvailableEventTypes()) {
			writer.printf("\t[%d]\t%s%n", i++, toString(eventType, "\t\t"));
		}
	}

	// FIXME: Change to default event options, as "current" isn't actually available in JDK9
	private void printCurrentEventTypeSettings(PrintWriter writer) throws FlightRecorderException {
		IConstrainedMap<EventOptionID> settings = flightRecorderService.getCurrentEventTypeSettings();
		SortedSet<EventOptionID> options = new TreeSet<>(settings.keySet());
		writer.printf("getCurrentEventTypeSettings()=%n");
		for (EventOptionID optionID : options) {
			writer.printf("\t%s=%n\t\t%s%n", optionID, settings.getPersistableString(optionID));
		}
	}

	// Provisional support for JFR 1.0 and JFR 2.0 meta data
	private String toString(IEventTypeInfo eventTypeInfo, String indent) {
		return eventTypeInfo.toString();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "Print";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void execute() {
		PrintStream oldOut = System.err;
		PrintStream newOut = null;
		IConnectionHandle handle = null;

		try {
			handle = rec.getServerHandle().connect("Print Recording Info");
			flightRecorderService = handle.getServiceOrThrow(IFlightRecorderService.class);
			newOut = createNewOutputStream();
			if (newOut != null) {
				System.setErr(newOut);
				printRecordingInfo(newOut);
				System.setErr(oldOut);
			} else {
				printRecordingInfo(System.err);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ConnectionException e) {
			e.printStackTrace();
		} catch (ServiceNotAvailableException e) {
			e.printStackTrace();
		} finally {
			IOToolkit.closeSilently(handle);
			IOToolkit.closeSilently(newOut);
		}
	}
}
