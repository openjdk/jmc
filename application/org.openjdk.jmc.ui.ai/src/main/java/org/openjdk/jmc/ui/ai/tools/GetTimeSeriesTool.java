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
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.ui.ai.IAITool;

public class GetTimeSeriesTool implements IAITool {

	private static final int MAX_POINTS = 200;
	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern ATTR_PATTERN = Pattern.compile("\"attribute\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern LIMIT_PATTERN = Pattern.compile("\"limit\"\\s*:\\s*(\\d+)"); //$NON-NLS-1$
	private static final Pattern FROM_PATTERN = Pattern.compile("\"fromSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$
	private static final Pattern TO_PATTERN = Pattern.compile("\"toSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$

	@Override
	public String getName() {
		return "get_time_series"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Extracts a time series of values for a specific event type and attribute." //$NON-NLS-1$
				+ " Returns timestamp-value pairs, useful for finding interesting time intervals." //$NON-NLS-1$
				+ " For example, use jdk.CPULoad with attribute jvmUser to find high-CPU periods," //$NON-NLS-1$
				+ " or jdk.GCHeapSummary with attribute heapUsed to track memory usage." //$NON-NLS-1$
				+ " Use get_attributes to discover available attributes for an event type."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\",\"description\":\"JFR event type ID, e.g. jdk.CPULoad\"}," //$NON-NLS-1$
				+ "\"attribute\":{\"type\":\"string\",\"description\":\"Attribute identifier to extract values for, e.g. jvmUser\"}," //$NON-NLS-1$
				+ "\"fromSeconds\":{\"type\":\"number\",\"description\":\"Start of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"toSeconds\":{\"type\":\"number\",\"description\":\"End of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"limit\":{\"type\":\"integer\",\"description\":\"Max data points to return (default 100)\"}" //$NON-NLS-1$
				+ "},\"required\":[\"eventType\",\"attribute\"]}"; //$NON-NLS-1$
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

		String attrId = JfrContext.extractString(ATTR_PATTERN, parametersJson);
		if (attrId == null) {
			return "Missing required parameter: attribute"; //$NON-NLS-1$
		}

		int limit = JfrContext.extractInt(LIMIT_PATTERN, parametersJson, 100);
		if (limit > MAX_POINTS) {
			limit = MAX_POINTS;
		}

		String from = JfrContext.extractString(FROM_PATTERN, parametersJson);
		String to = JfrContext.extractString(TO_PATTERN, parametersJson);
		IItemCollection filtered = JfrContext.filterItems(items, eventType, from, to);

		if (!filtered.hasItems()) {
			return "No events found for type: " + eventType; //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Time series for ").append(eventType).append(" / ").append(attrId).append(":\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append("Timestamp, Value\n"); //$NON-NLS-1$

		int count = 0;
		for (IItemIterable iterable : filtered) {
			IType<IItem> type = iterable.getType();

			// Find the requested attribute
			IMemberAccessor<?, IItem> valueAccessor = null;
			for (IAttribute<?> attr : type.getAttributes()) {
				if (attr.getIdentifier().equals(attrId)) {
					valueAccessor = attr.getAccessor(type);
					break;
				}
			}
			if (valueAccessor == null) {
				return "Attribute '" + attrId + "' not found on event type " + eventType; //$NON-NLS-1$ //$NON-NLS-2$
			}

			IMemberAccessor<IQuantity, IItem> timeAccessor = JfrAttributes.END_TIME.getAccessor(type);

			for (IItem item : iterable) {
				if (count >= limit) {
					sb.append("... (truncated at ").append(limit).append(" points)\n"); //$NON-NLS-1$ //$NON-NLS-2$
					return sb.toString();
				}
				IQuantity time = timeAccessor != null ? timeAccessor.getMember(item) : null;
				Object value = valueAccessor.getMember(item);
				String timeStr = time != null ? time.displayUsing(IDisplayable.AUTO) : "?"; //$NON-NLS-1$
				String valueStr = value instanceof IDisplayable ? ((IDisplayable) value).displayUsing(IDisplayable.AUTO)
						: value != null ? value.toString() : "null"; //$NON-NLS-1$
				sb.append(timeStr).append(", ").append(valueStr).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
				count++;
			}
		}
		return sb.toString();
	}
}
