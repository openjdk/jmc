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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

/**
 * Exercises published tool schemas and structured results over the packaged server's actual MCP
 * transport, including JSON deserialization of compound predicates. Failsafe always exercises the
 * packaged JVM uber-jar and also runs against the native executable when native.image.path is set.
 */
class StructuredToolsIT {

	private static final String TERRAIN_TYPE = "test.TransportTerrain";
	private static final String TICK_TYPE = "test.TransportTick";
	private static final long EXACT_ID = 9007199254740993L;

	@Name(TERRAIN_TYPE)
	static class Terrain extends Event {
		String vehicle;
		int hpAtStart;
		int iterations;
		double remainingSeconds;
		long exactId;
		long run;
	}

	@Name(TICK_TYPE)
	static class Tick extends Event {
		long run;
		long tick;
	}

	@Test
	void structuredToolsWorkThroughStdio(@TempDir Path directory) throws Exception {
		exercise(directory, false);
	}

	@Test
	@EnabledIfSystemProperty(named = "native.image.path", matches = ".+")
	void nativeStructuredToolsWorkThroughStdio(@TempDir Path directory) throws Exception {
		exercise(directory, true);
	}

	private static void exercise(Path directory, boolean nativeBinary) throws Exception {
		Path executable = nativeBinary ? Path.of(System.getProperty("native.image.path")).toAbsolutePath()
				: Path.of("target", "mcp-1.0.0-SNAPSHOT-runner.jar").toAbsolutePath();
		assertTrue(Files.isRegularFile(executable), "Packaged server is missing: " + executable);
		Path recording = createRecording(directory);
		IItemCollection terrain = JfrLoaderToolkit.loadEvents(recording.toFile()).apply(ItemFilters.type(TERRAIN_TYPE));
		IAttribute<IQuantity> remainingAttribute = numericAttribute(terrain, "remainingSeconds");
		IAttribute<IQuantity> exactIdAttribute = numericAttribute(terrain, "exactId");
		IItemCollection selected = terrain.apply(ItemFilters.and(
				ItemFilters.equals(numericAttribute(terrain, "hpAtStart"), UnitLookup.NUMBER_UNITY.quantity(0)),
				ItemFilters.equals(numericAttribute(terrain, "iterations"), UnitLookup.NUMBER_UNITY.quantity(6)),
				ItemFilters.more(remainingAttribute, UnitLookup.NUMBER_UNITY.quantity(0))));
		IItemCollection cars = selected
				.apply(ItemFilters.equals(Attribute.attr("vehicle", "Vehicle", UnitLookup.PLAIN_TEXT), "car"));
		Map<String, IQuantity> expectedRemaining = nativeStatistics(cars, remainingAttribute);
		Map<String, IQuantity> expectedExactId = nativeStatistics(cars, exactIdAttribute);
		Map<String, IQuantity> expectedDuration = nativeStatistics(cars, JfrAttributes.DURATION);
		try (Server server = new Server(executable, directory, nativeBinary)) {
			server.initialize();
			JsonArray tools = server.request("tools/list", new JsonObject()).getJsonObject("result")
					.getJsonArray("tools");
			assertPredicateSchema(tool(tools, "queryEvents"));
			assertPredicateSchema(tool(tools, "aggregateGroups"));
			JsonObject comparisonTool = tool(tools, "compareRecordings");
			assertPredicateSchema(comparisonTool);
			JsonArray comparisonRequired = comparisonTool.getJsonObject("inputSchema").getJsonArray("required");
			assertTrue(comparisonRequired.contains("baselineRecordingId"));
			assertTrue(comparisonRequired.contains("candidateRecordingId"));
			assertTrue(comparisonTool.containsKey("outputSchema"));
			assertTrue(tool(tools, "exportResultSet").containsKey("outputSchema"));
			JsonObject histogramTool = tool(tools, "getHistogram");
			assertPredicateSchema(histogramTool);
			assertTrue(histogramTool.containsKey("outputSchema"));
			JsonObject joinProperties = tool(tools, "joinIntervals").getJsonObject("inputSchema")
					.getJsonObject("properties");
			assertTrue(joinProperties.containsKey("matchAttributes"));
			assertTrue(joinProperties.containsKey("sameThread"));
			JsonObject stackProperties = tool(tools, "getStackStatistics").getJsonObject("inputSchema")
					.getJsonObject("properties");
			for (String field : List.of("sourceResultSet", "thread", "frameContains", "denominator")) {
				assertTrue(stackProperties.containsKey(field), "Stack schema is missing " + field);
			}

			JsonObject loaded = server.call("loadRecording", new JsonObject().put("path", recording.toString()));
			assertTrue(loaded.getJsonArray("content").getJsonObject(0).getString("text").contains("Loaded recording"),
					loaded.toString());
			JsonObject grouped = server.structured("aggregateGroups",
					new JsonObject().put("eventType", TERRAIN_TYPE).put("groupBy", "vehicle")
							.put("attributes", "remainingSeconds,exactId,duration").put("filters", predicates())
							.put("percentiles", new JsonArray().add(50).add(90)));
			assertEquals(3L, grouped.getLong("totalEvents"));
			assertEquals(2, grouped.getInteger("totalGroups"));
			JsonObject car = group(grouped.getJsonArray("groups"), "car");
			assertEquals(2L, car.getLong("count"));
			JsonObject remaining = car.getJsonObject("statistics").getJsonObject("remainingSeconds");
			assertNativeStatistics(expectedRemaining, remaining);
			assertNativeValue(expectedRemaining.get("90"), remaining.getJsonObject("percentiles").getJsonObject("90"));
			JsonObject exactId = car.getJsonObject("statistics").getJsonObject("exactId");
			assertEquals("9007199254740993", exactId.getJsonObject("min").getString("exactValue"));
			assertEquals("9007199254740994", exactId.getJsonObject("max").getString("exactValue"));
			assertNativeStatistics(expectedExactId, exactId);
			assertNativeStatistics(expectedDuration, car.getJsonObject("statistics").getJsonObject("duration"));
			assertCanonicalDuration(car.getJsonObject("statistics").getJsonObject("duration").getJsonObject("sum"));

			JsonObject query = server.structured("queryEvents",
					new JsonObject().put("eventType", TERRAIN_TYPE).put("filters", predicates())
							.put("columns", "vehicle,remainingSeconds,exactId,run,duration").put("limit", 1)
							.put("storeAs", "selected"));
			assertEquals(3L, query.getLong("total"));
			assertEquals(1, query.getInteger("returned"));
			assertEquals("selected", query.getString("storedAs"));
			assertTrue(query.getBoolean("truncated"));
			assertEquals("9007199254740993", query.getJsonArray("rows").getJsonObject(0).getJsonObject("attributes")
					.getJsonObject("exactId").getString("exactValue"));
			assertCanonicalDuration(
					query.getJsonArray("rows").getJsonObject(0).getJsonObject("attributes").getJsonObject("duration"));

			JsonObject first = server.structured("exportResultSet", new JsonObject().put("resultSet", "selected")
					.put("columns", "vehicle,exactId,duration").put("format", "json").put("offset", 0).put("limit", 2));
			assertEquals(3L, first.getLong("total"));
			assertEquals(2, first.getInteger("returned"));
			assertTrue(first.getBoolean("truncated"));
			assertEquals(2L, first.getLong("nextOffset"));
			assertEquals(EXACT_ID, first.getJsonArray("events").getJsonObject(0).getJsonObject("attributes")
					.getJsonObject("exactId").getLong("value"));
			assertCanonicalDuration(first.getJsonArray("events").getJsonObject(0).getJsonObject("attributes")
					.getJsonObject("duration"));
			JsonObject last = server.structured("exportResultSet",
					new JsonObject().put("resultSet", "selected").put("columns", "vehicle,exactId,duration")
							.put("offset", first.getLong("nextOffset")).put("limit", 2));
			assertEquals(1, last.getInteger("returned"));
			assertFalse(last.getBoolean("truncated"));
			assertNull(last.getValue("nextOffset"));
			assertEquals(3, first.getJsonArray("events").size() + last.getJsonArray("events").size());
			JsonObject csv = server.structured("exportResultSet", new JsonObject().put("resultSet", "selected")
					.put("columns", "vehicle,exactId").put("format", "csv").put("limit", 1));
			assertTrue(csv.getString("csv").startsWith("eventType,vehicle,exactId\r\n"));
			assertTrue(csv.getString("csv").contains("9007199254740993"));

			JsonObject joined = server.structured("joinIntervals",
					new JsonObject().put("childResultSet", "selected").put("parentEventType", TICK_TYPE)
							.put("matchAttributes", "run").put("childColumns", "vehicle,run")
							.put("parentColumns", "tick,run"));
			assertEquals(3L, joined.getLong("matchedChildren"));
			assertEquals(0L, joined.getLong("unmatchedChildren"));
			JsonObject parent = joined.getJsonArray("rows").getJsonObject(0).getJsonObject("parents")
					.getJsonObject(TICK_TYPE);
			assertEquals(TICK_TYPE, parent.getString("eventType"));
			assertEquals("7", parent.getJsonObject("attributes").getJsonObject("tick").getString("exactValue"));
			JsonObject stacks = server.structured("getStackStatistics", new JsonObject().put("eventType", TERRAIN_TYPE)
					.put("sourceResultSet", "selected").put("thread", Thread.currentThread().getName())
					.put("frameContains", "StructuredToolsIT.terrain").put("denominator", "source").put("limit", 5));
			assertEquals(3L, stacks.getLong("selectedEventCount"));
			assertEquals(3L, stacks.getLong("stackEventCount"));
			assertEquals(3.0, stacks.getDouble("totalWeight"));
			assertEquals("source", stacks.getJsonObject("denominator").getString("mode"));
			assertEquals(3L, stacks.getJsonObject("denominator").getLong("eventCount"));
			assertFalse(stacks.getJsonArray("nodes").isEmpty());
			JsonObject weighted = server.structured("getStackStatistics",
					new JsonObject().put("eventType", TERRAIN_TYPE).put("sourceResultSet", "selected")
							.put("attribute", "exactId").put("thread", Thread.currentThread().getName())
							.put("frameContains", "StructuredToolsIT.terrain").put("denominator", "source")
							.put("limit", 50));
			IQuantity totalWeight = UnitLookup.NUMBER_UNITY.quantity(EXACT_ID)
					.add(UnitLookup.NUMBER_UNITY.quantity(EXACT_ID + 1))
					.add(UnitLookup.NUMBER_UNITY.quantity(EXACT_ID + 2));
			assertEquals(TestItems.plain(totalWeight.numberValue()), weighted.getString("totalWeightExactValue"));
			assertEquals(totalWeight.numberValue().toString(),
					weighted.getJsonObject("denominator").getString("weightExactValue"));
			IQuantity selfWeight = UnitLookup.NUMBER_UNITY.quantity(0);
			for (int i = 0; i < weighted.getJsonArray("nodes").size(); i++) {
				JsonObject node = weighted.getJsonArray("nodes").getJsonObject(i);
				IQuantity self = UnitLookup.NUMBER_UNITY
						.quantity(Long.parseLong(node.getString("selfWeightExactValue")));
				IQuantity inclusive = UnitLookup.NUMBER_UNITY
						.quantity(Long.parseLong(node.getString("inclusiveWeightExactValue")));
				assertTrue(inclusive.compareTo(self) >= 0);
				selfWeight = selfWeight.add(self);
			}
			assertEquals(totalWeight, selfWeight);

			JsonObject histogram = server.structured("getHistogram",
					new JsonObject().put("eventType", TERRAIN_TYPE).put("resultSet", "selected")
							.put("attribute", "remainingSeconds").put("percentiles", new JsonArray().add(50).add(100)));
			assertEquals(3, histogram.getInteger("valueCount"));
			assertEquals("", histogram.getString("unit"));
			JsonArray histogramBuckets = histogram.getJsonArray("buckets");
			assertEquals(2, histogramBuckets.size(), histogram.toString());
			assertHistogramBucket(histogramBuckets.getJsonObject(0), 1, 2, 1);
			assertHistogramBucket(histogramBuckets.getJsonObject(1), 2, 5, 2);
			assertEquals(100.0, histogramBuckets.getJsonObject(1).getDouble("cumulativePercent"), 1e-9);
			JsonObject median = histogram.getJsonArray("percentiles").getJsonObject(0);
			assertEquals(2.0, median.getDouble("value"));
			assertEquals(2L, median.getLong("countAtOrAbove"));
			JsonObject maximum = histogram.getJsonArray("percentiles").getJsonObject(1);
			assertEquals(3.0, maximum.getDouble("value"));
			assertEquals(1L, maximum.getLong("countAtOrAbove"));
			JsonObject durations = server.structured("getHistogram", new JsonObject().put("eventType", TERRAIN_TYPE)
					.put("resultSet", "selected").put("bucketing", "linear").put("buckets", 4).put("unit", "us"));
			assertEquals("us", durations.getString("unit"));
			long counted = 0;
			for (int i = 0; i < durations.getJsonArray("buckets").size(); i++) {
				counted += durations.getJsonArray("buckets").getJsonObject(i).getLong("count");
			}
			assertEquals(3L, counted, durations.toString());
			JsonObject invalid = server
					.call("getHistogram", new JsonObject().put("eventType", TERRAIN_TYPE).put("bucketing", "cubic"))
					.getJsonObject("structuredContent");
			assertTrue(invalid.getString("error").contains("Unknown bucketing"), invalid.toString());

			Path candidate = directory.resolve("terrain-candidate.jfr");
			Files.copy(recording, candidate);
			JsonObject candidateLoaded = server.call("loadRecording",
					new JsonObject().put("path", candidate.toString()));
			assertTrue(candidateLoaded.getJsonArray("content").getJsonObject(0).getString("text")
					.contains("Loaded recording"), candidateLoaded.toString());
			JsonObject comparison = server.structured("compareRecordings",
					new JsonObject().put("baselineRecordingId", recording.toString())
							.put("candidateRecordingId", candidate.toString()).put("eventType", TERRAIN_TYPE)
							.put("groupBy", "vehicle").put("attributes", "remainingSeconds,exactId,duration")
							.put("filters", predicates()));
			assertEquals(recording.toFile().getCanonicalPath(), comparison.getString("baselineRecordingId"));
			assertEquals(candidate.toFile().getCanonicalPath(), comparison.getString("candidateRecordingId"));
			assertEquals(3L, comparison.getLong("baselineEvents"));
			assertEquals(3L, comparison.getLong("candidateEvents"));
			assertEquals(0L, comparison.getLong("countDelta"));
			assertEquals(2L, comparison.getLong("bothGroups"));
			JsonObject carComparison = group(comparison.getJsonArray("groups"), "car");
			assertEquals("both", carComparison.getString("presence"));
			assertEquals(0L, carComparison.getLong("countDelta"));
			for (String side : List.of("baseline", "candidate")) {
				JsonObject statistics = carComparison.getJsonObject(side).getJsonObject("statistics");
				assertNativeStatistics(expectedRemaining, statistics.getJsonObject("remainingSeconds"));
				assertNativeStatistics(expectedExactId, statistics.getJsonObject("exactId"));
				assertNativeStatistics(expectedDuration, statistics.getJsonObject("duration"));
				assertCanonicalDuration(statistics.getJsonObject("duration").getJsonObject("median"));
			}
			for (String attribute : List.of("remainingSeconds", "exactId")) {
				assertDecimal("0", carComparison.getJsonObject("changes").getJsonObject(attribute)
						.getJsonObject("median").getJsonObject("delta").getString("exactValue"));
			}
			JsonObject durationDelta = carComparison.getJsonObject("changes").getJsonObject("duration")
					.getJsonObject("median").getJsonObject("delta");
			assertCanonicalDuration(durationDelta);
			assertDecimal("0", durationDelta.getString("canonicalExactValue"));
		}
	}

	private static JsonArray predicates() {
		return new JsonArray()
				.add(new JsonObject().put("attribute", "hpAtStart").put("operator", "eq").put("value", "0"))
				.add(new JsonObject().put("attribute", "iterations").put("operator", "eq").put("value", "6"))
				.add(new JsonObject().put("attribute", "remainingSeconds").put("operator", "gt").put("value", "0"));
	}

	private static JsonObject tool(JsonArray tools, String name) {
		for (int i = 0; i < tools.size(); i++) {
			JsonObject tool = tools.getJsonObject(i);
			if (name.equals(tool.getString("name"))) {
				return tool;
			}
		}
		throw new AssertionError("Tool was not published: " + name);
	}

	private static void assertPredicateSchema(JsonObject tool) {
		JsonObject schema = tool.getJsonObject("inputSchema");
		JsonObject filters = resolve(schema, schema.getJsonObject("properties").getJsonObject("filters"));
		assertEquals("array", filters.getString("type"));
		JsonObject predicate = resolve(schema, filters.getJsonObject("items"));
		JsonObject properties = predicate.getJsonObject("properties");
		assertNotNull(properties, tool.toString());
		for (String field : List.of("attribute", "operator", "value", "unit")) {
			assertTrue(properties.containsKey(field), "Predicate schema is missing " + field + ": " + tool);
			assertEquals("string", resolve(schema, properties.getJsonObject(field)).getString("type"));
		}
	}

	private static JsonObject resolve(JsonObject schema, JsonObject node) {
		for (int i = 0; i < 10 && node.containsKey("$ref"); i++) {
			String reference = node.getString("$ref");
			assertTrue(reference.startsWith("#/"), "Unsupported schema reference: " + reference);
			JsonObject resolved = schema;
			for (String field : reference.substring(2).split("/")) {
				resolved = resolved.getJsonObject(field.replace("~1", "/").replace("~0", "~"));
				assertNotNull(resolved, "Missing schema reference: " + reference);
			}
			node = resolved;
		}
		return node;
	}

	private static JsonObject group(JsonArray groups, String vehicle) {
		for (int i = 0; i < groups.size(); i++) {
			JsonObject group = groups.getJsonObject(i);
			if (vehicle.equals(group.getJsonObject("key").getString("vehicle"))) {
				return group;
			}
		}
		throw new AssertionError("No group for vehicle " + vehicle + ": " + groups);
	}

	private static IAttribute<IQuantity> numericAttribute(IItemCollection items, String id) {
		for (IItemIterable iterable : items) {
			for (IAttribute<?> source : iterable.getType().getAttributes()) {
				if (id.equals(source.getIdentifier())) {
					return new Attribute<IQuantity>("(test.numeric:" + id + ")", id, null, UnitLookup.NUMBER) {
						@Override
						public <T> IMemberAccessor<IQuantity, T> customAccessor(IType<T> type) {
							IMemberAccessor<?, T> accessor = source.getAccessor(type);
							return accessor == null ? null : item -> {
								Object value = accessor.getMember(item);
								return value instanceof IQuantity quantity ? quantity
										: UnitLookup.NUMBER_UNITY.quantity((Number) value);
							};
						}
					};
				}
			}
		}
		throw new AssertionError("No numeric attribute " + id);
	}

	private static Map<String, IQuantity> nativeStatistics(IItemCollection items, IAttribute<IQuantity> attribute) {
		Map<String, IQuantity> expected = new LinkedHashMap<>();
		expected.put("sum", items.getAggregate(Aggregators.sum(attribute)));
		expected.put("avg", items.getAggregate(Aggregators.avg(attribute)));
		IQuantity min = items.getAggregate(Aggregators.min(attribute));
		IQuantity max = items.getAggregate(Aggregators.max(attribute));
		expected.put("min", min);
		expected.put("max", max);
		expected.put("stddev", items.getAggregate(Aggregators.stddevp(attribute)));
		// Every car group has two values; percentile interpolation uses native quantity arithmetic.
		expected.put("median", min.add(max.subtract(min).multiply(0.5)));
		expected.put("90", min.add(max.subtract(min).multiply(0.9)));
		return expected;
	}

	private static void assertNativeStatistics(Map<String, IQuantity> expected, JsonObject statistics) {
		for (String metric : List.of("sum", "avg", "min", "max", "stddev", "median")) {
			assertNativeValue(expected.get(metric), statistics.getJsonObject(metric));
		}
	}

	private static void assertNativeValue(IQuantity expected, JsonObject actual) {
		assertNotNull(actual);
		if (actual.containsKey("canonicalExactValue")) {
			IUnit sourceUnit = expected.getUnit();
			IUnit unit = sourceUnit.getIdentifier().equals(actual.getString("unit")) ? sourceUnit
					: sourceUnit.getContentType().getUnit(actual.getString("unit"));
			assertNotNull(unit);
			Number value = expected.numberValueIn(unit);
			assertEquals(TestItems.plain(value), actual.getString("exactValue"));
			IQuantity encoded = unit.quantity(value);
			assertEquals(TestItems.plain(encoded.numberValueIn(unit.getContentType().getDefaultUnit())),
					actual.getString("canonicalExactValue"));
		} else {
			assertEquals(TestItems.plain(expected.numberValue()), actual.getString("exactValue"));
		}
	}

	private static void assertHistogramBucket(JsonObject bucket, double from, double to, long count) {
		assertEquals(from, bucket.getDouble("from"), bucket.toString());
		assertEquals(to, bucket.getDouble("to"), bucket.toString());
		assertEquals(count, bucket.getLong("count"), bucket.toString());
	}

	private static void assertDecimal(String expected, String actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual)), actual);
	}

	private static void assertCanonicalDuration(JsonObject quantity) {
		assertNotNull(quantity);
		assertEquals("s", quantity.getString("canonicalUnit"));
		assertNotNull(quantity.getValue("canonicalValue"));
		assertTrue(new BigDecimal(quantity.getString("canonicalExactValue")).signum() >= 0);
	}

	private static Path createRecording(Path directory) throws Exception {
		Path path = directory.resolve("terrain.jfr");
		try (Recording recording = new Recording()) {
			recording.enable(Terrain.class).withStackTrace();
			recording.enable(Tick.class).withoutStackTrace();
			recording.start();
			Tick tick = new Tick();
			tick.run = 11;
			tick.tick = 7;
			tick.begin();
			terrain("car", 0, 6, 1, EXACT_ID);
			terrain("car", 0, 6, 3, EXACT_ID + 1);
			terrain("truck", 0, 6, 2, EXACT_ID + 2);
			terrain("car", 1, 6, 100, 2);
			terrain("car", 0, 5, 200, 3);
			terrain("car", 0, 6, 0, 4);
			tick.end();
			tick.commit();
			recording.stop();
			recording.dump(path);
		}
		return path;
	}

	private static void terrain(String vehicle, int hp, int iterations, double seconds, long id) {
		Terrain event = new Terrain();
		event.vehicle = vehicle;
		event.hpAtStart = hp;
		event.iterations = iterations;
		event.remainingSeconds = seconds;
		event.exactId = id;
		event.run = 11;
		event.commit();
	}

	private static final class Server implements AutoCloseable {
		private final Process process;
		private final Path stderr;
		private final Path log;
		private final InputStream stdout;
		private final OutputStream stdin;
		private int nextId;

		Server(Path executable, Path directory, boolean nativeBinary) throws Exception {
			stderr = directory.resolve("stderr.log");
			log = directory.resolve("server.log");
			Path java = Path.of(System.getProperty("java.home"), "bin", "java");
			ProcessBuilder builder = nativeBinary
					? new ProcessBuilder(executable.toString(), "-Dquarkus.mcp.server.stdio.enabled=true",
							"-Dquarkus.log.file.path=" + log)
					: new ProcessBuilder(java.toString(), "-Dquarkus.mcp.server.stdio.enabled=true",
							"-Dquarkus.log.file.path=" + log, "-jar", executable.toString());
			process = builder.directory(directory.toFile()).redirectError(stderr.toFile()).start();
			stdout = process.getInputStream();
			stdin = process.getOutputStream();
		}

		void initialize() throws Exception {
			JsonObject response = request("initialize",
					new JsonObject().put("protocolVersion", "2025-06-18").put("capabilities", new JsonObject()).put(
							"clientInfo", new JsonObject().put("name", "structured-tools-test").put("version", "1.0")));
			assertTrue(response.getJsonObject("result").containsKey("serverInfo"), response.toString());
			send(new JsonObject().put("jsonrpc", "2.0").put("method", "notifications/initialized"));
		}

		JsonObject request(String method, JsonObject params) throws Exception {
			int id = nextId++;
			send(new JsonObject().put("jsonrpc", "2.0").put("id", id).put("method", method).put("params", params));
			JsonObject response = readResponse(id);
			assertNotNull(response, () -> "No response for " + method + ". " + diagnostics());
			assertFalse(response.containsKey("error"), response.toString());
			return response;
		}

		JsonObject call(String name, JsonObject arguments) throws Exception {
			JsonObject result = request("tools/call", new JsonObject().put("name", name).put("arguments", arguments))
					.getJsonObject("result");
			assertNotNull(result, "No tool result for " + name);
			assertFalse(result.getBoolean("isError", false), result.toString());
			return result;
		}

		JsonObject structured(String name, JsonObject arguments) throws Exception {
			JsonObject result = call(name, arguments);
			JsonObject structured = result.getJsonObject("structuredContent");
			assertNotNull(structured, "No structured content for " + name + ": " + result);
			assertFalse(structured.containsKey("error"), structured.toString());
			return structured;
		}

		private void send(JsonObject request) throws Exception {
			stdin.write((request.encode() + "\n").getBytes(StandardCharsets.UTF_8));
			stdin.flush();
		}

		private JsonObject readResponse(int expectedId) throws Exception {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
			ByteArrayOutputStream line = new ByteArrayOutputStream();
			while (System.nanoTime() < deadline) {
				if (stdout.available() == 0) {
					if (!process.isAlive()) {
						return null;
					}
					Thread.sleep(10);
					continue;
				}
				int value = stdout.read();
				if (value < 0) {
					return null;
				}
				if (value != '\n') {
					line.write(value);
					continue;
				}
				String text = line.toString(StandardCharsets.UTF_8).trim();
				line.reset();
				if (!text.startsWith("{")) {
					continue;
				}
				JsonObject response = new JsonObject(text);
				if (response.getValue("id") instanceof Number id && id.intValue() == expectedId) {
					return response;
				}
			}
			return null;
		}

		private String diagnostics() {
			StringBuilder result = new StringBuilder();
			for (Path file : List.of(stderr, log)) {
				try {
					if (Files.exists(file)) {
						String text = Files.readString(file);
						result.append(file.getFileName()).append(": ")
								.append(text.substring(Math.max(0, text.length() - 3000))).append('\n');
					}
				} catch (Exception e) {
					result.append("Could not read ").append(file).append(": ").append(e.getMessage());
				}
			}
			return result.toString();
		}

		@Override
		public void close() throws Exception {
			try {
				stdin.close();
			} finally {
				process.destroyForcibly();
				process.waitFor(5, TimeUnit.SECONDS);
				stdout.close();
			}
		}
	}
}
