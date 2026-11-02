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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;

public class HeapObjectReaderTest {
	private static final int STRING_OFFSET = 32;
	private static final int ARRAY_OFFSET = 128;
	private static final long STRING_ID = 0xf1234567L;
	private static final long ARRAY_ID = 0x12345678L;
	private static final long STRING_CLASS_ID = 100;
	private static final long PARENT_CLASS_ID = 101;

	@Test
	public void testIndependentIdsAndBufferLifetime() throws Exception {
		Fixture fixture = new Fixture(4, false, false, 3, null);
		HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory);
		Assert.assertEquals(0, fixture.factory.created);
		Assert.assertEquals(STRING_CLASS_ID, reader.readId(fixture.object.getClazz()));
		Assert.assertEquals(0, fixture.factory.created);
		Assert.assertEquals(STRING_ID, reader.readId(fixture.object));
		Assert.assertEquals(ARRAY_ID, reader.readId(fixture.snapshot.getObjectForId(ARRAY_ID)));
		Assert.assertEquals(1, fixture.factory.created);
		reader.close();
		reader.close();
		Assert.assertEquals(1, fixture.factory.closes);
		Assert.assertEquals(0, fixture.shared.closes);
		try {
			reader.readId(fixture.object);
			Assert.fail("Closed reader must not reopen its buffer");
		} catch (IOException expected) {
			Assert.assertEquals(1, fixture.factory.created);
		}
	}

	@Test
	public void testWideIds() throws Exception {
		Fixture fixture = new Fixture(8, false, false, 3, null);
		long id = 0x7abcdef012345678L;
		ByteBuffer.wrap(fixture.image).putLong(STRING_OFFSET, id);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Assert.assertEquals(id, reader.readId(fixture.object));
		}
	}

	@Test
	public void testLegacyCharArraySliceAndInheritedFieldOffsets() throws Exception {
		Fixture fixture = new Fixture(4, true, false, 12, null);
		fixture.fields().putInt(42).putInt((int) ARRAY_ID).putInt(2).putInt(5);
		fixture.chars("xxHelloTAIL!", ByteOrder.BIG_ENDIAN);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Assert.assertEquals("Hel\u2026", reader.readString(fixture.object, 3));
			Assert.assertEquals("Hello", reader.readString(fixture.object, 5));
			Assert.assertEquals("Hello", reader.readString(fixture.object, 100));
		}
	}

	@Test
	public void testCharArrayPrefixReadIsBounded() throws Exception {
		Fixture fixture = new Fixture(4, false, false, 1_000_000_000, null);
		fixture.chars("a\u4f60\u03a9z", ByteOrder.BIG_ENDIAN);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Assert.assertEquals("a\u4f60\u03a9\u2026", reader.readString(fixture.object, 3));
		}
		Assert.assertEquals(4 + 6, fixture.factory.bytesRead);
		Assert.assertEquals(6, fixture.factory.maxRead);
	}

	@Test
	public void testCompactLatin1PrefixReadIsBounded() throws Exception {
		Fixture fixture = new Fixture(4, false, true, 1_000_000_000, null);
		fixture.fields().putInt((int) ARRAY_ID).put((byte) 0);
		fixture.data().put(new byte[] {'A', (byte) 0xe9, 'Z'});
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Assert.assertEquals("A\u00e9Z\u2026", reader.readString(fixture.object, 3));
		}
		Assert.assertEquals(4 + 1 + 3, fixture.factory.bytesRead);
		Assert.assertEquals(4, fixture.factory.maxRead);
	}

	@Test
	public void testCompactUtf16UsesDumpByteOrder() throws Exception {
		for (ByteOrder order : new ByteOrder[] {ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
			Fixture fixture = new Fixture(8, false, true, 8, order);
			fixture.fields().putLong(ARRAY_ID).put((byte) 1);
			fixture.chars("a\u4f60\ud83d\ude00", order);
			try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
				Assert.assertEquals("a\u4f60\ud83d\ude00", reader.readString(fixture.object, 4));
				Assert.assertEquals("a\u4f60\u2026", reader.readString(fixture.object, 2));
			}
		}
	}

	@Test
	public void testMissingAndInvalidStringData() throws Exception {
		Fixture fixture = new Fixture(4, true, false, 4, null);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			fixture.fields().putInt(0).putInt(0).putInt(0).putInt(0);
			Assert.assertNull(reader.readString(fixture.object, 10));
			fixture.fields().putInt(0).putInt((int) ARRAY_ID).putInt(-1).putInt(2);
			Assert.assertNull(reader.readString(fixture.object, 10));
			fixture.fields().putInt(0).putInt((int) ARRAY_ID).putInt(3).putInt(2);
			Assert.assertNull(reader.readString(fixture.object, 10));
		}
		Fixture compact = new Fixture(4, false, true, 4, null);
		compact.fields().putInt((int) ARRAY_ID).put((byte) 1);
		try (HeapObjectReader reader = new HeapObjectReader(compact.snapshot, compact.factory)) {
			Assert.assertNull("Unknown UTF16 byte order must not use the reader JVM's byte order",
					reader.readString(compact.object, 10));
		}
	}

	@Test
	public void testEmptyPreviewDoesNotReadArray() throws Exception {
		Fixture fixture = new Fixture(4, false, false, 1_000_000_000, null);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Assert.assertEquals("\u2026", reader.readString(fixture.object, 0));
		}
		Assert.assertEquals(4, fixture.factory.bytesRead);
	}

	@Test
	public void testReferenceNamesUseInstanceFieldLayout() throws Exception {
		Fixture fixture = new Fixture(4, true, false, 4, null);
		fixture.fields().putInt((int) STRING_ID).putInt((int) ARRAY_ID).putInt(0).putInt(4);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Map<Integer, String> names = reader.readReferenceNames(fixture.object);
			Assert.assertEquals(1, names.size());
			Assert.assertEquals("value", names.get(fixture.snapshot.getGlobalObjectIndexForId(ARRAY_ID)));
			Assert.assertFalse("Primitive field contents are not references",
					names.containsKey(fixture.object.getGlobalObjectIndex()));
		}
		Assert.assertEquals(4, fixture.factory.bytesRead);
	}

	@Test
	public void testReferenceNamesUseArrayIndices() throws Exception {
		Fixture fixture = new Fixture(8, false, false, 4, null, true);
		ByteBuffer.wrap(fixture.image).position(ARRAY_OFFSET + 2 * fixture.idSize + 8).putLong(STRING_ID)
				.putLong(ARRAY_ID).putLong(0).putLong(STRING_ID);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Map<Integer, String> names = reader.readReferenceNames(fixture.snapshot.getObjectForId(ARRAY_ID));
			Assert.assertEquals(2, names.size());
			Assert.assertEquals("[0], [3]", names.get(fixture.object.getGlobalObjectIndex()));
			Assert.assertEquals("[1]", names.get(fixture.snapshot.getGlobalObjectIndexForId(ARRAY_ID)));
		}
		Assert.assertEquals(4 * fixture.idSize, fixture.factory.bytesRead);
	}

	@Test
	public void testReferenceNamesSkipLargeArraysAndInstances() throws Exception {
		Fixture fixture = new Fixture(4, false, false, 1_000_000_000, null, true);
		JavaClass largeClass = new JavaClass(105, "LargeFields", 0, 0, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS,
				JavaClass.NO_VALUES, 65_537, 65_552);
		JavaObject large = new JavaObject(largeClass, 100_000_000L, new int[3], 0, 1);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Assert.assertTrue(reader.readReferenceNames(fixture.snapshot.getObjectForId(ARRAY_ID)).isEmpty());
			Assert.assertTrue(reader.readReferenceNames(large).isEmpty());
		}
		Assert.assertEquals("Skipped objects must not open or read a heap buffer", 0, fixture.factory.created);
	}

	@Test
	public void testReferenceNamesUseStaticMetadataWithoutReading() throws Exception {
		Fixture fixture = new Fixture(4, false, false, 4, null);
		JavaField field = JavaField.newInstance("owner", 'L', 4);
		JavaClass owner = new JavaClass(105, "StaticOwner", 0, 0, 0, 0, JavaClass.NO_FIELDS, new JavaField[] {field},
				new JavaThing[] {fixture.object}, 0, 16);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Assert.assertEquals("owner", reader.readReferenceNames(owner).get(fixture.object.getGlobalObjectIndex()));
		}
		Assert.assertEquals(0, fixture.factory.created);
	}

	@Test
	public void testReferenceNamesIgnoreUnresolvedTargets() throws Exception {
		Fixture fixture = new Fixture(4, false, false, 4, null);
		fixture.fields().putInt(0x0badf00d);
		try (HeapObjectReader reader = new HeapObjectReader(fixture.snapshot, fixture.factory)) {
			Assert.assertTrue(reader.readReferenceNames(fixture.object).isEmpty());
		}
	}

	private static final class Fixture {
		final byte[] image = new byte[256];
		final TrackingFactory factory = new TrackingFactory(image);
		final ForbiddenBuffer shared = new ForbiddenBuffer();
		final Snapshot snapshot;
		final JavaObject object;
		final int idSize;

		Fixture(int idSize, boolean legacy, boolean compact, int arrayLength, ByteOrder order) {
			this(idSize, legacy, compact, arrayLength, order, false);
		}

		Fixture(int idSize, boolean legacy, boolean compact, int arrayLength, ByteOrder order, boolean objectArray) {
			this.idSize = idSize;
			Snapshot.Builder builder = new Snapshot.Builder(image.length, idSize, idSize, null);
			JavaField value = field("value", 'L');
			JavaField[] fields = compact ? new JavaField[] {value, field("coder", 'B')}
					: legacy ? new JavaField[] {field("hash", 'I')} : new JavaField[] {value};
			int fieldSize = compact ? idSize + 1 : legacy ? idSize + 12 : idSize;
			builder.addClass(
					clazz(STRING_CLASS_ID, "java.lang.String", legacy ? PARENT_CLASS_ID : 0, fields, fieldSize));
			if (legacy) {
				builder.addClass(clazz(PARENT_CLASS_ID, "StringFields", 0,
						new JavaField[] {value, field("offset", 'I'), field("count", 'I')}, idSize + 8));
			}
			builder.addClass(
					clazz(102, "java.lang.ref.Reference", 0, new JavaField[] {field("referent", 'L')}, idSize));
			if (order != null) {
				builder.addClass(new JavaClass(103, "java.lang.StringUTF16", 0, 0, 0, 0, JavaClass.NO_FIELDS,
						new JavaField[] {field("HI_BYTE_SHIFT", 'I'), field("LO_BYTE_SHIFT", 'I')},
						new JavaThing[] {new JavaInt(order == ByteOrder.BIG_ENDIAN ? 8 : 0),
								new JavaInt(order == ByteOrder.BIG_ENDIAN ? 0 : 8)},
						0, 16));
			}
			builder.addJavaObject(STRING_ID, STRING_CLASS_ID, STRING_OFFSET, fieldSize);
			if (objectArray) {
				builder.addClass(clazz(104, "[Ljava.lang.Object;", 0, JavaClass.NO_FIELDS, 0));
				builder.addJavaObjectArray(ARRAY_ID, 104, ARRAY_OFFSET, arrayLength, 0);
			} else {
				builder.addJavaValueArray(ARRAY_ID, compact ? 'B' : 'C', ARRAY_OFFSET, arrayLength, 0);
			}
			if (idSize == 4) {
				ByteBuffer.wrap(image).putInt(STRING_OFFSET, (int) STRING_ID).putInt(ARRAY_OFFSET, (int) ARRAY_ID);
				fields().putInt((int) ARRAY_ID);
			} else {
				ByteBuffer.wrap(image).putLong(STRING_OFFSET, STRING_ID).putLong(ARRAY_OFFSET, ARRAY_ID);
				fields().putLong(ARRAY_ID);
			}
			snapshot = builder.buildSnapshot(new ReadBuffer.ByteArrayBufferFactory(image).create(null));
			snapshot.resetReadBuffer(new ReadBuffer.ByteArrayBufferFactory(image) {
				@Override
				public ReadBuffer create(Object supplementalInfo) {
					return shared;
				}
			});
			object = (JavaObject) snapshot.getObjectForId(STRING_ID);
		}

		ByteBuffer fields() {
			return ByteBuffer.wrap(image).position(STRING_OFFSET + 2 * idSize + 8);
		}

		ByteBuffer data() {
			return ByteBuffer.wrap(image).position(ARRAY_OFFSET + idSize + 9);
		}

		void chars(String value, ByteOrder order) {
			ByteBuffer bytes = data().order(order);
			for (int i = 0; i < value.length(); i++) {
				bytes.putChar(value.charAt(i));
			}
		}

		private JavaField field(String name, char type) {
			return JavaField.newInstance(name, type, idSize);
		}

		private JavaClass clazz(long id, String name, long superclass, JavaField[] fields, int fieldSize) {
			return new JavaClass(id, name, superclass, 0, 0, 0, fields, JavaClass.NO_FIELDS, JavaClass.NO_VALUES,
					fieldSize, 16);
		}
	}

	private static final class TrackingFactory extends ReadBuffer.ByteArrayBufferFactory {
		int created;
		int closes;
		int bytesRead;
		int maxRead;

		TrackingFactory(byte[] image) {
			super(image);
		}

		@Override
		public ReadBuffer create(Object supplementalInfo) {
			created++;
			ReadBuffer delegate = super.create(supplementalInfo);
			return new ReadBuffer() {
				@Override
				public void get(long position, byte[] bytes) throws IOException {
					get(position, bytes, bytes.length);
				}

				@Override
				public void get(long position, byte[] bytes, int length) throws IOException {
					bytesRead += length;
					maxRead = Math.max(maxRead, length);
					delegate.get(position, bytes, length);
				}

				@Override
				public int getInt(long position) throws IOException {
					throw new AssertionError("Unexpected numeric buffer access");
				}

				@Override
				public long getLong(long position) throws IOException {
					throw new AssertionError("Unexpected numeric buffer access");
				}

				@Override
				public void close() {
					closes++;
					delegate.close();
				}
			};
		}
	}

	private static final class ForbiddenBuffer extends ReadBuffer {
		int closes;

		@Override
		public void get(long position, byte[] bytes) {
			throw new AssertionError("Read through shared snapshot buffer");
		}

		@Override
		public void get(long position, byte[] bytes, int length) {
			throw new AssertionError("Read through shared snapshot buffer");
		}

		@Override
		public int getInt(long position) {
			throw new AssertionError("Read through shared snapshot buffer");
		}

		@Override
		public long getLong(long position) {
			throw new AssertionError("Read through shared snapshot buffer");
		}

		@Override
		public void close() {
			closes++;
		}
	}
}
