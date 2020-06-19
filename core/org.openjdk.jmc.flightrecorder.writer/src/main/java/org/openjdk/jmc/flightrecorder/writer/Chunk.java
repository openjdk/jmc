package org.openjdk.jmc.flightrecorder.writer;

import lombok.ToString;
import org.openjdk.jmc.flightrecorder.writer.api.Type;
import org.openjdk.jmc.flightrecorder.writer.api.TypedFieldValue;
import org.openjdk.jmc.flightrecorder.writer.api.TypedValue;
import org.openjdk.jmc.flightrecorder.writer.api.Types;

import java.util.function.Consumer;

/** A representation of JFR chunk - self contained set of JFR data. */
@ToString
final class Chunk {
	private final LEB128Writer writer = LEB128Writer.getInstance();
	private final long startTicks;
	private final long startNanos;

	Chunk() {
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

		if (value == null && builtin != TypesImpl.Builtin.STRING) {
			// skip the non-string built-in values
			return;
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
			writer.writeByte((byte) value);
			break;
		}
		case CHAR: {
			writer.writeChar((char) value);
			break;
		}
		case SHORT: {
			writer.writeShort((short) value);
			break;
		}
		case INT: {
			writer.writeInt((int) value);
			break;
		}
		case LONG: {
			writer.writeLong((long) value);
			break;
		}
		case FLOAT: {
			writer.writeFloat((float) value);
			break;
		}
		case DOUBLE: {
			writer.writeDouble((double) value);
			break;
		}
		case BOOLEAN: {
			writer.writeBoolean((boolean) value);
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

		LEB128Writer eventWriter = LEB128Writer.getInstance();
		eventWriter.writeLong(event.getType().getId());
		for (TypedFieldValueImpl fieldValue : event.getFieldValues()) {
			writeTypedValue(eventWriter, fieldValue.getValue());
		}

		writer.writeInt(eventWriter.length()) // write event size
				.writeBytes(eventWriter.export());
	}
}
