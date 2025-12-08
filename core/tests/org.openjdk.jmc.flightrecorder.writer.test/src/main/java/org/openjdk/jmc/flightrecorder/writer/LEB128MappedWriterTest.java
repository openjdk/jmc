/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, Datadog, Inc. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LEB128MappedWriterTest {
	@TempDir
	Path tempDir;

	private LEB128MappedWriter writer;
	private Path testFile;

	@BeforeEach
	void setup() throws IOException {
		testFile = tempDir.resolve("test-mmap.dat");
		writer = new LEB128MappedWriter(testFile, 4 * 1024 * 1024); // 4MB
	}

	@AfterEach
	void cleanup() throws IOException {
		if (writer != null) {
			writer.close();
		}
		if (testFile != null && Files.exists(testFile)) {
			Files.deleteIfExists(testFile);
		}
	}

	@Test
	void testBasicWrite() {
		writer.writeByte((byte) 42);
		assertEquals(1, writer.position());
		assertEquals(4 * 1024 * 1024, writer.capacity());
	}

	@Test
	void testWriteMultipleBytes() {
		byte[] data = {1, 2, 3, 4, 5};
		writer.writeBytes(data);
		assertEquals(5, writer.position());
	}

	@Test
	void testWriteLEB128() {
		writer.writeLong(127); // Fits in 1 byte
		int pos1 = writer.position();
		assertTrue(pos1 <= 2); // LEB128 encoded

		writer.writeLong(16383); // Needs 2 bytes
		int pos2 = writer.position();
		assertTrue(pos2 > pos1);
	}

	@Test
	void testWriteFloat() {
		writer.writeFloat(3.14f);
		assertEquals(4, writer.position());
	}

	@Test
	void testWriteDouble() {
		writer.writeDouble(3.14159);
		assertEquals(8, writer.position());
	}

	@Test
	void testWriteString() {
		writer.writeUTF("Hello");
		assertTrue(writer.position() > 5); // Length prefix + data
	}

	@Test
	void testCanFit() {
		assertTrue(writer.canFit(1000));
		assertTrue(writer.canFit(4 * 1024 * 1024));
		assertFalse(writer.canFit(4 * 1024 * 1024 + 1));
	}

	@Test
	void testCanFitAfterWrites() {
		byte[] data = new byte[1024];
		writer.writeBytes(data);

		assertTrue(writer.canFit(1000));
		assertFalse(writer.canFit(4 * 1024 * 1024));
	}

	@Test
	void testReset() {
		writer.writeByte((byte) 42);
		assertEquals(1, writer.position());

		writer.reset();
		assertEquals(0, writer.position());
	}

	@Test
	void testResetAndReuse() {
		writer.writeLong(127); // 1 byte in LEB128
		int pos1 = writer.position();

		writer.reset();
		writer.writeLong(127); // Same value, same encoded length
		int pos2 = writer.position();

		assertEquals(pos1, pos2); // Same position after reset with same value
	}

	@Test
	void testCopyTo() throws IOException {
		byte[] testData = {1, 2, 3, 4, 5};
		writer.writeBytes(testData);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer.copyTo(out);

		byte[] result = out.toByteArray();
		assertArrayEquals(testData, result);
	}

	@Test
	void testCopyToMultipleTimes() throws IOException {
		byte[] testData = {1, 2, 3, 4, 5};
		writer.writeBytes(testData);

		// Copy multiple times - should be idempotent
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		writer.copyTo(out1);

		ByteArrayOutputStream out2 = new ByteArrayOutputStream();
		writer.copyTo(out2);

		assertArrayEquals(out1.toByteArray(), out2.toByteArray());
	}

	@Test
	void testForce() {
		writer.writeByte((byte) 42);
		writer.force(); // Should not throw
		assertEquals(1, writer.position());
	}

	@Test
	void testGetDataSize() {
		assertEquals(0, writer.getDataSize());

		writer.writeByte((byte) 1);
		assertEquals(1, writer.getDataSize());

		writer.writeBytes(new byte[] {2, 3, 4});
		assertEquals(4, writer.getDataSize());
	}

	@Test
	void testExportBytes() {
		byte[] testData = {1, 2, 3, 4, 5};
		writer.writeBytes(testData);

		byte[] exported = writer.exportBytes();
		assertArrayEquals(testData, exported);
	}

	@Test
	void testFileExists() {
		assertTrue(Files.exists(testFile));
	}

	@Test
	void testCloseReleasesResources() throws IOException {
		writer.writeByte((byte) 42);
		writer.close();

		// File should still exist (caller responsible for deletion)
		assertTrue(Files.exists(testFile));
	}

	@Test
	void testWriteAtOffset() {
		writer.writeByte(5, (byte) 99);
		assertEquals(6, writer.position()); // Position updated

		byte[] data = writer.exportBytes();
		assertEquals(6, data.length);
		assertEquals(99, data[5]);
	}

	@Test
	void testLargeWrite() {
		// Write 1MB of data
		byte[] largeData = new byte[1024 * 1024];
		for (int i = 0; i < largeData.length; i++) {
			largeData[i] = (byte) (i % 256);
		}

		writer.writeBytes(largeData);
		assertEquals(1024 * 1024, writer.position());
		assertTrue(writer.canFit(1024 * 1024)); // Still has room
	}

	@Test
	void testMultipleWrites() {
		writer.writeByte((byte) 1);
		writer.writeShort((short) 2);
		writer.writeInt(3);
		writer.writeLong(4L);
		writer.writeFloat(5.0f);
		writer.writeDouble(6.0);

		assertTrue(writer.position() > 0);
		assertTrue(writer.position() < 100); // Reasonable size
	}

	@Test
	void testWriteNullBytes() {
		long offset = writer.writeBytes(0, null);
		assertEquals(0, offset);
		assertEquals(0, writer.position());
	}

	@Test
	void testCapacityUnchanged() {
		int initialCapacity = writer.capacity();

		writer.writeByte((byte) 1);
		writer.writeBytes(new byte[1000]);

		assertEquals(initialCapacity, writer.capacity()); // Capacity is fixed
	}
}
