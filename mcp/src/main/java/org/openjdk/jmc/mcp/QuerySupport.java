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
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.selection.NamedSelectionRegistry.Entry;
import org.openjdk.jmc.mcp.RecordingService.Recording;

/** Shared typed predicates and result-set selection for analytical queries. */
public final class QuerySupport {

	private static final Set<String> OPERATORS = Set.of("eq", "ne", "gt", "gte", "lt", "lte", "contains", "exists",
			"notExists");

	@RegisterForReflection
	public record Predicate(String attribute, String operator, String value, String unit) {
		public Predicate {
			// Clients commonly send an empty string for an unused unit.
			unit = unit == null || unit.isBlank() ? null : unit.trim();
		}
	}

	private QuerySupport() {
	}

	public static IItemCollection select(
		Recording recording, String resultSet, String eventType, Double fromSeconds, Double toSeconds,
		List<Predicate> filters) {
		validateTimeRange(fromSeconds, toSeconds);
		IItemCollection source = recording.getItems();
		if (resultSet != null && !resultSet.isBlank()) {
			source = recording.getSelections().getItems(resultSet).orElse(null);
			if (source == null) {
				throw new IllegalArgumentException("No stored result set named '" + resultSet + "'. Available: "
						+ recording.getSelections().list().stream().map(Entry::getName).toList());
			}
		}
		IItemCollection selected = JfrToolkit.filterItems(source, recording.getStart(), eventType, fromSeconds,
				toSeconds);
		if (filters == null || filters.isEmpty()) {
			return selected;
		}
		IItemCollection metadata = JfrToolkit.filterItems(source, recording.getStart(), eventType, null, null);
		if (!metadata.hasItems()) {
			metadata = JfrToolkit.filterItems(recording.getItems(), recording.getStart(), eventType, null, null);
		}
		List<IItemFilter> predicates = new ArrayList<>();
		for (Predicate filter : filters) {
			validatePredicate(filter);
			requireAttribute(metadata, filter.attribute());
			validateOperand(metadata, filter);
			predicates.add(itemFilter(filter));
		}
		return selected.apply(ItemFilters.and(predicates.toArray(IItemFilter[]::new)));
	}

	private static IItemFilter itemFilter(Predicate filter) {
		return type -> {
			IAttribute<?> attribute = attribute(type, filter.attribute());
			if (attribute == null) {
				return ("notExists".equals(filter.operator()) ? ItemFilters.all() : ItemFilters.none())
						.getPredicate(type);
			}
			if ("exists".equals(filter.operator())) {
				return ItemFilters.isNotNull(attribute).getPredicate(type);
			}
			if ("notExists".equals(filter.operator())) {
				return ItemFilters.isNull(attribute).getPredicate(type);
			}
			IMemberAccessor<?, IItem> accessor = attribute.getAccessor(type);
			if (accessor == null) {
				return ItemFilters.none().getPredicate(type);
			}
			return item -> matches(accessor.getMember(item), filter);
		};
	}

	public static void validateTimeRange(Double from, Double to) {
		if ((from != null && (!Double.isFinite(from) || from < 0)) || (to != null && (!Double.isFinite(to) || to < 0))
				|| (from != null && to != null && from > to)) {
			throw new IllegalArgumentException(
					"Time bounds must be finite, non-negative and fromSeconds <= toSeconds.");
		}
	}

	private static void validatePredicate(Predicate filter) {
		if (filter == null || filter.attribute() == null || filter.attribute().isBlank() || filter.operator() == null
				|| !OPERATORS.contains(filter.operator())) {
			throw new IllegalArgumentException("Each filter needs an attribute and an operator: " + OPERATORS);
		}
		if (!Set.of("exists", "notExists").contains(filter.operator()) && filter.value() == null) {
			throw new IllegalArgumentException("Filter '" + filter.attribute() + "' requires a value.");
		}
	}

	private static void validateOperand(IItemCollection metadata, Predicate filter) {
		if (Set.of("exists", "notExists").contains(filter.operator())) {
			return;
		}
		for (IItemIterable iterable : metadata) {
			if (!iterable.hasItems()) {
				continue;
			}
			IAttribute<?> attribute = attribute(iterable.getType(), filter.attribute());
			if (attribute == null || attribute.getAccessor(iterable.getType()) == null) {
				continue;
			}
			if (attribute.getContentType() instanceof KindOfQuantity<?> kind) {
				decimal(filter.value());
				if ("contains".equals(filter.operator())) {
					throw new IllegalArgumentException("contains requires a text attribute.");
				}
				if (filter.unit() != null && kind.getUnit(filter.unit()) == null) {
					throw new IllegalArgumentException("Unknown unit '" + filter.unit() + "' for " + filter.attribute());
				}
			} else if (isNumericAttribute(attribute)) {
				decimal(filter.value());
				if ("contains".equals(filter.operator()) || filter.unit() != null) {
					throw new IllegalArgumentException("Raw numeric attributes require numeric comparisons without units.");
				}
			} else if (filter.unit() != null || !Set.of("eq", "ne", "contains").contains(filter.operator())) {
				throw new IllegalArgumentException("Ordering comparisons and units require numeric attributes: "
						+ filter.attribute());
			}
		}
	}

	public static boolean isNumericAttribute(IAttribute<?> attribute) {
		Object kind = attribute.getContentType();
		return kind instanceof KindOfQuantity || kind == UnitLookup.RAW_NUMBER || kind == UnitLookup.RAW_LONG
				|| kind == UnitLookup.COUNT || kind == UnitLookup.INDEX || kind == UnitLookup.IDENTIFIER;
	}

	public static IAttribute<?> attribute(IType<IItem> type, String id) {
		for (IAttribute<?> attribute : type.getAttributes()) {
			if (attribute.getIdentifier().equals(id)) {
				return attribute;
			}
		}
		return switch (id) {
		case "duration" -> JfrAttributes.DURATION;
		case "startTime" -> JfrAttributes.START_TIME;
		case "endTime" -> JfrAttributes.END_TIME;
		case "eventThread" -> JfrAttributes.EVENT_THREAD;
		default -> null;
		};
	}

	public static IMemberAccessor<?, IItem> accessor(IType<IItem> type, String id) {
		IAttribute<?> attribute = attribute(type, id);
		return attribute != null ? attribute.getAccessor(type) : null;
	}

	public static Object value(IItem item, String id) {
		IMemberAccessor<?, IItem> accessor = accessor(typeOf(item), id);
		return accessor != null ? accessor.getMember(item) : null;
	}

	@SuppressWarnings("unchecked")
	public static IType<IItem> typeOf(IItem item) {
		return (IType<IItem>) item.getType();
	}

	public static void requireAttribute(IItemCollection items, String id) {
		for (IItemIterable iterable : items) {
			if (accessor(iterable.getType(), id) != null) {
				return;
			}
		}
		throw new IllegalArgumentException("Unknown attribute '" + id + "' in the selected event types.");
	}

	static boolean matches(Object member, Predicate filter) {
		if ("exists".equals(filter.operator())) {
			return member != null;
		}
		if ("notExists".equals(filter.operator())) {
			return member == null;
		}
		// Missing values never satisfy a comparison, including ne.
		if (member == null) {
			return false;
		}
		int comparison;
		if (member instanceof IQuantity quantity) {
			Number nativeNumber = quantity.numberValue();
			if (!finite(nativeNumber)) {
				return false;
			}
			IUnit unit = filter.unit() == null ? quantity.getUnit().getContentType().getDefaultUnit()
					: quantity.getUnit().getContentType().getUnit(filter.unit());
			if (unit == null) {
				throw new IllegalArgumentException("Unknown unit '" + filter.unit() + "' for " + filter.attribute());
			}
			BigDecimal operand = decimal(filter.value());
			BigDecimal operandInMemberUnit = convertOperand(operand, unit, quantity.getUnit());
			if (nativeNumber instanceof Long) {
				try {
					comparison = quantity.compareTo(quantity.getUnit().quantity(operandInMemberUnit.longValueExact()));
				} catch (ArithmeticException e) {
					// Fractional or out-of-range operands cannot be represented exactly as a JMC long quantity.
					comparison = decimal(nativeNumber).compareTo(operandInMemberUnit);
				}
			} else {
				// Keep the caller's adjacent integer operand distinct from a native double value.
				comparison = decimal(nativeNumber).compareTo(operandInMemberUnit);
			}
		} else if (member instanceof Number number) {
			if (!finite(number)) {
				return false;
			}
			comparison = decimal(number).compareTo(decimal(filter.value()));
		} else {
			if (filter.unit() != null) {
				throw new IllegalArgumentException("Units require a numeric quantity attribute.");
			}
			String value = member instanceof String || member instanceof Boolean ? member.toString()
					: JfrToolkit.format(member);
			if ("contains".equals(filter.operator())) {
				return value.contains(filter.value());
			}
			if (!Set.of("eq", "ne").contains(filter.operator())) {
				throw new IllegalArgumentException("Ordering comparisons require numeric attributes: " + filter.attribute());
			}
			comparison = value.compareTo(filter.value());
		}
		return switch (filter.operator()) {
		case "eq" -> comparison == 0;
		case "ne" -> comparison != 0;
		case "gt" -> comparison > 0;
		case "gte" -> comparison >= 0;
		case "lt" -> comparison < 0;
		case "lte" -> comparison <= 0;
		default -> throw new IllegalArgumentException("contains requires a text attribute.");
		};
	}

	public static BigDecimal decimal(Number number) {
		return decimal(number.toString());
	}

	private static boolean finite(Number number) {
		return !(number instanceof Double || number instanceof Float) || Double.isFinite(number.doubleValue());
	}

	private static BigDecimal decimal(String value) {
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Expected a finite numeric value: " + value);
		}
	}

	public static int compareQuantities(IQuantity first, IQuantity second) {
		if (first.getUnit().getContentType() != second.getUnit().getContentType()) {
			throw new IllegalArgumentException("Cannot compare quantities of different kinds.");
		}
		return first.compareTo(second);
	}

	/**
	 * Formats the numerical result supplied by JMC without recalculating its unit conversion.
	 */
	public static BigDecimal quantityDecimal(IQuantity quantity, IUnit target) {
		return decimal(quantity.numberValueIn(target));
	}

	/**
	 * Converts a caller's filter literal into the event member's unit before constructing a JMC
	 * quantity. This input boundary preserves adjacent long predicates when a fractional literal
	 * (for example, seconds) becomes an exact integral member value (for example, nanoseconds).
	 */
	private static BigDecimal convertOperand(BigDecimal value, IUnit source, IUnit target) {
		if (source.equals(target)) {
			return value;
		}
		try {
			Number converted = source.quantity(value.longValueExact()).numberValueIn(target);
			if (converted instanceof Long) {
				return decimal(converted);
			}
		} catch (ArithmeticException e) {
			// Keep fractional or out-of-range values as decimals rather than narrowing them.
		}
		var transform = source.valueTransformTo(target);
		if (transform.isInteger()) {
			BigDecimal offset = decimal(transform.targetNumber(0L));
			BigDecimal multiplier = decimal(transform.targetNumber(1L)).subtract(offset);
			return value.multiply(multiplier).add(offset);
		}
		var inverse = transform.invert();
		if (inverse.isInteger()) {
			BigDecimal offset = decimal(inverse.targetNumber(0L));
			BigDecimal multiplier = decimal(inverse.targetNumber(1L)).subtract(offset);
			BigDecimal numerator = value.subtract(offset);
			try {
				return numerator.divide(multiplier);
			} catch (ArithmeticException e) {
				return numerator.divide(multiplier, MathContext.DECIMAL128);
			}
		}
		return value.multiply(BigDecimal.valueOf(transform.getMultiplier()))
				.add(BigDecimal.valueOf(transform.getOffset()));
	}
}
