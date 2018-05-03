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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Comparator;
import java.util.Locale;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.ui.common.xydata.DefaultTimestampedData;
import org.openjdk.jmc.ui.common.xydata.ITimestampedData;

class PersistenceFile {

	static final String FILE_EXT = ".persisted_jmx_data"; //$NON-NLS-1$

	public static final FilenameFilter FILTER = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase(Locale.ENGLISH).endsWith(FILE_EXT);
		}
	};

	public static final Comparator<PersistenceFile> PERSISTENCE_FILE_START_COMPARATOR = new Comparator<PersistenceFile>() {
		@Override
		public int compare(PersistenceFile o1, PersistenceFile o2) {
			return (o1.start < o2.start) ? -1 : ((o1.start > o2.start) ? 1 : 0);
		}
	};

	static final int EVENT_SIZE = 8 + 8;
	final File file;
	ITimestampedData[] events;
	final long eventsStart;
	final int eventCount;
	final long start;
	final long end;
	final MRI mri;
	final long fileLen;

	PersistenceFile(File file) throws IOException {
		this.file = file;
		RandomAccessFile raf = new RandomAccessFile(file, "r"); //$NON-NLS-1$
		try {
			mri = MRI.createFromQualifiedName(raf.readUTF());
			fileLen = raf.length();
			eventsStart = raf.getFilePointer();
			eventCount = (int) ((fileLen - eventsStart) / EVENT_SIZE);
			if (eventCount > 0) {
				start = raf.readLong();
				raf.seek(eventsStart + (long) (eventCount - 1) * EVENT_SIZE);
				end = raf.readLong();
			} else {
				start = Long.MAX_VALUE;
				end = Long.MAX_VALUE;
			}
		} finally {
			IOToolkit.closeSilently(raf);
		}
	}

	boolean isCorrupt() {
		return eventsStart + (long) eventCount * EVENT_SIZE != fileLen;
	}

	synchronized ITimestampedData[] getEvents(long min, long max) throws IOException {
		if (events == null) {
			// TODO: For now read all data
			events = new ITimestampedData[eventCount];
			RandomAccessFile raf = new RandomAccessFile(file, "r"); //$NON-NLS-1$
			try {
				readEvents(raf, 0, eventCount);
			} finally {
				IOToolkit.closeSilently(raf);
			}
		}
		return events;
	}

	MRI getMRI() {
		return mri;
	}

	private void readEvents(RandomAccessFile raf, int index, int count) throws IOException {
		byte[] data = new byte[count * EVENT_SIZE];
		raf.seek(eventsStart + (long) index * EVENT_SIZE);
		raf.readFully(data);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
		for (int i = 0; i < count; i++) {
			events[index + i] = new DefaultTimestampedData(dis.readLong(), dis.readDouble());
		}
	}
}
