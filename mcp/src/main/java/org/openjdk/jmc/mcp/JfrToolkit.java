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

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 * Filtering, lookup and formatting helpers shared by the MCP tools. This is the headless equivalent
 * of the JMC UI's editor-bound JFR context - everything here works off an explicitly supplied
 * {@link IItemCollection} rather than the active editor.
 */
public final class JfrToolkit {

	private JfrToolkit() {
	}

	/**
	 * Returns the earliest start time across all events, used as the zero point for the
	 * seconds-from-recording-start parameters the tools accept.
	 */
	public static IQuantity getRecordingStart(IItemCollection items) {
		IQuantity earliest = null;
		for (IItemIterable iterable : items) {
			IMemberAccessor<IQuantity, IItem> accessor = JfrAttributes.START_TIME.getAccessor(iterable.getType());
			if (accessor == null) {
				continue;
			}
			for (IItem item : iterable) {
				IQuantity start = accessor.getMember(item);
				if (start != null && (earliest == null || start.compareTo(earliest) < 0)) {
					earliest = start;
				}
			}
		}
		return earliest;
	}

	public static IQuantity getRecordingEnd(IItemCollection items) {
		IQuantity latest = null;
		for (IItemIterable iterable : items) {
			IMemberAccessor<IQuantity, IItem> accessor = JfrAttributes.END_TIME.getAccessor(iterable.getType());
			if (accessor == null) {
				continue;
			}
			for (IItem item : iterable) {
				IQuantity end = accessor.getMember(item);
				if (end != null && (latest == null || end.compareTo(latest) > 0)) {
					latest = end;
				}
			}
		}
		return latest;
	}

	/**
	 * Builds a combined filter from event type and time range (seconds from recording start) and
	 * applies it.
	 */
	public static IItemCollection filterItems(
		IItemCollection items, IQuantity recordingStart, String eventType, Double fromSeconds, Double toSeconds) {
		IItemFilter typeFilter = eventType != null && !eventType.isBlank() ? ItemFilters.type(eventType) : null;
		IItemFilter timeFilter = buildTimeFilter(recordingStart, fromSeconds, toSeconds);

		if (typeFilter != null && timeFilter != null) {
			return items.apply(ItemFilters.and(typeFilter, timeFilter));
		} else if (typeFilter != null) {
			return items.apply(typeFilter);
		} else if (timeFilter != null) {
			return items.apply(timeFilter);
		}
		return items;
	}

	private static IItemFilter buildTimeFilter(IQuantity recordingStart, Double fromSeconds, Double toSeconds) {
		if (recordingStart == null || (fromSeconds == null && toSeconds == null)) {
			return null;
		}
		IItemFilter lower = null;
		IItemFilter upper = null;
		if (fromSeconds != null) {
			IQuantity start = recordingStart.add(UnitLookup.SECOND.quantity(fromSeconds));
			lower = ItemFilters.moreOrEqual(JfrAttributes.END_TIME, start);
		}
		if (toSeconds != null) {
			IQuantity end = recordingStart.add(UnitLookup.SECOND.quantity(toSeconds));
			upper = ItemFilters.lessOrEqual(JfrAttributes.START_TIME, end);
		}
		if (lower != null && upper != null) {
			return ItemFilters.and(lower, upper);
		}
		return lower != null ? lower : upper;
	}

	public static IMemberAccessor<?, IItem> findAccessor(IType<IItem> type, String attributeId) {
		for (IAttribute<?> attr : type.getAttributes()) {
			if (attr.getIdentifier().equals(attributeId)) {
				return attr.getAccessor(type);
			}
		}
		return null;
	}

	/**
	 * Finds a numeric (quantity) attribute by identifier on any of the event types in the
	 * collection, so callers can weight or aggregate by an attribute discovered through
	 * getAttributes rather than a hardcoded list.
	 */
	@SuppressWarnings("unchecked")
	public static IAttribute<IQuantity> findQuantityAttribute(IItemCollection items, String attributeId) {
		for (IItemIterable iterable : items) {
			for (IAttribute<?> attr : iterable.getType().getAttributes()) {
				if (attr.getIdentifier().equals(attributeId) && attr.getContentType() instanceof KindOfQuantity) {
					return (IAttribute<IQuantity>) attr;
				}
			}
		}
		return null;
	}

	/**
	 * Filtering by attribute needs both the attribute identifier and the value to match. Returns an
	 * error message when only one of the pair is supplied, null when the combination is usable.
	 */
	public static String validateFilterPair(String filterAttribute, String filterValue) {
		boolean hasAttribute = filterAttribute != null && !filterAttribute.isBlank();
		if (hasAttribute && filterValue == null) {
			return "Error: filterAttribute was given without filterValue - both are required to filter by attribute.";
		}
		if (!hasAttribute && filterValue != null) {
			return "Error: filterValue was given without filterAttribute - both are required to filter by attribute.";
		}
		return null;
	}

	/**
	 * Filters by the displayed value of an arbitrary attribute identifier. Matching on the display
	 * string keeps this usable for any attribute type without the caller having to know the
	 * underlying unit.
	 */
	public static IItemCollection filterByAttribute(IItemCollection items, String attributeId, String value) {
		List<IItem> matching = new ArrayList<>();
		if (value != null) {
			for (IItemIterable iterable : items) {
				IMemberAccessor<?, IItem> accessor = findAccessor(iterable.getType(), attributeId);
				if (accessor == null) {
					continue;
				}
				for (IItem item : iterable) {
					Object member = accessor.getMember(item);
					if (member != null && value.equals(format(member))) {
						matching.add(item);
					}
				}
			}
		}
		return ItemCollectionToolkit.build(matching.stream());
	}

	public static List<IItem> toItemList(IItemCollection items) {
		List<IItem> result = new ArrayList<>();
		for (IItemIterable iterable : items) {
			for (IItem item : iterable) {
				result.add(item);
			}
		}
		return result;
	}

	public static long countItems(IItemCollection items) {
		long count = 0;
		for (IItemIterable iterable : items) {
			count += iterable.getItemCount();
		}
		return count;
	}

	public static String format(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof IDisplayable) {
			return ((IDisplayable) value).displayUsing(IDisplayable.AUTO);
		}
		return value.toString();
	}

	public static String formatQuantity(IQuantity value) {
		return value != null ? value.displayUsing(IDisplayable.AUTO) : "N/A";
	}

	/**
	 * Describes an exception for the tools' error responses, falling back to the exception's class
	 * name when it carries no message (e.g. a bare NullPointerException).
	 */
	public static String describeError(Exception e) {
		String message = e.getMessage();
		return message != null && !message.isBlank() ? message : e.getClass().getSimpleName();
	}

	public static void appendItemAttributes(StringBuilder sb, IItem item, IType<IItem> type, String indent) {
		for (IAttribute<?> attr : type.getAttributes()) {
			IMemberAccessor<?, IItem> accessor = attr.getAccessor(type);
			if (accessor == null) {
				continue;
			}
			Object value = accessor.getMember(item);
			if (value != null) {
				sb.append(indent).append(attr.getName()).append(": ").append(format(value)).append("\n");
			}
		}
	}

	/**
	 * Resolves an optional caller supplied limit: absent or non-positive means the default, and the
	 * cap is always applied so a tool can never be asked for an unbounded amount of output.
	 */
	public static int clamp(Integer requested, int defaultValue, int max) {
		int value = requested == null || requested <= 0 ? defaultValue : requested;
		return Math.min(value, max);
	}
}
