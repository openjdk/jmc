package org.openjdk.jmc.flightrecorder;

import java.util.function.Consumer;

public interface IParserStats {

	public interface IEventStats {
		String getName();

		long getCount();

		long getTotalSize();
	}

	void forEachEventType(Consumer<IEventStats> consumer);

	short getMajorVersion();

	short getMinorVersion();

	int getChunkCount();

	long getSkippedEventCount();

	long getEventCountByType(String eventTypeName);

	long getEventTotalSizeByType(String eventTypeName);
}
