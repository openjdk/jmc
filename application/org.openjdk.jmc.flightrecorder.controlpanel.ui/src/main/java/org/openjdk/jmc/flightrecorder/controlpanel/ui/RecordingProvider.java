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

import static org.openjdk.jmc.common.IDisplayable.AUTO;
import static org.openjdk.jmc.common.IDisplayable.EXACT;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor.RecordingState.CREATED;
import static org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor.RecordingState.RUNNING;
import static org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor.RecordingState.STOPPED;
import static org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor.RecordingState.STOPPING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity.DualUnitFormatter;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.CloseRecordingAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.DumpLastPartRecordingAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.DumpRecordingAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.DumpWholeRecordingAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.EditRecordingAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.PrintRecordingDescriptorAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.actions.StopRecordingAction;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor.RecordingState;
import org.openjdk.jmc.ui.common.action.IActionProvider;
import org.openjdk.jmc.ui.common.action.IUserAction;
import org.openjdk.jmc.ui.common.resource.MCFile;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.misc.IGraphical;

/**
 */
public class RecordingProvider implements IActionProvider, IDescribable, IGraphical {
	private static final char INFINITY = '\u221E';
	private static final char DELAYED_CONTINUOUS = '\u2192';
	private static final char DELAYED_DURATION = '\u21E5';
	// FIXME: If the cutoff formatter makes sense elsewhere, consider adding it to TIMESPAN.
	private static final String CUTOFF = "__Cutoff"; //$NON-NLS-1$
	private static final CutoffFormatter CUTOFF_FORMATTER = new CutoffFormatter(UnitLookup.TIMESPAN, CUTOFF,
			"Human readable. Only showing one unit and integer values for seconds and below.", UnitLookup.SECOND); //$NON-NLS-1$

	private IQuantity timeRemaining;
	private IRecordingDescriptor recordingDescriptor;
	private MCFile dumpToFile;
	private final IServerHandle serverHandle;
	private boolean wasClosed = false;
	private boolean isOpening;

	private static class CutoffFormatter extends DualUnitFormatter {
		protected CutoffFormatter(LinearKindOfQuantity kindOfQuantity, String id, String name, LinearUnit cutoffUnit) {
			super(kindOfQuantity, id, name, cutoffUnit);
		}
	}

	RecordingProvider(IServerHandle serverHandle, IRecordingDescriptor recording) {
		this.serverHandle = serverHandle;
		recordingDescriptor = recording;
		dumpToFile = ControlPanel.getDefaultRecordingFile(serverHandle);
	}

	void updateRecording(IRecordingDescriptor recording, IConnectionHandle connectionHandle) {
		IQuantity timeRemaining = calculateTimeRemaining(recording, connectionHandle);
		synchronized (this) {
			recordingDescriptor = recording;
			this.timeRemaining = timeRemaining;
		}
	}

	private static IQuantity calculateTimeRemaining(
		IRecordingDescriptor recording, IConnectionHandle connectionHandle) {
		IQuantity startTime = recording.getStartTime();
		IQuantity duration = recording.getDuration();
		if ((startTime == null) || (duration == null)) {
			return null;
		}
		IQuantity serverTime = EPOCH_MS
				.quantity(getLocalTimeAsServerTime(System.currentTimeMillis(), connectionHandle));
		IQuantity remaining = startTime.add(duration).subtract(serverTime);
		return (remaining.longValue() >= 0) ? remaining : null;
	}

	private static long getLocalTimeAsServerTime(long localTime, IConnectionHandle connectionHandle) {
		// FIXME: JMC-4270 - Server time approximation is not reliable
//		IMBeanHelperService service = connectionHandle.getServiceOrNull(IMBeanHelperService.class);
//		return (service != null) ? service.getApproximateServerTime(localTime) : localTime;
		return localTime;
	}

	public IQuantity getEndTime() {
		IQuantity recStart = recordingDescriptor.getStartTime();
		IQuantity recDuration = recordingDescriptor.getDuration();
		IQuantity endTime = null;
		if (recStart != null && recDuration.compareTo(UnitLookup.SECOND.quantity(0)) > 0) {
			endTime = recStart.add(recDuration);
		} else {
			endTime = recordingDescriptor.getDataEndTime();
		}
		return endTime;
	}

	public synchronized IRecordingDescriptor getRecordingDescriptor() {
		return recordingDescriptor;
	}

	public synchronized IQuantity getTimeRemaining() {
		return timeRemaining;
	}

	public IServerHandle getServerHandle() {
		return serverHandle;
	}

	public synchronized void setClosed() {
		wasClosed = true;
	}

	public synchronized boolean wasClosed() {
		return wasClosed;
	}

	public synchronized void setDumpToFile(MCFile dumpToFile) {
		this.dumpToFile = dumpToFile;
	}

	public synchronized MCFile getDumpToFile() {
		return dumpToFile;
	}

	public synchronized void setIsOpening(boolean isOpening) {
		this.isOpening = isOpening;
	}

	private synchronized boolean isOpening() {
		return isOpening;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (getRecordingDescriptor().getState() == STOPPED || getRecordingDescriptor().getState() == CREATED) {
			return ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.ICON_FLIGHT_RECORDING_OFF);
		}
		return ControlPanel.getDefault().getMCImageDescriptor(ImageConstants.ICON_FLIGHT_RECORDING_ON);
	}

	@Override
	public String getName() {
		IRecordingDescriptor rd = getRecordingDescriptor();
		if (rd.getState() == RUNNING && rd.isContinuous()) {
			return rd.getName() + " (" + INFINITY + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (rd.getState() == STOPPED) {
			return rd.getName() + (isOpening() ? " (" + Messages.RECORDING_INFO_OPENING + ")" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (rd.getState() == CREATED) {
			return rd.getName() + " (" + (rd.isContinuous() ? DELAYED_CONTINUOUS : DELAYED_DURATION) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		IQuantity timeRemaining = getTimeRemaining();
		return rd.getName() + ((timeRemaining == null) ? "" : " (" + CUTOFF_FORMATTER.format(timeRemaining) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public String getDescription() {
		IRecordingDescriptor rd = getRecordingDescriptor();
		if (rd.getState() == RUNNING && rd.isContinuous()) {
			return NLS.bind(Messages.RECORDING_INFO_CONTINUOUS, rd.getStartTime().displayUsing(EXACT));
		}
		if (rd.getState() == STOPPED) {
			return NLS.bind(Messages.RECORDING_INFO_STOPPED,
					rd.getDataEndTime().subtract(rd.getDataStartTime()).displayUsing(AUTO));
		}
		if (rd.getState() == CREATED) {
			return rd.isContinuous() ? NLS.bind(Messages.RECORDING_INFO_CREATED_CONTINUOUS, rd.getStartTime())
					: NLS.bind(Messages.RECORDING_INFO_CREATED_DURATION, rd.getStartTime().displayUsing(EXACT),
							rd.getDuration().displayUsing(AUTO));
		}
		return NLS.bind(Messages.RECORDING_INFO_ONGOING, rd.getStartTime().displayUsing(EXACT));
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public Collection<? extends IActionProvider> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public List<? extends IUserAction> getActions() {
		List<IUserAction> actions = new ArrayList<>();
		if (isStarted()) {
			actions.add(new DumpRecordingAction(this));
			actions.add(new DumpWholeRecordingAction(this));
			actions.add(new DumpLastPartRecordingAction(this));
		}
		if (isAlive()) {
			actions.add(new EditRecordingAction(this));
			actions.add(new StopRecordingAction(this));
		}
		if (Environment.isDebug()) {
			actions.add(new PrintRecordingDescriptorAction(this));
		}
		actions.add(new CloseRecordingAction(this));
		return actions;
	}

	@Override
	public IUserAction getDefaultAction() {
		if (!isStarted()) {
			return new EditRecordingAction(this);
		} else if (FlightRecorderUI.getDefault().isSetLastPartToDump()) {
			return new DumpLastPartRecordingAction(this);
		} else if (FlightRecorderUI.getDefault().isSetDumpWhole()) {
			return new DumpWholeRecordingAction(this);
		} else {
			return new DumpRecordingAction(this);
		}
	}

	public boolean isAlive() {
		RecordingState state = getRecordingDescriptor().getState();
		return state == CREATED || state == RUNNING;
	}

	private boolean isStarted() {
		RecordingState state = getRecordingDescriptor().getState();
		return state == RUNNING || state == STOPPING || state == STOPPED;
	}

	public boolean isStopped() {
		RecordingState state = getRecordingDescriptor().getState();
		return state == STOPPED;
	}
}
