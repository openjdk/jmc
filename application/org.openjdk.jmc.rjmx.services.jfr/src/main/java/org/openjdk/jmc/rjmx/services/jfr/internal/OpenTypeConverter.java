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
package org.openjdk.jmc.rjmx.services.jfr.internal;

import static org.openjdk.jmc.common.unit.UnitLookup.FLAG;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.PERIOD_V1;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.POSITIVE_MEMORY;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.POSITIVE_TIMESPAN;
import static javax.management.openmbean.SimpleType.LONG;
import static javax.management.openmbean.SimpleType.STRING;

import java.util.Date;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;

/**
 * Conversion from {@link IConstraint constrained content types} into {@link OpenType} values as
 * used by JFR 1.0 (JDK 7/8).
 *
 * @param <P>
 *            The actual "open type" value class (typically primitive wrapper or {@link Date})
 * @param <T>
 *            The JMC {@link IConstraint} value class
 */
@SuppressWarnings("nls")
public abstract class OpenTypeConverter<P, T> {
	public static final OpenTypeConverter<Long, IQuantity> BYTES = new LongQuantity("bytes", UnitLookup.BYTE,
			POSITIVE_MEMORY);
	public static final OpenTypeConverter<Long, IQuantity> NANOSECONDS = new LongQuantity("nanos",
			UnitLookup.NANOSECOND, POSITIVE_TIMESPAN);
	public static final OpenTypeConverter<Long, IQuantity> MILLISECONDS = new LongQuantity("millis",
			UnitLookup.MILLISECOND, POSITIVE_TIMESPAN);
	public static final OpenTypeConverter<Long, IQuantity> MILLIS_PERIODICITY = new LongQuantity("millis_periodicity",
			UnitLookup.MILLISECOND, PERIOD_V1) {
		@Override
		public IQuantity fromOpenType(Long openValue) throws QuantityConversionException {
			return (openValue == 0L) ? CommonConstraints.EVERY_CHUNK_MAGIC_INSTANCE : super.fromOpenType(openValue);
		};
	};
	public static final OpenTypeConverter<Date, IQuantity> DATE = new OpenTypeConverter<Date, IQuantity>("date",
			Date.class, SimpleType.DATE, CommonConstraints.POINT_IN_TIME) {
		@Override
		public Date toOpenType(IQuantity value) throws QuantityConversionException {
			constraint.validate(value);
			return UnitLookup.toDate(value);
		}

		@Override
		public IQuantity fromOpenType(Date openValue) throws QuantityConversionException {
			IQuantity value = UnitLookup.fromDate(openValue);
			constraint.validate(value);
			return value;
		}
	};
	public static final OpenTypeConverter<Boolean, Boolean> BOOLEAN = new Identity<>("boolean", Boolean.class,
			SimpleType.BOOLEAN, FLAG);
	public static final OpenTypeConverter<String, String> FILE_NAME = new Identity<>("filename", String.class, STRING,
			PLAIN_TEXT);
	public static final OpenTypeConverter<String, String> TEXT = new Identity<>("string", String.class, STRING,
			PLAIN_TEXT);
	public static final OpenTypeConverter<String, String> UNKNOWN = new Identity<>("unknown", String.class, STRING,
			PLAIN_TEXT);

	public final String key;
	public final Class<P> type;
	public final OpenType<P> openType;
	public final IConstraint<T> constraint;

	private static class Identity<S> extends OpenTypeConverter<S, S> {
		private Identity(String key, Class<S> type, OpenType<S> openType, ContentType<S> contentType) {
			super(key, type, openType, contentType.getPersister());
		}

		@Override
		public S toOpenType(S value) throws QuantityConversionException {
			constraint.validate(value);
			return value;
		}

		@Override
		public S fromOpenType(S openValue) throws QuantityConversionException {
			constraint.validate(openValue);
			return openValue;
		}
	}

	private static class LongQuantity extends OpenTypeConverter<Long, IQuantity> {
		private final IUnit unit;

		protected LongQuantity(String key, IUnit unit, IConstraint<IQuantity> constraint) {
			super(key, Long.class, LONG, constraint);
			this.unit = unit;
		}

		@Override
		public Long toOpenType(IQuantity value) throws QuantityConversionException {
			// FIXME: Really special treat null here? Should ideally not reach this point.
			if (value == null) {
				// Skip validation too, since it is not really a valid value.
				return -1L;
			}
			constraint.validate(value);
			return value.longValueIn(unit);
		}

		@Override
		public IQuantity fromOpenType(Long openValue) throws QuantityConversionException {
			// FIXME: Really special treat -1 here? Should ideally not reach this point.
			if (openValue.longValue() == -1L) {
				// Skip validation too, since it is not really a valid value.
				return null;
			}
			IQuantity value = OpenTypeConverter.inGuessedUnit(unit.quantity(openValue));
			constraint.validate(value);
			return value;
		}

	}

	// FIXME: Move to a better place.
	public static IQuantity inGuessedUnit(IQuantity value) {
		IUnit unit = value.getType().getLargestExactUnit(value);
		return (unit != null) ? value.in(unit) : value;
	}

	protected OpenTypeConverter(String key, Class<P> type, OpenType<P> openType, IConstraint<T> constraint) {
		this.key = key;
		this.type = type;
		this.openType = openType;
		this.constraint = constraint;
	}

	public String getKey() {
		return key;
	}

	public Class<P> getType() {
		return type;
	}

	public OpenType<P> getOpenType() {
		return openType;
	}

	public abstract P toOpenType(T value) throws QuantityConversionException;

	public abstract T fromOpenType(P openValue) throws QuantityConversionException;
}
