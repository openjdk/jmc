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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.ui.ai.IAITool;

public class GetAttributesTool implements IAITool {

	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$

	@Override
	public String getName() {
		return "get_attributes"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Lists all available attributes for a given JFR event type, including their names," //$NON-NLS-1$
				+ " identifiers, and content types (e.g. timespan, memory, percentage)." //$NON-NLS-1$
				+ " Use this to discover what data is available before querying or aggregating."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\",\"description\":\"The JFR event type ID, e.g. jdk.JavaMonitorEnter\"}" //$NON-NLS-1$
				+ "},\"required\":[\"eventType\"]}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		IItemCollection items = JfrContext.getActiveItems();
		if (items == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		String eventType = extractString(TYPE_PATTERN, parametersJson);
		if (eventType == null) {
			return "Missing required parameter: eventType"; //$NON-NLS-1$
		}

		IItemCollection filtered = items.apply(ItemFilters.type(eventType));
		if (!filtered.hasItems()) {
			return "No events found for type: " + eventType; //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Attributes for ").append(eventType).append(":\n"); //$NON-NLS-1$ //$NON-NLS-2$

		for (IItemIterable iterable : filtered) {
			IType<IItem> type = iterable.getType();
			for (IAttribute<?> attr : type.getAttributes()) {
				sb.append("  ").append(attr.getIdentifier()); //$NON-NLS-1$
				sb.append(" (").append(attr.getName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
				if (attr.getContentType() != null) {
					sb.append(" [").append(attr.getContentType().getIdentifier()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				String desc = attr.getDescription();
				if (desc != null && !desc.isEmpty()) {
					sb.append(" - ").append(desc); //$NON-NLS-1$
				}
				sb.append("\n"); //$NON-NLS-1$
			}
			break; // All items of the same type share the same attributes
		}
		return sb.toString();
	}

	private static String extractString(Pattern pattern, String json) {
		Matcher m = pattern.matcher(json);
		return m.find() ? m.group(1) : null;
	}
}
