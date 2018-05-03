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
 * A linear transform, that is, a scale factor.
 * <p>
 * Note: This class has a natural ordering that is inconsistent with {@link #equals(Object) equals}.
 * (This is because in a certain subclass, {@link ImpreciseScaleFactor}, different instances may
 * express the same scale factor with different precision.)
 */
// FIXME: Find better method names.
// FIXME: Is it possible (or desirable) to extend Number?
public abstract class ScaleFactor implements IScalarAffineTransform, Comparable<ScaleFactor> {
	@Override
	public final IScalarAffineTransform concat(IScalarAffineTransform innerTransform) {
		// FIXME: Let subclasses do this, not very common case.
		if (innerTransform.isUnity()) {
			return this;
		}
		if (innerTransform instanceof ScaleFactor) {
			return concat((ScaleFactor) innerTransform);
		}
		if (innerTransform instanceof LongPreOffsetTransform) {
			LongPreOffsetTransform preOT = (LongPreOffsetTransform) innerTransform;
			return new LongPreOffsetTransform(preOT.preOffset, preOT.getMultiplier() * getMultiplier());
		}
		return SimpleAffineTransform.createWithPostOffset(innerTransform.getMultiplier() * getMultiplier(),
				targetNumber(innerTransform.getOffset()));
	}

	/**
	 * Concatenate (that is, multiply) this scale factor with {@code innerFactor}. This is just a
	 * special case of {@link #concat(IScalarAffineTransform)}.
	 *
	 * @return the combined scale factor
	 * @see #concat(IScalarAffineTransform)
	 */
	public abstract ScaleFactor concat(ScaleFactor innerFactor);

	@Override
	public final IScalarAffineTransform invertAndConcat(IScalarAffineTransform innerTransform) {
		if (innerTransform instanceof ScaleFactor) {
			return invertAndConcat((ScaleFactor) innerTransform);
		}
		return invert().concat(innerTransform);
	}

	/**
	 * Concatenate (that is, multiply) the inverse of this scale factor with {@code innerFactor}.
	 * This is just a special case of {@link #invertAndConcat(IScalarAffineTransform)}.
	 *
	 * @return the combined scale factor
	 * @see #invertAndConcat(IScalarAffineTransform)
	 */
	public ScaleFactor invertAndConcat(ScaleFactor innerFactor) {
		if (equals(innerFactor)) {
			return DecimalScaleFactor.get(0);
		}
		return invert().concat(innerFactor);
	}

	@Override
	public abstract ScaleFactor invert();

	@Override
	public final Number getOffset() {
		return 0;
	}

	@Override
	public double targetFloor(double srcNumericalValue) {
		return Math.floor(targetValue(srcNumericalValue));
	}

	@Override
	public int targetIntFloor(Number srcNumericalValue) {
		if ((srcNumericalValue instanceof Long) || (srcNumericalValue instanceof LongStored<?>)) {
			long longRes = targetFloor(srcNumericalValue.longValue());
			int intRes = (int) longRes;
			if (intRes != longRes) {
				return (longRes >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			}
			return intRes;
		}
		return (int) targetFloor(srcNumericalValue.doubleValue());
	}

	// FIXME: Override in subclasses to optimize? This would be a main point of these classes.
	@Override
	public Number targetNumber(long srcNumericalValue) {
		if (isInteger() && !targetOutOfRange(srcNumericalValue, Long.MAX_VALUE)) {
			return targetValue(srcNumericalValue);
		}
		return targetValue((double) srcNumericalValue);
	}

	@Override
	public Number targetNumber(Number srcNumericalValue) {
		if (srcNumericalValue instanceof Long) {
			return targetNumber(srcNumericalValue.longValue());
		}
		return targetValue(srcNumericalValue.doubleValue());
	}

	@Override
	public int compareTo(ScaleFactor other) {
		return Double.compare(getMultiplier(), other.getMultiplier());
	}

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();
}
