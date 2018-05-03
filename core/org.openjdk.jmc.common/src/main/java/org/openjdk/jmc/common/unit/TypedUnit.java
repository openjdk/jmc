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
 * Type parameterized extension of {@link IUnit}. This construction exists to reduce clutter for
 * casual users of IUnit, while still providing type safety for internal implementations. (Proposed
 * "self-variance" extensions to Java, in JDK 9 or beyond, may directly support this with a single
 * interface.)
 */
public abstract class TypedUnit<U extends TypedUnit<U>> implements IUnit {
	public static interface UnitSelector<U extends TypedUnit<U>> {
		U getPreferredUnit(ITypedQuantity<U> quantity, double minNumericalValue, double maxNumericalValue);

		/**
		 * Get the largest unit, if any, in which this quantity can be expressed exactly, typically
		 * with an integer. If the quantity has zero magnitude (
		 * <code>{@link IQuantity#doubleValue()} == 0.0</code>), {@link IQuantity#getUnit()
		 * quantity.getUnit()} will be returned. Thus, if you want to find out a maximum common unit
		 * for a set of quantities (not recommended), only use the non-zero quantities.
		 * <p>
		 * Note that this may be a fairly expensive operation, and isn't intended to be used
		 * excessively. The only valid use case is for guessing the original unit in which a
		 * quantity was expressed, after it has been stored or transmitted using a legacy mechanism
		 * with a fixed unit.
		 *
		 * @return a unit or {@code null}
		 */
		U getLargestExactUnit(ITypedQuantity<U> quantity);
	}

	/**
	 * Get the typed {@link Class} object of the actual {@link TypedUnit} subclass {@code U}. This
	 * method is intended to simplify generic code, written in {@link TypedUnit}, to go from loosely
	 * typed arguments into stricter parameterized arguments.
	 */
	protected abstract Class<U> getUnitClass();

	/**
	 * Return a unit with the same origin (if absolute) as this unit, but with the given
	 * {@code deltaUnit} as its delta unit. {@link LinearUnit Linear units} will return
	 * {@code deltaUnit}.
	 *
	 * @throws IllegalArgumentException
	 *             if {@code deltaUnit} is of the wrong kind, although linear units are not
	 *             guaranteed to verify this here
	 */
	protected abstract U getScaledUnit(LinearUnit deltaUnit);

	@Override
	public abstract KindOfQuantity<U> getContentType();

	@Override
	public ITypedQuantity<U> quantity(Number value) {
		if ((value instanceof Long) || (value instanceof Integer) || (value instanceof Short)
				|| (value instanceof Byte)) {
			return quantity(value.longValue());
		} else {
			return quantity(value.doubleValue());
		}
	}

	@Override
	public abstract ITypedQuantity<U> quantity(long numericalValue);

	@Override
	public abstract ITypedQuantity<U> quantity(double numericalValue);

	@Override
	public IScalarAffineTransform valueTransformTo(IUnit targetUnit) {
		Class<U> unitClass = getUnitClass();
		if (!unitClass.isInstance(targetUnit)) {
			throw new IllegalArgumentException("Cannot convert from unit " + this //$NON-NLS-1$
					+ " into unit of type " + targetUnit.getContentType().getIdentifier()); //$NON-NLS-1$
		}
		return valueTransformTo(unitClass.cast(targetUnit));
	}

	/**
	 * Get a transform for transforming numerical quantity values expressed in this unit to
	 * numerical quantity values expressed in {@code targetUnit}. This method is typically only used
	 * internally by the quantity implementations. Note that this method differs from
	 * {@link #valueTransformTo(IUnit)} only by stricter typing.
	 *
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 */
	public abstract IScalarAffineTransform valueTransformTo(U targetUnit);

	protected final ITypedQuantity<U> addPossiblyIntegral(
		long numericalAugend, IScalarAffineTransform addendValueTransform, long numericalAddend) {
		if (addendValueTransform.isInteger() && !addendValueTransform.targetOutOfRange(numericalAddend,
				Long.MAX_VALUE - Math.abs(numericalAugend))) {
			return quantity(numericalAugend + addendValueTransform.targetValue(numericalAddend));
		}
		return quantity(numericalAugend + addendValueTransform.targetValue((double) numericalAddend));
	}

	protected abstract ITypedQuantity<U> add(long numericalAugend, LinearUnit addendUnit, long numericalAddend);

	protected abstract ITypedQuantity<LinearUnit> subtractSame(
		long numericalMinuend, U subtrahendUnit, long numericalSubtrahend);

	protected abstract ITypedQuantity<U> floorQuantize(long numericalValue, ITypedQuantity<LinearUnit> quanta);

	protected abstract ITypedQuantity<U> floorQuantize(double numericalValue, ITypedQuantity<LinearUnit> quanta);

	protected abstract String localizedFormatFor(
		Number numericalValue, boolean useBreakingSpace, boolean allowCustomUnit);

	protected abstract String persistableStringFor(Number numericalValue);
}
