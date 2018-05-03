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

import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_NS;
import static org.openjdk.jmc.common.unit.UnitLookup.NANOSECOND;

import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Date;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.messages.internal.Messages;

public class TimestampUnit extends TypedUnit<TimestampUnit> {
	private final LinearUnit timeOffsetUnit;
	private final String unitId;
	private final String unitDescription;

	TimestampUnit(LinearUnit timeOffsetUnit) {
		this.timeOffsetUnit = timeOffsetUnit;
		unitId = "epoch" + timeOffsetUnit.getIdentifier(); //$NON-NLS-1$
		String multiplier = timeOffsetUnit.asWellKnownQuantity().displayUsing(IDisplayable.EXACT);
		unitDescription = MessageFormat.format(Messages.getString(Messages.TimestampKind_SINCE_1970_MSG), multiplier);
	}

	@Override
	protected final Class<TimestampUnit> getUnitClass() {
		return TimestampUnit.class;
	};

	@Override
	public LinearUnit getDeltaUnit() {
		return timeOffsetUnit;
	};

	@Override
	protected TimestampUnit getScaledUnit(LinearUnit deltaUnit) {
		// FIXME: Obtain unit from kind of quantity?
		return (deltaUnit == timeOffsetUnit) ? this : new TimestampUnit(deltaUnit);
	}

	@Override
	public KindOfQuantity<TimestampUnit> getContentType() {
		return TimestampKind.INSTANCE;
	}

	@Override
	public ITypedQuantity<TimestampUnit> quantity(Number numericalValue) {
		// FIXME: Allow DoubleStored?
		return new ScalarQuantity.LongStored<>(numericalValue.longValue(), this);
	}

	@Override
	public ITypedQuantity<TimestampUnit> quantity(long numericalValue) {
		return new ScalarQuantity.LongStored<>(numericalValue, this);
	}

	@Override
	public ITypedQuantity<TimestampUnit> quantity(double numericalValue) {
		// FIXME: Allow DoubleStored?
		return new ScalarQuantity.LongStored<>((long) numericalValue, this);
	}

	@Override
	public IScalarAffineTransform valueTransformTo(TimestampUnit targetUnit) {
		// Need to check _our_ kind of quantity here, since only our corresponding linear kind will be checked later.
		if (targetUnit.getContentType() != getContentType()) {
			throw new IllegalArgumentException("Cannot convert from unit " + this //$NON-NLS-1$
					+ " into unit of type " + targetUnit.getContentType().getIdentifier()); //$NON-NLS-1$
		}
		return timeOffsetUnit.valueTransformTo(targetUnit.timeOffsetUnit);
	}

	@Override
	public boolean isLinear() {
		return false;
	}

	@Override
	protected ITypedQuantity<TimestampUnit> add(long numericalAugend, LinearUnit addendUnit, long numericalAddend) {
		int comparision = getDeltaUnit().compareTo(addendUnit);
		if (comparision == 0) {
			long sum = numericalAugend + numericalAddend;
			// See Math.addExact() since JDK 8.
			// HD 2-12 Overflow iff both arguments have the opposite sign of the result
			if (((numericalAugend ^ sum) & (numericalAddend ^ sum)) >= 0) {
				return quantity(numericalAugend + numericalAddend);
			}
			return quantity(numericalAugend + (double) numericalAddend);
		} else if (comparision < 0) {
			return addPossiblyIntegral(numericalAugend, addendUnit.valueTransformTo(getDeltaUnit()), numericalAddend);
		} else {
			return getScaledUnit(addendUnit).addPossiblyIntegral(numericalAddend,
					getDeltaUnit().valueTransformTo(addendUnit), numericalAugend);
		}
	}

	@Override
	protected ITypedQuantity<LinearUnit> subtractSame(
		long numericalMinuend, TimestampUnit subtrahendUnit, long numericalSubtrahend) {
		int comparision = getDeltaUnit().compareTo(subtrahendUnit.getDeltaUnit());
		if (comparision == 0) {
			/*
			 * NOTE: This optimization only works for now since we know that all Timestamps have the
			 * same origin! If this no longer holds in the future, fold this case in with the one
			 * below.
			 */
			long sum = numericalMinuend - numericalSubtrahend;
			// See Math.addExact() since JDK 8.
			// HD 2-12 Overflow iff both arguments (in an addition) have the opposite sign of the result
			if (((numericalMinuend ^ sum) & ((-numericalSubtrahend) ^ sum)) >= 0) {
				return getDeltaUnit().quantity(numericalMinuend - numericalSubtrahend);
			}
			return getDeltaUnit().quantity(numericalMinuend - (double) numericalSubtrahend);
		} else if (comparision < 0) {
			return getDeltaUnit().addPossiblyIntegral(numericalMinuend, subtrahendUnit.valueTransformTo(this),
					-numericalSubtrahend);
		} else {
			return subtrahendUnit.getDeltaUnit().addPossiblyIntegral(-numericalSubtrahend,
					valueTransformTo(subtrahendUnit), numericalMinuend);
		}
	}

	@Override
	protected ITypedQuantity<TimestampUnit> floorQuantize(long numericalValue, ITypedQuantity<LinearUnit> quanta) {
		// Assuming our origin (1970-01-01) is quanta aligned, which is false for quanta > 10 years.
		ITypedQuantity<LinearUnit> offset = timeOffsetUnit.floorQuantize(numericalValue, quanta);
		/*
		 * NOTE: Currently timestamps are always stored as a long with a fixed origin. That means
		 * that, for now, the best thing we could do is use the smallest delta unit of this unit and
		 * that of the quantized offset, as long as it isn't smaller than a nanosecond (for today to
		 * fit in a long). This also has the side effects of eliminating the quantization of chart
		 * selections to integer s, ms or us, and prevent chart zoom in from being blocked way too
		 * early.
		 */
		int comparision = getDeltaUnit().compareTo(offset.getUnit());
		if (comparision == 0) {
			return quantity(offset.longValue());
		} else if (comparision < 0) {
			return quantity(offset.clampedLongValueIn(getDeltaUnit()));
		} else if (NANOSECOND.compareTo(offset.getUnit()) >= 0) {
			return EPOCH_NS.quantity(offset.clampedLongValueIn(NANOSECOND));
		} else {
			return getScaledUnit(offset.getUnit()).quantity(offset.longValue());
		}
	}

	@Override
	protected ITypedQuantity<TimestampUnit> floorQuantize(double numericalValue, ITypedQuantity<LinearUnit> quanta) {
		// Assuming our origin (1970-01-01) is quanta aligned, which is false for quanta > 10 years.
		ITypedQuantity<LinearUnit> offset = timeOffsetUnit.floorQuantize(numericalValue, quanta);
		/*
		 * NOTE: Currently timestamps are always stored as a long with a fixed origin. That means
		 * that, for now, the best thing we could do is use the smallest delta unit of this unit and
		 * that of the quantized offset, as long as it isn't smaller than a nanosecond (for today to
		 * fit in a long). This also has the side effects of eliminating the quantization of chart
		 * selections to integer s, ms or us, and prevent chart zoom in from being blocked way too
		 * early.
		 */
		int comparision = getDeltaUnit().compareTo(offset.getUnit());
		if (comparision == 0) {
			return quantity(offset.longValue());
		} else if (comparision < 0) {
			return quantity(offset.clampedLongValueIn(getDeltaUnit()));
		} else if (NANOSECOND.compareTo(offset.getUnit()) >= 0) {
			return EPOCH_NS.quantity(offset.clampedLongValueIn(NANOSECOND));
		} else {
			return getScaledUnit(offset.getUnit()).quantity(offset.longValue());
		}
	}

	@Override
	public String toString() {
		return getIdentifier();
	}

	@Override
	public String getIdentifier() {
		return unitId;
	}

	@Override
	public String getLocalizedSymbol() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getAppendableSuffix(boolean useBreakingSpace) {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getLocalizedDescription() {
		return unitDescription;
	}

	@Override
	public String[] getAltLocalizedNames() {
		return EMPTY_STRING_ARRAY;
	}

	private long floorValueIn(long numericalValue, TimestampUnit targetUnit) {
		return valueTransformTo(targetUnit).targetFloor(numericalValue);
	}

	@Override
	protected String localizedFormatFor(Number numericalValue, boolean useBreakingSpace, boolean allowCustomUnit) {
		StringBuffer out = new StringBuffer();
		long seconds = floorValueIn(numericalValue.longValue(), TimestampKind.SECONDS_UNIT);
		// Construct date exactly on the floor second.
		Date date = new Date(seconds * 1000);
		DateFormat formatter = TimestampKind.getDateTimeFormatter();
		FieldPosition secondPos = new FieldPosition(DateFormat.SECOND_FIELD);
		formatter.format(date, out, secondPos);
		if (!equals(TimestampKind.SECONDS_UNIT)) {
			ScaleFactor restFactor = (ScaleFactor) TimestampKind.SECONDS_UNIT.valueTransformTo(this);
			long rest = numericalValue.longValue() - restFactor.targetFloor(seconds);

			/*
			 * FIXME: Currently reusing useBreakingSpace flag to determine if the result has to be
			 * parsable. Don't do this. Preferably make the good looking formatting parsable
			 * instead.
			 */
			int fractionPos = useBreakingSpace ? out.length() : secondPos.getEndIndex();

			out.insert(fractionPos++, DecimalFormatSymbols.getInstance().getDecimalSeparator());

			DecimalScaleFactor decimalFactor;
			if (restFactor instanceof DecimalScaleFactor) {
				decimalFactor = (DecimalScaleFactor) restFactor;
			} else {
				decimalFactor = (DecimalScaleFactor) TimestampKind.SECONDS_UNIT
						.valueTransformTo(TimestampKind.NANOS_UNIT);
				rest = floorValueIn(rest, TimestampKind.NANOS_UNIT);
			}
			String restStr = Long.toString(rest);
			out.insert(fractionPos, restStr);
			out.insert(fractionPos, "000000000000000000000000", restStr.length(), decimalFactor.powerOf10); //$NON-NLS-1$
			return out.toString();

		}
		// Most localized date formats seems not to contain non-breaking spaces, but they could, so filter them out.
		if (useBreakingSpace) {
			return out.toString().replace('\u00a0', ' ');
		}
		return out.toString();
	}

	@Override
	protected String persistableStringFor(Number numericalValue) {
		return numericalValue.toString() + ' ' + getIdentifier();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other instanceof TimestampUnit) {
			TimestampUnit otherUnit = (TimestampUnit) other;
			return otherUnit.timeOffsetUnit.equals(timeOffsetUnit);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return timeOffsetUnit.hashCode();
	}
}
