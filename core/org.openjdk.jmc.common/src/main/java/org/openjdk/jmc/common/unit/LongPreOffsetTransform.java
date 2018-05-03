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

import org.openjdk.jmc.common.unit.ScalarQuantity.LongStored;

/**
 * {@link IScalarAffineTransform Transform} that adds an offset first and then multiplies with the
 * multiplier. This is typically used to reduce cancellation errors when the offset is larger than
 * can be accurately represented in a double.
 */
public class LongPreOffsetTransform implements IScalarAffineTransform {
	final long preOffset;
	private final double multiplier;

	public LongPreOffsetTransform(long preOffset, double multiplier) {
		this.preOffset = preOffset;
		this.multiplier = multiplier;
	}

	@Override
	public Number getOffset() {
		return preOffset * multiplier;
	}

	@Override
	public double getMultiplier() {
		return multiplier;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof LongPreOffsetTransform) {
			LongPreOffsetTransform otherPreOT = (LongPreOffsetTransform) other;
			return (preOffset == otherPreOT.preOffset) && (multiplier == otherPreOT.multiplier);
		}
		return false;
	}

	@Override
	public int hashCode() {
		long bits = Double.doubleToRawLongBits(preOffset) ^ Double.doubleToRawLongBits(multiplier);
		return (int) (bits ^ (bits >>> 32));
	}

	@Override
	public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
		if (multiplier >= 1.0) {
			return (srcNumericalValue > ((maxAbsValue / multiplier) - preOffset))
					|| (srcNumericalValue < (((~maxAbsValue) / multiplier) - preOffset));
		} else {
			return (targetValue(srcNumericalValue) > maxAbsValue) || (targetValue(srcNumericalValue) < (~maxAbsValue));
		}
	}

	@Override
	public boolean targetOutOfRange(double srcNumericalValue, long maxAbsValue) {
		if (multiplier >= 1.0) {
			return (srcNumericalValue > ((maxAbsValue / multiplier) - preOffset))
					|| (srcNumericalValue < (((~maxAbsValue) / multiplier) - preOffset));
		} else {
			return (targetValue(srcNumericalValue) > maxAbsValue) || (targetValue(srcNumericalValue) < (~maxAbsValue));
		}
	}

	@Override
	public long targetValue(long srcNumericalValue) {
		return Math.round((srcNumericalValue + preOffset) * multiplier);
	}

	@Override
	public long targetFloor(long srcNumericalValue) {
		return (long) Math.floor((srcNumericalValue + preOffset) * multiplier);
	}

	@Override
	public double targetFloor(double srcNumericalValue) {
		return Math.floor((srcNumericalValue + preOffset) * multiplier);
	}

	@Override
	public int targetIntFloor(Number srcNumericalValue) {
		if ((srcNumericalValue instanceof Long) || (srcNumericalValue instanceof LongStored<?>)) {
			return (int) Math.floor((srcNumericalValue.longValue() + preOffset) * multiplier);
		}
		return (int) targetFloor(srcNumericalValue.doubleValue());
	}

	@Override
	public Number targetNumber(long srcNumericalValue) {
		return (srcNumericalValue + preOffset) * multiplier;
	}

	@Override
	public Number targetNumber(Number srcNumericalValue) {
		if ((srcNumericalValue instanceof Long) || (srcNumericalValue instanceof LongStored<?>)) {
			// NOTE: Mustn't use targetValue() as the result cannot be guaranteed to be exact.
			return targetNumber(srcNumericalValue.longValue());
		}
		return targetValue(srcNumericalValue.doubleValue());
	}

	@Override
	public double targetValue(double srcNumericalValue) {
		return (srcNumericalValue + preOffset) * multiplier;
	}

	@Override
	public IScalarAffineTransform invert() {
		return new LongPostOffsetTransform(1.0 / multiplier, -preOffset);
	}

	@Override
	public boolean isUnity() {
		return false;
	}

	@Override
	public boolean isInteger() {
		return false;
	}

	@Override
	public IScalarAffineTransform concat(IScalarAffineTransform innerTransform) {
		if (innerTransform.isUnity()) {
			return this;
		}
		return SimpleAffineTransform.createWithPostOffset(multiplier * innerTransform.getMultiplier(),
				targetNumber(innerTransform.getOffset()));
	}

	@Override
	public IScalarAffineTransform invertAndConcat(IScalarAffineTransform innerTransform) {
		if (equals(innerTransform)) {
			return DecimalScaleFactor.get(0);
		}
		if (innerTransform.isUnity()) {
			return invert();
		}
		double invertedMultiplier = 1.0 / multiplier;
		double otherOffset = innerTransform.getOffset().doubleValue();
		return new SimpleAffineTransform(innerTransform.getMultiplier() * invertedMultiplier,
				(otherOffset * invertedMultiplier) - preOffset);
	}
}
