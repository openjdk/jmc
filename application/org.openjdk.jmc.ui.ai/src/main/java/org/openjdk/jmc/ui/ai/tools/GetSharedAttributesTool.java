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
package org.openjdk.jmc.ui.ai.tools;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.ui.ai.IAITool;

/**
 * Tool that reveals attributes shared across multiple event types in the recording. These are
 * natural correlation paths for finding related events - e.g. gcId links GarbageCollection events
 * to GCPhasePause events, spanId links distributed tracing events, etc.
 */
public class GetSharedAttributesTool implements IAITool {

	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$

	@Override
	public String getName() {
		return "get_shared_attributes"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Lists attributes that are shared across multiple event types in the recording." //$NON-NLS-1$
				+ " Shared attributes (like gcId, spanId, ecid) are natural correlation paths" //$NON-NLS-1$
				+ " for finding related events using find_related_events." //$NON-NLS-1$
				+ " If eventType is specified, shows only shared attributes available on that type" //$NON-NLS-1$
				+ " and which other types share them. Call this early in analysis to discover" //$NON-NLS-1$
				+ " how events in the recording can be correlated."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\"," //$NON-NLS-1$
				+ "\"description\":\"Optional: event type to find correlation paths from. If omitted, lists all shared attributes.\"}" //$NON-NLS-1$
				+ "}}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		IItemCollection items = JfrContext.getActiveItems();
		if (items == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		SharedAttributeIndex index = SharedAttributeIndex.getIndex(items);
		String eventType = JfrContext.extractString(TYPE_PATTERN, parametersJson);

		if (eventType != null) {
			return formatForType(index, eventType);
		}
		return formatAll(index);
	}

	private String formatForType(SharedAttributeIndex index, String eventType) {
		Map<String, Set<String>> shared = index.getSharedAttributesForType(eventType);
		if (shared.isEmpty()) {
			return "No shared attributes found for " + eventType //$NON-NLS-1$
					+ ". This event type has no attributes that are also present on other event types."; //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Shared attributes on ").append(eventType).append(" (correlation paths to other types):\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
		for (Map.Entry<String, Set<String>> entry : shared.entrySet()) {
			String attrId = entry.getKey();
			sb.append("  ").append(attrId); //$NON-NLS-1$
			sb.append(" (").append(index.getDisplayName(attrId)).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
			String contentType = index.getContentType(attrId);
			if (contentType != null) {
				sb.append(" [").append(contentType).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			sb.append("\n    Shared with: "); //$NON-NLS-1$
			sb.append(String.join(", ", entry.getValue())); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	private String formatAll(SharedAttributeIndex index) {
		Map<String, Set<String>> shared = index.getSharedAttributes();
		if (shared.isEmpty()) {
			return "No shared attributes found across event types in this recording."; //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Shared attributes across event types (potential correlation paths):\n\n"); //$NON-NLS-1$
		for (Map.Entry<String, Set<String>> entry : shared.entrySet()) {
			String attrId = entry.getKey();
			Set<String> types = entry.getValue();
			sb.append("  ").append(attrId); //$NON-NLS-1$
			sb.append(" (").append(index.getDisplayName(attrId)).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
			String contentType = index.getContentType(attrId);
			if (contentType != null) {
				sb.append(" [").append(contentType).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			sb.append(" - ").append(types.size()).append(" types\n"); //$NON-NLS-1$ //$NON-NLS-2$
			for (String type : types) {
				sb.append("    ").append(type).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return sb.toString();
	}
}
