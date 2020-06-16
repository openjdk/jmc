package org.openjdk.jmc.flightrecorder.internal;

import java.util.Set;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;

public class EventArrays {

	private final EventArray[] arrays;
	private final Set<IRange<IQuantity>> chunkTimeranges;

	public EventArrays(EventArray[] arrays, Set<IRange<IQuantity>> ranges) {
		this.arrays = arrays;
		this.chunkTimeranges = ranges;
	}

	public EventArray[] getArrays() {
		return arrays;
	}

	public Set<IRange<IQuantity>> getChunkTimeranges() {
		return chunkTimeranges;
	}

}
