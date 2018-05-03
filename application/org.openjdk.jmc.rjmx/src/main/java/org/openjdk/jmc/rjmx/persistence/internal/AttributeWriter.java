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
package org.openjdk.jmc.rjmx.persistence.internal;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.subscription.IMRIValueListener;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;

class AttributeWriter implements IMRIValueListener {

	static final String SERIES_FILE_NAME = "series.info"; //$NON-NLS-1$
	private long currentFileSize;
	private DataOutputStream currentFileStream;
	private File currentFile;
	private File dir;
	private final MRI mri;
	private boolean isEnabled;
	private Boolean isRunning;
	private long maxFileSize;

	AttributeWriter(MRI mri, File persistenceDir, long maxFileSize) {
		this.mri = mri;
		setMaxFileSize(maxFileSize);
		setPersistenceDir(persistenceDir);
	}

	synchronized void setMaxFileSize(long maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	synchronized void setPersistenceDir(File persistenceDir) {
		persistenceDir = new File(persistenceDir, StringToolkit.encodeFilename(mri.getQualifiedName()));
		if (!persistenceDir.equals(dir)) {
			// Directory changed
			dir = persistenceDir;
			IOToolkit.closeSilently(currentFileStream);
			currentFileStream = null;
			currentFile = null;
			if (dir.isDirectory()) {
				ArrayList<PersistenceFile> existingFiles = new ArrayList<>();
				File[] listFiles = dir.listFiles(PersistenceFile.FILTER);
				if (listFiles != null) {
					for (File f : listFiles) {
						try {
							PersistenceFile pfr = new PersistenceFile(f);
							if (!pfr.isCorrupt()) {
								existingFiles.add(pfr);
							}
						} catch (Exception e) {
							// Ignore invalid files
						}
					}
				}
				Collections.sort(existingFiles, PersistenceFile.PERSISTENCE_FILE_START_COMPARATOR);
				if (existingFiles.size() > 0) {
					currentFile = existingFiles.get(existingFiles.size() - 1).file;
					currentFileSize = currentFile.length();
				}
			}
			if (!Boolean.FALSE.equals(isRunning)) {
				isRunning = null;
			}
		}
	}

	synchronized void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	synchronized boolean isEnabled() {
		return isEnabled;
	}

	synchronized void start() {
		isRunning = null;
	}

	synchronized void stop() {
		isRunning = false;
		IOToolkit.closeSilently(currentFileStream);
		currentFileStream = null;
	}

	@Override
	public void valueChanged(MRIValueEvent event) {
		Object value = event.getValue();
		if (value instanceof Number) {
			write(event.getTimestamp() * 1000 * 1000L, ((Number) event.getValue()).doubleValue());
		} else if (value != null) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"Can not write value of type " + value.getClass().getCanonicalName()); //$NON-NLS-1$
		}
	}

	private synchronized void write(long timestamp, double value) {
		if (isEnabled && !Boolean.FALSE.equals(isRunning)) {
			if (isRunning == null) {
				if (dir.isDirectory() || dir.mkdirs()) {
					writeSeriesStart(timestamp);
					isRunning = true;
				} else {
					RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
							"Could not create directory " + dir.getPath()); //$NON-NLS-1$
					return;
				}
			}
			try {
				ensureWriter();
				doWrite(timestamp, value);
			} catch (IOException e) {
				try {
					createNewWriter();
					doWrite(timestamp, value);
				} catch (IOException e1) {
					IOToolkit.closeSilently(currentFileStream);
					currentFileStream = null;
					currentFile = null;
					RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Failed to write event", e1); //$NON-NLS-1$
				}
			}
		}
	}

	private void writeSeriesStart(long timestamp) {
		DataOutputStream seriesFile = null;
		try {
			seriesFile = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(dir, SERIES_FILE_NAME), true)));
			seriesFile.writeLong(timestamp);
		} catch (IOException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Failed to write series start", e); //$NON-NLS-1$
		} finally {
			IOToolkit.closeSilently(seriesFile);
		}
	}

	private void doWrite(long timestamp, double value) throws IOException {
		currentFileStream.writeLong(timestamp);
		currentFileStream.writeDouble(value);
		currentFileSize += PersistenceFile.EVENT_SIZE;
	}

	private void ensureWriter() throws IOException {
		if (currentFileSize + PersistenceFile.EVENT_SIZE > maxFileSize) {
			createNewWriter();
		} else if (currentFileStream == null) {
			if (currentFile != null) {
				currentFileStream = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(currentFile, true)));
			} else {
				createNewWriter();
			}
		}
	}

	private void createNewWriter() throws IOException {
		IOToolkit.closeSilently(currentFileStream);
		currentFileStream = null;
		File file;
		do {
			String fileName = "values_" + System.currentTimeMillis() + PersistenceFile.FILE_EXT; //$NON-NLS-1$
			file = new File(dir, fileName);
		} while (file.exists());
		currentFileStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
		currentFileStream.writeUTF(mri.getQualifiedName());
		currentFileStream.flush();
		currentFileSize = file.length();
		currentFile = file;
	}
}
