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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;

import org.openjdk.jmc.common.unit.LinearKindOfQuantity.LinearUnitSelector;

public class CustomUnitSelector implements LinearUnitSelector {
	protected final LinearKindOfQuantity kindOfQuantity;
	protected final double[] unitMultiplierArr;
	protected final LinearUnit[] unitArr;
	protected final double[] tickMultiplierArr;
	protected final ArrayList<ITypedQuantity<LinearUnit>> tickList;
	protected final LinearUnitSelector smallSelector;
	protected final LinearUnitSelector bigSelector;

	@SuppressWarnings("nls")
	public CustomUnitSelector(LinearKindOfQuantity kindOfQuantity, LinearUnitSelector smallSelector,
			Collection<LinearUnit> units, LinearUnitSelector bigSelector, SortedSet<ITypedQuantity<LinearUnit>> ticks) {
		this.kindOfQuantity = kindOfQuantity;
		this.smallSelector = smallSelector;
		this.bigSelector = bigSelector;
		unitArr = units.toArray(new LinearUnit[units.size()]);
		Arrays.sort(unitArr);
		unitMultiplierArr = new double[unitArr.length];
		for (int i = 0; i < unitArr.length; i++) {
			unitMultiplierArr[i] = unitArr[i].valueTransformTo(kindOfQuantity.atomUnit).getMultiplier();
		}

		tickList = new ArrayList<>(ticks);
		tickMultiplierArr = new double[tickList.size()];
		for (int i = 0; i < tickMultiplierArr.length; i++) {
			tickMultiplierArr[i] = tickList.get(i).doubleValueIn(kindOfQuantity.atomUnit);
			if ((i > 0) && (tickMultiplierArr[i] > tickMultiplierArr[i - 1] * 2)) {
				throw new IllegalArgumentException("Tick with growth factor > 2: " + tickList.get(i));
			}
		}
		// Double the first entry to handle that Arrays.binarySearch may return index beyond the length.
		tickList.add(0, tickList.get(0));
	}

	@Override
	public LinearUnit getPreferredUnit(
		ITypedQuantity<LinearUnit> quantity, double minNumericalValue, double maxNumericalValue) {
		LinearUnit atomUnit = kindOfQuantity.atomUnit;
		double absVal = Math.abs(quantity.doubleValueIn(atomUnit));

		double scaledVal = (minNumericalValue > 1) ? absVal / minNumericalValue : absVal;

		if (scaledVal < unitMultiplierArr[0]) {
			return smallSelector.getPreferredUnit(quantity, minNumericalValue, maxNumericalValue);
		}

		if (absVal < unitMultiplierArr[unitArr.length - 1] * maxNumericalValue) {
			// Since the expected number of units currently is around 5, no need to use Arrays.binarySearch().
			for (int i = 1; i < unitMultiplierArr.length; i++) {
				if (scaledVal < unitMultiplierArr[i]) {
					return unitArr[i - 1];
				}
			}
			return unitArr[unitArr.length - 1];
		}

		return bigSelector.getPreferredUnit(quantity, minNumericalValue, maxNumericalValue);
	}

	@Override
	public ITypedQuantity<LinearUnit> snapToBestBetweenHalfAndEqual(ITypedQuantity<LinearUnit> upperLimit) {
		assert Math.abs(upperLimit.doubleValue()) > 0;
		double absVal = Math.abs(upperLimit.doubleValueIn(kindOfQuantity.atomUnit));

		if (absVal < tickMultiplierArr[0]) {
			return smallSelector.snapToBestBetweenHalfAndEqual(upperLimit);
		}

		if (absVal > tickMultiplierArr[tickMultiplierArr.length - 1]) {
			return bigSelector.snapToBestBetweenHalfAndEqual(upperLimit);
		}

		int pos = Arrays.binarySearch(tickMultiplierArr, absVal);
		if (pos < 0) {
			pos = -1 - pos;
		}
		// FIXME: Else add one? So that exactly equal can be returned.

		return tickList.get(pos);
	}

	@Override
	public LinearUnit getLargestExactUnit(ITypedQuantity<LinearUnit> quantity) {
		// FIXME: Decide on return value for zero quantity.
		if (quantity.doubleValue() == 0.0) {
			return quantity.getUnit();
		}

		double absVal = Math.abs(quantity.doubleValueIn(kindOfQuantity.atomUnit));

		/*
		 * FIXME: Use a slightly different multiplier array derived from tickList.
		 * 
		 * If unitArr also is derived from tickList it can still be used.
		 */
		if (absVal < unitMultiplierArr[0]) {
			// No point in asking anyone but the small selector.
			return smallSelector.getLargestExactUnit(quantity);
		}

		int pos;
		if (absVal > unitMultiplierArr[unitMultiplierArr.length - 1]) {
			LinearUnit bigUnit = bigSelector.getLargestExactUnit(quantity);
			if (bigUnit != null) {
				return bigUnit;
			}
			pos = unitMultiplierArr.length - 1;
		} else {
			// FIXME: Arrays.binarySearch is overkill with the expected 5 values.
			pos = Arrays.binarySearch(unitMultiplierArr, absVal);
			if (pos >= 0) {
				// FIXME: Need to check accuracy due to limited double precision?
				return unitArr[pos];
			}
			// FIXME: Subtract one? Can this pos ever match?
			pos = -1 - pos;
		}

		while (pos >= 0) {
			if ((absVal % unitMultiplierArr[pos]) == 0) {
				// FIXME: Need to check accuracy due to limited double precision?
				return unitArr[pos];
			}
			pos--;
		}

		return smallSelector.getLargestExactUnit(quantity);
	}
}
