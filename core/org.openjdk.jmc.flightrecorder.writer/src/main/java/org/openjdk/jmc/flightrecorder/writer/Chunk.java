/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.writer;

import java.io.IOException;
import java.util.function.Consumer;

import org.openjdk.jmc.flightrecorder.writer.api.Types;

/** A representation of JFR chunk - self contained set of JFR data. */
final class Chunk {
	private final LEB128Writer writer;
	private final ThreadMmapManager mmapManager;
	private final long threadId;
	private final long startTicks;
	private final long startNanos;

	Chunk() {
		this(LEB128Writer.getInstance(), null);
	}

	Chunk(LEB128Writer writer, ThreadMmapManager mmapManager) {
		this.writer = writer;
		this.mmapManager = mmapManager;
		this.threadId = Thread.currentThread().getId();
		this.startTicks = System.nanoTime();
		this.startNanos = System.currentTimeMillis() * 1_000_000L;
	}

	/**
	 * Finalize the chunk. The chunk should not be used after it has been finished.
	 */
	void finish(Consumer<LEB128Writer> completer) {
		completer.accept(writer);
	}

	void writeTypedValue(LEB128Writer writer, TypedValueImpl value) {
		if (value == null) {
			throw new IllegalArgumentException();
		}

		TypeImpl t = value.getType();
		if (t.isBuiltin()) {
			writeBuiltinType(writer, value);
		} else {
			if (value.getType().hasConstantPool()) {
				writer.writeLong(value.getConstantPoolIndex());
			} else {
				writeFields(writer, value);
			}
		}
	}

	private void writeFields(LEB128Writer writer, TypedValueImpl value) {
		for (TypedFieldValueImpl fieldValue : value.getFieldValues()) {
			if (fieldValue.getField().isArray()) {
				writer.writeInt(fieldValue.getValues().length); // array size
				for (TypedValueImpl tValue : fieldValue.getValues()) {
					writeTypedValue(writer, tValue);
				}
			} else {
				writeTypedValue(writer, fieldValue.getValue());
			}
		}
	}

	private void writeBuiltinType(LEB128Writer writer, TypedValueImpl typedValue) {
		TypeImpl type = typedValue.getType();
		Object value = typedValue.getValue();
		TypesImpl.Builtin builtin = Types.Builtin.ofType(type);
		if (builtin == null) {
			throw new IllegalArgumentException();
		}

		switch (builtin) {
		case STRING: {
			if (value == null) {
				writer.writeByte((byte) 0);
			} else if (((String) value).isEmpty()) {
				writer.writeByte((byte) 1);
			} else {
				long idx = typedValue.getConstantPoolIndex();
				if (idx > Long.MIN_VALUE) {
					writer.writeByte((byte) 2).writeLong(idx);
				} else {
					writer.writeCompactUTF((String) value);
				}
			}
			break;
		}
		case BYTE: {
			writer.writeByte(value == null ? (byte) 0 : (byte) value);
			break;
		}
		case CHAR: {
			writer.writeChar(value == null ? (char) 0 : (char) value);
			break;
		}
		case SHORT: {
			writer.writeShort(value == null ? (short) 0 : (short) value);
			break;
		}
		case INT: {
			writer.writeInt(value == null ? 0 : (int) value);
			break;
		}
		case LONG: {
			writer.writeLong(value == null ? 0L : (long) value);
			break;
		}
		case FLOAT: {
			writer.writeFloat(value == null ? 0.0f : (float) value);
			break;
		}
		case DOUBLE: {
			writer.writeDouble(value == null ? 0.0 : (double) value);
			break;
		}
		case BOOLEAN: {
			writer.writeBoolean(value != null && (boolean) value);
			break;
		}
		default: {
			throw new IllegalArgumentException("Unsupported built-in type " + type.getTypeName());
		}
		}
	}

	void writeEvent(TypedValueImpl event) {
		if (!"jdk.jfr.Event".equals(event.getType().getSupertype())) {
			throw new IllegalArgumentException();
		}

		// Serialize event to temporary heap-based buffer
		LEB128Writer eventWriter = LEB128Writer.getInstance();
		eventWriter.writeLong(event.getType().getId());
		for (TypedFieldValueImpl fieldValue : event.getFieldValues()) {
			writeTypedValue(eventWriter, fieldValue.getValue());
		}

		int eventSize = eventWriter.length();

		// Check if active buffer has space (size prefix + event data)
		// LEB128 encoding uses at most 5 bytes for int32
		int requiredSpace = 5 + eventSize;

		// Get current active writer (may change after rotation)
		LEB128Writer activeWriter;
		if (mmapManager != null) {
			try {
				// Always get the current active writer in mmap mode
				activeWriter = mmapManager.getActiveWriter(threadId);
				if (activeWriter instanceof LEB128MappedWriter) {
					LEB128MappedWriter mmapWriter = (LEB128MappedWriter) activeWriter;
					if (!mmapWriter.canFit(requiredSpace)) {
						// Trigger rotation - swap buffers and flush inactive in background
						mmapManager.rotateChunk(threadId);
						// Get the NEW active writer after rotation
						activeWriter = mmapManager.getActiveWriter(threadId);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Chunk rotation failed for thread " + threadId, e);
			}
		} else {
			// Heap mode - use the fixed writer
			activeWriter = writer;
		}

		// Write event to active writer (might be rotated)
		activeWriter.writeInt(eventSize) // write event size
				.writeBytes(eventWriter.export());
	}

	@Override
	public String toString() {
		return "Chunk [writer=" + writer + ", startTicks=" + startTicks + ", startNanos=" + startNanos + "]";
	}
}
