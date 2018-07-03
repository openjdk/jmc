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
package org.openjdk.jmc.flightrecorder.internal.parser.v0;

import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.util.DataInputToolkit;

/**
 * Represents a pointer into a limited extent within an array of byte data.
 * <p>
 * The offset instance is initialized by reading an integer value (4 bytes) at a specified position
 * in the array. This value represents the maximum permitted number of bytes that may be read from
 * the array starting from the start position.
 * <p>
 * The get and increase methods allow for tracking the current position in the array.
 */
class Offset {

	private int offset;
	private final int offsetLimit;

	/**
	 * Constructs a instance by reading the part length from an array of data.
	 *
	 * @param data
	 *            the data to read the length from
	 * @param startOffset
	 *            the position where the extent starts
	 * @throws InvalidJfrFileException
	 *             if the permitted limit would be after the end of the data array
	 */
	Offset(byte[] data, int startOffset) throws InvalidJfrFileException {
		int structSize = DataInputToolkit.readInt(data, startOffset);
		int structEnd = startOffset + structSize;
		if (structSize < DataInputToolkit.INTEGER_SIZE || structEnd > data.length) {
			throw new InvalidJfrFileException();
		} else {
			offset = startOffset + DataInputToolkit.INTEGER_SIZE;
			offsetLimit = structEnd;
		}
	}

	/**
	 * Increase the offset.
	 *
	 * @param amount
	 *            amount to increase the offset with
	 * @throws InvalidJfrFileException
	 *             if the offset is increased beyond the permitted limit
	 */
	void increase(int amount) throws InvalidJfrFileException {
		if (amount < 0) {
			throw new InvalidJfrFileException();
		}
		int newOffset = offset + amount;
		if (newOffset > offsetLimit) {
			throw new InvalidJfrFileException();
		}
		offset = newOffset;
	}

	/**
	 * Get the current offset.
	 *
	 * @return the current offset
	 */
	int get() {
		return offset;
	}

	/**
	 * Get the current offset and then increase it.
	 *
	 * @param amount
	 *            amount to increase the offset with
	 * @return the current offset, before incrementing it
	 * @throws InvalidJfrFileException
	 *             if the offset is increased beyond the permitted limit
	 */
	int getAndIncrease(int amount) throws InvalidJfrFileException {
		int current = offset;
		increase(amount);
		return current;
	}

	/**
	 * Get the permitted offset limit.
	 *
	 * @return the offset limit
	 */
	int getEnd() {
		return offsetLimit;
	}

}
