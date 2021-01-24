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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.unit.QuantityConversionException.Problem;
import org.openjdk.jmc.common.util.FormatThreadLocal;

class TimestampKind extends KindOfQuantity<TimestampUnit> {
	private static final Pattern NUMBER_UNIT_PATTERN = Pattern.compile("^(-?\\d+)\\s*([a-zA-Z%]*)$"); //$NON-NLS-1$
	// NOTE: Only to be used for formatting, not parsing due to "yy" vs. "yyyy" behaviour. Always clone().
	private static final FormatThreadLocal<DateFormat> DATE_TIME_FORMATTER_HOLDER;
	static TimestampUnit NANOS_UNIT;
	static TimestampUnit MICROS_UNIT;
	static TimestampUnit MILLIS_UNIT;
	static TimestampUnit SECONDS_UNIT;
	static TimestampKind INSTANCE;
	private static IFormatter<IQuantity> YEAR_TO_DAY_FORMATTER;
	private static IFormatter<IQuantity> YEAR_TO_SECONDS_FORMATTER;
	private static IFormatter<IQuantity> YEAR_TO_MILLIS_FORMATTER;
	private static IFormatter<IQuantity> YEAR_TO_MICROS_FORMATTER;
	private static IFormatter<IQuantity> YEAR_TO_NANOS_FORMATTER;
	private static IFormatter<IQuantity> HOUR_TO_SECONDS_FORMATTER;
	private static IFormatter<IQuantity> HOUR_TO_MILLIS_FORMATTER;
	private static IFormatter<IQuantity> HOUR_TO_MICROS_FORMATTER;
	private static IFormatter<IQuantity> HOUR_TO_NANOS_FORMATTER;
	private static IFormatter<IQuantity> MILLIS_FORMATTER;
	private static IFormatter<IQuantity> MICROS_FORMATTER;
	private static IFormatter<IQuantity> NANOS_FORMATTER;

	private static class LegacyFormatter implements IFormatter<IQuantity> {
		protected final FormatThreadLocal<DateFormat> dfHolder;

		public LegacyFormatter(DateFormat df) {
			this(new FormatThreadLocal<>(df));
		}

		public LegacyFormatter(FormatThreadLocal<DateFormat> dfHolder) {
			this.dfHolder = dfHolder;
		}

		protected final Date dateFor(IQuantity q) {
			return new Date(q.clampedFloorIn(SECONDS_UNIT) * 1000);
		}

		@Override
		public String format(IQuantity q) {
			return dfHolder.get().format(dateFor(q));
		}
	}

	private static class LegacyAndFractionFormatter extends LegacyFormatter {
		private final LinearUnit fractionUnit;
		private final int numDigits;

		public LegacyAndFractionFormatter(FormatThreadLocal<DateFormat> dfHolder, IUnit resolutionUnit) {
			super(dfHolder);
			fractionUnit = resolutionUnit.getDeltaUnit();
			numDigits = ((DecimalScaleFactor) SECONDS_UNIT.valueTransformTo(resolutionUnit)).powerOf10;
		}

		@Override
		public String format(IQuantity q) {
			Date date = dateFor(q);
			StringBuffer out = new StringBuffer();
			FieldPosition secondPos = new FieldPosition(DateFormat.SECOND_FIELD);
			dfHolder.get().format(date, out, secondPos);
			int fractionPos = secondPos.getEndIndex();

			// NOTE: Must consistently use floor to avoid overflow from 999999 us to 1000 ms.
			long rest = q.subtract(MILLIS_UNIT.quantity(date.getTime())).clampedFloorIn(fractionUnit);
			out.insert(fractionPos++, DecimalFormatSymbols.getInstance().getDecimalSeparator());
			String restStr = Long.toString(rest);
			out.insert(fractionPos, restStr);
			out.insert(fractionPos, "000000000000000000000000", restStr.length(), numDigits); //$NON-NLS-1$
			return out.toString();
		}
	}

	private static class FractionFormatter implements IFormatter<IQuantity> {
		private final LinearUnit fractionUnit;
		private final int numDigits;

		public FractionFormatter(IUnit resolutionUnit) {
			fractionUnit = resolutionUnit.getDeltaUnit();
			numDigits = ((DecimalScaleFactor) SECONDS_UNIT.valueTransformTo(resolutionUnit)).powerOf10;
		}

		@Override
		public String format(IQuantity q) {
			// NOTE: Must consistently use floor to avoid overflow from 999999 us to 1000 ms.
			long rest = q.subtract(SECONDS_UNIT.quantity(q.clampedFloorIn(SECONDS_UNIT))).clampedFloorIn(fractionUnit);
			StringBuffer out = new StringBuffer(numDigits + 1);
			out.append(DecimalFormatSymbols.getInstance().getDecimalSeparator());
			String restStr = Long.toString(rest);
			out.append("000000000000000000000000", restStr.length(), numDigits); //$NON-NLS-1$
			out.append(restStr);
			return out.toString();
		}
	}

	static {
		DateFormat df = patchYear(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
		DATE_TIME_FORMATTER_HOLDER = new FormatThreadLocal<>(df);
	}

	private static DateFormat patchYear(DateFormat df) {
		if (df instanceof SimpleDateFormat) {
			/*
			 * Ensure years are more than two digits when formatting, so that round trips work even
			 * if outside the -80, +20 years from now range. Parsing pattern must still use "yy" in
			 * order to convert to current century when a date is entered with exactly two digits.
			 * (Locales always using the full year are not affected by this.)
			 */
			SimpleDateFormat sdf = (SimpleDateFormat) df;
			String pattern = sdf.toPattern();
			String newPattern = pattern.replaceFirst("y{2,4}", "yyyy"); //$NON-NLS-1$ //$NON-NLS-2$
			sdf.applyPattern(newPattern);
		}
		return df;
	}

	/**
	 * Only for formatting, not parsing!
	 */
	static DateFormat getDateTimeFormatter() {
		return DATE_TIME_FORMATTER_HOLDER.get();
	}

	public static TimestampKind buildContentType(LinearKindOfQuantity timespan) {
		NANOS_UNIT = new TimestampUnit(timespan.getUnit(DecimalPrefix.NANO));
		MICROS_UNIT = new TimestampUnit(timespan.getUnit(DecimalPrefix.MICRO));
		MILLIS_UNIT = new TimestampUnit(timespan.getUnit(DecimalPrefix.MILLI));
		SECONDS_UNIT = new TimestampUnit(timespan.getUnit(DecimalPrefix.NONE));
		INSTANCE = new TimestampKind();
		INSTANCE.addUnit(NANOS_UNIT);
		INSTANCE.addUnit(MICROS_UNIT);
		INSTANCE.addUnit(MILLIS_UNIT);
		INSTANCE.addUnit(SECONDS_UNIT);

		YEAR_TO_DAY_FORMATTER = new LegacyFormatter(patchYear(DateFormat.getDateInstance(DateFormat.SHORT)));
		YEAR_TO_SECONDS_FORMATTER = new LegacyFormatter(DATE_TIME_FORMATTER_HOLDER);
		YEAR_TO_MILLIS_FORMATTER = new LegacyAndFractionFormatter(DATE_TIME_FORMATTER_HOLDER, MILLIS_UNIT);
		YEAR_TO_MICROS_FORMATTER = new LegacyAndFractionFormatter(DATE_TIME_FORMATTER_HOLDER, MICROS_UNIT);
		YEAR_TO_NANOS_FORMATTER = new LegacyAndFractionFormatter(DATE_TIME_FORMATTER_HOLDER, NANOS_UNIT);
		FormatThreadLocal<DateFormat> timeHolder = new FormatThreadLocal<>(
				DateFormat.getTimeInstance(DateFormat.MEDIUM));
		HOUR_TO_SECONDS_FORMATTER = new LegacyFormatter(timeHolder);
		HOUR_TO_MILLIS_FORMATTER = new LegacyAndFractionFormatter(timeHolder, MILLIS_UNIT);
		HOUR_TO_MICROS_FORMATTER = new LegacyAndFractionFormatter(timeHolder, MICROS_UNIT);
		HOUR_TO_NANOS_FORMATTER = new LegacyAndFractionFormatter(timeHolder, NANOS_UNIT);
		MILLIS_FORMATTER = new FractionFormatter(MILLIS_UNIT);
		MICROS_FORMATTER = new FractionFormatter(MICROS_UNIT);
		NANOS_FORMATTER = new FractionFormatter(NANOS_UNIT);
		return INSTANCE;
	}

	private TimestampKind() {
		super("timestamp"); //$NON-NLS-1$
	}

	@Override
	public TimestampUnit getPreferredUnit(IQuantity value, double minNumericalValue, double maxNumericalValue) {
		// FIXME: Consider minNumericalValue and maxNumericalValue
		LinearUnit timeOffsetUnit = value.getUnit().getDeltaUnit();
		ITypedQuantity<LinearUnit> asTimeSpan = timeOffsetUnit.quantity(value.longValue());
		return new TimestampUnit(
				timeOffsetUnit.getContentType().getPreferredUnit(asTimeSpan, minNumericalValue, maxNumericalValue));
	}

	@Override
	public TimestampUnit getLargestExactUnit(IQuantity value) {
		LinearUnit timeOffsetUnit = value.getUnit().getDeltaUnit();
		ITypedQuantity<LinearUnit> asTimeSpan = timeOffsetUnit.quantity(value.longValue());
		return new TimestampUnit(timeOffsetUnit.getContentType().getLargestExactUnit(asTimeSpan));
	}

	@Override
	public IFormatter<IQuantity> getFormatterResolving(IRange<IQuantity> range) {
		LinearUnit resolutionUnit = (LinearUnit) range.getExtent().getType().getPreferredUnit(range.getExtent(), 1,
				1000);

		if (resolutionUnit.compareTo(UnitLookup.DAY) >= 0) {
			return YEAR_TO_DAY_FORMATTER;
		} else if (resolutionUnit.compareTo(UnitLookup.SECOND) >= 0) {
			return new IIncrementalFormatter() {
				@Override
				public String format(IQuantity q) {
					return YEAR_TO_SECONDS_FORMATTER.format(q);
				}

				@Override
				public String formatContext(IQuantity firstShown) {
					return YEAR_TO_DAY_FORMATTER.format(firstShown);
				}

				@Override
				public String formatAdjacent(IQuantity previous, IQuantity current) {
					if (previous == null) {
						return YEAR_TO_SECONDS_FORMATTER.format(current);
					} else if (YEAR_TO_DAY_FORMATTER.format(previous).equals(YEAR_TO_DAY_FORMATTER.format(current))) {
						return HOUR_TO_SECONDS_FORMATTER.format(current);
					} else {
						return YEAR_TO_SECONDS_FORMATTER.format(current);
					}
				}
			};
		}

		final IFormatter<IQuantity> fullFormat;
		final IFormatter<IQuantity> middleFormat;
		final IFormatter<IQuantity> minimalFormat;

		if (resolutionUnit.compareTo(UnitLookup.MILLISECOND) >= 0) {
			fullFormat = YEAR_TO_MILLIS_FORMATTER;
			middleFormat = HOUR_TO_MILLIS_FORMATTER;
			minimalFormat = MILLIS_FORMATTER;
		} else if (resolutionUnit.compareTo(UnitLookup.MICROSECOND) >= 0) {
			fullFormat = YEAR_TO_MICROS_FORMATTER;
			middleFormat = HOUR_TO_MICROS_FORMATTER;
			minimalFormat = MICROS_FORMATTER;
		} else {
			fullFormat = YEAR_TO_NANOS_FORMATTER;
			middleFormat = HOUR_TO_NANOS_FORMATTER;
			minimalFormat = NANOS_FORMATTER;
		}

		return new IIncrementalFormatter() {
			@Override
			public String format(IQuantity q) {
				return fullFormat.format(q);
			}

			@Override
			public String formatContext(IQuantity firstShown) {
				return YEAR_TO_SECONDS_FORMATTER.format(firstShown);
			}

			@Override
			public String formatAdjacent(IQuantity previous, IQuantity current) {
				if (previous == null) {
					return fullFormat.format(current);
				} else if (YEAR_TO_SECONDS_FORMATTER.format(previous)
						.equals(YEAR_TO_SECONDS_FORMATTER.format(current))) {
					return minimalFormat.format(current);
				} else if (YEAR_TO_DAY_FORMATTER.format(previous).equals(YEAR_TO_DAY_FORMATTER.format(current))) {
					return middleFormat.format(current);
				} else {
					return fullFormat.format(current);
				}
			}
		};
	}

	@Override
	public KindOfQuantity<LinearUnit> getDeltaKind() {
		return UnitLookup.TIMESPAN;
	}

	@Override
	public TimestampUnit getDefaultUnit() {
		/*
		 * FIXME: Since MILLIS_UNIT is default in Java, maybe return that?
		 *
		 * If the intent is the same as for getLegacyImplicitUnit(), that method should be pulled up
		 * from LinearKindOfQuantity.
		 */
		return NANOS_UNIT;
	}

	@Override
	public ITypedQuantity<TimestampUnit> parsePersisted(String persistedQuantity) throws QuantityConversionException {
		Matcher m = NUMBER_UNIT_PATTERN.matcher(persistedQuantity.trim());
		if (m.matches()) {
			TimestampUnit unit = getUnit(m.group(2));
			if (unit != null) {
				try {
					return unit.quantity(Long.parseLong(m.group(1)));
				} catch (RuntimeException e) {

				}
			} else if (m.group(2).length() == 0) {
				throw QuantityConversionException.noUnit(persistedQuantity, getDefaultUnit().quantity(1234.0));
			} else {
				throw QuantityConversionException.unknownUnit(persistedQuantity, getDefaultUnit().quantity(1234.0));
			}
		}
		throw QuantityConversionException.unparsable(persistedQuantity, getDefaultUnit().quantity(1234.0));
	}

	@Override
	public ITypedQuantity<TimestampUnit> parseInteractive(String interactiveQuantity)
			throws QuantityConversionException {
		try {
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
			ParsePosition pos = new ParsePosition(0);
			Date date = df.parse(interactiveQuantity, pos);
			if (date != null) {
				long time = date.getTime();
				String rest = interactiveQuantity.substring(pos.getIndex()).trim();
				if (!rest.isEmpty()) {
					DecimalFormat format = new DecimalFormat();
					format.setParseBigDecimal(true);
					pos.setIndex(0);
					BigDecimal bd = (BigDecimal) format.parse(rest, pos);
					if (pos.getIndex() < rest.length()) {
						throw QuantityConversionException.unparsable(interactiveQuantity,
								MILLIS_UNIT.quantity(System.currentTimeMillis()));
					}
					if (rest.length() > 10) {
						// FIXME: This is not a recommended usage of QuantityConversionException.
						throw new QuantityConversionException.Quantity(Problem.TOO_SMALL_MAGNITUDE, interactiveQuantity,
								NANOS_UNIT.quantity(1));
					} else if (rest.length() > 7) {
						return NANOS_UNIT.quantity(
								time * 1000000 + bd.multiply(BigDecimal.valueOf(1000000000)).longValueExact());
					} else if (rest.length() > 4) {
						return MICROS_UNIT
								.quantity(time * 1000 + bd.multiply(BigDecimal.valueOf(1000000)).longValueExact());
					}
					return MILLIS_UNIT.quantity(time + bd.multiply(BigDecimal.valueOf(1000)).longValueExact());
				}
				return SECONDS_UNIT.quantity(time / 1000);
			}
		} catch (RuntimeException e) {
		}
		throw QuantityConversionException.unparsable(interactiveQuantity,
				MILLIS_UNIT.quantity(System.currentTimeMillis()));
	}
}
