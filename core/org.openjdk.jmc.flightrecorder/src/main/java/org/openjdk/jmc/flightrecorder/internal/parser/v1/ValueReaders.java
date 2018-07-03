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
package org.openjdk.jmc.flightrecorder.internal.parser.v1;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.StructContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MemberAccessorToolkit;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;

class ValueReaders {
	interface IValueReader {
		Object read(IDataInput in, boolean allowUnresolvedReference) throws IOException, InvalidJfrFileException;

		void skip(IDataInput in) throws IOException, InvalidJfrFileException;

		Object resolve(Object value) throws InvalidJfrFileException;

		ContentType<?> getContentType();
	}

	private static class ConstantReference {
		final long key;

		ConstantReference(long key) {
			this.key = key;
		}

		@Override
		public int hashCode() {
			return (int) ((key >>> 32) ^ key);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || obj instanceof ConstantReference && key == ((ConstantReference) obj).key;
		}
	}

	static class PoolReader implements IValueReader {
		private final FastAccessNumberMap<Object> constantPool;
		private final ContentType<?> contentType;

		PoolReader(FastAccessNumberMap<Object> pool, ContentType<?> contentType) {
			this.constantPool = pool;
			this.contentType = contentType;
		}

		@Override
		public Object read(IDataInput in, boolean allowUnresolvedReference)
				throws IOException, InvalidJfrFileException {
			long constantIndex = in.readLong();
			Object constant = constantPool.get(constantIndex);
			return (allowUnresolvedReference && (constant == null)) ? new ConstantReference(constantIndex) : constant;
		}

		@Override
		public void skip(IDataInput in) throws IOException, InvalidJfrFileException {
			in.readLong();
		}

		@Override
		public Object resolve(Object value) throws InvalidJfrFileException {
			if (value instanceof ConstantReference) {
				return constantPool.get(((ConstantReference) value).key);
			}
			return value;
		}

		@Override
		public ContentType<?> getContentType() {
			return contentType;
		}
	}

	static class ArrayReader implements IValueReader {
		private final IValueReader elementReader;

		ArrayReader(IValueReader elementReader) {
			this.elementReader = elementReader;
		}

		@Override
		public Object read(IDataInput in, boolean allowUnresolvedReference)
				throws IOException, InvalidJfrFileException {
			int size = in.readInt();
			Object[] values = new Object[size];
			for (int i = 0; i < values.length; i++) {
				values[i] = elementReader.read(in, allowUnresolvedReference);
			}
			return values;
		}

		@Override
		public void skip(IDataInput in) throws IOException, InvalidJfrFileException {
			int size = in.readInt();
			for (int i = 0; i < size; i++) {
				elementReader.skip(in);
			}
		}

		@Override
		public Object resolve(Object value) throws InvalidJfrFileException {
			Object[] valueArray = (Object[]) value;
			for (int i = 0; i < valueArray.length; i++) {
				valueArray[i] = elementReader.resolve(valueArray[i]);
			}
			return valueArray;
		}

		@Override
		public ContentType<?> getContentType() {
			return UnitLookup.UNKNOWN;
		}
	}

	static class QuantityReader implements IValueReader {
		private final String typeIdentifier;
		private final IUnit unit;
		private final boolean floatingPoint;
		private final boolean unsignedOrFloat;

		QuantityReader(String typeIdentifier, IUnit unit, boolean unsigned) throws InvalidJfrFileException {
			this.typeIdentifier = typeIdentifier;
			this.unit = unit;
			if (PrimitiveReader.isFloat(typeIdentifier)) {
				floatingPoint = true;
				unsignedOrFloat = true;
			} else if (PrimitiveReader.isDouble(typeIdentifier)) {
				floatingPoint = true;
				unsignedOrFloat = false;
			} else if (PrimitiveReader.isNumeric(typeIdentifier)) {
				floatingPoint = false;
				unsignedOrFloat = unsigned;
			} else {
				throw new InvalidJfrFileException("Unknown numeric type: " + typeIdentifier); //$NON-NLS-1$
			}
		}

		@Override
		public Object read(IDataInput in, boolean allowUnresolvedReference)
				throws IOException, InvalidJfrFileException {
			if (floatingPoint) {
				return quantity(PrimitiveReader.readDouble(in, unsignedOrFloat));
			} else {
				return quantity(PrimitiveReader.readLong(in, typeIdentifier, unsignedOrFloat));
			}
		}

		@Override
		public void skip(IDataInput in) throws IOException, InvalidJfrFileException {
			if (floatingPoint) {
				PrimitiveReader.readDouble(in, unsignedOrFloat);
			} else {
				PrimitiveReader.readLong(in, typeIdentifier, unsignedOrFloat);
			}
		};

		IQuantity quantity(Number numericalValue) {
			return unit.quantity(numericalValue);
		}

		IQuantity quantity(long numericalValue) {
			return unit.quantity(numericalValue);
		}

		IQuantity quantity(double numericalValue) {
			return unit.quantity(numericalValue);
		}

		@Override
		public Object resolve(Object value) throws InvalidJfrFileException {
			return value;
		}

		@Override
		public ContentType<?> getContentType() {
			return unit.getContentType();
		}
	}

	static class TicksTimestampReader extends QuantityReader {
		private final ChunkStructure header;

		TicksTimestampReader(String typeIdentifier, ChunkStructure header, boolean unsigned)
				throws InvalidJfrFileException {
			super(typeIdentifier, null, unsigned);
			this.header = header;
		}

		@Override
		IQuantity quantity(long numericalValue) {
			return header.ticsTimestamp(numericalValue);
		}

		@Override
		IQuantity quantity(Number numericalValue) {
			return quantity(numericalValue.longValue());
		}

		@Override
		IQuantity quantity(double numericalValue) {
			return quantity((long) numericalValue);
		}

		@Override
		public ContentType<?> getContentType() {
			return UnitLookup.TIMESTAMP;
		}
	}

	static class StringReader implements IValueReader {
		static final String STRING = "java.lang.String"; //$NON-NLS-1$

		private final FastAccessNumberMap<Object> constantPool;

		StringReader(FastAccessNumberMap<Object> constantPool) {
			this.constantPool = constantPool;
		}

		@Override
		public Object read(IDataInput in, boolean allowUnresolvedReference)
				throws IOException, InvalidJfrFileException {
			byte encoding = in.readByte();
			if (encoding == SeekableInputStream.STRING_ENCODING_CONSTANT_POOL) {
				long constantIndex = in.readLong();
				Object constant = constantPool.get(constantIndex);
				return (allowUnresolvedReference && (constant == null)) ? new ConstantReference(constantIndex)
						: constant;
			}
			return in.readRawString(encoding);
		}

		@Override
		public void skip(IDataInput in) throws IOException, InvalidJfrFileException {
			in.skipString();
		}

		@Override
		public Object resolve(Object value) throws InvalidJfrFileException {
			if (value instanceof ConstantReference) {
				return constantPool.get(((ConstantReference) value).key);
			}
			return value;
		}

		@Override
		public ContentType<?> getContentType() {
			return UnitLookup.PLAIN_TEXT;
		}
	}

	// FIXME: Rearrange to remove string switching on every method. (Java 8 could simplify this.)
	static class PrimitiveReader implements IValueReader {
		private static final String DOUBLE = "double"; //$NON-NLS-1$
		private static final String FLOAT = "float"; //$NON-NLS-1$
		private static final String LONG = "long"; //$NON-NLS-1$
		private static final String INT = "int"; //$NON-NLS-1$
		private static final String CHAR = "char"; //$NON-NLS-1$
		private static final String SHORT = "short"; //$NON-NLS-1$
		private static final String BYTE = "byte"; //$NON-NLS-1$
		private static final String BOOLEAN = "boolean"; //$NON-NLS-1$

		private final String typeIdentifier;
		private final ContentType<?> contentType;

		PrimitiveReader(String typeIdentifier) throws InvalidJfrFileException {
			this.typeIdentifier = typeIdentifier;
			switch (typeIdentifier) {
			case BOOLEAN:
				contentType = UnitLookup.FLAG;
				break;
			case BYTE:
			case SHORT:
			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
				contentType = UnitLookup.RAW_NUMBER;
				break;
			default:
				throw new InvalidJfrFileException("Unknown primitive type: " + typeIdentifier); //$NON-NLS-1$
			}
		}

		@Override
		public Object read(IDataInput in, boolean allowUnresolvedReference)
				throws IOException, InvalidJfrFileException {
			switch (typeIdentifier) {
			case BOOLEAN:
				return in.readBoolean();
			case BYTE:
				return in.readByte();
			case SHORT:
				return in.readShort();
			case CHAR:
				return in.readChar();
			case INT:
				return in.readInt();
			case LONG:
				return in.readLong();
			case FLOAT:
				return in.readFloat();
			case DOUBLE:
				return in.readDouble();
			default:
				throw new InvalidJfrFileException("Unknown primitive type: " + typeIdentifier); //$NON-NLS-1$
			}
		}

		@Override
		public void skip(IDataInput in) throws IOException, InvalidJfrFileException {
			switch (typeIdentifier) {
			case BOOLEAN:
				in.readBoolean();
				return;
			case FLOAT:
				in.readFloat();
				return;
			case DOUBLE:
				in.readDouble();
				return;
			case CHAR:
				in.readChar();
				return;
			default:
				readLong(in, typeIdentifier, false);
				return;
			}
		}

		@Override
		public ContentType<?> getContentType() {
			return contentType;
		}

		@Override
		public Object resolve(Object value) throws InvalidJfrFileException {
			return value;
		}

		static boolean isFloat(String typeIdentifier) {
			return FLOAT.equals(typeIdentifier);
		}

		static boolean isDouble(String typeIdentifier) {
			return DOUBLE.equals(typeIdentifier);
		}

		static boolean isNumeric(String typeIdentifier) {
			switch (typeIdentifier) {
			case BYTE:
			case SHORT:
			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
				return true;
			default:
				return false;
			}
		}

		static long readLong(IDataInput in, String typeIdentifier, boolean unsigned)
				throws IOException, InvalidJfrFileException {
			switch (typeIdentifier) {
			case BYTE:
				return unsigned ? in.readByte() : in.readUnsignedByte();
			case SHORT:
				return unsigned ? in.readUnsignedShort() : in.readShort();
			case INT:
				return unsigned ? in.readUnsignedInt() : in.readInt();
			case LONG:
				return in.readLong();
			default:
				throw new InvalidJfrFileException("Unknown integer type: " + typeIdentifier); //$NON-NLS-1$
			}
		}

		static double readDouble(IDataInput in, boolean fromFloat) throws IOException {
			return fromFloat ? in.readFloat() : in.readDouble();
		}
	}

	static abstract class AbstractStructReader implements IValueReader {
		final List<IValueReader> valueReaders;

		AbstractStructReader(int fieldCount) {
			valueReaders = new ArrayList<>(fieldCount);
		}

		@Override
		public void skip(IDataInput in) throws IOException, InvalidJfrFileException {
			for (int i = 0; i < valueReaders.size(); i++) {
				valueReaders.get(i).skip(in);
			}
		}

		abstract void addField(String identifier, String name, String description, IValueReader reader)
				throws InvalidJfrFileException;
	}

	static class StructReader extends AbstractStructReader {
		private final StructContentType<Object[]> contentType;

		StructReader(StructContentType<Object[]> contentType, int fieldCount) {
			super(fieldCount);
			this.contentType = contentType;
		}

		@Override
		public Object read(IDataInput in, boolean allowUnresolvedReference)
				throws IOException, InvalidJfrFileException {
			Object[] values = new Object[valueReaders.size()];
			for (int i = 0; i < values.length; i++) {
				values[i] = valueReaders.get(i).read(in, allowUnresolvedReference);
			}
			return values;
		}

		@Override
		public Object resolve(Object value) throws InvalidJfrFileException {
			Object[] valueArray = (Object[]) value;
			for (int i = 0; i < valueArray.length; i++) {
				valueArray[i] = valueReaders.get(i).resolve(valueArray[i]);
			}
			return valueArray;
		}

		@Override
		public ContentType<?> getContentType() {
			return contentType;
		}

		@Override
		public void addField(String identifier, String name, String description, IValueReader reader) {
			int index = valueReaders.size();
			valueReaders.add(reader);
			@SuppressWarnings("unchecked")
			IMemberAccessor<Object, Object[]> ma = (IMemberAccessor<Object, Object[]>) MemberAccessorToolkit
					.arrayElement(index);
			@SuppressWarnings("unchecked")
			ContentType<Object> resolveType = (ContentType<Object>) reader.getContentType();
			contentType.addField(identifier, resolveType, name, description, ma);
		}
	}

	static class ReflectiveReader extends AbstractStructReader {
		// FIXME: Change the reflective setting of fields to avoid the conversion workarounds that some classes have to make. See JMC-5966

		// String to prefix reserved java keywords with when looking for a class field
		private static final String RESERVED_IDENTIFIER_PREFIX = "_"; //$NON-NLS-1$
		private final List<Field> fields;
		private final Class<?> klass;
		private final ContentType<?> ct;

		<T> ReflectiveReader(Class<T> klass, int fieldCount, ContentType<? super T> ct) {
			super(fieldCount);
			this.klass = klass;
			this.ct = ct;
			fields = new ArrayList<>(fieldCount);
		}

		@Override
		public Object read(IDataInput in, boolean allowUnresolvedReference)
				throws IOException, InvalidJfrFileException {
			try {
				Object thread = klass.newInstance();
				for (int i = 0; i < valueReaders.size(); i++) {
					Object val = valueReaders.get(i).read(in, allowUnresolvedReference);
					Field f = fields.get(i);
					if (f != null) {
						f.set(thread, val);
					}
				}
				return thread;
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Object resolve(Object value) throws InvalidJfrFileException {
			try {
				for (int i = 0; i < valueReaders.size(); i++) {
					Field f = fields.get(i);
					if (f != null) {
						Object val = valueReaders.get(i).resolve(f.get(value));
						f.set(value, val);
					}
				}
				return value;
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public ContentType<?> getContentType() {
			return ct;
		}

		@Override
		void addField(String identifier, String name, String description, IValueReader reader)
				throws InvalidJfrFileException {
			valueReaders.add(reader);
			try {
				try {
					fields.add(klass.getField(identifier));
				} catch (NoSuchFieldException e) {
					fields.add(klass.getField(RESERVED_IDENTIFIER_PREFIX + identifier));
				}
			} catch (NoSuchFieldException e) {
				Logger.getLogger(ReflectiveReader.class.getName()).log(Level.WARNING,
						"Could not find field with name '" + identifier + "' in reader for '" + ct.getIdentifier() //$NON-NLS-1$ //$NON-NLS-2$
								+ "'"); //$NON-NLS-1$
				fields.add(null);
			}
		}
	}
}
