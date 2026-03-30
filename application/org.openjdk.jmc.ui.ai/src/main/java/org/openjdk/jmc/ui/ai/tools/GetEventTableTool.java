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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.ui.ai.IAITool;

public class GetEventTableTool implements IAITool {

	private static final int MAX_ROWS = 200;
	private static final Pattern TYPE_PATTERN = Pattern.compile("\"eventType\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern FILTER_ATTR_PATTERN = Pattern
			.compile("\"filterAttribute\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern FILTER_VALUE_PATTERN = Pattern
			.compile("\"filterValue\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern COLS_PATTERN = Pattern.compile("\"columns\"\\s*:\\s*\\[([^\\]]*)\\]"); //$NON-NLS-1$
	private static final Pattern COL_VALUE_PATTERN = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$
	private static final Pattern LIMIT_PATTERN = Pattern.compile("\"limit\"\\s*:\\s*(\\d+)"); //$NON-NLS-1$
	private static final Pattern FROM_PATTERN = Pattern.compile("\"fromSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$
	private static final Pattern TO_PATTERN = Pattern.compile("\"toSeconds\"\\s*:\\s*([\\d.]+)"); //$NON-NLS-1$

	@Override
	public String getName() {
		return "get_event_table"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Returns JFR events in a compact table format with events as rows and attributes as columns." //$NON-NLS-1$
				+ " Like the Event Browser in JMC. Can filter by event type, time range, and/or attribute value." //$NON-NLS-1$
				+ " Use filterAttribute + filterValue to find all events across all types sharing a" //$NON-NLS-1$
				+ " contextual attribute (e.g. gcId=57 finds all GC-related events for that collection)." //$NON-NLS-1$
				+ " Use get_shared_attributes to discover correlation attributes."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"eventType\":{\"type\":\"string\",\"description\":\"JFR event type ID (optional - omit to search all types)\"}," //$NON-NLS-1$
				+ "\"filterAttribute\":{\"type\":\"string\",\"description\":\"Attribute ID to filter on (e.g. gcId)\"}," //$NON-NLS-1$
				+ "\"filterValue\":{\"type\":\"string\",\"description\":\"Value the attribute must match\"}," //$NON-NLS-1$
				+ "\"columns\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}," //$NON-NLS-1$
				+ "\"description\":\"Attribute IDs to include as columns (default: all)\"}," //$NON-NLS-1$
				+ "\"fromSeconds\":{\"type\":\"number\",\"description\":\"Start of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"toSeconds\":{\"type\":\"number\",\"description\":\"End of time range in seconds from recording start\"}," //$NON-NLS-1$
				+ "\"limit\":{\"type\":\"integer\",\"description\":\"Max rows (default 50)\"}," //$NON-NLS-1$
				+ "\"storeAs\":{\"type\":\"string\",\"description\":\"Store the result set under this name for later reference by other tools\"}" //$NON-NLS-1$
				+ "}}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		IItemCollection items = JfrContext.getActiveItems();
		if (items == null) {
			return "No flight recording is currently open."; //$NON-NLS-1$
		}

		String eventType = JfrContext.extractString(TYPE_PATTERN, parametersJson);
		String filterAttr = JfrContext.extractString(FILTER_ATTR_PATTERN, parametersJson);
		String filterValue = JfrContext.extractString(FILTER_VALUE_PATTERN, parametersJson);

		int limit = JfrContext.extractInt(LIMIT_PATTERN, parametersJson, 50);
		if (limit > MAX_ROWS) {
			limit = MAX_ROWS;
		}

		List<String> requestedColumns = parseColumns(parametersJson);
		String from = JfrContext.extractString(FROM_PATTERN, parametersJson);
		String to = JfrContext.extractString(TO_PATTERN, parametersJson);
		IItemCollection filtered = JfrContext.filterItems(items, eventType, from, to);

		// Apply attribute value filter before storing
		if (filterAttr != null && filterValue != null) {
			filtered = filterByAttribute(filtered, filterAttr, filterValue);
		}

		// Store the fully filtered result if requested
		String storeAs = JfrContext.extractString(JfrContext.STORE_AS_PATTERN, parametersJson);
		if (storeAs != null) {
			JfrContext.store(storeAs, filtered);
		}

		if (!filtered.hasItems()) {
			return "No events found."; //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();
		int rowCount = 0;
		String lastTypeId = null;

		for (IItemIterable iterable : filtered) {
			IType<IItem> type = iterable.getType();
			String typeId = type.getIdentifier();

			List<IAttribute<?>> columns = selectColumns(type, requestedColumns);
			List<IMemberAccessor<?, IItem>> accessors = new ArrayList<>();
			for (IAttribute<?> col : columns) {
				accessors.add(col.getAccessor(type));
			}

			for (IItem item : iterable) {
				if (rowCount >= limit) {
					sb.append("... (").append(limit).append(" rows shown)\n"); //$NON-NLS-1$ //$NON-NLS-2$
					return sb.toString();
				}

				// Write type header when scanning multiple types
				if (eventType == null && !typeId.equals(lastTypeId)) {
					if (rowCount > 0) {
						sb.append("\n"); //$NON-NLS-1$
					}
					sb.append("--- ").append(typeId).append(" ---\n"); //$NON-NLS-1$ //$NON-NLS-2$
					// Write column header for this type
					for (int i = 0; i < columns.size(); i++) {
						if (i > 0) {
							sb.append("\t"); //$NON-NLS-1$
						}
						sb.append(columns.get(i).getName());
					}
					sb.append("\n"); //$NON-NLS-1$
					lastTypeId = typeId;
				} else if (eventType != null && rowCount == 0) {
					// Single type - write header once
					for (int i = 0; i < columns.size(); i++) {
						if (i > 0) {
							sb.append("\t"); //$NON-NLS-1$
						}
						sb.append(columns.get(i).getName());
					}
					sb.append("\n"); //$NON-NLS-1$
				}

				for (int i = 0; i < accessors.size(); i++) {
					if (i > 0) {
						sb.append("\t"); //$NON-NLS-1$
					}
					IMemberAccessor<?, IItem> accessor = accessors.get(i);
					if (accessor != null) {
						Object value = accessor.getMember(item);
						sb.append(formatValue(value));
					}
				}
				sb.append("\n"); //$NON-NLS-1$
				rowCount++;
			}
		}
		if (rowCount == 0) {
			return "No events match the specified criteria."; //$NON-NLS-1$
		}
		return sb.toString();
	}

	private IItemCollection filterByAttribute(IItemCollection items, String attrId, String value) {
		java.util.List<IItem> matching = new java.util.ArrayList<>();
		for (IItemIterable iterable : items) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<?, IItem> accessor = findAccessor(type, attrId);
			if (accessor == null) {
				continue;
			}
			for (IItem item : iterable) {
				if (matchesValue(accessor.getMember(item), value)) {
					matching.add(item);
				}
			}
		}
		return org.openjdk.jmc.common.item.ItemCollectionToolkit.build(matching.stream());
	}

	private IMemberAccessor<?, IItem> findAccessor(IType<IItem> type, String attrId) {
		for (IAttribute<?> attr : type.getAttributes()) {
			if (attr.getIdentifier().equals(attrId)) {
				return attr.getAccessor(type);
			}
		}
		return null;
	}

	private boolean matchesValue(Object value, String expected) {
		if (value == null) {
			return false;
		}
		String display = value instanceof IDisplayable ? ((IDisplayable) value).displayUsing(IDisplayable.AUTO)
				: value.toString();
		return expected.equals(display);
	}

	private List<IAttribute<?>> selectColumns(IType<IItem> type, List<String> requestedColumns) {
		List<IAttribute<?>> allAttrs = type.getAttributes();
		if (requestedColumns == null || requestedColumns.isEmpty()) {
			return allAttrs;
		}
		List<IAttribute<?>> selected = new ArrayList<>();
		for (String colId : requestedColumns) {
			for (IAttribute<?> attr : allAttrs) {
				if (attr.getIdentifier().equals(colId)) {
					selected.add(attr);
					break;
				}
			}
		}
		return selected.isEmpty() ? allAttrs : selected;
	}

	private List<String> parseColumns(String json) {
		java.util.regex.Matcher colsMatcher = COLS_PATTERN.matcher(json);
		if (!colsMatcher.find()) {
			return null;
		}
		String colsArray = colsMatcher.group(1);
		List<String> cols = new ArrayList<>();
		java.util.regex.Matcher valMatcher = COL_VALUE_PATTERN.matcher(colsArray);
		while (valMatcher.find()) {
			cols.add(valMatcher.group(1));
		}
		return cols.isEmpty() ? null : cols;
	}

	private static String formatValue(Object value) {
		if (value == null) {
			return ""; //$NON-NLS-1$
		}
		if (value instanceof IDisplayable) {
			return ((IDisplayable) value).displayUsing(IDisplayable.AUTO);
		}
		return value.toString();
	}
}
