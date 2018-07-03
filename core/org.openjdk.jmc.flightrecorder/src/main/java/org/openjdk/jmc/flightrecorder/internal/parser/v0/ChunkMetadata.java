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

import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ProducerDescriptor;
import org.openjdk.jmc.flightrecorder.internal.util.DataInputToolkit;

/**
 * Values from the metadata descriptor event in a JFR v0 chunk.
 */
class ChunkMetadata {
	private final ProducerDescriptor[] producers;
	private final IQuantity startTime;
	private final IQuantity endTime;
	private final long startTimeNanos;
	private final long startTicks;
	private final double ticksPerNano;
	private final int previousCheckPoint;
	private final LinearUnit ticksUnit;

	private static final TypedArrayParser<ProducerDescriptor> PRODUCERS_PARSER = new TypedArrayParser<>(
			new ProducerParser());

	ChunkMetadata(byte[] data, int metadataOffset) throws InvalidJfrFileException {
		Offset offset = new Offset(data, metadataOffset);
		offset.increase(DataInputToolkit.INTEGER_SIZE); // event type
		producers = PRODUCERS_PARSER.read(data, offset);
		long startTimeMillis = NumberReaders.readLong(data, offset);
		startTime = UnitLookup.EPOCH_MS.quantity(startTimeMillis);
		startTimeNanos = startTimeMillis * 1000 * 1000;
		endTime = UnitLookup.EPOCH_MS.quantity(NumberReaders.readLong(data, offset));
		startTicks = NumberReaders.readLong(data, offset);
		ticksPerNano = ((double) NumberReaders.readLong(data, offset)) / (1000 * 1000 * 1000);
		previousCheckPoint = (int) NumberReaders.readLong(data, offset);
		// FIXME: Make unpersistable, possibly "Custom". Avoid plural unit name.
		ticksUnit = UnitLookup.TIMESPAN.makeUnit("ticks", //$NON-NLS-1$
				UnitLookup.TIMESPAN.getUnit(DecimalPrefix.NANO).quantity(1 / ticksPerNano));
		// FIXME: Make "ticks" based timestamp unit with recording provided origo to avoid nanosecond long/double precision issues?
		// Omit data not in use
		// locale = UTFStringParser.readString(data, offset);
		// gmtOffset = IntegerParser.readInt(data, offset);
	}

	ProducerDescriptor[] getProducers() {
		return producers;
	}

	IQuantity getStartTime() {
		return startTime;
	}

	IQuantity getEndTime() {
		return endTime;
	}

	long getStartTicks() {
		return startTicks;
	}

	LinearUnit getTicksUnit() {
		return ticksUnit;
	}

	int getPreviousCheckPoint() {
		return previousCheckPoint;
	}

	long asNanoTimestamp(long relativeTicks) {
		return startTimeNanos + (long) ((relativeTicks - startTicks) / ticksPerNano);
	}
}
