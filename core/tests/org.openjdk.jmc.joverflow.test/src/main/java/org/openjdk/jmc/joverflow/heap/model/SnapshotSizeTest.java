/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;

public class SnapshotSizeTest {
	private static final long OBJECT_CLASS = 1;
	private static final long SAMPLE_CLASS = 10;
	private static final long SAMPLE_OBJECT = 100;
	private static final int OBJECT_OFFSET = 32;

	@Test
	public void testCompressedReferencesKeepSerializedFieldWidths() throws Exception {
		Fixture fixture = new Fixture(8, 0);
		fixture.addClass(SAMPLE_CLASS, "StreamModel", OBJECT_CLASS, "LLLL", 32);
		fixture.addObject(SAMPLE_OBJECT, SAMPLE_CLASS, OBJECT_OFFSET, 32);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(8, snapshot.getHprofPointerSize());
			Assert.assertEquals(4, snapshot.getPointerSize());
			Assert.assertEquals(12, snapshot.getObjectHeaderSize());
			JavaClass clazz = snapshot.getClassForName("StreamModel");
			Assert.assertEquals(32, clazz.getFieldsSizeInFile());
			Assert.assertEquals(32, clazz.getInstanceSize());
			JavaObject object = (JavaObject) snapshot.getObjectForId(SAMPLE_OBJECT);
			Assert.assertEquals(32, object.getSize());
			JavaThing[] fields = object.getFields();
			Assert.assertEquals(4, fields.length);
			for (JavaThing field : fields) {
				Assert.assertNull(field);
			}
		} finally {
			snapshot.discard();
		}
	}

	@Test
	public void testWideReferences() throws Exception {
		Fixture fixture = new Fixture(8, 8);
		fixture.addClass(SAMPLE_CLASS, "StreamModel", OBJECT_CLASS, "LLLL", 32);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(8, snapshot.getPointerSize());
			Assert.assertEquals(16, snapshot.getObjectHeaderSize());
			Assert.assertEquals(48, snapshot.getClassForName("StreamModel").getInstanceSize());
		} finally {
			snapshot.discard();
		}
	}

	@Test
	public void test32BitReferences() throws Exception {
		Fixture fixture = new Fixture(4, 4);
		fixture.addClass(SAMPLE_CLASS, "StreamModel", OBJECT_CLASS, "LLLL", 16);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(8, snapshot.getObjectHeaderSize());
			Assert.assertEquals(24, snapshot.getClassForName("StreamModel").getInstanceSize());
		} finally {
			snapshot.discard();
		}
	}

	@Test
	public void testInheritedReferencesAreCountedOnce() throws Exception {
		Fixture fixture = new Fixture(8, 4);
		fixture.addClass(11, "Base", OBJECT_CLASS, "L", 8);
		fixture.addClass(SAMPLE_CLASS, "Derived", 11, "LLL", 32);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(16, snapshot.getClassForName("Base").getInstanceSize());
			Assert.assertEquals(32, snapshot.getClassForName("Derived").getInstanceSize());
		} finally {
			snapshot.discard();
		}
	}

	@Test
	public void testInheritedMixedFields() throws Exception {
		Fixture fixture = new Fixture(8, 4);
		fixture.addClass(11, "Base", OBJECT_CLASS, "B", 1);
		fixture.addClass(SAMPLE_CLASS, "Derived", 11, "BIL", 14);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(16, snapshot.getClassForName("Base").getInstanceSize());
			Assert.assertEquals(24, snapshot.getClassForName("Derived").getInstanceSize());
		} finally {
			snapshot.discard();
		}
	}

	@Test
	public void testPrimitiveInheritanceKeepsExistingAlignment() throws Exception {
		Fixture fixture = new Fixture(8, 4);
		fixture.addClass(11, "Base", OBJECT_CLASS, "B", 1);
		fixture.addClass(12, "Middle", 11, "B", 2);
		fixture.addClass(SAMPLE_CLASS, "Derived", 12, "B", 3);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(16, snapshot.getClassForName("Base").getInstanceSize());
			Assert.assertEquals(16, snapshot.getClassForName("Middle").getInstanceSize());
			Assert.assertEquals(16, snapshot.getClassForName("Derived").getInstanceSize());
		} finally {
			snapshot.discard();
		}
	}

	@Test
	public void testPrimitiveFields() throws Exception {
		Fixture fixture = new Fixture(8, 4);
		fixture.addClass(SAMPLE_CLASS, "Primitives", OBJECT_CLASS, "ZBCSIFJD", 30);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(48, snapshot.getClassForName("Primitives").getInstanceSize());
			Assert.assertEquals(30, snapshot.getClassForName("Primitives").getFieldsSizeInFile());
		} finally {
			snapshot.discard();
		}
	}

	@Test
	public void testIncompleteClassDescriptionKeepsEstimate() throws Exception {
		Fixture fixture = new Fixture(8, 4);
		fixture.addClass(SAMPLE_CLASS, "NativePayload", OBJECT_CLASS, "L", 16);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(32, snapshot.getClassForName("NativePayload").getInstanceSize());
		} finally {
			snapshot.discard();
		}
	}

	@Test
	public void testArrayAndClassObjectSizes() throws Exception {
		Fixture fixture = new Fixture(8, 4);
		fixture.builder.addJavaValueArray(200, 'I', 128, 3, 12);
		JavaClass arrayClass = fixture.addClass(11, "[Ljava.lang.Object;", OBJECT_CLASS, "", 0);
		int initialArrayInstanceSize = arrayClass.getInstanceSize();
		fixture.builder.addJavaObjectArray(201, 11, 192, 3, 24);
		Snapshot snapshot = fixture.build();
		try {
			Assert.assertEquals(0, snapshot.getClassForName("[I").getInstanceSize());
			Assert.assertEquals(initialArrayInstanceSize, arrayClass.getInstanceSize());
			Assert.assertEquals(32, snapshot.getObjectForId(200).getSize());
			Assert.assertEquals(32, snapshot.getObjectForId(201).getSize());
			Assert.assertEquals(snapshot.getClassForName("java.lang.Class").getInstanceSize(), arrayClass.getSize());
		} finally {
			snapshot.discard();
		}
	}

	private static final class Fixture {
		final byte[] image = new byte[1024];
		final Snapshot.Builder builder;
		final int idSize;

		Fixture(int idSize, int referenceSize) {
			this.idSize = idSize;
			builder = new Snapshot.Builder(image.length, idSize, referenceSize, null);
			addClass(OBJECT_CLASS, "java.lang.Object", 0, "", 0);
			JavaField[] referenceFields = {JavaField.newInstance("referent", 'L', builder.getPointerSize())};
			builder.addClass(new JavaClass(2, "java.lang.ref.Reference", OBJECT_CLASS, 0, 0, 0, referenceFields,
					JavaClass.NO_FIELDS, JavaClass.NO_VALUES, idSize, builder.getInMemoryInstanceSize(idSize)));
			addClass(3, "java.lang.Class", OBJECT_CLASS, "LI", idSize + 4);
			JavaField[] stringFields = {JavaField.newInstance("value", 'L', builder.getPointerSize())};
			builder.addClass(new JavaClass(4, "java.lang.String", OBJECT_CLASS, 0, 0, 0, stringFields,
					JavaClass.NO_FIELDS, JavaClass.NO_VALUES, idSize, builder.getInMemoryInstanceSize(idSize)));
		}

		JavaClass addClass(long id, String name, long superclass, String types, int sizeInFile) {
			JavaField[] fields = new JavaField[types.length()];
			for (int i = 0; i < fields.length; i++) {
				fields[i] = JavaField.newInstance("field" + i, types.charAt(i), builder.getPointerSize());
			}
			JavaClass clazz = new JavaClass(id, name, superclass, 0, 0, 0, fields, JavaClass.NO_FIELDS,
					JavaClass.NO_VALUES, sizeInFile, builder.getInMemoryInstanceSize(sizeInFile));
			builder.addClass(clazz);
			return clazz;
		}

		void addObject(long id, long classId, int offset, int sizeInFile) {
			if (idSize == 8) {
				ByteBuffer.wrap(image).putLong(offset, id);
			} else {
				ByteBuffer.wrap(image).putInt(offset, (int) id);
			}
			builder.addJavaObject(id, classId, offset, sizeInFile);
		}

		Snapshot build() throws Exception {
			return builder.buildSnapshot(new ReadBuffer.ByteArrayBufferFactory(image).create(null));
		}
	}
}
