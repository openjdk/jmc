/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.configuration.internal;

import static org.openjdk.jmc.common.unit.BinaryPrefix.EXBI;
import static org.openjdk.jmc.common.unit.DecimalPrefix.MICRO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.MILLI;
import static org.openjdk.jmc.common.unit.DecimalPrefix.NANO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.NONE;
import static org.openjdk.jmc.common.unit.UnitLookup.BYTE;
import static org.openjdk.jmc.common.unit.UnitLookup.DAY;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.common.unit.UnitLookup.HOUR;
import static org.openjdk.jmc.common.unit.UnitLookup.MEMORY;
import static org.openjdk.jmc.common.unit.UnitLookup.MINUTE;
import static org.openjdk.jmc.common.unit.UnitLookup.NANOSECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;
import static org.openjdk.jmc.common.unit.UnitLookup.YEAR;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import org.openjdk.jmc.common.unit.ComparableConstraint;
import org.openjdk.jmc.common.unit.CustomUnitSelector;
import org.openjdk.jmc.common.unit.DecimalPrefix;
import org.openjdk.jmc.common.unit.DecimalUnitSelector;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IFormatter;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity.LinearUnitSelector;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.unit.WrappingPersister;
import org.openjdk.jmc.flightrecorder.configuration.ConfigurationToolkit;

/**
 * Constraints common for multiple JFR versions.
 */
public class CommonConstraints {
	public final static IQuantity EVERY_CHUNK_MAGIC_INSTANCE = NANOSECOND.quantity(0);
	public final static IQuantity BEGIN_CHUNK_MAGIC_INSTANCE = NANOSECOND.quantity(0);
	public final static IQuantity END_CHUNK_MAGIC_INSTANCE = NANOSECOND.quantity(0);
	private final static String EVERY_CHUNK = "everyChunk"; //$NON-NLS-1$
	private final static String BEGIN_CHUNK = "beginChunk"; //$NON-NLS-1$
	private final static String END_CHUNK = "endChunk"; //$NON-NLS-1$

	/**
	 * Constraint that persists and parses duration values compatible with the non-SI-conforming
	 * representation that JFR unfortunately uses. However, the output is always SI-conforming.
	 */
	private static class TimePersisterBrokenSI extends WrappingPersister<IQuantity> {
		private static final LinearUnitSelector UNIT_SELECTOR;
		private static final IQuantity MIN_REPRESENTABLE = NANOSECOND.quantity(1);

		static {
			// Error message claims: '1 min' is not a valid timespan. Shoule be numeric value followed by a unit, i.e. 20 ms. Valid units are ns, us, s, m, h and d.
			DecimalPrefix[] prefixes = {NANO, MICRO, MILLI, NONE};
			LinearUnitSelector systemSelector = new DecimalUnitSelector(TIMESPAN, Arrays.asList(prefixes));
			// Omitting MINUTE since JFR cannot handle SI and we do not want to spread non-SI.
			LinearUnit[] units = {HOUR, DAY};
			// Simplest tick set that will pass the checks in CustomUnitSelector. Not actually used.
			SortedSet<ITypedQuantity<LinearUnit>> ticks = new TreeSet<>(Collections.singleton(HOUR.quantity(1)));
			UNIT_SELECTOR = new CustomUnitSelector(TIMESPAN, systemSelector, Arrays.asList(units),
					new DecimalUnitSelector(TIMESPAN, DAY), ticks);
		}

		public TimePersisterBrokenSI() {
			super(TIMESPAN);
		}

		@Override
		@SuppressWarnings("nls")
		public String persistableString(IQuantity value) {
			if (MIN_REPRESENTABLE.compareTo(value) > 0) {
				return "0 ns";
			}
			@SuppressWarnings("unchecked")
			ITypedQuantity<LinearUnit> typedValue = (ITypedQuantity<LinearUnit>) value;
			LinearUnit unit = UNIT_SELECTOR.getLargestExactUnit(typedValue);
			if (unit == null) {
				unit = UNIT_SELECTOR.getPreferredUnit(typedValue, 1, 1000000000);
				if (unit == null) {
					unit = NANOSECOND;
				}
			}
			// NOTE: We might theoretically get out of range here, but in practice values are constrained in magnitude when this class is used.
			return super.persistableString(unit.quantity(value.clampedLongValueIn(unit)));
		}

		@Override
		@SuppressWarnings("nls")
		public IQuantity parsePersisted(String persistedValue) throws QuantityConversionException {
			if (persistedValue.endsWith("m")) {
				persistedValue += "in";
				// FIXME: Log or warn that non-SI-conforming input was received.
			}
			IQuantity value = super.parsePersisted(persistedValue);
			if ((value.getUnit() == SECOND) && (value.longValue() % 60 == 0)) {
				return MINUTE.quantity(value.clampedLongValueIn(MINUTE));
			}
			return value;
		}
	}

	private static final class PeriodPersister extends TimePersisterBrokenSI implements IFormatter<IQuantity> {
		@Override
		public boolean validate(IQuantity value) {
			if (value == EVERY_CHUNK_MAGIC_INSTANCE) {
				return true;
			}
			return super.validate(value);
		}

		@Override
		public String persistableString(IQuantity value) {
			return (value == EVERY_CHUNK_MAGIC_INSTANCE) ? EVERY_CHUNK : super.persistableString(value);
		}

		@Override
		public IQuantity parsePersisted(String persistedValue) throws QuantityConversionException {
			return EVERY_CHUNK.equals(persistedValue) ? EVERY_CHUNK_MAGIC_INSTANCE
					: super.parsePersisted(persistedValue);
		}

		@Override
		@SuppressWarnings("nls")
		public String interactiveFormat(IQuantity value) {
			return (value == EVERY_CHUNK_MAGIC_INSTANCE) ? "" : super.interactiveFormat(value);
		}

		@Override
		public IQuantity parseInteractive(String interactiveValue) throws QuantityConversionException {
			return (interactiveValue.isEmpty()) ? EVERY_CHUNK_MAGIC_INSTANCE : super.parseInteractive(interactiveValue);
		}

		@Override
		public String format(IQuantity value) {
			// FIXME: Migrate localized value from configuration GUI.
			return (value == EVERY_CHUNK_MAGIC_INSTANCE)
					? Messages.getString(Messages.CommonConstraints_ONCE_EVERY_CHUNK) : super.interactiveFormat(value);
		}
	}

	private static final class PeriodPersisterV2 extends TimePersisterBrokenSI implements IFormatter<IQuantity> {

		@Override
		public boolean validate(IQuantity value) {
			if (value == EVERY_CHUNK_MAGIC_INSTANCE || value == BEGIN_CHUNK_MAGIC_INSTANCE
					|| value == END_CHUNK_MAGIC_INSTANCE) {
				return true;
			}
			return super.validate(value);
		}

		@Override
		public String persistableString(IQuantity value) {
			if (value == EVERY_CHUNK_MAGIC_INSTANCE) {
				return EVERY_CHUNK;
			} else if (value == BEGIN_CHUNK_MAGIC_INSTANCE) {
				return BEGIN_CHUNK;
			} else if (value == END_CHUNK_MAGIC_INSTANCE) {
				return END_CHUNK;
			}
			return super.persistableString(value);
		}

		@Override
		public IQuantity parsePersisted(String persistedValue) throws QuantityConversionException {
			if (EVERY_CHUNK.equals(persistedValue)) {
				return EVERY_CHUNK_MAGIC_INSTANCE;
			} else if (BEGIN_CHUNK.equals(persistedValue)) {
				return BEGIN_CHUNK_MAGIC_INSTANCE;
			} else if (END_CHUNK.equals(persistedValue)) {
				return END_CHUNK_MAGIC_INSTANCE;
			}
			return super.parsePersisted(persistedValue);
		}

		@Override
		public String interactiveFormat(IQuantity value) {
			if (value == EVERY_CHUNK_MAGIC_INSTANCE) {
				return ""; //$NON-NLS-1$
			}
			return super.interactiveFormat(value);
		}

		@Override
		public IQuantity parseInteractive(String interactiveValue) throws QuantityConversionException {
			if (interactiveValue.isEmpty()) {
				return EVERY_CHUNK_MAGIC_INSTANCE;
			}
			return super.parseInteractive(interactiveValue);
		}

		@Override
		public String format(IQuantity value) {
			// FIXME: Migrate localized value from configuration GUI.
			if (value == EVERY_CHUNK_MAGIC_INSTANCE) {
				return Messages.getString(Messages.CommonConstraints_ONCE_EVERY_CHUNK);
			} else if (value == BEGIN_CHUNK_MAGIC_INSTANCE) {
				return Messages.getString(Messages.CommonConstraints_BEGINNING_OF_EVERY_CHUNK);
			} else if (value == END_CHUNK_MAGIC_INSTANCE) {
				return Messages.getString(Messages.CommonConstraints_END_OF_EVERY_CHUNK);
			}
			return super.interactiveFormat(value);
		}
	}

	private static class LongInUnitPersister extends WrappingPersister<IQuantity> {
		private final IUnit unit;

		public LongInUnitPersister(IUnit unit) {
			super(unit.getContentType());
			this.unit = unit;
		}

		@Override
		public String persistableString(IQuantity value) {
			return Long.toString(value.clampedLongValueIn(unit));
		}

		@Override
		public IQuantity parsePersisted(String persistedValue) throws QuantityConversionException {
			try {
				return unit.quantity(Long.parseLong(persistedValue));
			} catch (NumberFormatException e) {
				throw QuantityConversionException.unparsable(persistedValue, unit.quantity(1234), this);
			}
		}
	}

	public final static IConstraint<IQuantity> POSITIVE_TIMESPAN = new ComparableConstraint<>(
			new TimePersisterBrokenSI(), SECOND.quantity(0), YEAR.quantity(200));
	public final static IConstraint<IQuantity> PERIOD_V1 = new ComparableConstraint<>(new PeriodPersister(),
			NANOSECOND.quantity(1), YEAR.quantity(1));
	public final static IConstraint<IQuantity> PERIOD_V2 = new ComparableConstraint<>(new PeriodPersisterV2(),
			NANOSECOND.quantity(1), YEAR.quantity(1));
	public final static IConstraint<IQuantity> POSITIVE_MEMORY = new ComparableConstraint<>(
			new LongInUnitPersister(BYTE), BYTE.quantity(0), MEMORY.getUnit(EXBI).quantity(4));
	public final static IConstraint<IQuantity> POINT_IN_TIME = new ComparableConstraint<>(
			new LongInUnitPersister(EPOCH_MS), EPOCH_MS.quantity(0), EPOCH_MS.quantity(Long.MAX_VALUE));

	private static final Map<String, IConstraint<?>> JFR1_TYPE_TO_CONSTRAINT;

	private static final Map<String, IConstraint<?>> JFR2_TYPE_TO_CONSTRAINT;
	private static final Map<IConstraint<?>, String> CONSTRAINT_TO_JFR2_TYPE;

	static {
		JFR1_TYPE_TO_CONSTRAINT = makeJFRv1ConstraintMap();
		JFR2_TYPE_TO_CONSTRAINT = makeJFRv2ConstraintMap();

		Map<IConstraint<?>, String> constraintToContentType = new HashMap<>();
		for (Entry<String, IConstraint<?>> entry : JFR2_TYPE_TO_CONSTRAINT.entrySet()) {
			constraintToContentType.put(entry.getValue(), entry.getKey());
		}
		// Remove constraint used for fallback when no content type was specified, so that this isn't persisted.
		// This may need to be adjusted so that fallbacks can be distinguished if usage changes.
		constraintToContentType.remove(UnitLookup.PLAIN_TEXT.getPersister());
		CONSTRAINT_TO_JFR2_TYPE = constraintToContentType;
	}

	@SuppressWarnings("nls")
	private static Map<String, IConstraint<?>> makeJFRv1ConstraintMap() {
		Map<String, IConstraint<?>> jfr1types = new HashMap<>();
		// These are the content types known to have been used in JFC files. More might have worked. Unsure if there's any specification.
		jfr1types.put("timespan", POSITIVE_TIMESPAN);
		jfr1types.put("text", UnitLookup.PLAIN_TEXT.getPersister());
		return jfr1types;
	}

	@SuppressWarnings("nls")
	private static Map<String, IConstraint<?>> makeJFRv2ConstraintMap() {
		Map<String, IConstraint<?>> jfr2types = new HashMap<>();
		final String jfrPkg = "jdk.jfr.";
		jfr2types.put(jfrPkg + "Percentage", UnitLookup.PERCENTAGE);
		jfr2types.put(jfrPkg + "Timespan", POSITIVE_TIMESPAN);
		jfr2types.put(jfrPkg + "Timestamp", POINT_IN_TIME);
		jfr2types.put(jfrPkg + "Period", PERIOD_V2);
		// Format never seen, not sufficiently specified. (Used to be mixed up with PERIOD.)
//		jfr2types.put(jfrPkg + "Frequency", null);
		jfr2types.put(jfrPkg + "Flag", UnitLookup.FLAG.getPersister());
		jfr2types.put(jfrPkg + "MemoryAddress", UnitLookup.ADDRESS);
		jfr2types.put(jfrPkg + "DataAmount", POSITIVE_MEMORY);
		return jfr2types;
	}

	public static IConstraint<?> forContentTypeV1(String typeName) {
		IConstraint<?> constraint = JFR1_TYPE_TO_CONSTRAINT.get(typeName);
		return (constraint != null) ? constraint : UnitLookup.PLAIN_TEXT.getPersister();
	}

	public static String toMatchingContentTypeV2(IConstraint<?> constraint) {
		return CONSTRAINT_TO_JFR2_TYPE.get(constraint);
	}

	public static IConstraint<?> forContentTypeV2(String typeName, String defaultPersistedString) {
		IConstraint<?> constraint = JFR2_TYPE_TO_CONSTRAINT.get(typeName);
		if (constraint != null) {
			try {
				constraint.parsePersisted(defaultPersistedString);
				return constraint;
			} catch (QuantityConversionException e) {
				ConfigurationToolkit.getLogger().log(Level.WARNING, e.getMessage(), e);
				// Fall through to PLAIN_TEXT
			}
		} else if ("true".equals(defaultPersistedString) || "false".equals(defaultPersistedString)) { //$NON-NLS-1$ //$NON-NLS-2$
			// Patch for Thread (at least)
			return UnitLookup.FLAG.getPersister();
		} else {
			// FIXME: Only log newly encountered (determined by inserting into other set, preferably).
			ConfigurationToolkit.getLogger().log(Level.WARNING, "Couldn't find constraint for " + typeName); //$NON-NLS-1$
		}
		return UnitLookup.PLAIN_TEXT.getPersister();
	}

	public static IConstraint<?> forContentTypeV2(String typeName) {
		IConstraint<?> constraint = JFR2_TYPE_TO_CONSTRAINT.get(typeName);
		return (constraint != null) ? constraint : UnitLookup.PLAIN_TEXT.getPersister();
	}
}
