/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.joverflow.heap.model;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;

/**
 * Reads object labels through a private heap dump buffer, independently of the snapshot's lazy
 * readers. Instances are not thread safe and should be closed when no longer needed.
 */
public final class HeapObjectReader implements AutoCloseable {
	private static final int MAX_NAMED_REFERENCES = 256;
	private static final int MAX_INSTANCE_BYTES = 64 * 1024;
	private static final int MAX_REFERENCE_LABEL_LENGTH = 150;
	private final Snapshot snapshot;
	private final ReadBuffer.Factory factory;
	private final int idSize;
	private final byte[] numberBytes = new byte[8];
	private ReadBuffer buffer;
	private boolean closed;

	/**
	 * @param factory
	 *            opens an owned buffer for the same heap dump on first read; must support
	 *            {@code create(null)}
	 */
	public HeapObjectReader(Snapshot snapshot, ReadBuffer.Factory factory) {
		this.snapshot = Objects.requireNonNull(snapshot);
		this.factory = Objects.requireNonNull(factory);
		idSize = snapshot.getHprofPointerSize();
	}

	public long readId(JavaHeapObject object) throws IOException {
		checkOpen();
		if (object instanceof JavaClass) {
			return object.readId(); // Class IDs are already in memory.
		}
		return readNumber(((JavaLazyReadObject) object).getObjOfsInFile(), idSize);
	}

	/**
	 * Returns names of verified direct references, keyed by the target's global object index.
	 * Examines at most 256 reference fields, static fields or array elements. Arrays larger than
	 * 256 elements and instances with more than 64 KiB of field data are skipped entirely.
	 */
	public Map<Integer, String> readReferenceNames(JavaHeapObject object) throws IOException {
		checkOpen();
		Map<Integer, String> names = new LinkedHashMap<>();
		if (object instanceof JavaObjectArray) {
			JavaObjectArray array = (JavaObjectArray) object;
			int length = array.getLength();
			if (length < 0 || length > MAX_NAMED_REFERENCES) {
				return Collections.emptyMap();
			}
			long offset = array.getObjOfsInFile() + 2L * idSize + 8;
			for (int i = 0; i < length; i++) {
				addReferenceName(names,
						snapshot.getGlobalObjectIndexForId(readNumber(offset + (long) i * idSize, idSize)),
						"[" + i + "]");
			}
		} else if (object instanceof JavaObject) {
			JavaObject instance = (JavaObject) object;
			JavaClass clazz = instance.getClazz();
			int length = clazz.getFieldsSizeInFile();
			if (length < 0 || length > MAX_INSTANCE_BYTES) {
				return Collections.emptyMap();
			}
			int offset = 0;
			int references = 0;
			long fields = instance.getObjOfsInFile() + 2L * idSize + 8;
			for (JavaClass current = clazz; current != null; current = current.getSuperclass()) {
				for (JavaField field : current.getDefinedFields()) {
					int size = field.isReference() ? idSize : field.getSizeInInstance();
					if (size <= 0 || offset > length - size) {
						return Collections.unmodifiableMap(names);
					}
					if (field.isReference()) {
						addReferenceName(names, snapshot.getGlobalObjectIndexForId(readNumber(fields + offset, idSize)),
								field.getName());
						if (++references == MAX_NAMED_REFERENCES) {
							return Collections.unmodifiableMap(names);
						}
					}
					offset += size;
				}
			}
		} else if (object instanceof JavaClass) {
			JavaClass clazz = (JavaClass) object;
			JavaField[] fields = clazz.getStaticFields();
			JavaThing[] values = clazz.getStaticValues();
			for (int i = 0, count = Math.min(MAX_NAMED_REFERENCES, fields.length); i < count; i++) {
				if (fields[i].isReference() && values[i] instanceof JavaHeapObject) {
					addReferenceName(names, ((JavaHeapObject) values[i]).getGlobalObjectIndex(), fields[i].getName());
				}
			}
		}
		return Collections.unmodifiableMap(names);
	}

	private void addReferenceName(Map<Integer, String> names, int target, String name) {
		if (target == Snapshot.NO_GLOBAL_OBJECT_INDEX) {
			return;
		}
		String previous = names.get(target);
		if (previous != null) {
			if (previous.length() >= MAX_REFERENCE_LABEL_LENGTH) {
				return;
			}
			name = previous + ", " + name;
		}
		names.put(target, name.length() <= MAX_REFERENCE_LABEL_LENGTH ? name
				: name.substring(0, MAX_REFERENCE_LABEL_LENGTH - 1) + '\u2026');
	}

	/**
	 * Reads at most {@code maxChars} UTF-16 code units, appending an ellipsis if truncated. Returns
	 * unquoted text, or null for missing arrays or unsupported layouts.
	 */
	public String readString(JavaObject object, int maxChars) throws IOException {
		checkOpen();
		if (maxChars < 0) {
			throw new IllegalArgumentException("Negative String preview length: " + maxChars);
		}
		JavaClass stringClass = snapshot.getClassForName("java.lang.String");
		if (object.getClazz() != stringClass) {
			return null;
		}
		int valueField = fieldOffset(stringClass, "value", 'L');
		if (valueField < 0) {
			return null;
		}
		long fields = object.getObjOfsInFile() + 2L * idSize + 8;
		JavaHeapObject value = snapshot.getObjectForId(readNumber(fields + valueField, idSize));
		if (!(value instanceof JavaValueArray)) {
			return null;
		}
		JavaValueArray array = (JavaValueArray) value;
		int width;
		boolean bigEndian = true; // HPROF char[] elements use big-endian order.
		if (array.getElementType() == 'C') {
			width = 2;
		} else if (array.getElementType() == 'B') {
			int coderField = fieldOffset(stringClass, "coder", 'B');
			int coder = coderField < 0 ? 0 : (int) readNumber(fields + coderField, 1);
			if (coder != 0 && coder != 1) {
				return null;
			}
			width = coder + 1;
			if (coder == 1) {
				JavaClass utf16 = snapshot.getClassForName("java.lang.StringUTF16");
				JavaThing highShift = utf16 == null ? null : utf16.getStaticField("HI_BYTE_SHIFT");
				JavaThing lowShift = utf16 == null ? null : utf16.getStaticField("LO_BYTE_SHIFT");
				if (!(highShift instanceof JavaInt) || !(lowShift instanceof JavaInt)) {
					return null;
				}
				int high = ((JavaInt) highShift).getValue();
				int low = ((JavaInt) lowShift).getValue();
				if (!((high == 8 && low == 0) || (high == 0 && low == 8))) {
					return null;
				}
				bigEndian = high == 8;
			}
		} else {
			return null;
		}
		int length = array.getLength();
		if (length < 0 || (array.getElementType() == 'B' && length % width != 0)) {
			return null;
		}
		int capacity = array.getElementType() == 'C' ? length : length / width;
		int offsetField = fieldOffset(stringClass, "offset", 'I');
		int countField = fieldOffset(stringClass, "count", 'I');
		int offset = offsetField < 0 ? 0 : (int) readNumber(fields + offsetField, 4);
		int count = countField < 0 ? capacity : (int) readNumber(fields + countField, 4);
		if (offset < 0 || count < 0 || offset > capacity || count > capacity - offset) {
			return null;
		}
		int previewLength = Math.min(count, maxChars);
		char[] chars = new char[previewLength];
		byte[] bytes = new byte[Math.min(previewLength, 4096) * width];
		long data = array.getObjOfsInFile() + idSize + 9L + (long) offset * width;
		for (int done = 0; done < previewLength;) {
			int chunk = Math.min(previewLength - done, bytes.length / width);
			readBytes(data + (long) done * width, bytes, chunk * width);
			for (int i = 0; i < chunk; i++) {
				int first = bytes[i * width] & 0xff;
				if (width == 1) {
					chars[done + i] = (char) first;
				} else {
					int second = bytes[i * width + 1] & 0xff;
					chars[done + i] = (char) (bigEndian ? (first << 8) | second : first | (second << 8));
				}
			}
			done += chunk;
		}
		String result = new String(chars);
		return previewLength < count ? result + '\u2026' : result;
	}

	@Override
	public void close() {
		closed = true;
		if (buffer != null) {
			buffer.close();
			buffer = null;
		}
	}

	private int fieldOffset(JavaClass clazz, String name, char type) {
		int offset = 0;
		for (JavaClass current = clazz; current != null; current = current.getSuperclass()) {
			for (JavaField field : current.getDefinedFields()) {
				int size = field.isReference() ? idSize : field.getSizeInInstance();
				if (name.equals(field.getName()) && (field.getTypeId() == type || type == 'L' && field.isReference())) {
					return offset <= clazz.getFieldsSizeInFile() - size ? offset : -1;
				}
				offset += size;
			}
		}
		return -1;
	}

	private long readNumber(long offset, int size) throws IOException {
		readBytes(offset, numberBytes, size);
		long value = 0;
		for (int i = 0; i < size; i++) {
			value = (value << 8) | (numberBytes[i] & 0xff);
		}
		return value;
	}

	private void readBytes(long offset, byte[] bytes, int length) throws IOException {
		if (buffer == null) {
			buffer = factory.create(null);
		}
		try {
			buffer.get(offset, bytes, length);
		} catch (BufferUnderflowException | IndexOutOfBoundsException e) {
			throw new IOException("Cannot read heap object at offset " + offset, e);
		}
	}

	private void checkOpen() throws IOException {
		if (closed) {
			throw new IOException("Heap object reader is closed");
		}
	}
}
