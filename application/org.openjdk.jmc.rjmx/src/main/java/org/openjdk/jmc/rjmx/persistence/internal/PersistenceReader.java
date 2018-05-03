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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.rjmx.services.IAttributeStorage;
import org.openjdk.jmc.rjmx.services.IAttributeStorageService;
import org.openjdk.jmc.rjmx.services.MRIDataSeries;
import org.openjdk.jmc.rjmx.subscription.IMRIService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;

public class PersistenceReader implements IMRIService, IAttributeStorageService {

	private static class FileIterator implements Iterator<ITimestampedData> {
		int nextIndex;
		ITimestampedData[] currentFileData;
		Iterator<PersistenceFile> files;
		long min;
		long max;
		ITimestampedData next;

		FileIterator(Iterator<PersistenceFile> data, long min, long max) {
			files = data;
			this.min = min;
			this.max = max;
			if (max > min) {
				binarySearchFirst();
			}
		}

		private void binarySearchFirst() {
			if (findNextFile()) {
				int highBound = currentFileData.length - 1;
				while (highBound >= nextIndex) {
					int middleIndex = (nextIndex + highBound) >>> 1;
					long middleX = currentFileData[middleIndex].getX();
					if (middleX == min) {
						nextIndex = middleIndex;
						break;
					}
					if (middleX < min) {
						nextIndex = middleIndex + 1;
					} else {
						highBound = middleIndex - 1;
					}
				}
				readNext();
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public ITimestampedData next() {
			if (!hasNext()) {
				throw new IllegalStateException();
			}
			ITimestampedData tmp = next;
			if (currentFileData != null && nextIndex < currentFileData.length || findNextFile()) {
				readNext();
			} else {
				next = null;
			}
			return tmp;
		}

		private void readNext() {
			next = currentFileData[nextIndex++];
			next = next.getX() < max ? next : null;
		}

		private boolean findNextFile() {
			currentFileData = null;
			while (files.hasNext()) {
				PersistenceFile file = files.next();
				if (file.end >= min) {
					if (file.start >= max) {
						return false;
					}
					try {
						currentFileData = file.getEvents(min, max);
						if (currentFileData.length > 0) {
							nextIndex = 0;
							return true;
						}
					} catch (IOException e) {
						RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not read events from file", e); //$NON-NLS-1$
					}
				}
			}
			return false;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static class PersistentMRIDataSeries implements MRIDataSeries {
		List<PersistenceFile> files;
		MRI mri;
		long min;
		long max;

		PersistentMRIDataSeries(MRI mri, List<PersistenceFile> files, long min, long max) {
			this.files = files;
			this.mri = mri;
			this.min = min;
			this.max = max;
		}

		@Override
		public Iterator<ITimestampedData> createIterator(long min, long max) {
			if (this.min <= max && this.max >= min) {
				return new FileIterator(files.iterator(), this.min, this.max);
			} else {
				return Collections.<ITimestampedData> emptyList().iterator();
			}
		}

		@Override
		public MRI getAttribute() {
			return mri;
		}
	}

	private static class PersistenceStorage implements IAttributeStorage {
		List<PersistentMRIDataSeries> series = new ArrayList<>();

		@Override
		public void addObserver(Observer o) {
		}

		@Override
		public void deleteObserver(Observer o) {
		}

		@Override
		public List<PersistentMRIDataSeries> getDataSeries() {
			return series;
		}

		@Override
		public long getDataStart() {
			if (series.size() > 0) {
				List<PersistenceFile> files = series.get(0).files;
				if (files.size() > 0) {
					return files.get(0).start;
				}
			}
			return Long.MAX_VALUE;
		}

		@Override
		public long getDataEnd() {
			if (series.size() > 0) {
				List<PersistenceFile> files = series.get(series.size() - 1).files;
				if (files.size() > 0) {
					return files.get(files.size() - 1).end;
				}
			}
			return Long.MIN_VALUE;
		}
	}

	private final Map<MRI, PersistenceStorage> storages = new HashMap<>();

	/**
	 * @param directory
	 *            A directory to read data from. If null then the directory set in preferences is
	 *            used.
	 * @param uid
	 *            A uid string that determines which subdirectory to use. If null then the specified
	 *            directory is used instead of a subdirectory.
	 */
	public PersistenceReader(File directory, String uid) {
		if (directory == null) {
			directory = getPersistenceDirectoryPreference();
		}
		if (uid != null) {
			directory = new File(directory, StringToolkit.encodeFilename(uid));
		}
		if (directory.isDirectory()) {
			File[] pFiles = directory.listFiles();
			if (pFiles != null) {
				for (File attributeDir : pFiles) {
					extractFromAttributeDir(attributeDir);
				}
			}
		}
	}

	private void extractFromAttributeDir(File attributeDir) {
		if (!attributeDir.isDirectory()) {
			return;
		}
		MRI mri;
		try {
			mri = MRI.createFromQualifiedName(URLDecoder.decode(attributeDir.getName(), "UTF-8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			// This should never happen
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			// Log warning and ignore directory
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"Could not create MRI from directory name " + attributeDir.getName(), e); //$NON-NLS-1$
			return;
		}
		ArrayList<PersistenceFile> files = new ArrayList<>();
		File[] listFiles = attributeDir.listFiles(PersistenceFile.FILTER);
		if (listFiles != null) {
			for (File f : listFiles) {
				try {
					PersistenceFile pf = new PersistenceFile(f);
					if (pf.eventCount > 0) {
						files.add(pf);
					}
				} catch (Exception e) {
					// Log warning and ignore file
					RJMXPlugin.getDefault().getLogger().log(Level.WARNING, "Could not add file " + f.getPath(), e); //$NON-NLS-1$
				}
			}
		}
		Collections.sort(files, PersistenceFile.PERSISTENCE_FILE_START_COMPARATOR);
		List<Long> seriesStart = new ArrayList<>();
		DataInputStream seriesFileStream = null;
		File seriesFile = new File(attributeDir, AttributeWriter.SERIES_FILE_NAME);
		try {
			seriesFileStream = new DataInputStream(new BufferedInputStream(new FileInputStream(seriesFile)));
			while (seriesFileStream.available() > 0) {
				seriesStart.add(seriesFileStream.readLong());
			}
		} catch (IOException e) {
			// Log warning and ignore file
			RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
					"Error while reading persisted data from file " + seriesFile.getPath(), e); //$NON-NLS-1$
			return;
		} finally {
			IOToolkit.closeSilently(seriesFileStream);
		}
		PersistenceStorage storage = new PersistenceStorage();
		if (seriesStart.isEmpty()) {
			storage.series.add(new PersistentMRIDataSeries(mri, files, Long.MIN_VALUE, Long.MAX_VALUE));
		} else {
			for (int i = 0; i < seriesStart.size(); i++) {
				long seriesEnd = i + 1 < seriesStart.size() ? seriesStart.get(i + 1) : Long.MAX_VALUE;
				storage.series.add(new PersistentMRIDataSeries(mri, files, seriesStart.get(i), seriesEnd));
			}
		}
		storages.put(mri, storage);
	}

	private static File getPersistenceDirectoryPreference() {
		File persistenceDirectory = new File(RJMXPlugin.getDefault().getRJMXPreferences()
				.get(PreferencesKeys.PROPERTY_PERSISTENCE_DIRECTORY, PreferencesKeys.DEFAULT_PERSISTENCE_DIRECTORY));
		return persistenceDirectory;
	}

	@Override
	public Set<MRI> getMRIs() {
		return storages.keySet();
	}

	@Override
	public boolean isMRIAvailable(MRI mri) {
		return true;
	}

	@Override
	public IAttributeStorage getAttributeStorage(MRI attributeDescriptor) {
		return storages.get(attributeDescriptor);
	}

	@Override
	public int getRetainedLength(MRI attributeDescriptor) {
		// Not interesting for persisted data.
		return 0;
	}
}
