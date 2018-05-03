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

import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.Chunk;
import org.openjdk.jmc.flightrecorder.internal.util.DataInputToolkit;

class ChunkStructure {

	private final static int SIZE = 7 * DataInputToolkit.LONG_SIZE + DataInputToolkit.INTEGER_SIZE;
	private final static int COMPRESSED_INTS = 1;
	private final long chunkSize;
	private final long constantPoolOffset;
	private final long metadataOffset;
	private final long startTimeNanos;
	private final long durationNanos;
	private final long startTicks;
	private final double ticksPerNano;
	private final int features;
	private final int bodyOffset;
	private final LinearUnit ticksUnit;

	ChunkStructure(Chunk chunkInput) throws IOException, InvalidJfrFileException {
		int position = chunkInput.getPosition();
		byte[] buffer = chunkInput.fill(position + SIZE);
		chunkSize = DataInputToolkit.readLong(buffer, position);
		position += DataInputToolkit.LONG_SIZE;
		constantPoolOffset = DataInputToolkit.readLong(buffer, position);
		position += DataInputToolkit.LONG_SIZE;
		metadataOffset = DataInputToolkit.readLong(buffer, position);
		position += DataInputToolkit.LONG_SIZE;
		startTimeNanos = DataInputToolkit.readLong(buffer, position);
		position += DataInputToolkit.LONG_SIZE;
		durationNanos = DataInputToolkit.readLong(buffer, position);
		position += DataInputToolkit.LONG_SIZE;
		startTicks = DataInputToolkit.readLong(buffer, position);
		position += DataInputToolkit.LONG_SIZE;
		ticksPerNano = DataInputToolkit.readLong(buffer, position) / 1000000000.0; // ticsPerSecond -> ticksPerNano
		position += DataInputToolkit.LONG_SIZE;
		features = DataInputToolkit.readInt(buffer, position);
		bodyOffset = position + DataInputToolkit.INTEGER_SIZE;
		ticksUnit = UnitLookup.TIMESPAN.makeUnit("ticks", //$NON-NLS-1$
				UnitLookup.TIMESPAN.getUnit(DecimalPrefix.NANO).quantity(1 / ticksPerNano));
	}

	long getBodyStartOffset() {
		return bodyOffset;
	}

	long getMetadataOffset() {
		return metadataOffset;
	}

	long getChunkSize() {
		return chunkSize;
	}

	long getConstantPoolOffset() {
		return constantPoolOffset;
	}

	boolean isIntegersCompressed() {
		return (features & COMPRESSED_INTS) != 0;
	}

	LinearUnit getTicksTimespanUnit() {
		return ticksUnit;
	}

	IQuantity ticsTimestamp(long relativeTicks) {
		return UnitLookup.EPOCH_NS.quantity(startTimeNanos + (long) ((relativeTicks - startTicks) / ticksPerNano));
	}

	IRange<IQuantity> getChunkRange() {
		return QuantityRange.createWithExtent(UnitLookup.EPOCH_NS.quantity(startTimeNanos),
				UnitLookup.TIMESPAN.getUnit(DecimalPrefix.NANO).quantity(durationNanos));
	}

	long getStartTimeNanos() {
		return startTimeNanos;
	}

}
