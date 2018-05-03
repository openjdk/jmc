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
package org.openjdk.jmc.flightrecorder.configuration.events;

import java.io.File;
import java.io.IOException;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.spi.IConfigurationStorageDelegate;

/**
 * Interface for JDK Flight Recorder configurations. Note that this interface is (and should
 * remain) agnostic as to how the configuration is represented.
 */
public interface IEventConfiguration {
	String JFC_FILE_EXTENSION = ".jfc"; //$NON-NLS-1$

	/**
	 * @return the name of this configuration, as shown when selecting among configuration
	 *         templates.
	 */
	String getName();

	/**
	 * Set the name of this configuration, as shown when selecting among configuration templates.
	 *
	 * @param name
	 */
	void setName(String name);

	/**
	 * @return the description of this configuration, as shown when selecting among configuration
	 *         templates.
	 */
	String getDescription();

	/**
	 * Set the description of this configuration, as shown when selecting among configuration
	 * templates.
	 *
	 * @param description
	 */
	void setDescription(String description);

	/**
	 * @return a description of where this configuration is stored, or null if this is unimportant
	 *         to the user.
	 */
	String getLocationInfo();

	/**
	 * Whether this particular configuration offers some kind of simplified, higher level control
	 * over what to include in the recording, compared to setting individual low level options.
	 *
	 * @return true iff the configuration has some kind of simplified control mechanism.
	 */
	boolean hasControlElements();

	/**
	 * Remove all kinds of simplified, higher level controls over what to include in the recording,
	 * compared to setting individual low level options. This is typically called when the
	 * configuration is about to be modified by the user at the low level, and higher level controls
	 * could interfere with that.
	 *
	 * @return true iff the configuration had some kind of simplified control mechanism.
	 */
	boolean removeControlElements();

	/**
	 * If this configuration can likely be deleted permanently, that is, beyond the life cycle of
	 * this instance and any repositories containing it. Note that returning true here does not
	 * guarantee that a {@link #delete()} will succeed, only that it can be attempted.
	 *
	 * @return
	 */
	boolean isDeletable();

	/**
	 * Attempt to delete this configuration permanently, that is, beyond the life cycle of this
	 * instance and any repositories containing it.
	 *
	 * @return true iff this configuration was deleted permanently.
	 */
	boolean delete();

	/**
	 * If this configuration can likely be saved permanently, that is, beyond the life cycle of this
	 * instance and any repositories containing it. This method can be used to enable or disable
	 * save options. Note that returning true here does not guarantee that a {@link #save()} will
	 * succeed, only that it can be attempted.
	 *
	 * @return
	 */
	boolean isSaveable();

	/**
	 * Save this configuration permanently, that is, beyond the life cycle of this instance and any
	 * repositories containing it.
	 *
	 * @return true iff this configuration was saved permanently.
	 */
	boolean save();

	/**
	 * If this configuration can be cloned, and it makes sense to do it.
	 *
	 * @return
	 */
	boolean isCloneable();

	/**
	 * Warning, this creates a clone with the same underlying storage. This might not be what you
	 * want.
	 *
	 * @return a clone using the same storage as this configuration
	 * @see #createCloneWithStorage(IConfigurationStorageDelegate)
	 */
	IEventConfiguration createClone();

	/**
	 * Create a clone using the specified underlying storage.
	 *
	 * @param storageDelegate
	 * @return a clone using {@code storageDelegate} as underlying storage.
	 */
	IEventConfiguration createCloneWithStorage(IConfigurationStorageDelegate storageDelegate);

	/**
	 * Create a working copy of this configuration, with no backing storage. The returned
	 * configuration will return this configuration from its {@link #getOriginal()} method.
	 *
	 * @return a new {@link IEventConfiguration}
	 */
	IEventConfiguration createWorkingCopy();

	/**
	 * If this is a working copy configuration, return the original configuration, otherwise return
	 * null.
	 *
	 * @return a {@link IEventConfiguration} or null
	 */
	IEventConfiguration getOriginal();

	/**
	 * Check if the settings held by this configuration is the same as those held by {@code other}.
	 * The textual representation doesn't matter, nor does control elements.
	 *
	 * @param other
	 * @return true if the settings are the same, false otherwise
	 */
	boolean equalSettings(IEventConfiguration other);

	IConstrainedMap<EventOptionID> getEventOptions(IMutableConstrainedMap<EventOptionID> options);

	/**
	 * Get the persistable string for the option specified by {@code optionID}. If the option is
	 * unset, {@code null} will be returned.
	 *
	 * @param optionID
	 */
	String getPersistableString(EventOptionID optionID);

	/**
	 * Set the option specified by {@code optionID} to the given persisted value.
	 *
	 * @param optionID
	 * @param persisted
	 */
	void putPersistedString(EventOptionID optionID, String persisted);

	/**
	 * If this configuration can be exported, and it makes sense to do it.
	 *
	 * @return
	 */
	boolean isExportable();

	/**
	 * Export this configuration to the given file.
	 *
	 * @param file
	 *            the destination file.
	 * @throws IOException
	 *             if there were trouble writing to {@code file}.
	 */
	void exportToFile(File file) throws IOException;

	/**
	 * The schema version for this configuration
	 *
	 * @return schema version
	 */
	SchemaVersion getVersion();

	/**
	 * Gets the location of the underlying storage delegate, see
	 * {@link IConfigurationStorageDelegate#getLocationPath()}.
	 *
	 * @return a string that represents the file system location of this template, or null.
	 */
	String getLocationPath();
}
