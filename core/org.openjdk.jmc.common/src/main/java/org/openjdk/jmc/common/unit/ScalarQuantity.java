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

/**
 * A scalar physical quantity value, conceptually a (real) number with a unit.
 * <p>
 * Note: This class has a natural ordering that is inconsistent with {@link #equals(Object) equals}.
 * (This is because different instances may express the same quantity with different precision. This
 * is inherent to implementations of {@link IQuantity}, irrespective of class hierarchies.)
 */
abstract class ScalarQuantity<U extends TypedUnit<U>> extends Number implements ITypedQuantity<U> {
	private static final long serialVersionUID = 1L;

	protected final U unit;

	protected ScalarQuantity(U unit) {
		this.unit = unit;
	}

	@Override
	public U getUnit() {
		return unit;
	}

	@Override
	public KindOfQuantity<U> getType() {
		return unit.getContentType();
	}

	@Override
	public int compareTo(IQuantity other) {
		if (other instanceof ScalarQuantity) {
			try {
				return Double.compare(doubleValue(), other.doubleValueIn(unit));
			} catch (IllegalArgumentException iae) {
				// Different kinds of quantity, fall through.
				/*
				 * FIXME: Quantity values of the same kind are probably compared a lot more often
				 * than those of different kinds, hence this exception path in the latter case. What
				 * we really should do is make a separate Comparator to handle the different kind of
				 * quantity case. This method should always throw an IllegalArgumentException in
				 * that case.
				 */
			}
		}
		return unit.getContentType().getIdentifier().compareTo(other.getUnit().getContentType().getIdentifier());
	}

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	@Override
	public IQuantity in(IUnit targetUnit) {
		if (targetUnit == unit) {
			return this;
		}
		return targetUnit.quantity(numberValueIn(targetUnit));
	}

	@Override
	public ITypedQuantity<U> in(U targetUnit) {
		if (targetUnit == unit) {
			return this;
		}
		return targetUnit.quantity(numberValueIn(targetUnit));
	}

	@Override
	public long longValueIn(IUnit targetUnit) throws QuantityConversionException {
		return longValueIn(targetUnit, Long.MAX_VALUE);
	}

	@Override
	public final String interactiveFormat() {
		return localizedFormat(true, false);
	}

	@Override
	public final String interactiveFormat(boolean allowCustomUnit) {
		return localizedFormat(true, allowCustomUnit);
	}

	@Override
	public String displayUsing(String formatIdentifier) {
		return unit.getContentType().getFormatter(formatIdentifier).format(this);
	}

	@Override
	public boolean isLinear() {
		return unit.isLinear();
	}

	@Override
	@SuppressWarnings("nls")
	public IQuantity add(IQuantity addend) throws IllegalArgumentException {
		if (!addend.isLinear()) {
			if (isLinear()) {
				// A linear unit doesn't know how to add non-linear quantities,
				// but the non-linear unit allows adding specific kinds of linear quantities.
				return addend.add(this);
			}
			throw new IllegalArgumentException(
					"Absolute quantities like " + addend + " cannot be added to " + this + ".");
		} else {
			@SuppressWarnings("unchecked")
			ITypedQuantity<LinearUnit> linearAddend = (ITypedQuantity<LinearUnit>) addend;
			return add(linearAddend);
		}
	}

	@Override
	public IQuantity subtract(IQuantity subtrahend) throws IllegalArgumentException {
		if (subtrahend.isLinear()) {
			@SuppressWarnings("unchecked")
			ITypedQuantity<LinearUnit> linearSubtrahend = (ITypedQuantity<LinearUnit>) subtrahend;
			return subtractLinear(linearSubtrahend);
		}
		@SuppressWarnings("unchecked")
		ITypedQuantity<U> typedSubtrahend = (ITypedQuantity<U>) subtrahend;
		// NOTE: When/if we have more than two kinds of direct TypedUnit subclasses, this could throw a ClassCastException.
		return subtract(typedSubtrahend);
	}

	protected abstract ITypedQuantity<U> subtractLinear(ITypedQuantity<LinearUnit> subtrahend);

	public static class LongStored<U extends TypedUnit<U>> extends ScalarQuantity<U> {
		private static final long serialVersionUID = 1L;

		private final long numericalValue;

		protected LongStored(long numericalValue, U unit) {
			super(unit);
			this.numericalValue = numericalValue;
		}

		@Override
		public int intValue() {
			return (int) numericalValue;
		}

		@Override
		public long longValue() {
			return numericalValue;
		}

		@Override
		public float floatValue() {
			return numericalValue;
		}

		@Override
		public double doubleValue() {
			return numericalValue;
		}

		@Override
		public Number numberValue() {
			return numericalValue;
		}

		@Override
		public long longValueIn(IUnit targetUnit, long maxAbsValue) throws QuantityConversionException {
			IScalarAffineTransform transform = unit.valueTransformTo(targetUnit);
			if (transform.targetOutOfRange(numericalValue, maxAbsValue)) {
				if (numericalValue < 0) {
					throw QuantityConversionException.tooLow(this, targetUnit.quantity(~maxAbsValue));
				}
				throw QuantityConversionException.tooHigh(this, targetUnit.quantity(maxAbsValue));
			}
			// FIXME: Check precision!
			return transform.targetValue(numericalValue);
		}

		@Override
		public long clampedLongValueIn(IUnit targetUnit) {
			return unit.valueTransformTo(targetUnit).targetValue(numericalValue);
		}

		@Override
		public long clampedFloorIn(IUnit targetUnit) {
			return unit.valueTransformTo(targetUnit).targetFloor(numericalValue);
		}

		@Override
		public int clampedIntFloorIn(IUnit targetUnit) {
			return unit.valueTransformTo(targetUnit).targetIntFloor(numericalValue);
		}

		@Override
		public double doubleValueIn(IUnit targetUnit) {
			return unit.valueTransformTo(targetUnit).targetValue((double) numericalValue);
		}

		@Override
		public Number numberValueIn(IUnit targetUnit) {
			return unit.valueTransformTo(targetUnit).targetNumber(numericalValue);
		}

		@Override
		public ITypedQuantity<U> floorQuantize(ITypedQuantity<LinearUnit> quanta) {
			return unit.floorQuantize(numericalValue, quanta);
		}

		@Override
		public String persistableString() {
			// Delegating to unit to handle custom derived units.
			return unit.persistableStringFor(numericalValue);
		}

		@Override
		public String localizedFormat(boolean useBreakingSpace, boolean allowCustomUnit) {
			// Delegating to unit to handle custom derived units.
			return unit.localizedFormatFor(numericalValue, useBreakingSpace, allowCustomUnit);
		}

		@Override
		public int hashCode() {
			return (int) numericalValue;
		}

		@Override
		public int compareTo(IQuantity other) {
			// Improve precision at least in this common case.
			if ((other instanceof LongStored) && (unit.equals(other.getUnit()))) {
				@SuppressWarnings("rawtypes")
				long otherVal = ((LongStored) other).numericalValue;
				return (numericalValue == otherVal) ? 0 : ((numericalValue < otherVal) ? -1 : 1);
			}
			return super.compareTo(other);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other instanceof LongStored) {
				@SuppressWarnings("rawtypes")
				LongStored otherQuantity = (LongStored) other;
				return (numericalValue == otherQuantity.numericalValue) && unit.equals(otherQuantity.unit);
			}
			return false;
		}

		@Override
		public String toString() {
			return String.valueOf(numericalValue) + unit.toString();
		}

		@Override
		public ITypedQuantity<U> add(ITypedQuantity<LinearUnit> addend) throws IllegalArgumentException {
			if (addend instanceof LongStored) {
				return unit.add(numericalValue, addend.getUnit(), addend.longValue());
			}
			// Assume addend is our corresponding linear kind of quantity, or an IllegalArgumentException will be thrown.
			return unit.quantity(numericalValue + addend.doubleValueIn(unit.getDeltaUnit()));
		}

		@Override
		protected ITypedQuantity<U> subtractLinear(ITypedQuantity<LinearUnit> subtrahend) {
			if (subtrahend instanceof LongStored) {
				return unit.add(numericalValue, subtrahend.getUnit(), -subtrahend.longValue());
			}
			// Assume subtrahend is our corresponding linear kind of quantity, or an IllegalArgumentException will be thrown.
			return unit.quantity(numericalValue - subtrahend.doubleValueIn(unit.getDeltaUnit()));
		}

		@Override
		public ITypedQuantity<LinearUnit> subtract(ITypedQuantity<U> subtrahend) throws IllegalArgumentException {
			if (subtrahend instanceof LongStored) {
				return unit.subtractSame(numericalValue, subtrahend.getUnit(), subtrahend.longValue());
			}
			// Assume subtrahend is our kind of quantity, or an IllegalArgumentException will be thrown.
			return unit.getDeltaUnit().quantity(numericalValue - subtrahend.doubleValueIn(unit));
		}

		@Override
		@SuppressWarnings("nls")
		public ITypedQuantity<U> multiply(long factor) throws UnsupportedOperationException {
			if (!isLinear()) {
				throw new UnsupportedOperationException(
						"Multiplication of non-linear quantities like " + this + " is not supported.");
			}
			return unit.quantity(numericalValue * factor);
		}

		@Override
		@SuppressWarnings("nls")
		public ITypedQuantity<U> multiply(double factor) throws UnsupportedOperationException {
			if (!isLinear()) {
				throw new UnsupportedOperationException(
						"Multiplication of non-linear quantities like " + this + " is not supported.");
			}
			return unit.quantity(numericalValue * factor);
		}

		@Override
		@SuppressWarnings("nls")
		public double ratioTo(IQuantity denominator) throws UnsupportedOperationException, IllegalArgumentException {
			if (!isLinear()) {
				throw new UnsupportedOperationException(
						"Ratios are not defined for non-linear quantities like " + this);
			}
			return numericalValue / denominator.doubleValueIn(unit);
		}
	}

	public static class DoubleStored<U extends TypedUnit<U>> extends ScalarQuantity<U> {
		private static final long serialVersionUID = 1L;

		private final double numericalValue;

		protected DoubleStored(double numericalValue, U unit) {
			super(unit);
			this.numericalValue = numericalValue;
		}

		@Override
		public int intValue() {
			return (int) numericalValue;
		}

		@Override
		public long longValue() {
			return (long) numericalValue;
		}

		@Override
		public float floatValue() {
			return (float) numericalValue;
		}

		@Override
		public double doubleValue() {
			return numericalValue;
		}

		@Override
		public Number numberValue() {
			return numericalValue;
		}

		@Override
		public long longValueIn(IUnit targetUnit, long maxAbsValue) throws QuantityConversionException {
			IScalarAffineTransform factor = unit.valueTransformTo(targetUnit);
			if (factor.targetOutOfRange(numericalValue, maxAbsValue)) {
				if (numericalValue < 0) {
					throw QuantityConversionException.tooLow(this, targetUnit.quantity(~maxAbsValue));
				}
				throw QuantityConversionException.tooHigh(this, targetUnit.quantity(maxAbsValue));
			}
			// FIXME: Check precision!
			return (long) factor.targetValue(numericalValue);
		}

		@Override
		public long clampedLongValueIn(IUnit targetUnit) {
			return (long) unit.valueTransformTo(targetUnit).targetValue(numericalValue);
		}

		@Override
		public long clampedFloorIn(IUnit targetUnit) {
			return (long) unit.valueTransformTo(targetUnit).targetFloor(numericalValue);
		}

		@Override
		public int clampedIntFloorIn(IUnit targetUnit) {
			return unit.valueTransformTo(targetUnit).targetIntFloor(numericalValue);
		}

		@Override
		public double doubleValueIn(IUnit targetUnit) {
			return unit.valueTransformTo(targetUnit).targetValue(numericalValue);
		}

		@Override
		public Number numberValueIn(IUnit targetUnit) {
			return unit.valueTransformTo(targetUnit).targetValue(numericalValue);
		}

		@Override
		public ITypedQuantity<U> floorQuantize(ITypedQuantity<LinearUnit> quanta) {
			return unit.floorQuantize(numericalValue, quanta);
		}

		@Override
		public String persistableString() {
			// Delegating to unit to handle custom derived units.
			return unit.persistableStringFor(numericalValue);
		}

		@Override
		public String localizedFormat(boolean useBreakingSpace, boolean allowCustomUnit) {
			// Delegating to unit to handle custom derived units.
			return unit.localizedFormatFor(numericalValue, useBreakingSpace, allowCustomUnit);
		}

		@Override
		public int hashCode() {
			long bits = Double.doubleToLongBits(numericalValue);
			return (int) (bits ^ (bits >>> 32));
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other instanceof DoubleStored) {
				@SuppressWarnings("rawtypes")
				DoubleStored otherQuantity = (DoubleStored) other;
				return (numericalValue == otherQuantity.numericalValue) && unit.equals(otherQuantity.unit);
			}
			return false;
		}

		@Override
		public String toString() {
			return String.valueOf(numericalValue) + unit.toString();
		}

		@Override
		public ITypedQuantity<U> add(ITypedQuantity<LinearUnit> addend) throws IllegalArgumentException {
			// Assume addend is our corresponding linear kind of quantity, or an IllegalArgumentException will be thrown.
			return unit.quantity(numericalValue + addend.doubleValueIn(unit.getDeltaUnit()));
		}

		@Override
		protected ITypedQuantity<U> subtractLinear(ITypedQuantity<LinearUnit> subtrahend) {
			// Assume subtrahend is our corresponding linear kind of quantity, or an IllegalArgumentException will be thrown.
			return unit.quantity(numericalValue - subtrahend.doubleValueIn(unit.getDeltaUnit()));
		}

		@Override
		public ITypedQuantity<LinearUnit> subtract(ITypedQuantity<U> subtrahend) throws IllegalArgumentException {
			// Assume subtrahend is our kind of quantity, or an IllegalArgumentException will be thrown.
			return unit.getDeltaUnit().quantity(numericalValue - subtrahend.doubleValueIn(unit));
		}

		@Override
		@SuppressWarnings("nls")
		public ITypedQuantity<U> multiply(long factor) throws UnsupportedOperationException {
			if (!isLinear()) {
				throw new UnsupportedOperationException(
						"Multiplication of non-linear quantities like " + this + " is not supported.");
			}
			return unit.quantity(numericalValue * factor);
		}

		@Override
		@SuppressWarnings("nls")
		public ITypedQuantity<U> multiply(double factor) throws UnsupportedOperationException {
			if (!isLinear()) {
				throw new UnsupportedOperationException(
						"Multiplication of non-linear quantities like " + this + " is not supported.");
			}
			return unit.quantity(numericalValue * factor);
		}

		@Override
		@SuppressWarnings("nls")
		public double ratioTo(IQuantity denominator) throws UnsupportedOperationException, IllegalArgumentException {
			if (!isLinear()) {
				throw new UnsupportedOperationException(
						"Ratios are not defined for non-linear quantities like " + this);
			}
			return numericalValue / denominator.doubleValueIn(unit);
		}
	}
}
