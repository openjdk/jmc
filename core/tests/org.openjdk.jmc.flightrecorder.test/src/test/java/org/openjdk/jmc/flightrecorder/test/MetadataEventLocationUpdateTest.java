package org.openjdk.jmc.flightrecorder.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.test.util.RecordingToolkit;
import org.openjdk.jmc.flightrecorder.util.ChunkReader;

/**
 * In an upcoming release (12 or 13) the metadata event cannot be counted on being the last event in
 * the chunk anymore. These tests make sure the parser still work.
 */
public final class MetadataEventLocationUpdateTest {
	private static final int CHUNK_COUNT_FLUSH_RECORDINGS = 2;
	private static final int CHUNK_COUNT_METADATA_RECORDINGS = 1;
	private static final String[] TYPES_TO_CHECK = {"jdk.GCPhaseParallel", "jdk.CompilerInlining"}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String[] TYPES_TO_CHECK_FLUSH = {"jdk.ModuleExport", "jdk.BooleanFlag", "jdk.JavaMonitorWait"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static final long[] EXPECTED_COUNTS_CONTROL = {12584, 7283};
	private static final long[] EXPECTED_COUNTS_NEW = {27738, 6883};

	private static final long[] EXPECTED_COUNTS_FLUSH_CONTROL = {1512, 1500, 991};
	private static final long[] EXPECTED_COUNTS_FLUSH_INCREMENTAL = {1512, 1500, 860};

	private static final int EXPECTED_NUMBER_OF_TYPES_FLUSH_RECORDINGS = 133;

	private static final String RECORDING_METADATA_CONTROL = "metadata_control.jfr"; //$NON-NLS-1$
	private static final String RECORDING_METADATA_NEW = "metadata_new.jfr"; //$NON-NLS-1$

	private static final String RECORDING_FLUSH_METADATA = "flush_metadata.jfr"; //$NON-NLS-1$
	private static final String RECORDING_FLUSH_INCREMENTAL_METADATA = "flush_incremental_metadata.jfr"; //$NON-NLS-1$

	@Test
	public void testChunkSplitter() throws IOException, CouldNotLoadRecordingException {
		// Should not be affected, but just for good measure
		int controlFlushChunkCount = countChunks(ChunkReader
				.readChunks(RecordingToolkit.getNamedRecordingResource(RECORDING_FLUSH_INCREMENTAL_METADATA)));
		int incrementalFlushChunkCount = countChunks(ChunkReader
				.readChunks(RecordingToolkit.getNamedRecordingResource(RECORDING_FLUSH_INCREMENTAL_METADATA)));
		int controlMetadataChunkCount = countChunks(
				ChunkReader.readChunks(RecordingToolkit.getNamedRecordingResource(RECORDING_METADATA_CONTROL)));
		int newMetadataChunkCount = countChunks(
				ChunkReader.readChunks(RecordingToolkit.getNamedRecordingResource(RECORDING_METADATA_NEW)));

		assertEquals(CHUNK_COUNT_FLUSH_RECORDINGS, controlFlushChunkCount);
		assertEquals(CHUNK_COUNT_FLUSH_RECORDINGS, incrementalFlushChunkCount);
		assertEquals(CHUNK_COUNT_METADATA_RECORDINGS, controlMetadataChunkCount);
		assertEquals(CHUNK_COUNT_METADATA_RECORDINGS, newMetadataChunkCount);
	}

	@Test
	public void testGetEventTypes() throws IOException, CouldNotLoadRecordingException {
		IItemCollection controlEvents = RecordingToolkit.getNamedRecording(RECORDING_METADATA_CONTROL);
		IItemCollection newEvents = RecordingToolkit.getNamedRecording(RECORDING_METADATA_NEW);

		IAggregator<Set<String>, ?> distinctTypesAggregator = Aggregators.distinct(JfrAttributes.EVENT_TYPE_ID);
		Set<String> controlTypes = controlEvents.getAggregate(distinctTypesAggregator);
		Set<String> newTypes = newEvents.getAggregate(distinctTypesAggregator);
		newTypes.removeAll(controlTypes);
		// The new flush event should be the one remaining
		assertTrue(newTypes.contains("jdk.Flush")); //$NON-NLS-1$
		assertEquals(1, newTypes.size());
	}

	@Test
	public void testCountsInRecordings() throws IOException, CouldNotLoadRecordingException {
		IItemCollection controlEvents = RecordingToolkit.getNamedRecording(RECORDING_METADATA_CONTROL);
		IItemCollection newEvents = RecordingToolkit.getNamedRecording(RECORDING_METADATA_NEW);
		for (int i = 0; i < TYPES_TO_CHECK.length; i++) {
			String typeId = TYPES_TO_CHECK[i];
			long countControl = controlEvents.apply(ItemFilters.type(typeId)).getAggregate(Aggregators.count())
					.longValue();
			long countNew = newEvents.apply(ItemFilters.type(typeId)).getAggregate(Aggregators.count()).longValue();
			assertEquals(EXPECTED_COUNTS_CONTROL[i], countControl);
			assertEquals(EXPECTED_COUNTS_NEW[i], countNew);
		}
	}

	@Test
	public void testCountsInFlushRecordings() throws IOException, CouldNotLoadRecordingException {
		IItemCollection controlFlushEvents = RecordingToolkit.getNamedRecording(RECORDING_FLUSH_METADATA);
		IItemCollection incrementalFlushEvents = RecordingToolkit
				.getNamedRecording(RECORDING_FLUSH_INCREMENTAL_METADATA);
		for (int i = 0; i < TYPES_TO_CHECK_FLUSH.length; i++) {
			String typeId = TYPES_TO_CHECK_FLUSH[i];
			long countControl = controlFlushEvents.apply(ItemFilters.type(typeId)).getAggregate(Aggregators.count())
					.longValue();
			long countNew = incrementalFlushEvents.apply(ItemFilters.type(typeId)).getAggregate(Aggregators.count())
					.longValue();
			assertEquals(EXPECTED_COUNTS_FLUSH_CONTROL[i], countControl);
			assertEquals(EXPECTED_COUNTS_FLUSH_INCREMENTAL[i], countNew);
		}
	}

	@Test
	public void testIncrementalMetadataTypeCounts() throws IOException, CouldNotLoadRecordingException {
		IItemCollection controlEvents = RecordingToolkit.getNamedRecording(RECORDING_FLUSH_METADATA);
		IItemCollection incremetalMetadataEvents = RecordingToolkit
				.getNamedRecording(RECORDING_FLUSH_INCREMENTAL_METADATA);
		Set<String> controlTypes = getKnownTypes(controlEvents);
		Set<String> incrementalTypes = getKnownTypes(incremetalMetadataEvents);
		assertEquals(EXPECTED_NUMBER_OF_TYPES_FLUSH_RECORDINGS, controlTypes.size());
		assertEquals(EXPECTED_NUMBER_OF_TYPES_FLUSH_RECORDINGS, incrementalTypes.size());
	}

	private static int countChunks(Iterator<byte[]> readChunks) {
		int count = 0;
		while (readChunks.hasNext()) {
			readChunks.next();
			count++;
		}
		return count;
	}

	private static Set<String> getKnownTypes(IItemCollection items) {
		Set<String> types = new HashSet<>();
		Iterator<IItemIterable> iterable = items.iterator();
		while (iterable.hasNext()) {
			types.add(iterable.next().getType().getIdentifier());
		}
		return types;
	}
}
