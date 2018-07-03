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
package org.openjdk.jmc.common.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * ObjectInputStream that validates the classes and count of objects and bytes read from it.
 * <p>
 * Reading must be done using the {@code safeRead} methods which sets validation parameters before
 * reading. Any attempt to use {@code read} methods will be blocked.
 * <p>
 * See <a href="https://www.owasp.org/index.php/Deserialization_of_untrusted_data">OWASP</a>.
 */
public class ValidatingObjectInputStream extends ObjectInputStream {
	private ValidatingObjectInputStream.LimitedInputStream in;
	private Collection<Class<?>> safeClasses = null;
	private int maxObjects = 0;
	private int readObjects = 0;

	private ValidatingObjectInputStream(ValidatingObjectInputStream.LimitedInputStream in) throws IOException {
		super(in);
		this.in = in;
		enableResolveObject(true);
	}

	/**
	 * Create a new input stream for reading objects. This stream will be initialized so that no
	 * objects are permitted to be read. To read objects you should use
	 * {@link #safeReadObject(Class, Collection, int, long)} which updates the validation parameters
	 * before reading.
	 *
	 * @param in
	 *            stream to read from
	 * @return a new input stream for reading objects
	 * @throws IOException
	 *             on I/O error
	 */
	public static ValidatingObjectInputStream build(InputStream in) throws IOException {
		ValidatingObjectInputStream.LimitedInputStream lin = new LimitedInputStream(in, 4); // 4 bytes for ObjectInputStream header
		return new ValidatingObjectInputStream(lin);
	}

	/**
	 * Update validation parameters and read the next object from the stream.
	 *
	 * @param <T>
	 *            type of returned object
	 * @param type
	 *            Type to return. This type will be whitelisted. If the stored object may be of a
	 *            subclass to this type then the permitted subclasses must be included in
	 *            {@code safeClasses}.
	 * @param safeClasses
	 *            Collection of whitelisted classes. This must include all classes used within the
	 *            stored object.
	 * @param maxObjects
	 *            Maximum number of objects to read. This must be large enough to permit valid use,
	 *            especially for collection objects.
	 * @param maxBytes
	 *            Maximum number of bytes to read from the stream. This must be large enough to
	 *            permit valid use, especially for collection objects.
	 * @return the object read from the stream
	 * @throws IOException
	 *             on I/O error
	 * @throws ClassNotFoundException
	 *             if the class of the stored object can't be found
	 * @throws ClassCastException
	 *             if the object is not of the specified type
	 */
	public <T> T safeReadObject(Class<T> type, Collection<Class<?>> safeClasses, int maxObjects, long maxBytes)
			throws ClassNotFoundException, IOException, ClassCastException {
		Set<Class<?>> typeAndSafeClasses = new HashSet<>();
		typeAndSafeClasses.add(type);
		if (safeClasses != null) {
			typeAndSafeClasses.addAll(safeClasses);
		}
		updateValidation(typeAndSafeClasses, maxObjects, maxBytes);
		@SuppressWarnings("unchecked")
		T obj = (T) readObject();
		zeroValidation();
		return obj;
	}

	/**
	 * Update validation parameters and read the next long from the stream.
	 *
	 * @return the long value read from the stream
	 * @throws IOException
	 *             on I/O error
	 */
	public long safeReadLong() throws IOException {
		updateValidation(null, 0, 10);
		return super.readLong();
	}

	private void updateValidation(Collection<Class<?>> safeClasses, int maxObjects, long maxBytes) {
		this.safeClasses = safeClasses;
		this.maxObjects = maxObjects;
		this.readObjects = 0;
		in.updateValidation(maxBytes);
	}

	private void zeroValidation() {
		this.safeClasses = null;
		this.maxObjects = 0;
		in.updateValidation(0);
	}

	@Override
	protected Object resolveObject(Object obj) throws IOException {
		if (readObjects++ > maxObjects) {
			throw new SecurityException("Attempting to deserialize too many objects from stream, limit=" + maxObjects); //$NON-NLS-1$
		}
		return super.resolveObject(obj);
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		Class<?> clazz = super.resolveClass(desc);
		if (safeClasses != null && (safeClasses.contains(clazz) || clazz.isArray())) {
			return clazz;
		}
		throw new SecurityException("Attempting to deserialize non-whitelisted class: " + clazz.getName()); //$NON-NLS-1$
	}

	/**
	 * Input stream that limits the amount of data that is permitted to be read.
	 */
	private static class LimitedInputStream extends FilterInputStream {
		private long maxBytes;
		private long readBytes = 0;

		/**
		 * @param in
		 *            stream to read from
		 * @param maxBytes
		 *            Maximum number of bytes to read from the stream. This must be large enough to
		 *            permit valid use, especially for collection objects.
		 */
		public LimitedInputStream(InputStream in, long maxBytes) {
			super(in);
			this.maxBytes = maxBytes;
		}

		@Override
		public int read() throws IOException {
			checkLength(1);
			int val = super.read();
			if (val != -1) {
				readBytes++;
			}
			return val;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			checkLength(len);
			int val = super.read(b, off, len);
			if (val > 0) {
				readBytes += val;
			}
			return val;
		}

		private void checkLength(int len) {
			if (readBytes + len > maxBytes) {
				throw new SecurityException("Attempting to read too many bytes from stream, read=" + (readBytes + len) //$NON-NLS-1$
						+ " limit=" + maxBytes); //$NON-NLS-1$
			}
		}

		public void updateValidation(long maxBytes) {
			this.maxBytes = maxBytes;
			this.readBytes = 0;
		}
	}
}
