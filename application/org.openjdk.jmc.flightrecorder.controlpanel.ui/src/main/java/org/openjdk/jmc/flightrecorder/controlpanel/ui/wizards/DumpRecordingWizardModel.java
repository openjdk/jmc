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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.wizards;

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;

import org.eclipse.osgi.util.NLS;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.RecordingProvider;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.subscription.IMBeanHelperService;
import org.openjdk.jmc.ui.common.resource.MCFile;

/**
 */
public class DumpRecordingWizardModel {

	private final IConnectionHandle m_connection;
	private final RecordingProvider m_recordingProvider;
	private MCFile m_path;

	private IQuantity m_recordingStartTime;

	/**
	 * Creates a new wizard page for given recording.
	 *
	 * @param recording
	 *            the recording to eventually dump
	 * @throws ConnectionException
	 */
	public DumpRecordingWizardModel(RecordingProvider recording) throws ConnectionException {
		m_recordingProvider = recording;
		m_path = recording.getDumpToFile();
		m_recordingStartTime = recording.getRecordingDescriptor().getStartTime();
		if (m_recordingStartTime == null) {
			m_recordingStartTime = currentServerTime();
		}
		m_connection = recording.getServerHandle()
				.connect(NLS.bind(Messages.DUMP_RECORDING_CONNECTION, recording.getName()));
	}

	/**
	 * @return the possible end time for this recording, start time plus duration or current time
	 *         for continuous recordings
	 */
	public IQuantity recordingEndTime() {
		IQuantity endTime = m_recordingProvider.getEndTime();
		if (endTime != null && recordingIsStopped()) {
			return endTime;
		} else {
			return currentServerTime();
		}
	}

	/**
	 * Retrieves the updated recording descriptor and tests its state.
	 *
	 * @return <tt>true</tt> if the recording has stopped, <tt>false</tt> otherwise
	 */
	private boolean recordingIsStopped() {
		return m_recordingProvider != null && m_recordingProvider.isStopped();
	}

	private IQuantity currentServerTime() {
		IMBeanHelperService service = m_connection.getServiceOrDummy(IMBeanHelperService.class);
		return EPOCH_MS.quantity(service.getApproximateServerTime(System.currentTimeMillis()));
	}

	public RecordingProvider getRecordingProvider() {
		return m_recordingProvider;
	}

	public MCFile getPath() {
		return m_path;
	}

	public IQuantity getRecordingStartTime() {
		return m_recordingStartTime;
	}

	public void setPath(MCFile path) {
		m_path = path;

	}

	public void dispose() {
		IOToolkit.closeSilently(m_connection);
	}
}
