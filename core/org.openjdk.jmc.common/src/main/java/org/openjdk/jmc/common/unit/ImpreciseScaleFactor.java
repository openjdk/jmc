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
 * Quick and dirty {@link ScaleFactor} implementation. Should be replaced with more precise
 * (rational) converters.
 */
public class ImpreciseScaleFactor extends ScaleFactor {
	private final Number numberFactor;

	public ImpreciseScaleFactor(Number factor) {
		numberFactor = factor;
	}

	@Override
	public ScaleFactor concat(ScaleFactor innerFactor) {
		if (innerFactor.isUnity()) {
			return this;
		}
		return new ImpreciseScaleFactor(innerFactor.targetValue(numberFactor.doubleValue()));
	}

	@Override
	public ScaleFactor invert() {
		return new ImpreciseScaleFactor(1.0 / numberFactor.doubleValue());
	}

	@Override
	public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
		if (numberFactor.doubleValue() >= 1.0) {
			if (srcNumericalValue >= 0) {
				return srcNumericalValue > (maxAbsValue / getMultiplier());
			} else {
				return srcNumericalValue < ((~maxAbsValue) / getMultiplier());
			}
		} else {
			if (srcNumericalValue >= 0) {
				return targetValue(srcNumericalValue) > maxAbsValue;
			} else {
				return targetValue(srcNumericalValue) < (~maxAbsValue);
			}
		}
	}

	@Override
	public boolean targetOutOfRange(double srcNumericalValue, long maxAbsValue) {
		if (numberFactor.doubleValue() >= 1.0) {
			if (srcNumericalValue >= 0) {
				return srcNumericalValue > (maxAbsValue / getMultiplier());
			} else {
				return srcNumericalValue < ((~maxAbsValue) / getMultiplier());
			}
		} else {
			if (srcNumericalValue >= 0) {
				return targetValue(srcNumericalValue) > maxAbsValue;
			} else {
				return targetValue(srcNumericalValue) < (~maxAbsValue);
			}
		}
	}

	@Override
	public double targetValue(double srcNumericalValue) {
		return srcNumericalValue * numberFactor.doubleValue();
	}

	@Override
	public long targetValue(long srcNumericalValue) {
		return Math.round(srcNumericalValue * numberFactor.doubleValue());
	}

	@Override
	public long targetFloor(long srcNumericalValue) {
		return (long) Math.floor(srcNumericalValue * numberFactor.doubleValue());
	}

	@Override
	public Number targetNumber(long srcNumericalValue) {
		return targetValue((double) srcNumericalValue);
	}

	@Override
	public Number targetNumber(Number srcNumericalValue) {
		return targetValue(srcNumericalValue.doubleValue());
	}

	@Override
	public boolean isUnity() {
		// NOTE: Since we're imprecise, we cannot return true.
		return false;
	}

	@Override
	public boolean isInteger() {
		// NOTE: Since we're imprecise, we cannot return true (yet).
		return false;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof ImpreciseScaleFactor)
				&& numberFactor.equals(((ImpreciseScaleFactor) other).numberFactor);
	}

	@Override
	public int hashCode() {
		return numberFactor.hashCode();
	}

	@Override
	public String toString() {
		return numberFactor.toString();
	}

	@Override
	public double getMultiplier() {
		return numberFactor.doubleValue();
	}
}
