/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives the tools against a real recording from the JMC core test resources. The recording is
 * referenced in place rather than copied here, so the test is skipped when the mcp module is built
 * outside a full JMC checkout.
 */
@QuarkusTest
class JfrToolsTest {

	private static final Path RECORDING = Paths.get("..", "core", "tests",
			"org.openjdk.jmc.flightrecorder.serializers.test", "src", "main", "resources", "recordings",
			"hotmethods.jfr");

	@Inject
	RecordingTools recordingTools;

	@Inject
	MetadataTools metadataTools;

	@Inject
	QueryTools queryTools;

	@Inject
	AnalysisTools analysisTools;

	@Inject
	CorrelationTools correlationTools;

	private String recordingId;

	@BeforeEach
	void loadRecording() throws IOException {
		Assumptions.assumeTrue(Files.isReadable(RECORDING), "Test recording not available: " + RECORDING);
		String result = recordingTools.loadRecording(RECORDING.toString());
		assertTrue(result.startsWith("Loaded recording:"), result);
		// A recording is addressable by the canonical path, whatever spelling was used to load it.
		recordingId = RECORDING.toFile().getCanonicalPath();
	}

	@Test
	void loadReportsEventCountAndDuration() {
		String info = recordingTools.getRecordingInfo(recordingId);
		assertTrue(info.contains("46309"), info);
		assertTrue(info.contains("78 event types"), info);
	}

	@Test
	void eventTypesIncludeExecutionSamples() {
		String types = metadataTools.getEventTypes(recordingId);
		assertTrue(types.contains("jdk.ExecutionSample"), types);
		assertTrue(types.contains("24526 events"), types);
	}

	@Test
	void attributesAreListedForType() {
		String attrs = metadataTools.getAttributes("jdk.ExecutionSample", recordingId);
		assertTrue(attrs.contains("stackTrace"), attrs);
		assertTrue(attrs.contains("eventThread"), attrs);
	}

	@Test
	void unknownRecordingIsReportedNotThrown() {
		String result = metadataTools.getEventTypes("/no/such/recording.jfr");
		assertTrue(result.startsWith("Error:"), result);
		assertTrue(result.contains("Unknown recording"), result);
	}

	@Test
	void stackTraceWeightsSumToSampleCount() {
		String trace = analysisTools.getStackTrace(null, null, null, null, 5, recordingId);
		// The self weights of all nodes must account for every sample in the recording.
		assertTrue(trace.contains("Total weight: 24526"), trace);
		assertTrue(trace.contains("countIntersection"), trace);
		// A correct total means real percentages rather than a row of zeroes.
		assertFalse(trace.contains("  0.0% (17906)"), trace);
	}

	@Test
	void aggregateStoresAndDetailsExtremeEvent() {
		String result = analysisTools.aggregateEvents("jdk.GCPhasePause", "max", null, null, recordingId);
		assertTrue(result.contains("Max(duration)"), result);
		assertTrue(result.contains("stored as 'jdk.GCPhasePause.max'"), result);
		// Details must come from the event's own type, so GC specific attributes show up.
		assertTrue(result.contains("GC Identifier"), result);
	}

	@Test
	void ruleResultsRespectMinimumSeverity() {
		String warnings = analysisTools.getRuleResults("WARNING", false, recordingId);
		assertTrue(warnings.contains("MethodProfiling"), warnings);
		// IGNORE sorts after WARNING in the enum, so an ordinal comparison would leak it in here.
		assertFalse(warnings.contains("Severity: Ignore"), warnings);
	}

	@Test
	void eventTableReportsUnknownColumns() {
		String table = queryTools.getEventTable("jdk.ExecutionSample", null, null, "startTime,bogusColumn", null, null,
				2, null, recordingId);
		assertTrue(table.contains("bogusColumn"), table);
		assertTrue(table.contains("column skipped"), table);
		assertTrue(table.contains("Start Time"), table);
	}

	@Test
	void timeSeriesReturnsValuesForCpuLoad() {
		String series = queryTools.getTimeSeries("jdk.CPULoad", "jvmUser", null, null, 3, recordingId);
		assertTrue(series.contains("Timestamp, Value"), series);
		assertTrue(series.contains("%"), series);
	}

	@Test
	void timeSeriesRejectsUnknownAttribute() {
		String series = queryTools.getTimeSeries("jdk.CPULoad", "notAnAttribute", null, null, 3, recordingId);
		assertTrue(series.contains("not found on event type"), series);
	}

	@Test
	void setOperationsComposeStoredResults() {
		queryTools.getEventTable("jdk.GCPhasePause", null, null, null, null, null, 100, "pauses", recordingId);
		queryTools.getEventTable("jdk.GarbageCollection", null, null, null, null, null, 100, "collections",
				recordingId);

		String intersection = correlationTools.combineResultSets("pauses", "collections", "intersect", "both",
				recordingId);
		assertTrue(intersection.contains("= 0 events"), intersection);

		String union = correlationTools.combineResultSets("pauses", "collections", "union", "either", recordingId);
		assertTrue(union.contains("= 14 events"), union);

		String selfIntersect = correlationTools.combineResultSets("pauses", "pauses", "intersect", "same", recordingId);
		assertTrue(selfIntersect.contains("= 7 events"), selfIntersect);

		String subtract = correlationTools.combineResultSets("pauses", "pauses", "subtract", "none", recordingId);
		assertTrue(subtract.contains("= 0 events"), subtract);

		String sets = correlationTools.listResultSets(recordingId);
		assertTrue(sets.contains("either: 14 events"), sets);
	}

	@Test
	void combineRejectsUnknownSetAndOperation() {
		queryTools.getEventTable("jdk.GCPhasePause", null, null, null, null, null, 10, "pauses", recordingId);
		assertTrue(correlationTools.combineResultSets("nope", "pauses", "intersect", "out", recordingId)
				.contains("No stored result set named 'nope'"));
		assertTrue(correlationTools.combineResultSets("pauses", "pauses", "frobnicate", "out", recordingId)
				.contains("Unknown operation"));
	}

	@Test
	void findRelatedEventsRequiresScoping() {
		String result = correlationTools.findRelatedEvents("concurrent", null, null, null, null, null, null, null, null,
				null, null, recordingId);
		assertTrue(result.contains("either 'reference'"), result);
	}

	@Test
	void findRelatedEventsUsesStoredReference() {
		analysisTools.aggregateEvents("jdk.GCPhasePause", "max", null, null, recordingId);
		String related = correlationTools.findRelatedEvents("concurrent", "jdk.GCPhasePause.max", null, null, null,
				null, null, Boolean.FALSE, null, "around-pause", 20, recordingId);
		assertTrue(related.contains("Concurrent events"), related);
		assertTrue(related.contains("Reference time range"), related);
		assertNotNull(correlationTools.listResultSets(recordingId));
		assertTrue(correlationTools.listResultSets(recordingId).contains("around-pause"));
	}

	@Test
	void deleteResultSetRemovesIt() {
		queryTools.getEventTable("jdk.GCPhasePause", null, null, null, null, null, 10, "doomed", recordingId);
		assertTrue(correlationTools.deleteResultSet("doomed", recordingId).contains("Deleted"));
		assertTrue(correlationTools.deleteResultSet("doomed", recordingId).contains("No stored result set"));
	}

	@Test
	void sharedAttributesRevealCorrelationPaths() {
		String shared = metadataTools.getSharedAttributes("jdk.GCPhasePause", recordingId);
		assertTrue(shared.contains("gcId"), shared);
		assertTrue(shared.contains("Shared with"), shared);
	}

	@Test
	void eventTableCorrelatesAcrossTypesByGcId() {
		String gcId = "8";
		String table = queryTools.getEventTable(null, "gcId", gcId, "gcId", null, null, 50, "gc8", recordingId);
		// Correlating on gcId alone must pull in more than one event type.
		assertTrue(table.contains("jdk.GCPhasePause"), table);
		assertTrue(table.contains("jdk.GarbageCollection"), table);
	}

	@Test
	void versionIsReported() {
		assertTrue(recordingTools.getVersion().startsWith("jmc-mcp-server "));
	}

	@Test
	void unloadFreesTheRecording() {
		assertTrue(recordingTools.unloadRecording(recordingId).startsWith("Unloaded"));
		assertTrue(metadataTools.getEventTypes(recordingId).startsWith("Error:"));
		assertEquals("No such recording loaded: " + recordingId, recordingTools.unloadRecording(recordingId));
	}

	@Test
	void jfrEventsListEveryAttributeOnItsOwnLine() {
		String events = queryTools.getJfrEvents("jdk.GCPhasePause", null, null, 2, recordingId);
		assertTrue(events.contains("Event #1:"), events);
		assertTrue(events.contains("GC Identifier"), events);
	}

	@Test
	void listRecordingsIncludesTheLoadedRecording() {
		assertTrue(recordingTools.listRecordings().contains(recordingId));
	}

	@Test
	void loadRecordingReportsFailureForAnUnreadableFile(@TempDir Path tempDir) throws IOException {
		Path bogus = tempDir.resolve("not-a-recording.jfr");
		Files.writeString(bogus, "this is not a JFR recording");
		assertTrue(recordingTools.loadRecording(bogus.toString()).startsWith("Error loading recording:"));
	}

	@Test
	void ambiguousRecordingIdIsRejectedWhenSeveralAreLoaded(@TempDir Path tempDir) throws IOException {
		Path secondCopy = tempDir.resolve("hotmethods-copy.jfr");
		Files.copy(RECORDING, secondCopy);
		assertTrue(recordingTools.loadRecording(secondCopy.toString()).startsWith("Loaded recording:"));
		try {
			assertTrue(metadataTools.getEventTypes(null).contains("Several recordings are loaded"));
		} finally {
			recordingTools.unloadRecording(secondCopy.toFile().getCanonicalPath());
		}
	}

	@Test
	void findRelatedEventsIncludesReferenceByDefaultAndCanExcludeIt() {
		String included = correlationTools.findRelatedEvents("contained", null, "jdk.GarbageCollection", null, null,
				null, null, Boolean.FALSE, null, null, 50, recordingId);
		assertTrue(included.contains("Contained events"), included);
		assertTrue(included.contains("  jdk.GarbageCollection: "), included);

		String excluded = correlationTools.findRelatedEvents("contained", null, "jdk.GarbageCollection", null, null,
				null, null, Boolean.FALSE, Boolean.FALSE, null, 50, recordingId);
		assertTrue(excluded.contains("Contained events"), excluded);
		assertFalse(excluded.contains("  jdk.GarbageCollection: "), excluded);
	}

	@Test
	void eventTableRejectsHalfSpecifiedAttributeFilter() {
		String missingValue = queryTools.getEventTable(null, "gcId", null, null, null, null, 10, null, recordingId);
		assertTrue(missingValue.contains("without filterValue"), missingValue);

		String missingAttribute = queryTools.getEventTable(null, null, "8", null, null, null, 10, null, recordingId);
		assertTrue(missingAttribute.contains("without filterAttribute"), missingAttribute);
	}

	@Test
	void eventTableSkipsTypesWithoutTheRequestedColumns() {
		String table = queryTools.getEventTable(null, "gcId", "8", "longestPause", null, null, 50, null, recordingId);
		// GCPhasePause events share gcId but carry no longestPause column, so their rows are
		// skipped with a note instead of falling back to all attributes.
		assertTrue(table.contains("jdk.GCPhasePause: none of the requested columns exist on this type"), table);
		assertTrue(table.contains("Longest Pause"), table);
	}

	@Test
	void stackTraceResolvesWeightAttributeFromEventMetadata() {
		String memoryWeighted = analysisTools.getStackTrace("jdk.GCHeapSummary", "heapUsed", null, null, 5,
				recordingId);
		assertTrue(memoryWeighted.contains("weighted by heapUsed (KiB)"), memoryWeighted);

		String unknown = analysisTools.getStackTrace(null, "bogusWeight", null, null, 5, recordingId);
		assertTrue(unknown.contains("No numeric attribute 'bogusWeight'"), unknown);
	}
}
