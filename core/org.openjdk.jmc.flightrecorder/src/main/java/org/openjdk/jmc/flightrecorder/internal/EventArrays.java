package org.openjdk.jmc.flightrecorder.internal;

import java.util.Set;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.internal.parser.ParserStats;

public class EventArrays {

	private final EventArray[] arrays;
	private final Set<IRange<IQuantity>> chunkTimeranges;
	private final ParserStats parserStats;

	public EventArrays(EventArray[] arrays, Set<IRange<IQuantity>> ranges, ParserStats parserStats) {
		this.arrays = arrays;
		this.chunkTimeranges = ranges;
		this.parserStats = parserStats;
	}

	public EventArray[] getArrays() {
		return arrays;
	}

	public Set<IRange<IQuantity>> getChunkTimeranges() {
		return chunkTimeranges;
	}

	public ParserStats getParserStats() {
		return parserStats;
	}

}
