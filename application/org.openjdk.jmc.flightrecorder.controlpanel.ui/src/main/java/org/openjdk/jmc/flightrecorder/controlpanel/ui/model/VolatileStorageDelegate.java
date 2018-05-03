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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.model;

import java.io.InputStream;

import org.openjdk.jmc.flightrecorder.configuration.spi.IConfigurationStorageDelegate;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;

/**
 * Storage delegate for templates that cannot be saved back to where they came from. In other words,
 * read only templates.
 */
public class VolatileStorageDelegate implements IConfigurationStorageDelegate {
	private static VolatileStorageDelegate LAST_STARTED = new VolatileStorageDelegate(
			Messages.VOLATILE_CONFIGURATION_LAST_STARTED, false);
	private static VolatileStorageDelegate ON_SERVER = new VolatileStorageDelegate(
			Messages.VOLATILE_CONFIGURATION_ON_SERVER, false);
	private static VolatileStorageDelegate RUNNING_RECORDING = new VolatileStorageDelegate(
			Messages.VOLATILE_CONFIGURATION_RUNNING_RECORDING, false);
	private static VolatileStorageDelegate WORKING_COPY = new VolatileStorageDelegate(
			Messages.VOLATILE_CONFIGURATION_WORKING_COPY, true);

	private final String locationInfo;
	private final boolean deleteable;

	public static IConfigurationStorageDelegate getLastStartedDelegate() {
		return LAST_STARTED;
	}

	public static IConfigurationStorageDelegate getOnServerDelegate() {
		return ON_SERVER;
	}

	public static IConfigurationStorageDelegate getRunningRecordingDelegate() {
		return RUNNING_RECORDING;
	}

	public static IConfigurationStorageDelegate getWorkingCopyDelegate() {
		return WORKING_COPY;
	}

	private VolatileStorageDelegate(String locationInfo, boolean deleteable) {
		this.locationInfo = locationInfo;
		this.deleteable = deleteable;
	}

	@Override
	public InputStream getContents() {
		return null;
	}

	@Override
	public boolean isSaveable() {
		return false;
	}

	@Override
	public boolean save(String fileContent) {
		return false;
	}

	@Override
	public boolean isDeletable() {
		return deleteable;
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public String getLocationInfo() {
		return locationInfo;
	}

	@Override
	public String getLocationPath() {
		return null;
	}
}
