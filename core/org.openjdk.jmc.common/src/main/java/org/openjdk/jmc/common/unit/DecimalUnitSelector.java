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

import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.common.unit.LinearKindOfQuantity.LinearUnitSelector;

public class DecimalUnitSelector implements LinearUnitSelector {
	protected final LinearKindOfQuantity kindOfQuantity;
	protected final LinearUnit exponentialBaseUnit;
	protected final ScaleFactor baseToAtomFactor;
	protected final Map<ScaleFactor, LinearUnit> unitCache = new HashMap<>();

	public DecimalUnitSelector(LinearKindOfQuantity kindOfQuantity) {
		this(kindOfQuantity, kindOfQuantity.atomUnit);
	}

	public DecimalUnitSelector(LinearKindOfQuantity kindOfQuantity, LinearUnit exponentialBaseUnit) {
		this.kindOfQuantity = kindOfQuantity;
		this.exponentialBaseUnit = exponentialBaseUnit;
		baseToAtomFactor = exponentialBaseUnit.valueTransformTo(kindOfQuantity.atomUnit);
		cachePlain(-3, "\u00d70.001"); //$NON-NLS-1$
		cachePlain(-2, "\u00d70.01"); //$NON-NLS-1$
		cachePlain(-1, "\u00d70.1"); //$NON-NLS-1$
		unitCache.put(DecimalScaleFactor.get(0), exponentialBaseUnit);
		cachePlain(1, "\u00d710"); //$NON-NLS-1$
		cachePlain(2, "\u00d7100"); //$NON-NLS-1$
		cachePlain(3, "\u00d71000"); //$NON-NLS-1$
	}

	public DecimalUnitSelector(LinearKindOfQuantity kindOfQuantity, Iterable<DecimalPrefix> prefixes) {
		// Prefixes are only allowed with the atom unit
		this(kindOfQuantity, kindOfQuantity.atomUnit);
		for (DecimalPrefix prefix : prefixes) {
			unitCache.put(prefix.scaleFactor(), kindOfQuantity.getUnit(prefix));
		}
	}

	protected void cachePlain(int powerOf10, String unitId) {
		DecimalScaleFactor factor = DecimalScaleFactor.get(powerOf10);
		String unitName = unitId.replace('.', DecimalFormatSymbols.getInstance().getDecimalSeparator())
				+ exponentialBaseUnit.getAppendableSuffix(false);
		ScaleFactor atomFactor = baseToAtomFactor.concat(factor);
		/*
		 * FIXME: Use a custom unit here, instead of just a null id, to avoid persisting and
		 * interactive editing?
		 *
		 * Check if doing that may bring undesirable consequences.
		 */
		LinearUnit unit = new LinearUnit(kindOfQuantity, null, atomFactor, unitName, unitName);
		unitCache.put(factor, unit);
	}

	@Override
	public LinearUnit getPreferredUnit(
		ITypedQuantity<LinearUnit> quantity, double minNumericalValue, double maxNumericalValue) {
		double absVal = Math.abs(quantity.doubleValueIn(exponentialBaseUnit));
		if ((absVal > minNumericalValue) && (absVal < maxNumericalValue)) {
			return exponentialBaseUnit;
		}
		if (minNumericalValue > 1) {
			return getRegularUnit(absVal / minNumericalValue, maxNumericalValue / minNumericalValue);
		}
		return getRegularUnit(absVal, maxNumericalValue);
	}

	private LinearUnit getRegularUnit(double absValInBaseUnit, double maxNumericalValue) {
		if (maxNumericalValue >= 1000.0) {
			return getUnit(DecimalScaleFactor.getEngFloorFactor(absValInBaseUnit));
		} else {
			return getUnit(DecimalScaleFactor.getSciFloorFactor(absValInBaseUnit));
		}
	}

	private LinearUnit getUnit(DecimalScaleFactor factor) {
		LinearUnit unit = unitCache.get(factor);
		if (unit == null) {
			String unitName = factor.asExponentialStringBuilder(true)
					.append(exponentialBaseUnit.getAppendableSuffix(false)).toString();
			ScaleFactor atomFactor = baseToAtomFactor.concat(factor);
			unit = new LinearUnit(kindOfQuantity, null, atomFactor, unitName, unitName);
			unitCache.put(factor, unit);
		}
		return unit;
	}

	@Override
	public ITypedQuantity<LinearUnit> snapToBestBetweenHalfAndEqual(ITypedQuantity<LinearUnit> upperLimit) {
		assert Math.abs(upperLimit.doubleValue()) > 0;
		// Requesting a smaller unit to attempt to ensure that "base" below is even.
		LinearUnit quantaUnit = getPreferredUnit(upperLimit, 10, 10000);
		double max = upperLimit.doubleValueIn(quantaUnit);
		// Start with (+/-) a power of 10
		long base = (long) (Math.pow(10, DecimalPrefix.getFloorLog10(max)) * Math.signum(max));
		double quotient = max / base;
		// Multiply with the least of 1, 2, 2.5, 5 to get in the allowed range.
		if (quotient > 5) {
			return quantaUnit.quantity(base * 5);
		} else if (quotient > 4) {
			/*
			 * FIXME: Replace factor 2.5 with 4 below? (The test above should always use 4.)
			 *
			 * We could most likely keep it as integer since we tried to choose quantaUnit such that
			 * base is even.
			 */
			if ((base & 1) == 0) {
				return quantaUnit.quantity((base * 5) >> 1);
			}
			return quantaUnit.quantity(base * 2.5);
		} else if (quotient > 2) {
			return quantaUnit.quantity(base * 2);
		}
		return quantaUnit.quantity(base);
	}

	@Override
	public LinearUnit getLargestExactUnit(ITypedQuantity<LinearUnit> quantity) {
		// FIXME: Decide on return value for zero quantity.
		if (quantity.doubleValue() == 0.0) {
			return quantity.getUnit();
		}

		double absVal = Math.abs(quantity.doubleValueIn(exponentialBaseUnit));
		int log10 = DecimalPrefix.getFloorLog1000(absVal) * 3;

		// FIXME: Iterate over a collection of specified units/prefixes instead (capped by absVal).
		final int minLog10 = DecimalPrefix.YOCTO.powerOf10();

		while (log10 >= minLog10) {
			DecimalScaleFactor inverseFactor = DecimalScaleFactor.get(-log10);
			double scaled = inverseFactor.targetValue(absVal);
			if (scaled == Math.rint(scaled)) {
				LinearUnit unit = unitCache.get(DecimalScaleFactor.get(log10));
				if ((unit != null) && (unit.getIdentifier() != null)) {
					return unit;
				}
			}
			log10 -= 3;
		}

		return null;
	}
}
