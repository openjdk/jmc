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
package org.openjdk.jmc.common.unit;

import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.unit.BinaryPrefix.GIBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.NOBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.PEBI;
import static org.openjdk.jmc.common.unit.BinaryPrefix.YOBI;
import static org.openjdk.jmc.common.unit.DecimalPrefix.MICRO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.MILLI;
import static org.openjdk.jmc.common.unit.DecimalPrefix.NANO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.NONE;
import static org.openjdk.jmc.common.unit.DecimalPrefix.PICO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.YOCTO;
import static org.openjdk.jmc.common.unit.DecimalPrefix.YOTTA;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCClassLoader;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCModule;
import org.openjdk.jmc.common.IMCOldObject;
import org.openjdk.jmc.common.IMCOldObjectArray;
import org.openjdk.jmc.common.IMCOldObjectField;
import org.openjdk.jmc.common.IMCOldObjectGcRoot;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.messages.internal.Messages;
import org.openjdk.jmc.common.util.LabeledIdentifier;
import org.openjdk.jmc.common.util.MethodToolkit;

/**
 * This class is responsible for configuring the different units that are available in Mission
 * Control.
 */
@SuppressWarnings("nls")
final public class UnitLookup {
	private static final String UNIT_ID_SEPARATOR = ":";
	public static final LinearKindOfQuantity MEMORY = createMemory();
	public static final LinearKindOfQuantity TIMESPAN = createTimespan();
	/*
	 * NOTE: These 3 (count, index, and identifier) cannot be persisted/restored due to Long(1) and
	 * Integer(1) not being equal or comparable. We either need to split into concrete wrappers,
	 * support a custom Comparator, or wrap into a (simple) IQuantity.
	 */
	public static final ContentType<Number> COUNT = createCount();
	public static final ContentType<Number> INDEX = createIndex();
	public static final ContentType<Number> IDENTIFIER = createIdentifier();
	public static final KindOfQuantity<TimestampUnit> TIMESTAMP = createTimestamp(TIMESPAN);
	public static final LinearKindOfQuantity PERCENTAGE = createPercentage();
	public static final LinearKindOfQuantity NUMBER = createNumber();
	/**
	 * NOTE: Temporary placeholder for raw numerical values, primitive wrappers. Use sparingly.
	 */
	public static final ContentType<Number> RAW_NUMBER = createRawNumber();
	/**
	 * NOTE: Temporary placeholder for raw long values to allow for comparable uses.
	 */
	public static final ContentType<Long> RAW_LONG = createRawLong();
	public static final ContentType<IUnit> UNIT = createSyntheticContentType("unit");
	public static final ContentType<Object> UNKNOWN = createSyntheticContentType("unknown");
	public static final ContentType<String> PLAIN_TEXT = createStringContentType("text");
	public static final ContentType<IMCOldObject> OLD_OBJECT = createSyntheticContentType("oldObject");
	public static final ContentType<IMCOldObjectArray> OLD_OBJECT_ARRAY = createSyntheticContentType("oldObjectArray");
	public static final ContentType<IMCOldObjectField> OLD_OBJECT_FIELD = createSyntheticContentType("oldObjectField");
	public static final ContentType<IMCOldObjectGcRoot> OLD_OBJECT_GC_ROOT = createSyntheticContentType(
			"oldObjectGcRoot");
	public static final ContentType<IMCMethod> METHOD = createSyntheticContentType("method");
	public static final ContentType<IMCType> CLASS = createJavaTypeContentType("class");
	public static final ContentType<IMCClassLoader> CLASS_LOADER = createSyntheticContentType("classLoader");
	public static final ContentType<IMCPackage> PACKAGE = createSyntheticContentType("package");
	public static final ContentType<IMCModule> MODULE = createSyntheticContentType("module");
	public static final ContentType<IMCStackTrace> STACKTRACE = createSyntheticContentType("stacktrace");
	public static final ContentType<IMCFrame> STACKTRACE_FRAME = createSyntheticContentType("frame");
	public static final ContentType<IMCThread> THREAD = createSyntheticContentType("thread");
	public static final ContentType<IMCThreadGroup> THREAD_GROUP = createSyntheticContentType("threadGroup");
	public static final ContentType<LabeledIdentifier> LABELED_IDENTIFIER = createSyntheticContentType(
			"labeledIdentifier");
	public static final LinearKindOfQuantity ADDRESS = createAddress();
	public static final ContentType<Boolean> FLAG = createFlag("boolean");
	public static final ContentType<IType<?>> TYPE = createSyntheticContentType("type");
	public static final TimestampUnit EPOCH_MS = TIMESTAMP.getUnit("epochms");
	public static final TimestampUnit EPOCH_NS = TIMESTAMP.getUnit("epochns");
	public static final TimestampUnit EPOCH_S = TIMESTAMP.getUnit("epochs");
	public static final LinearUnit NUMBER_UNITY = NUMBER.getUnit(NONE);
	public static final LinearUnit ADDRESS_UNITY = ADDRESS.getUnit(NONE);
	public static final LinearUnit PERCENT_UNITY = PERCENTAGE.getUnit("");
	public static final LinearUnit PERCENT = PERCENTAGE.getUnit("%");
	public static final LinearUnit BYTE = MEMORY.getUnit(NOBI);
	public static final LinearUnit GIBIBYTE = MEMORY.getUnit(GIBI);
	public static final LinearUnit NANOSECOND = TIMESPAN.getUnit(NANO);
	public static final LinearUnit MICROSECOND = TIMESPAN.getUnit(MICRO);
	public static final LinearUnit MILLISECOND = TIMESPAN.getUnit(MILLI);
	public static final LinearUnit SECOND = TIMESPAN.getUnit(NONE);
	public static final LinearUnit MINUTE = TIMESPAN.getUnit("min");
	public static final LinearUnit HOUR = TIMESPAN.getUnit("h");
	public static final LinearUnit DAY = TIMESPAN.getUnit("d");
	public static final LinearUnit YEAR = TIMESPAN.getUnit("a");

	// Attributes matching RAW_NUMBER and UNIT content types. Use sparingly.
	public static final IAttribute<Number> NUMERICAL_ATTRIBUTE = attr("numerical", "Numerical", //$NON-NLS-1$ //$NON-NLS-2$
			"The numerical value of a quantity", RAW_NUMBER);
	public static final IAttribute<IUnit> UNIT_ATTRIBUTE = attr("unit", "Unit", "The unit of a quantity", UNIT); //$NON-NLS-1$ //$NON-NLS-2$

	private static final List<ContentType<?>> CONTENT_TYPES;
	private static final Map<String, RangeContentType<?>> RANGE_CONTENT_TYPES;

	static {
		List<KindOfQuantity<?>> quantityKinds = new ArrayList<>();
		quantityKinds.add(MEMORY);
		quantityKinds.add(TIMESPAN);
		quantityKinds.add(TIMESTAMP);
		quantityKinds.add(PERCENTAGE);
		quantityKinds.add(NUMBER);
		quantityKinds.add(ADDRESS);

		Map<String, RangeContentType<?>> rangeTypes = new HashMap<>();
		for (KindOfQuantity<?> kind : quantityKinds) {
			rangeTypes.put(kind.getIdentifier(), RangeContentType.create(kind));
		}
		RANGE_CONTENT_TYPES = Collections.unmodifiableMap(rangeTypes);

		List<ContentType<?>> types = new ArrayList<ContentType<?>>(quantityKinds);
		types.add(COUNT);
		types.add(INDEX);
		types.add(IDENTIFIER);
		types.add(LABELED_IDENTIFIER);
		types.add(PLAIN_TEXT);
		types.add(STACKTRACE);
		types.add(STACKTRACE_FRAME);
		types.add(METHOD);
		types.add(CLASS);
		types.add(CLASS_LOADER);
		types.add(PACKAGE);
		types.add(MODULE);
		types.add(THREAD);
		types.add(THREAD_GROUP);
		types.add(FLAG);
		types.add(TYPE);
		types.add(OLD_OBJECT);
		// FIXME: Should we add the OLD_OBJECT_* subtypes?
		types.add(UNKNOWN);
		CONTENT_TYPES = Collections.unmodifiableList(types);
	}

	public static final ContentType<IRange<IQuantity>> TIMERANGE = getRangeType(TIMESTAMP);

	@SuppressWarnings("unchecked")
	public static <M extends Comparable<? super M>> RangeContentType<M> getRangeType(ContentType<M> endPointType) {
		return (RangeContentType<M>) RANGE_CONTENT_TYPES.get(endPointType.getIdentifier());
	}

	private static abstract class LeafContentType<T> extends ContentType<T> implements IPersister<T> {
		private LeafContentType(String identifier) {
			super(identifier);
		}

		protected final void checkNull(Object value) {
			if (value == null) {
				throw new NullPointerException();
			}
		}

		@Override
		public IConstraint<T> combine(IConstraint<?> other) {
			return (this == other) ? this : null;
		}

		@Override
		public IPersister<T> getPersister() {
			return this;
		}
	}

	public static List<KindOfQuantity<?>> getKindsOfQuantity() {
		return Arrays.<KindOfQuantity<?>> asList(MEMORY, TIMESPAN, TIMESTAMP, NUMBER, PERCENTAGE);
	}

	public static List<ContentType<?>> getAllContentTypes() {
		return CONTENT_TYPES;
	}

	/**
	 * Convert a {@link Date} instance to a {@link IQuantity}, preserving {@code null}.
	 *
	 * @param timestamp
	 *            a {@link Date} instance, or {@code null}
	 * @return an {@link IQuantity} implementation instance, or {@code null}
	 */
	public static IQuantity fromDate(Date timestamp) {
		return (timestamp != null) ? EPOCH_MS.quantity(timestamp.getTime()) : null;
	}

	/**
	 * Convert an {@link IQuantity} representing a timestamp to a {@link Date}, preserving
	 * {@code null}.
	 *
	 * @param timestamp
	 *            a timestamp {@link IQuantity}, or {@code null}
	 * @return a {@link Date} instance, or {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code timestamp} is not of the timestamp kind
	 */
	public static Date toDate(IQuantity timestamp) {
		return (timestamp != null) ? new Date(timestamp.clampedLongValueIn(EPOCH_MS)) : null;
	}

	// FIXME: Doesn't really belong here. For now, make sure to not expose more.
	static Logger getLogger() {
		return Logger.getLogger("org.openjdk.jmc.common.unit");
	}

	private static <T> ContentType<T> createSyntheticContentType(String id) {
		ContentType<T> contentType = new ContentType<>(id);
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Default"));
		return contentType;
	}

	private static ContentType<Boolean> createFlag(String id) {
		ContentType<Boolean> contentType = new LeafContentType<Boolean>(id) {

			@Override
			public boolean validate(Boolean value) {
				// Overriding this method seems sufficient and simplest to check that the class is correct.
				checkNull(value);
				return false;
			}

			@Override
			public String persistableString(Boolean value) {
				return value.toString();
			}

			@Override
			public Boolean parsePersisted(String persistedValue) throws QuantityConversionException {
				if (persistedValue.equals("true")) {
					return Boolean.TRUE;
				}
				if (persistedValue.equals("false")) {
					return Boolean.FALSE;
				}
				throw QuantityConversionException.unparsable(persistedValue, Boolean.TRUE, this);
			}

			@Override
			public String interactiveFormat(Boolean content) {
				// FIXME: Define better localized strings
				return content.toString();
			}

			@Override
			public Boolean parseInteractive(String interactiveValue) throws QuantityConversionException {
				checkNull(interactiveValue);
				// FIXME: Define better localized strings
				return Boolean.valueOf(interactiveValue);
			}
		};
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Default"));
		return contentType;
	}

	private static ContentType<String> createStringContentType(String id) {
		ContentType<String> contentType = new LeafContentType<String>(id) {

			@Override
			public boolean validate(String value) {
				// Overriding this method seems sufficient and simplest to check that the class is correct.
				checkNull(value);
				return false;
			}

			@Override
			public String persistableString(String value) {
				validate(value);
				return value;
			}

			@Override
			public String parsePersisted(String persistedValue) {
				checkNull(persistedValue);
				return persistedValue;
			}

			@Override
			public String interactiveFormat(String value) {
				validate(value);
				return value;
			}

			@Override
			public String parseInteractive(String interactiveValue) {
				checkNull(interactiveValue);
				return interactiveValue;
			}
		};
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Default"));
		return contentType;
	}

	private static ContentType<IMCType> createJavaTypeContentType(String id) {
		ContentType<IMCType> contentType = new LeafContentType<IMCType>(id) {

			@Override
			public boolean validate(IMCType value) {
				// Overriding this method seems sufficient and simplest to check that the class is correct.
				checkNull(value);
				return false;
			}

			@Override
			public String persistableString(IMCType value) {
				return value.getFullName();
			}

			@Override
			public IMCType parsePersisted(String persistedValue) {
				checkNull(persistedValue);
				return MethodToolkit.typeFromBinaryJLS(persistedValue);
			}

			@Override
			public String interactiveFormat(IMCType value) {
				return value.getFullName();
			}

			@Override
			public IMCType parseInteractive(String interactiveValue) {
				checkNull(interactiveValue);
				return MethodToolkit.typeFromBinaryJLS(interactiveValue);
			}
		};
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Default"));

		return contentType;
	}

	private static LinearKindOfQuantity createNumber() {
		LinearKindOfQuantity number = new LinearKindOfQuantity("number", "", EnumSet.range(NONE, NONE));

		number.addFormatter(new LinearKindOfQuantity.AutoFormatter(number, "Dynamic", 0.001, 1000000));
		number.addFormatter(new KindOfQuantity.ExactFormatter<>(number));
		number.addFormatter(new KindOfQuantity.VerboseFormatter<>(number));

		// FIXME: Verify that scientific and engineering notation formatting works properly.
		number.addFormatter(new LinearKindOfQuantity.AutoFormatter(number,
				DisplayFormatter.SCIENTIFIC_NOTATION_IDENTIFIER, "Scientific Notation", 1.0, 10, 3));
		number.addFormatter(new LinearKindOfQuantity.AutoFormatter(number,
				DisplayFormatter.ENGINEERING_NOTATION_IDENTIFIER, "Engineering Notation", 1.0, 1000, 3));
		return number;
	}

	private static LinearKindOfQuantity createAddress() {
		LinearKindOfQuantity address = new LinearKindOfQuantity("address", "", EnumSet.range(NONE, NONE)) {
			// NOTE: Only overriding the interactive format. Persisted value can still be decimal.
			@Override
			public String interactiveFormat(IQuantity quantity) {
				return formatHexNumber(quantity);
			}
		};
		address.addFormatter(new DisplayFormatter<IQuantity>(address, IDisplayable.AUTO, "Dynamic") {
			@Override
			public String format(IQuantity quantity) {
				return formatHexNumber(quantity);
			}
		});
		return address;
	}

	private static String formatHexNumber(IQuantity quantity) {
		return String.format("0x%08X", quantity.longValue());
	}

	// FIXME: Rename to createPrimitiveNumber? Remove?
	private static ContentType<Number> createRawNumber() {
		ContentType<Number> contentType = new ContentType<>("raw number");
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Value"));
		return contentType;
	}

	private static ContentType<Long> createRawLong() {
		ContentType<Long> contentType = new ContentType<>("raw long");
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Value"));
		return contentType;
	}

	private static LinearKindOfQuantity createMemory() {
		LinearKindOfQuantity memory = new LinearKindOfQuantity("memory", "B", EnumSet.range(NOBI, PEBI),
				EnumSet.range(NOBI, YOBI));

		memory.addFormatter(new LinearKindOfQuantity.AutoFormatter(memory, "Dynamic", 1.0, 1024));
		memory.addFormatter(new KindOfQuantity.ExactFormatter<>(memory));
		memory.addFormatter(new KindOfQuantity.VerboseFormatter<>(memory));

		return memory;
	}

	private static void addQuantities(
		Collection<ITypedQuantity<LinearUnit>> result, LinearUnit unit, Number ... numbers) {
		for (Number number : numbers) {
			result.add(unit.quantity(number));
		}
	}

	private static LinearKindOfQuantity createTimespan() {
		EnumSet<DecimalPrefix> commonPrefixes = EnumSet.range(PICO, MILLI);
		commonPrefixes.add(NONE);
		LinearKindOfQuantity timeSpan = new LinearKindOfQuantity("timespan", "s", commonPrefixes,
				EnumSet.range(YOCTO, YOTTA));
		LinearUnit second = timeSpan.atomUnit;
		LinearUnit minute = timeSpan.makeUnit("min", second.quantity(60));
		LinearUnit hour = timeSpan.makeUnit("h", minute.quantity(60));
		LinearUnit day = timeSpan.makeUnit("d", hour.quantity(24));
		// UCUM defines the symbol "wk" for the week.
		LinearUnit week = timeSpan.makeUnit("wk", day.quantity(7));
		// The Julian year (annum, symbol "a") is defined by UCUM for use with SI, since it is the basis for the light year (so this is exact).
		LinearUnit year = timeSpan.makeUnit("a", hour.quantity(8766));
		// A mean Julian month is 1/12 of a Julian year = 365.25*24/12 h = 730.5 h = 43 830 min (exactly).
//		LinearUnit month = timeSpan.makeUnit("mo", minute.quantity(43830));

		LinearUnit[] units = {minute, hour, day, week, year};
		for (LinearUnit unit : units) {
			timeSpan.addUnit(unit);
		}

		// Tick marks and bucket sizes, also used for timestamps.
		SortedSet<ITypedQuantity<LinearUnit>> ticks = new TreeSet<>();
		addQuantities(ticks, second, 1, 2, 4, 5, 10, 15, 30);
		addQuantities(ticks, minute, 1, 2, 4, 5, 10, 15, 30);
		addQuantities(ticks, hour, 1, 2, 4, 6, 12);
		addQuantities(ticks, day, 1, 2, 4);
		addQuantities(ticks, week, 1, 2, 4, 5, 10);
		addQuantities(ticks, year, 0.25, 0.5, 1);

		DecimalUnitSelector yearSelector = new DecimalUnitSelector(timeSpan, year);
		CustomUnitSelector selector;
		selector = new CustomUnitSelector(timeSpan, timeSpan.unitSelector, Arrays.asList(units), yearSelector, ticks);
		timeSpan.setDefaultSelector(selector);

		timeSpan.addFormatter(new LinearKindOfQuantity.DualUnitFormatter(timeSpan, IDisplayable.AUTO, "Human readable",
				timeSpan.getUnit(NANO)));
		// FIXME: LKOQ.AutoFormatter uses IDisplayable.AUTO id which collides with the DualUnitFormatter above. Sync with FLR behavior?
		timeSpan.addFormatter(new LinearKindOfQuantity.AutoFormatter(timeSpan, "Dynamic"));
		timeSpan.addFormatter(new KindOfQuantity.ExactFormatter<>(timeSpan));
		timeSpan.addFormatter(new KindOfQuantity.VerboseFormatter<>(timeSpan));

		return timeSpan;
	}

	private static TimestampKind createTimestamp(LinearKindOfQuantity timespan) {
		TimestampKind timestampContentType = TimestampKind.buildContentType(timespan);
		timestampContentType
				.addFormatter(new DisplayFormatter<IQuantity>(timestampContentType, IDisplayable.AUTO, "Dynamic") {
					@Override
					public String format(IQuantity quantity) {
						try {
							// NOTE: This used to return the floor value.
							Date date = new Date(quantity.longValueIn(TimestampKind.MILLIS_UNIT));
							return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(date);
						} catch (QuantityConversionException e) {
							return Messages.getString(Messages.UnitLookup_TIMESTAMP_OUT_OF_RANGE);
						}
					}
				});

		timestampContentType.addFormatter(new KindOfQuantity.ExactFormatter<>(timestampContentType));
		timestampContentType.addFormatter(new KindOfQuantity.VerboseFormatter<>(timestampContentType));

//		contentType.addDisplayUnit(new DisplayUnit(contentType, "nanos", "D:HH:MM:SS ns"));
//		contentType.addDisplayUnit(new DisplayUnit(contentType, "micros", "D:HH:MM:SS us"));
//		contentType.addDisplayUnit(new DisplayUnit(contentType, "seconds", "D:HH:MM:SS"));

		return timestampContentType;
	}

	private static LinearKindOfQuantity createPercentage() {
		LinearKindOfQuantity percentage = new LinearKindOfQuantity("percentage", "%", EnumSet.range(NONE, NONE));
		LinearUnit percentUnit = percentage.atomUnit;
		// Use identifier "", like Number. Relying on externalization to get symbol like "x100 %".
		LinearUnit unity = percentage.makeUnit("", percentUnit.quantity(100));
		percentage.addUnit(unity);

		percentage.addFormatter(new LinearKindOfQuantity.AutoFormatter(percentage, "Dynamic", 0.001, 1000000));
		percentage.addFormatter(new KindOfQuantity.ExactFormatter<>(percentage));
		percentage.addFormatter(new KindOfQuantity.VerboseFormatter<>(percentage));
		percentage.addFormatter(new DisplayFormatter<>(percentage, "accuracy2digits", "Accuracy 2 digits)"));
		percentage.addFormatter(new DisplayFormatter<>(percentage, "accuracy0digits", "Accuracy 0 digits)"));
		percentage.addFormatter(new DisplayFormatter<>(percentage, "accuracy1digit", "Accuracy 1 digit)"));
		percentage.addFormatter(new DisplayFormatter<>(percentage, "accuracy3digits", "Accuracy 3 digits)"));
		return percentage;
	}

	private static ContentType<Number> createCount() {
		ContentType<Number> contentType = new ContentType<>("count");
//		contentType.addDisplayUnit(
//				new DisplayUnit(contentType, DisplayUnit.ENGINEERING_NOTATION_IDENTIFIER, "Engineering Notation"));
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Value"));

//		contentType.addDisplayUnit(
//				new DisplayUnit(contentType, DisplayUnit.SCIENTIFIC_NOTATION_IDENTIFIER, "Scientific Notation"));
		return contentType;
	}

	private static ContentType<Number> createIdentifier() {
		ContentType<Number> contentType = new ContentType<>("identifier");
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Value"));
		return contentType;
	}

	private static ContentType<Number> createIndex() {
		ContentType<Number> contentType = new ContentType<>("index");
		contentType.addFormatter(new DisplayFormatter<>(contentType, IDisplayable.AUTO, "Value"));
		return contentType;
	}

	public static String getUnitIdentifier(IUnit unit) {
		if (unit.getIdentifier() == null) {
			throw new IllegalArgumentException("Cannot get identifier for impersistable unit :" + unit);
		}
		KindOfQuantity<?> ct = unit.getContentType();
		return ct.getIdentifier() + UNIT_ID_SEPARATOR + unit.getIdentifier();
	}

	public static IUnit getUnitOrDefault(String unitIdentifier) {
		IUnit unit = getUnitOrNull(unitIdentifier);
		return unit == null ? NUMBER.getUnit(NONE) : unit;
	}

	public static IUnit getUnitOrNull(String unitIdentifier) {
		if (unitIdentifier != null) {
			String[] parts = unitIdentifier.split(UNIT_ID_SEPARATOR, 2);
			if (parts.length == 2) {
				ContentType<?> ct = getContentType(parts[0]);
				if (ct instanceof KindOfQuantity) {
					IUnit unit = ((KindOfQuantity<?>) ct).getUnit(parts[1]);
					if (unit != null) {
						return unit;
					} else if (ct instanceof LinearKindOfQuantity) {
						LinearKindOfQuantity kindOfQuantity = (LinearKindOfQuantity) ct;
						String id = parts[1];
						LinearUnit linUnit = kindOfQuantity.getCachedUnit(id);
						if (linUnit != null) {
							return linUnit;
						}
						try {
							// FIXME: Parse using UCUM (Unified Code for Units of Measure) syntax instead? Or by only allowing integers?
							ITypedQuantity<LinearUnit> quantity = kindOfQuantity.parsePersisted(id);
							return kindOfQuantity.makeCustomUnit(quantity);
						} catch (QuantityConversionException e) {
							getLogger().log(Level.WARNING, e.getMessage(), e);
						}
					}
				}
			}
		}
		return null;
	}

	public static ContentType<?> getContentType(String identifier) {
		String[] parts = identifier.split(UNIT_ID_SEPARATOR, 2);
		if (parts.length > 2) {
			return UNKNOWN;
		} else if (parts.length == 2) {
			identifier = parts[0];
		}
		for (ContentType<?> type : CONTENT_TYPES) {
			if (identifier.equals(type.getIdentifier())) {
				return type;
			}
		}
		return UNKNOWN;
	}
}
