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
 * {@link IScalarAffineTransform} used as the inverse of {@link LongPreOffsetTransform} to avoid
 * losing the precision of the long offset. Otherwise similar to {@link SimpleAffineTransform}.
 */
public class LongPostOffsetTransform implements IScalarAffineTransform {
	private final double multiplier;
	private final long offset;

	public LongPostOffsetTransform(double multiplier, long offset) {
		this.multiplier = multiplier;
		this.offset = offset;
	}

	@Override
	public Number getOffset() {
		return offset;
	}

	@Override
	public double getMultiplier() {
		return multiplier;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof LongPostOffsetTransform) {
			LongPostOffsetTransform otherSAT = (LongPostOffsetTransform) other;
			return (multiplier == otherSAT.multiplier) && (offset == otherSAT.offset);
		}
		return false;
	}

	@Override
	public int hashCode() {
		long bits = Double.doubleToRawLongBits(multiplier) ^ Double.doubleToRawLongBits(offset);
		return (int) (bits ^ (bits >>> 32));
	}

	@Override
	public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
		if (multiplier >= 1.0) {
			return (srcNumericalValue > ((maxAbsValue - offset) / multiplier))
					|| (srcNumericalValue < (((~maxAbsValue) - offset) / multiplier));
		} else {
			return (targetValue(srcNumericalValue) > maxAbsValue) || (targetValue(srcNumericalValue) < (~maxAbsValue));
		}
	}

	@Override
	public boolean targetOutOfRange(double srcNumericalValue, long maxAbsValue) {
		if (multiplier >= 1.0) {
			return (srcNumericalValue > ((maxAbsValue - offset) / multiplier))
					|| (srcNumericalValue < (((~maxAbsValue) - offset) / multiplier));
		} else {
			return (targetValue(srcNumericalValue) > maxAbsValue) || (targetValue(srcNumericalValue) < (~maxAbsValue));
		}
	}

	@Override
	public long targetValue(long srcNumericalValue) {
		// FIXME: Avoid calculation overflow. Clamp result to possible range!
		return Math.round(srcNumericalValue * multiplier) + offset;
	}

	@Override
	public long targetFloor(long srcNumericalValue) {
		// FIXME: Avoid calculation overflow. Clamp result to possible range!
		return ((long) Math.floor(srcNumericalValue * multiplier)) + offset;
	}

	@Override
	public double targetFloor(double srcNumericalValue) {
		return Math.floor(srcNumericalValue * multiplier + offset);
	}

	@Override
	public int targetIntFloor(Number srcNumericalValue) {
		// FIXME: Avoid calculation overflow. Clamp result to possible range!
		return (int) targetFloor(srcNumericalValue.doubleValue());
	}

	@Override
	public Number targetNumber(long srcNumericalValue) {
		// FIXME: Avoid calculation overflow. Clamp result to possible range!
		return Math.round(srcNumericalValue * multiplier) + offset;
	}

	@Override
	public Number targetNumber(Number srcNumericalValue) {
		// FIXME: Avoid calculation overflow. Clamp result to possible range!
		return Math.round(srcNumericalValue.doubleValue() * multiplier) + offset;
	}

	@Override
	public double targetValue(double srcNumericalValue) {
		return srcNumericalValue * multiplier + offset;
	}

	@Override
	public IScalarAffineTransform invert() {
		return new LongPreOffsetTransform(-offset, 1.0 / multiplier);
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
		if (innerTransform instanceof ScaleFactor) {
			return new LongPostOffsetTransform(multiplier * innerTransform.getMultiplier(), offset);
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
				(otherOffset - offset) * invertedMultiplier);
	}
}
