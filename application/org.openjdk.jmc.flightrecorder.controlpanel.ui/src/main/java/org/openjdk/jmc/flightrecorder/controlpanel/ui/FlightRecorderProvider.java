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
package org.openjdk.jmc.flightrecorder.controlpanel.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.DumpAnyRecordingAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.StartRecordingAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.IServerHandle.State;
import org.openjdk.jmc.rjmx.JVMSupportToolkit;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.ui.misc.SimpleActionProvider;
import org.openjdk.jmc.ui.common.action.IActionProvider;
import org.openjdk.jmc.ui.common.action.IUserAction;
import org.openjdk.jmc.ui.common.util.IDisconnectable;
import org.openjdk.jmc.ui.misc.IGraphical;
import org.openjdk.jmc.ui.misc.IRefreshable;
import org.openjdk.jmc.ui.misc.WarningDescriptorHelper;

/**
 */
public class FlightRecorderProvider
		implements IActionProvider, IRefreshable, IDescribable, IGraphical, IDisconnectable {

	private final IServerHandle server;
	private final Map<Long, RecordingProvider> children = new HashMap<>();
	private IConnectionHandle refreshConnection;
	private final List<? extends IUserAction> actions = Arrays.asList(new StartRecordingAction(this),
			new DumpAnyRecordingAction(this));

	private static List<SimpleActionProvider> LOADING_RECORDINGS = Arrays.asList(
			new SimpleActionProvider(Messages.RECORDING_INFO_LOADING, Messages.RECORDING_INFO_LOADING_DESCRIPTION));
	private final List<SimpleActionProvider> NO_RECORDINGS = Arrays.asList(new SimpleActionProvider(
			Messages.RECORDING_INFO_NO_RECORDINGS, Messages.RECORDING_INFO_NO_RECORDINGS_DESCRIPTION, null,
			Arrays.asList(new StartRecordingAction(this)), 0));

	private final WarningDescriptorHelper warningDescriptorHelper = new WarningDescriptorHelper();

	FlightRecorderProvider(IServerHandle server) {
		this.server = server;
		String errorMessage = JVMSupportToolkit.checkFlightRecorderSupport(server, true);
		if (errorMessage != null) {
			warningDescriptorHelper.setSpecificWarning(errorMessage);
		}
	}

	@Override
	public synchronized Collection<? extends IActionProvider> getChildren() {
		return getRefreshConnection() == null ? LOADING_RECORDINGS
				: (children.size() > 0 ? children.values() : NO_RECORDINGS);
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	public List<? extends IUserAction> getActions() {
		return actions;
	}

	@Override
	public IUserAction getDefaultAction() {
		return actions.get(0);
	}

	@Override
	public boolean refresh() {
		IConnectionHandle connection = getRefreshConnection();
		try {
			if (connection == null) {
				connection = server.connect(Messages.FLIGHT_RECORDER_MONITOR);
			}
			refreshRecordings(connection);
		} catch (Exception e) {
			IOToolkit.closeSilently(connection);
			/*
			 * FIXME: ConnectionHandle state is not always up to date.
			 * 
			 * It can be CONNECTED even when an exception has been thrown, since the connection has
			 * not yet been closed.
			 */
			State state = server.getState();
			if (state == State.DISPOSED || state == State.FAILED) {
				warningDescriptorHelper.setWarning(Messages.FLIGHT_RECORDER_SERVER_DISPOSED);
				return false;
			} else {
				warningDescriptorHelper.setWarning(e.getLocalizedMessage());
				throw new RuntimeException(e.getLocalizedMessage(), e);
			}
		} finally {
			if (!reuseRefreshConnection(connection)) {
				IOToolkit.closeSilently(connection);
			}
		}
		warningDescriptorHelper.resetWarning();
		return true;
	}

	public void refreshRecordings(IConnectionHandle ch) throws Exception {
		IFlightRecorderService service = getService(ch);
		List<IRecordingDescriptor> availableRecordings = service.getAvailableRecordings();
		Set<Long> recordingsIds = getRecordingIds();
		for (IRecordingDescriptor rec : availableRecordings) {
			RecordingProvider c = getRecording(rec);
			c.updateRecording(rec, ch);
			recordingsIds.remove(rec.getId());
		}
		removeRecordings(recordingsIds);
	}

	public RecordingProvider findRecording(Long id, IConnectionHandle connectionHandle) throws Exception {
		refreshRecordings(connectionHandle);
		synchronized (this) {
			return children.get(id);
		}
	}

	public synchronized RecordingProvider getRecording(IRecordingDescriptor rec) {
		long id = rec.getId();
		RecordingProvider c = children.get(id);
		if (c == null) {
			c = new RecordingProvider(server, rec);
			children.put(id, c);
		}
		return c;
	}

	private synchronized Set<Long> getRecordingIds() {
		return new HashSet<>(children.keySet());
	}

	public RecordingProvider getSnapshotRecording(IConnectionHandle ch) throws Exception {
		IFlightRecorderService service = getService(ch);
		IRecordingDescriptor snapshotRecording = service.getSnapshotRecording();
		if (snapshotRecording != null) {
			return getRecording(snapshotRecording);
		}
		return null;
	}

	private IFlightRecorderService getService(IConnectionHandle ch) throws FlightRecorderException {
		IFlightRecorderService service = ch.getServiceOrNull(IFlightRecorderService.class);
		if (service == null) {
			throw new FlightRecorderException(JVMSupportToolkit.getNoFlightRecorderErrorMessage(ch, false));
		} else if (!service.isEnabled()) {
			throw new FlightRecorderException(Messages.FLIGHT_RECORDER_NOT_ENABLED);
		}
		return service;
	}

	private synchronized void removeRecordings(Iterable<Long> recordingIds) {
		for (Long recId : recordingIds) {
			children.remove(recId);
		}
	}

	private synchronized boolean reuseRefreshConnection(IConnectionHandle connection) {
		if (refreshConnection == null || !refreshConnection.isConnected()) {
			refreshConnection = connection;
			return true;
		}
		return refreshConnection == connection;
	}

	private synchronized IConnectionHandle getRefreshConnection() {
		return refreshConnection == null || !refreshConnection.isConnected() ? null : refreshConnection;
	}

	private synchronized IConnectionHandle removeRefreshConnection() {
		IConnectionHandle tmp = refreshConnection;
		refreshConnection = null;
		return tmp;
	}

	public IServerHandle getServerHandle() {
		return server;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return warningDescriptorHelper.getImageDescriptor(
				ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.ICON_FLIGHT_RECORDING));
	}

	@Override
	public String getName() {
		return warningDescriptorHelper.getName(Messages.FLIGHT_RECORDER_NAME);
	}

	@Override
	public String getDescription() {
		return warningDescriptorHelper.getDescription(Messages.FLIGHT_RECORDER_DESCRIPTION);
	}

	@Override
	public String toString() {
		return getServerHandle().getServerDescriptor().getDisplayName() + "/" + getName(); //$NON-NLS-1$
	}

	public void setWarning(String message) {
		warningDescriptorHelper.setWarning(message);
	}

	public void resetWarning() {
		warningDescriptorHelper.resetWarning();
	}

	@Override
	public void disconnect() {
		IOToolkit.closeSilently(removeRefreshConnection());
	}

	@Override
	public boolean isConnected() {
		synchronized (this) {
			return refreshConnection != null && refreshConnection.isConnected();
		}
	}
}
