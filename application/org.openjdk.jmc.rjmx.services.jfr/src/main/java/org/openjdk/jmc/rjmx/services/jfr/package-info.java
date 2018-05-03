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
/**
 * This package contains the service to control the flight recorder, and all classes related to that
 * service.
 * <p>
 * The following example will print the names of all ongoing recordings:
 *
 * <pre>
 * <code>
 * IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(); //$NON-NLS-1$
 * IServerHandle serverHandle = IServerHandle.create(descriptor);
 * IConnectionHandle handle = serverHandle.connect("Get JFR recording info"); //$NON-NLS-1$
 * try {
 * 	IFlightRecorderService jfr = handle.getServiceOrThrow(IFlightRecorderService.class);
 * 	for (IRecordingDescriptor desc : jfr.getAvailableRecordings()) {
 * 		System.out.println(desc.getName());
 * 	}
 * } finally {
 * 	IOToolkit.closeSilently(handle);
 * }
 *</code>
 * </pre>
 *
 * The following will start a time bound recording, and then transfer the recording to a local file
 * when completed:
 *
 * <pre>
 * <code>
 * IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(); //$NON-NLS-1$
 * IServerHandle serverHandle = IServerHandle.create(descriptor);
 * IConnectionHandle handle = serverHandle.connect("Start time bound flight recording");
 * try {
 * 	IFlightRecorderService jfr = handle.getServiceOrThrow(IFlightRecorderService.class);
 *
 * 	long duration = 5000;
 * 	IConstrainedMap<String> defaultRecordingOptions = jfr.getDefaultRecordingOptions();
 *  IDescribedMap<EventOptionID> defaultEventOptions = jfr.getDefaultEventOptions();
 * 	IConstrainedMap<String> recordingOptions = new RecordingOptionsBuilder(defaultRecordingOptions.mutableCopy())
 * 			.name("MyRecording").duration(duration).build();
 * 	IRecordingDescriptor recording = jfr.start(recordingOptions, defaultEventOptions);
 * 	Thread.sleep(duration);
 * 	while (recording.getState() != IRecordingDescriptor.RecordingState.STOPPED) {
 * 		Thread.sleep(1000);
 * 		recording = jfr.getUpdatedRecordingDescription(recording);
 * 	}
 * 	InputStream is = jfr.openStream(recording, true);
 * 	writeStreamToFile(is);
 * } finally {
 * 	IOToolkit.closeSilently(handle);
 * }
 * </code>
 * </pre>
 */
package org.openjdk.jmc.rjmx.services.jfr;
