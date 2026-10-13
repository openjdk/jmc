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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.vertx.core.json.Json;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;

/**
 * Encodes event values without the rounding used for human-readable tables. The returned values use
 * only JSON primitives, lists and maps, avoiding reflection on JMC implementation classes.
 */
final class EventValues {

	private EventValues() {
	}

	static Object encode(Object value) {
		if (value instanceof IQuantity quantity) {
			return encodeQuantity(quantity);
		}
		if (value instanceof IMCThread thread) {
			Map<String, Object> encoded = new LinkedHashMap<>();
			encoded.put("id", thread.getThreadId());
			encoded.put("name", thread.getThreadName());
			return encoded;
		}
		if (value instanceof Number number) {
			return encodeNumber(number, null);
		}
		if (value == null || value instanceof String || value instanceof Boolean) {
			return value;
		}
		return JfrToolkit.format(value);
	}

	static Map<String, Object> encodeNumber(Number number, IUnit unit) {
		if (unit != null) {
			return encodeQuantity(unit.quantity(number));
		}
		boolean finite = finite(number);
		Map<String, Object> encoded = new LinkedHashMap<>();
		encoded.put("value", finite ? number : null);
		// JSON consumers using doubles can recover large integers from this string.
		encoded.put("exactValue", exactString(number));
		encoded.put("unit", "");
		encoded.put("quantityKind", "number");
		return encoded;
	}

	private static Map<String, Object> encodeQuantity(IQuantity quantity) {
		Map<String, Object> encoded = encodeNumber(quantity.numberValue(), null);
		IUnit unit = quantity.getUnit();
		IUnit canonicalUnit = quantity.getType().getDefaultUnit();
		Number canonical = quantity.numberValueIn(canonicalUnit);
		encoded.put("unit", unit.getIdentifier());
		encoded.put("quantityKind", quantity.getType().getIdentifier());
		encoded.put("canonicalValue", finite(canonical) ? canonical : null);
		encoded.put("canonicalExactValue", exactString(canonical));
		encoded.put("canonicalUnit", canonicalUnit.getIdentifier());
		return encoded;
	}

	private static String exactString(Number number) {
		if (number instanceof BigDecimal decimal) {
			return decimal.toPlainString();
		}
		if ((number instanceof Double || number instanceof Float) && finite(number)) {
			return new BigDecimal(number.toString()).toPlainString();
		}
		return number.toString();
	}

	private static boolean finite(Number number) {
		return !(number instanceof Double || number instanceof Float) || Double.isFinite(number.doubleValue());
	}

	static Map<String, Object> event(IItem item, List<String> columns) {
		IType<IItem> type = QuerySupport.typeOf(item);
		Map<String, Object> attributes = new LinkedHashMap<>();
		if (columns == null || columns.isEmpty()) {
			for (IAttribute<?> attribute : type.getAttributes()) {
				IMemberAccessor<?, IItem> accessor = attribute.getAccessor(type);
				attributes.put(attribute.getIdentifier(), encode(accessor == null ? null : accessor.getMember(item)));
			}
			for (String column : List.of("startTime", "endTime", "duration", "eventThread")) {
				IMemberAccessor<?, IItem> accessor = QuerySupport.accessor(type, column);
				if (accessor != null && !attributes.containsKey(column)) {
					attributes.put(column, encode(accessor.getMember(item)));
				}
			}
		} else {
			for (String column : columns) {
				IMemberAccessor<?, IItem> accessor = QuerySupport.accessor(type, column);
				attributes.put(column, encode(accessor == null ? null : accessor.getMember(item)));
			}
		}
		Map<String, Object> encoded = new LinkedHashMap<>();
		encoded.put("eventType", type.getIdentifier());
		encoded.put("attributes", attributes);
		return encoded;
	}

	static String json(Object value) {
		return Json.encode(value);
	}

	/**
	 * An empty list means all available attributes. Explicit lists preserve the caller's order.
	 */
	static List<String> columns(String commaSeparated) {
		if (commaSeparated == null || commaSeparated.isBlank()) {
			return List.of();
		}
		Set<String> columns = new LinkedHashSet<>();
		for (String entry : commaSeparated.split(",", -1)) {
			String column = entry.trim();
			if (column.isEmpty()) {
				throw new IllegalArgumentException("Attribute identifiers must not be empty");
			}
			if (!columns.add(column)) {
				throw new IllegalArgumentException("Duplicate attribute identifier: " + column);
			}
		}
		return new ArrayList<>(columns);
	}
}
