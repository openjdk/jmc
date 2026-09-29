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

import java.util.Map;
import java.util.Set;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.mcp.RecordingService.Recording;

/**
 * Tools for discovering what is in a recording - event types, their attributes, and the attributes
 * that link event types together.
 */
public class MetadataTools {

	@Inject
	RecordingService recordings;

	@Tool(description = "Lists all JFR event types in the recording with their event counts. Call this early - "
			+ "the event type IDs it returns are the eventType arguments for all the querying tools.")
	String getEventTypes(
		@ToolArg(description = "The recordingId from loadRecording. Leave empty when only one recording is loaded.", required = false) String recordingId) {
		try {
			Recording recording = recordings.get(recordingId);
			StringBuilder sb = new StringBuilder();
			sb.append("Event types in the recording:\n");
			for (IItemIterable iterable : recording.getItems()) {
				long count = iterable.getItemCount();
				if (count > 0) {
					sb.append("  ").append(iterable.getType().getIdentifier());
					sb.append(" (").append(iterable.getType().getName()).append("): ");
					sb.append(count).append(" events\n");
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "Error: " + JfrToolkit.describeError(e);
		}
	}

	@Tool(description = "Lists all available attributes for a given JFR event type, including their identifiers, "
			+ "names, and content types (e.g. timespan, memory, percentage). Use this to discover what data is "
			+ "available before querying, aggregating, or building a time series.")
	String getAttributes(
		@ToolArg(description = "The JFR event type ID, e.g. jdk.JavaMonitorEnter") String eventType,
		@ToolArg(description = "The recordingId from loadRecording. Leave empty when only one recording is loaded.", required = false) String recordingId) {
		try {
			Recording recording = recordings.get(recordingId);
			IItemCollection filtered = recording.getItems().apply(ItemFilters.type(eventType));
			if (!filtered.hasItems()) {
				return "No events found for type: " + eventType;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("Attributes for ").append(eventType).append(":\n");
			for (IItemIterable iterable : filtered) {
				IType<IItem> type = iterable.getType();
				for (IAttribute<?> attr : type.getAttributes()) {
					sb.append("  ").append(attr.getIdentifier());
					sb.append(" (").append(attr.getName()).append(")");
					if (attr.getContentType() != null) {
						sb.append(" [").append(attr.getContentType().getIdentifier()).append("]");
					}
					String desc = attr.getDescription();
					if (desc != null && !desc.isEmpty()) {
						sb.append(" - ").append(desc);
					}
					sb.append("\n");
				}
				break; // All items of the same type share the same attributes
			}
			return sb.toString();
		} catch (Exception e) {
			return "Error: " + JfrToolkit.describeError(e);
		}
	}

	@Tool(description = "Lists attributes that are shared across multiple event types in the recording. Shared "
			+ "attributes (like gcId, spanId, ecid) are natural correlation paths for relating events - e.g. gcId "
			+ "links jdk.GarbageCollection to jdk.GCPhasePause. If eventType is given, shows only the shared "
			+ "attributes present on that type and which other types share them. Call this early in an "
			+ "investigation to discover how the events in this particular recording can be correlated, then use "
			+ "getEventTable with filterAttribute/filterValue to pull one correlated group.")
	String getSharedAttributes(
		@ToolArg(description = "Optional event type to find correlation paths from. Omit to list all shared attributes.", required = false) String eventType,
		@ToolArg(description = "The recordingId from loadRecording. Leave empty when only one recording is loaded.", required = false) String recordingId) {
		try {
			Recording recording = recordings.get(recordingId);
			SharedAttributeIndex index = recording.getSharedAttributes();
			return eventType != null && !eventType.isBlank() ? formatForType(index, eventType) : formatAll(index);
		} catch (Exception e) {
			return "Error: " + JfrToolkit.describeError(e);
		}
	}

	private String formatForType(SharedAttributeIndex index, String eventType) {
		Map<String, Set<String>> shared = index.getSharedAttributesForType(eventType);
		if (shared.isEmpty()) {
			return "No shared attributes found for " + eventType
					+ ". This event type has no attributes that are also present on other event types.";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Shared attributes on ").append(eventType).append(" (correlation paths to other types):\n\n");
		for (Map.Entry<String, Set<String>> entry : shared.entrySet()) {
			appendAttribute(sb, index, entry.getKey());
			sb.append("\n    Shared with: ").append(String.join(", ", entry.getValue())).append("\n");
		}
		return sb.toString();
	}

	private String formatAll(SharedAttributeIndex index) {
		Map<String, Set<String>> shared = index.getSharedAttributes();
		if (shared.isEmpty()) {
			return "No shared attributes found across event types in this recording.";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Shared attributes across event types (potential correlation paths):\n\n");
		for (Map.Entry<String, Set<String>> entry : shared.entrySet()) {
			appendAttribute(sb, index, entry.getKey());
			sb.append(" - ").append(entry.getValue().size()).append(" types\n");
			for (String type : entry.getValue()) {
				sb.append("    ").append(type).append("\n");
			}
		}
		return sb.toString();
	}

	private void appendAttribute(StringBuilder sb, SharedAttributeIndex index, String key) {
		sb.append("  ").append(index.getIdentifier(key));
		sb.append(" (").append(index.getDisplayName(key)).append(")");
		String contentType = index.getContentType(key);
		if (contentType != null) {
			sb.append(" [").append(contentType).append("]");
		}
	}
}
