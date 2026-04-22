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

import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.ui.ai.IAITool;

public class GetJfrEventsTool implements IAITool {

	private static final int MAX_EVENTS = 100;
	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern LIMIT_PATTERN = Pattern.compile("\"limit\"\\s*:\\s*(\\d+)"); //$NON-NLS-1$
	private static final Pattern FROM_PATTERN = Pattern.compile("\"fromSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$
	private static final Pattern TO_PATTERN = Pattern.compile("\"toSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$

	@Override
	public String getName() {
		return "get_jfr_events"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Retrieves JFR events from the open recording, filtered by event type." //$NON-NLS-1$
				+ " Returns event attributes as text. Use get_event_types first to discover available types."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\",\"description\":\"The JFR event type ID, e.g. jdk.GCPhasePause\"}," //$NON-NLS-1$
				+ "\"fromSeconds\":{\"type\":\"number\",\"description\":\"Start of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"toSeconds\":{\"type\":\"number\",\"description\":\"End of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"limit\":{\"type\":\"integer\",\"description\":\"Max events to return (default 50)\"}" //$NON-NLS-1$
				+ "},\"required\":[\"eventType\"]}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		IItemCollection items = JfrContext.getActiveItems();
		if (items == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		String eventType = JfrContext.extractString(TYPE_PATTERN, parametersJson);
		if (eventType == null) {
			return "Missing required parameter: eventType"; //$NON-NLS-1$
		}

		int limit = JfrContext.extractInt(LIMIT_PATTERN, parametersJson, 50);
		if (limit > MAX_EVENTS) {
			limit = MAX_EVENTS;
		}

		String from = JfrContext.extractString(FROM_PATTERN, parametersJson);
		String to = JfrContext.extractString(TO_PATTERN, parametersJson);
		IItemCollection filtered = JfrContext.filterItems(items, eventType, from, to);
		if (!filtered.hasItems()) {
			return "No events found for type: " + eventType; //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (IItemIterable iterable : filtered) {
			IType<IItem> type = iterable.getType();
			for (IItem item : iterable) {
				if (count >= limit) {
					sb.append("... (truncated, ").append(limit).append(" of more events shown)\n"); //$NON-NLS-1$ //$NON-NLS-2$
					return sb.toString();
				}
				sb.append("Event #").append(count + 1).append(":\n"); //$NON-NLS-1$ //$NON-NLS-2$
				for (IAttribute<?> attr : type.getAttributes()) {
					IMemberAccessor<?, IItem> accessor = attr.getAccessor(type);
					if (accessor != null) {
						Object value = accessor.getMember(item);
						if (value != null) {
							String display = value instanceof IDisplayable
									? ((IDisplayable) value).displayUsing(IDisplayable.AUTO) : value.toString();
							sb.append("  ").append(attr.getName()).append(": ").append(display).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				}
				count++;
			}
		}
		return sb.toString();
	}

}
