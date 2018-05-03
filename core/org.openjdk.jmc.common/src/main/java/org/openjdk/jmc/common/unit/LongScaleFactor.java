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
 * Short term {@link ScaleFactor} implementation to improve custom units.
 */
public class LongScaleFactor extends ScaleFactor {
	private final long longMultiplier;

	public LongScaleFactor(long factor) {
		longMultiplier = factor;
	}

	@Override
	public ScaleFactor concat(ScaleFactor innerFactor) {
		if (innerFactor.isUnity()) {
			return this;
		}
		Number multiplier = innerFactor.targetNumber(longMultiplier);
		if (multiplier instanceof Long) {
			return new LongScaleFactor(multiplier.longValue());
		}
		return new ImpreciseScaleFactor(multiplier);
	}

	@Override
	public ScaleFactor invertAndConcat(ScaleFactor innerFactor) {
		if (innerFactor instanceof LongScaleFactor) {
			long innerMult = ((LongScaleFactor) innerFactor).longMultiplier;
			if (innerMult % longMultiplier == 0) {
				return new LongScaleFactor(innerMult / longMultiplier);
			}
		}
		return super.invertAndConcat(innerFactor);
	}

	@Override
	public ScaleFactor invert() {
		return new ImpreciseScaleFactor(1.0 / longMultiplier);
	}

	@Override
	public boolean targetOutOfRange(double srcNumericalValue, long maxAbsValue) {
		if (srcNumericalValue >= 0) {
			return srcNumericalValue > (maxAbsValue / getMultiplier());
		} else {
			return srcNumericalValue < ((~maxAbsValue) / getMultiplier());
		}
	}

	@Override
	public double targetValue(double srcNumericalValue) {
		return srcNumericalValue * longMultiplier;
	}

	@Override
	public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
		if (srcNumericalValue >= 0) {
			return srcNumericalValue > (maxAbsValue / longMultiplier);
		} else {
			return srcNumericalValue < ((~maxAbsValue) / longMultiplier);
		}
	}

	@Override
	public long targetValue(long srcNumericalValue) {
		// Floor and round are the same when scaling up.
		return targetFloor(srcNumericalValue);
	}

	@Override
	public long targetFloor(long srcNumericalValue) {
		// Floor and round are the same when scaling up.
		if (targetOutOfRange(srcNumericalValue, Long.MAX_VALUE)) {
			return (srcNumericalValue >= 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
		}
		return srcNumericalValue * longMultiplier;
	}

	@Override
	public Number targetNumber(long srcNumericalValue) {
		if (!targetOutOfRange(srcNumericalValue, Long.MAX_VALUE)) {
			return srcNumericalValue * longMultiplier;
		}
		return targetValue((double) srcNumericalValue);
	}

	@Override
	public boolean isUnity() {
		return (longMultiplier == 1L);
	}

	@Override
	public boolean isInteger() {
		return true;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof LongScaleFactor) && (longMultiplier == ((LongScaleFactor) other).longMultiplier);
	}

	@Override
	public int hashCode() {
		// NOTE: Need to ensure that if isUnity() is true, the hash code is 0.
		return (int) longMultiplier;
	}

	@Override
	public String toString() {
		return String.valueOf(longMultiplier);
	}

	@Override
	public double getMultiplier() {
		return longMultiplier;
	}
}
