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
package org.openjdk.jmc.rjmx.services.jfr.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.management.openmbean.OpenDataException;

import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

/**
 * Class representing an {@link InputStream} from an IRecordingDescriptor between two dates.
 */
// FIXME: If the invoke operations are folded into the IFlightRecorderService implementations then we can avoid having two versions of this stream class
public final class JfrRecordingInputStreamV2 extends InputStream {
	private final static String OPEN_STREAM = "openStream"; //$NON-NLS-1$
	private final static String READ_STREAM = "readStream"; //$NON-NLS-1$
	private final static String CLOSE_STREAM = "closeStream"; //$NON-NLS-1$
	// FIXME: This seems to have no advantage over using null. Remove!
	private final static Long UNKNOWN_STREAM = Long.valueOf(-1L);

	private final IRecordingDescriptor recording;
	private final IFlightRecorderCommunicationHelper helper;

	private byte[] buf = new byte[0];
	private int count = 0;
	private int pos = 0;
	private boolean closed = false;
	private boolean endOfStream = false;
	private boolean removeOnClose = true;
	private final Date startTime;
	private final Date endTime;
	private Long streamIdentifier = UNKNOWN_STREAM;

	public JfrRecordingInputStreamV2(IFlightRecorderCommunicationHelper helper, IRecordingDescriptor recording,
			Date startTime, Date endTime, boolean removeOnClose) {
		this.recording = recording;
		this.helper = helper;
		this.startTime = startTime;
		this.endTime = endTime;
		this.removeOnClose = removeOnClose;
		FlightRecorderServiceV1.LOGGER.log(Level.INFO, "Attempting to open stream from " + recording + " between " //$NON-NLS-1$ //$NON-NLS-2$
				+ startTime + " to " + endTime); //$NON-NLS-1$

	}

	public JfrRecordingInputStreamV2(IFlightRecorderCommunicationHelper helper, IRecordingDescriptor recording,
			boolean removeOnClose) {
		this(helper, recording, null, null, removeOnClose);
	}

	@Override
	public synchronized int read() throws IOException {
		if (pos >= buf.length) {
			if (closed || endOfStream) {
				return -1;
			}
			fill();
			if (endOfStream) {
				return -1;
			}
		}
		return buf[pos++] & 0xff;
	}

	private void fill() throws IOException {
		if (UNKNOWN_STREAM.equals(streamIdentifier)) {
			readStreamIdentifier();
		}
		buf = readStream(streamIdentifier);
		if (buf != null) {
			count += buf.length;
			pos = 0;
		} else {
			pos = 0;
			count = 0;
			buf = new byte[0];
			endOfStream = true;
		}
	}

	private void readStreamIdentifier() throws IOException {
		if (startTime == null) {
			streamIdentifier = openStream(recording);
		} else {
			streamIdentifier = openStream(recording, startTime, endTime);
		}
	}

	private void ensureOpen() throws IOException {
		if (closed) {
			throw new IOException("Stream closed"); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized int available() throws IOException {
		ensureOpen();
		return count - pos;
	}

	@Override
	public void close() throws IOException {
		if (closed == true) {
			return;
		}
		closed = true;
		if (!UNKNOWN_STREAM.equals(streamIdentifier)) {
			closeStream(streamIdentifier);
		}
		if (removeOnClose) {
			try {
				helper.closeRecording(recording);
			} catch (FlightRecorderException e) {
				IOException ioe = new IOException(e.getMessage());
				ioe.initCause(e);
				throw ioe;
			}
		}
	}

	private void closeStream(Long streamIdentifier) throws IOException {
		invokeOperation(CLOSE_STREAM, streamIdentifier);
	}

	private byte[] readStream(Long streamIdentifier) throws IOException {
		return (byte[]) invokeOperation(READ_STREAM, streamIdentifier);
	}

	private Long openStream(IRecordingDescriptor descriptor) throws IOException {
		return openStream(descriptor, null, null);
	}

	private Long openStream(IRecordingDescriptor descriptor, Date startTime, Date endTime) throws IOException {
		// FIXME: Replace with suitable IConstrainedMap.
		Map<String, String> options = new HashMap<>();
		if (startTime != null) {
			options.put("startTime", Long.toString(startTime.getTime())); //$NON-NLS-1$
		}
		if (endTime != null) {
			options.put("endTime", Long.toString(endTime.getTime())); //$NON-NLS-1$
		}
		Long streamId;
		try {
			streamId = (Long) invokeOperation(OPEN_STREAM, descriptor.getId(),
					RecordingOptionsToolkitV2.createTabularData(options));
		} catch (OpenDataException e) {
			throw new IOException(e);
		}
		if (streamId == null) {
			return UNKNOWN_STREAM;
		}
		return streamId;
	}

	/**
	 * If a method invocation fails, we will close the stream immediately.
	 */
	private Object invokeOperation(String name, Object ... params) throws IOException {
		try {
			return helper.invokeOperation(name, params);
		} catch (Exception ioe) {
			FlightRecorderServiceV1.LOGGER.info("Failed to invoke operation " + name + ". Will now close! Message was: " //$NON-NLS-1$ //$NON-NLS-2$
					+ ioe.getMessage());
			try {
				close();
			} catch (IOException ioe2) {
				// Don't care;
			}
			IOException iot = new IOException(ioe.getLocalizedMessage());
			iot.initCause(ioe);
			throw iot;
		}
	}
}
