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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.spi.IConfigurationStorageDelegate;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;

/**
 * Storage delegate for templates that are saved to a private directory of the OSGi instance area.
 */
public class PrivateStorageDelegate implements IConfigurationStorageDelegate {
	private final File file;

	public static IConfigurationStorageDelegate getDelegate() throws IOException {
		File dir = ConfigurationRepositoryFactory.getCreatedStorageDir();
		File file = File.createTempFile("template-", IEventConfiguration.JFC_FILE_EXTENSION, dir); //$NON-NLS-1$
		return new PrivateStorageDelegate(file);
	}

	/**
	 * Only for internal use and by the {@link ConfigurationRepositoryFactory} when reading existing
	 * templates.
	 *
	 * @param file
	 */
	PrivateStorageDelegate(File file) {
		this.file = file;
	}

	@Override
	public InputStream getContents() {
		try {
			return file.exists() ? new FileInputStream(file) : null;
		} catch (FileNotFoundException e) {
			// Should not happen.
			return null;
		}
	}

	@Override
	public boolean isSaveable() {
		return true;
	}

	@Override
	public boolean save(String fileContent) throws IOException {
		try {
			// Ensure charset exists before opening file for writing.
			Charset charset = Charset.forName(CHARSET_UTF8);
			Writer out = new OutputStreamWriter(new FileOutputStream(file), charset);
			try {
				out.write(fileContent);
				out.flush();
			} finally {
				IOToolkit.closeSilently(out);
			}
			return true;
		} catch (IllegalCharsetNameException e) {
			return false;
		} catch (FileNotFoundException e) {
			return false;
		}
	}

	@Override
	public boolean isDeletable() {
		return true;
	}

	@Override
	public boolean delete() {
		return file.delete();
	}

	@Override
	public String getLocationInfo() {
		return file.exists() ? null : Messages.CONFIG_DELETED;
	}

	@Override
	public String getLocationPath() {
		return file != null ? file.getAbsolutePath() : null;
	}
}
