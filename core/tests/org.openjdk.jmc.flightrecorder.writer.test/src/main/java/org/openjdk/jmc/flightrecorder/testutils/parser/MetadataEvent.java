/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.testutils.parser;

import org.jctools.maps.NonBlockingHashMapLong;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JFR Chunk metadata
 * <p>
 * It contains the chunk specific type specifications
 */
public final class MetadataEvent {
	private static final byte[] COMMON_BUFFER = new byte[4096]; // reusable byte buffer

	public final int size;
	public final long startTime;
	public final long duration;
	public final long metadataId;

	private final NonBlockingHashMapLong<String> eventTypeNameMapBacking = new NonBlockingHashMapLong<>(256);
	private final LongMapping<String> eventTypeMap;

	MetadataEvent(RecordingStream stream) throws IOException {
		size = (int) stream.readVarint();
		long typeId = stream.readVarint();
		if (typeId != 0) {
			throw new IOException("Unexpected event type: " + typeId + " (should be 0)");
		}
		startTime = stream.readVarint();
		duration = stream.readVarint();
		metadataId = stream.readVarint();
		readElements(stream, readStringTable(stream));
		eventTypeMap = eventTypeNameMapBacking::get;
	}

	/**
	 * Lazily compute and return the mappings of event type ids to event type names
	 *
	 * @return mappings of event type ids to event type names
	 */
	public LongMapping<String> getEventTypeNameMap() {
		return eventTypeMap;
	}

	private String[] readStringTable(RecordingStream stream) throws IOException {
		int stringCnt = (int) stream.readVarint();
		String[] stringConstants = new String[stringCnt];
		for (int stringIdx = 0; stringIdx < stringCnt; stringIdx++) {
			stringConstants[stringIdx] = readUTF8(stream);
		}
		return stringConstants;
	}

	private void readElements(RecordingStream stream, String[] stringConstants) throws IOException {
		// get the element name
		int stringPtr = (int) stream.readVarint();
		boolean isClassElement = "class".equals(stringConstants[stringPtr]);

		// process the attributes
		int attrCount = (int) stream.readVarint();
		String superType = null;
		String name = null;
		String id = null;
		for (int i = 0; i < attrCount; i++) {
			int keyPtr = (int) stream.readVarint();
			int valPtr = (int) stream.readVarint();
			// ignore anything but 'class' elements
			if (isClassElement) {
				if ("superType".equals(stringConstants[keyPtr])) {
					superType = stringConstants[valPtr];
				} else if ("name".equals(stringConstants[keyPtr])) {
					name = stringConstants[valPtr];
				} else if ("id".equals(stringConstants[keyPtr])) {
					id = stringConstants[valPtr];
				}
			}
		}
		// only event types are currently collected
		if (name != null && id != null && "jdk.jfr.Event".equals(superType)) {
			eventTypeNameMapBacking.put(Long.parseLong(id), name);
		}
		// now inspect all the enclosed elements
		int elemCount = (int) stream.readVarint();
		for (int i = 0; i < elemCount; i++) {
			readElements(stream, stringConstants);
		}
	}

	private String readUTF8(RecordingStream stream) throws IOException {
		byte id = stream.read();
		if (id == 0) {
			return null;
		} else if (id == 1) {
			return "";
		} else if (id == 3) {
			int size = (int) stream.readVarint();
			byte[] content = size <= COMMON_BUFFER.length ? COMMON_BUFFER : new byte[size];
			stream.read(content, 0, size);
			return new String(content, 0, size, StandardCharsets.UTF_8);
		} else if (id == 4) {
			int size = (int) stream.readVarint();
			char[] chars = new char[size];
			for (int i = 0; i < size; i++) {
				chars[i] = (char) stream.readVarint();
			}
			return new String(chars);
		} else {
			throw new IOException("Unexpected string constant id: " + id);
		}
	}

	@Override
	public String toString() {
		return "Metadata{" + "size=" + size + ", startTime=" + startTime + ", duration=" + duration + ", metadataId="
				+ metadataId + '}';
	}
}
