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

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.common.unit.LinearKindOfQuantity.LinearUnitSelector;
import org.openjdk.jmc.common.unit.ScalarQuantity.LongStored;

public class BinaryUnitSelector implements LinearUnitSelector {
	protected final LinearKindOfQuantity kindOfQuantity;
	protected final Map<ScaleFactor, LinearUnit> unitCache = new HashMap<>();

	public BinaryUnitSelector(LinearKindOfQuantity kindOfQuantity) {
		this.kindOfQuantity = kindOfQuantity;
		unitCache.put(BinaryScaleFactor.get(0), kindOfQuantity.atomUnit);
	}

	public BinaryUnitSelector(LinearKindOfQuantity kindOfQuantity, Iterable<BinaryPrefix> prefixes) {
		this(kindOfQuantity);
		for (BinaryPrefix prefix : prefixes) {
			unitCache.put(prefix.scaleFactor(), kindOfQuantity.getUnit(prefix));
		}
	}

	@Override
	public LinearUnit getPreferredUnit(
		ITypedQuantity<LinearUnit> quantity, double minNumericalValue, double maxNumericalValue) {
		LinearUnit atomUnit = kindOfQuantity.atomUnit;
		double absVal = Math.abs(quantity.doubleValueIn(atomUnit));
		if ((absVal > minNumericalValue) && (absVal < maxNumericalValue)) {
			return atomUnit;
		}
		if (minNumericalValue > 1) {
			return getRegularUnit(absVal / minNumericalValue);
		}
		return getRegularUnit(absVal);
	}

	private LinearUnit getRegularUnit(double absValInAtomUnit) {
		BinaryScaleFactor factor = BinaryScaleFactor.getFloor1024Factor(absValInAtomUnit);
		return getUnit(factor);
	}

	private LinearUnit getUnit(BinaryScaleFactor factor) {
		LinearUnit unit = unitCache.get(factor);
		if (unit == null) {
			String unitName = factor.asExponentialStringBuilder(true)
					.append(kindOfQuantity.atomUnit.getAppendableSuffix(false)).toString();
			/*
			 * FIXME: Use a custom unit here, instead of just a null id, to avoid persisting and
			 * interactive editing?
			 * 
			 * Check if doing that may bring undesirable consequences.
			 */
			unit = new LinearUnit(kindOfQuantity, null, factor, unitName, unitName);
			unitCache.put(factor, unit);
		}
		return unit;
	}

	@Override
	public ITypedQuantity<LinearUnit> snapToBestBetweenHalfAndEqual(ITypedQuantity<LinearUnit> upperLimit) {
		assert Math.abs(upperLimit.doubleValue()) > 0;
		LinearUnit quantaUnit = getPreferredUnit(upperLimit, 1, 1024);
		int quantaLog2 = BinaryPrefix.getFloorLog2(upperLimit.doubleValueIn(quantaUnit));
		if (quantaLog2 >= 0) {
			long quantaSize = 1L << quantaLog2;
			return quantaUnit.quantity(quantaSize);
		} else {
			return quantaUnit.quantity(Math.scalb(1, quantaLog2));
		}
	}

	@Override
	public LinearUnit getLargestExactUnit(ITypedQuantity<LinearUnit> quantity) {
		// FIXME: Decide on return value for zero quantity.
		if (quantity.doubleValue() == 0.0) {
			return quantity.getUnit();
		}

		int log1024;
		if (quantity instanceof LongStored) {
			long val = quantity.longValue();
			if (val == 0) {
				return kindOfQuantity.atomUnit;
			}
			log1024 = BinaryPrefix.getAlignmentLog1024(val);
		} else {
			double val = quantity.doubleValue();
			if (val == 0.0) {
				return kindOfQuantity.atomUnit;
			}
			log1024 = BinaryPrefix.getAlignmentLog1024(val);
		}
		/*
		 * FIXME: Normalize to powers of 1024 _after_ factor concatenation.
		 * 
		 * This code isn't correct. It just happen to work as long as the input quantities are
		 * expressed in the same prefixed units.
		 */
		ScaleFactor factor = BinaryScaleFactor.get(log1024 * 10);
		factor = factor.concat(quantity.getUnit().valueTransformTo(kindOfQuantity.atomUnit));
		LinearUnit unit = unitCache.get(factor);
		if ((unit != null) && (unit.getIdentifier() != null)) {
			return unit;
		}

		return null;
	}
}
