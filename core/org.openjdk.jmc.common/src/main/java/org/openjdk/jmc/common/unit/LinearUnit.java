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

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;

/**
 * A measurement unit for a particular kind of quantity.
 */
public class LinearUnit extends TypedUnit<LinearUnit> implements Comparable<LinearUnit> {
	private final LinearKindOfQuantity kindOfQuantity;
	private final ScaleFactor factorToAtom;
	private final String unitId;
	private final String idSuffix;
	private final String unitSymbol;
	private final String nonBreakingSuffix;
	private final String breakingSuffix;
	private final String unitDescription;
	private final String[] altNames;
	private final Map<String, ? extends LinearUnit> parseMap;

	protected static class Custom extends LinearUnit {
		private final ScaleFactor factorToDefinition;
		private final LinearUnit definitionUnit;

		protected Custom(LinearKindOfQuantity kindOfQuantity, String unitId, ScaleFactor factorToDefinition,
				LinearUnit definitionUnit, String unitSymbol, String unitDesc, String ... altNames) {
			super(kindOfQuantity, unitId, definitionUnit.factorToAtom.concat(factorToDefinition), unitSymbol, unitDesc,
					altNames);
			this.factorToDefinition = factorToDefinition;
			this.definitionUnit = definitionUnit;
		}

		@Override
		protected boolean isCustom() {
			return true;
		}

		@Override
		public ITypedQuantity<LinearUnit> asWellKnownQuantity() {
			return definitionUnit.quantity(factorToDefinition.targetNumber(1));
		}

		@Override
		protected String persistableStringFor(Number numericalValue) {
			return definitionUnit.persistableStringFor(factorToDefinition.targetNumber(numericalValue));
		}

		@Override
		protected String localizedFormatFor(Number numericalValue, boolean useBreakingSpace, boolean allowCustomUnit) {
			if (allowCustomUnit) {
				return super.localizedFormatFor(numericalValue, useBreakingSpace, allowCustomUnit);
			}
			// FIXME: Replace allowCustomUnit boolean above with three valued enum? To include "=" (if Long) or "~=" (otherwise)?
			return definitionUnit.localizedFormatFor(factorToDefinition.targetNumber(numericalValue), useBreakingSpace,
					false);
		}

		// Just to silence SpotBugs.
		@Override
		public boolean equals(Object other) {
			return super.equals(other);
		}

		// Just to silence SpotBugs.
		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}

	public LinearUnit(LinearKindOfQuantity kindOfQuantity, String unitId, ScaleFactor factorToAtom, String unitSymbol,
			String unitDesc, String ... altNames) {
		this.kindOfQuantity = kindOfQuantity;
		this.unitId = unitId;
		idSuffix = ((unitId == null) || (unitId.length() == 0)) ? "" : (" " + unitId); //$NON-NLS-1$ //$NON-NLS-2$
		this.unitSymbol = unitSymbol;
		nonBreakingSuffix = (unitSymbol.length() == 0) ? "" : ("\u00a0" + unitSymbol); //$NON-NLS-1$ //$NON-NLS-2$
		breakingSuffix = nonBreakingSuffix.replace('\u00a0', ' ');
		unitDescription = unitDesc;
		this.factorToAtom = factorToAtom;
		this.altNames = (altNames != null) ? altNames : EMPTY_STRING_ARRAY;
		parseMap = Collections.singletonMap(unitSymbol.replace('\u00a0', ' '), this);
	}

	@Override
	protected final Class<LinearUnit> getUnitClass() {
		return LinearUnit.class;
	}

	@Override
	public LinearKindOfQuantity getContentType() {
		return kindOfQuantity;
	}

	@Override
	public LinearUnit getDeltaUnit() {
		return this;
	}

	@Override
	protected LinearUnit getScaledUnit(LinearUnit deltaUnit) {
		return deltaUnit;
	}

	@Override
	public String getAppendableSuffix(boolean useBreakingSpace) {
		return useBreakingSpace ? breakingSuffix : nonBreakingSuffix;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other instanceof LinearUnit) {
			LinearUnit otherUnit = (LinearUnit) other;
			return kindOfQuantity.equals(otherUnit.kindOfQuantity) && factorToAtom.equals(otherUnit.factorToAtom);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (kindOfQuantity.hashCode() << 16) + factorToAtom.hashCode();
	}

	@Override
	public int compareTo(LinearUnit other) {
		// FIXME: Throw exception if different kindOfQuantity.
		return factorToAtom.compareTo(other.factorToAtom);
	}

	@Override
	public ITypedQuantity<LinearUnit> quantity(long numericalValue) {
		return new ScalarQuantity.LongStored<>(numericalValue, this);
	}

	@Override
	public ITypedQuantity<LinearUnit> quantity(double numericalValue) {
		return new ScalarQuantity.DoubleStored<>(numericalValue, this);
	}

	@Override
	public ScaleFactor valueTransformTo(LinearUnit targetUnit) {
		if (targetUnit.getContentType() != getContentType()) {
			throw new IllegalArgumentException("Cannot convert from unit " + this //$NON-NLS-1$
					+ " into unit of type " + targetUnit.getContentType().getIdentifier()); //$NON-NLS-1$
		}
		return targetUnit.valueFactorToAtom().invertAndConcat(factorToAtom);
	}

	private ScaleFactor valueFactorToAtom() {
		return factorToAtom;
	}

	@Override
	public boolean isLinear() {
		return true;
	}

	@Override
	protected ITypedQuantity<LinearUnit> add(long numericalAugend, LinearUnit addendUnit, long numericalAddend) {
		int comparision = compareTo(addendUnit);
		if (comparision == 0) {
			long sum = numericalAugend + numericalAddend;
			// See Math.addExact() since JDK 8.
			// HD 2-12 Overflow iff both arguments have the opposite sign of the result
			if (((numericalAugend ^ sum) & (numericalAddend ^ sum)) >= 0) {
				return quantity(numericalAugend + numericalAddend);
			}
			return quantity(numericalAugend + (double) numericalAddend);
		} else if (comparision < 0) {
			return addPossiblyIntegral(numericalAugend, addendUnit.valueTransformTo(this), numericalAddend);
		} else {
			return addendUnit.addPossiblyIntegral(numericalAddend, valueTransformTo(addendUnit), numericalAugend);
		}
	}

	@Override
	protected ITypedQuantity<LinearUnit> subtractSame(
		long numericalMinuend, LinearUnit subtrahendUnit, long numericalSubtrahend) {
		return add(numericalMinuend, subtrahendUnit, -numericalSubtrahend);
	}

	private long floorQuantize(long value, long alignment) {
		if (value >= 0) {
			return (value / alignment) * alignment;
		} else {
			// Trick to do floor divide for negative values although Java uses symmetric divide.
			return (~(~value / alignment)) * alignment;
		}
	}

	@Override
	protected ITypedQuantity<LinearUnit> floorQuantize(long numericalValue, ITypedQuantity<LinearUnit> quanta) {
		LinearUnit quantaUnit = quanta.getUnit();
		if (quanta instanceof ScalarQuantity.LongStored) {
			long quantaSize = quanta.longValue();
			ScaleFactor scaleToQuanta = valueTransformTo(quantaUnit);
			/*
			 * FIXME: Simplify by concatenating the ScaleFactor with an exact inverse of
			 * LongScaleFactor(quantaSize), if that is ever implemented.
			 */
			if (!scaleToQuanta.targetOutOfRange(numericalValue, Long.MAX_VALUE - quantaSize)) {
				long alignedNumerical = floorQuantize(scaleToQuanta.targetFloor(numericalValue), quantaSize);
				return quantaUnit.quantity(alignedNumerical);
			}
			// Cannot express start as long in quantaUnit. Fall through.
		}
		double quantaInThisUnit = quanta.doubleValueIn(this);
		long maybeLongQuanta = (long) quantaInThisUnit;
		if (quantaInThisUnit == maybeLongQuanta) {
			// Quanta can be expressed exactly as long in this unit. Great!
			return quantity(floorQuantize(numericalValue, maybeLongQuanta));
		}
		/*
		 * FIXME: Attempt to handle 2.5 ns quanta by quantizing to 2 * quanta? Might require another
		 * bucket.
		 *
		 * For now falling back to doubles, although resolution likely is insufficient.
		 */
		double alignedNumerical = Math.floor(numericalValue / quantaInThisUnit) * quantaInThisUnit;
		return quantity(alignedNumerical);
	}

	@Override
	protected ITypedQuantity<LinearUnit> floorQuantize(double numericalValue, ITypedQuantity<LinearUnit> quanta) {
		double quantaInStartUnit = quanta.doubleValueIn(this);
		double alignedStartNumerical = Math.floor(numericalValue / quantaInStartUnit) * quantaInStartUnit;
		return quantity(alignedStartNumerical);
	}

	@Override
	public String toString() {
		return getIdentifier() + '[' + getLocalizedSymbol() + ']';
	}

	@Override
	public String getIdentifier() {
		return unitId;
	}

	@Override
	public String getLocalizedSymbol() {
		return unitSymbol;
	}

	@Override
	public String getLocalizedDescription() {
		return unitDescription;
	}

	@Override
	public String[] getAltLocalizedNames() {
		return altNames;
	}

	protected boolean isCustom() {
		return false;
	}

	/**
	 * @return a quantity with the same magnitude as this unit, defined in a non-custom unit
	 *         (possibly itself)
	 */
	public ITypedQuantity<LinearUnit> asWellKnownQuantity() {
		return quantity(1);
	}

	@Override
	protected String persistableStringFor(Number numericalValue) {
		return String.valueOf(numericalValue) + idSuffix;
	}

	@Override
	protected String localizedFormatFor(Number numericalValue, boolean useBreakingSpace, boolean allowCustomUnit) {
		NumberFormat formatter = LinearKindOfQuantity.getNumberFormat(useBreakingSpace);
		return formatter.format(numericalValue) + getAppendableSuffix(useBreakingSpace);
	}

	/**
	 * Parse an interactive string, like {@link KindOfQuantity#parseInteractive(String)}, with the
	 * addition that this unit is accepted, even if not generally by the kind of quantity. Only
	 * guaranteed to be able to parse strings produced by
	 * {@link #localizedFormatFor(Number, boolean, boolean)} for this unit or by
	 * {@link IQuantity#interactiveFormat()} for quantities of this kind of quantity, and in the
	 * same locale. Only use this for interactive input, never for persisted strings.
	 *
	 * @param interactiveQuantity
	 *            interactive string to parse
	 * @return a valid quantity for this kind of quantity
	 * @throws QuantityConversionException
	 *             if parsing failed
	 */
	public ITypedQuantity<LinearUnit> customParseInteractive(String interactiveQuantity)
			throws QuantityConversionException {
		return kindOfQuantity.parseInteractive(interactiveQuantity, parseMap);
	}
}
