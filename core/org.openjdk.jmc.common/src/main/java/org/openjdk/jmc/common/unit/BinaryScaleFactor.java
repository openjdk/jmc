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

public class BinaryScaleFactor extends ScaleFactor {
	protected final int powerOf2;

	private static class Unity extends BinaryScaleFactor {
		public Unity() {
			super(0);
		}

		@Override
		public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
			if (srcNumericalValue >= 0) {
				return srcNumericalValue > maxAbsValue;
			} else {
				return srcNumericalValue < (~maxAbsValue);
			}
		}

		@Override
		public long targetValue(long srcNumericalValue) {
			return srcNumericalValue;
		}

		@Override
		public long targetFloor(long srcNumericalValue) {
			return srcNumericalValue;
		}

		@Override
		public double targetValue(double srcNumericalValue) {
			return srcNumericalValue;
		}

		@Override
		public BinaryScaleFactor invert() {
			return this;
		}
	}

	private static class UpSmall extends BinaryScaleFactor {
		public UpSmall(int powerOf2) {
			super(powerOf2);
			assert powerOf2 > 0;
			assert powerOf2 < 64;
		}

		@Override
		public ScaleFactor concat(ScaleFactor innerFactor) {
			if (innerFactor.isUnity()) {
				return this;
			}
			if (innerFactor instanceof BinaryScaleFactor) {
				return get(powerOf2 + ((BinaryScaleFactor) innerFactor).powerOf2);
			}
			long longMultiplier = 1L << powerOf2;
			Number multiplier = innerFactor.targetNumber(longMultiplier);
			if (multiplier instanceof Long) {
				return new LongScaleFactor(multiplier.longValue());
			}
			return new ImpreciseScaleFactor(multiplier);
		}

		@Override
		public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
			if (srcNumericalValue >= 0) {
				return srcNumericalValue > (maxAbsValue >> powerOf2);
			} else {
				return srcNumericalValue < ((~maxAbsValue) >> powerOf2);
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
			return srcNumericalValue << powerOf2;
		}

		@Override
		public BinaryScaleFactor invert() {
			return Cache.NEG_POWERS[powerOf2];
		}
	}

	private static class DownSmall extends BinaryScaleFactor {
		public DownSmall(int powerOf2) {
			super(powerOf2);
			assert powerOf2 < 0;
			assert powerOf2 > -64;
		}

		@Override
		public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
			if (srcNumericalValue >= 0) {
				return targetValue(srcNumericalValue) > maxAbsValue;
			} else {
				return targetValue(srcNumericalValue) < (~maxAbsValue);
			}
		}

		@Override
		public long targetValue(long srcNumericalValue) {
			/*
			 * Round by adding half of the divisor and then floor divide (by shifting). Need to
			 * shift down first, almost all the way, before adding the half in order to avoid
			 * overflow.
			 */
			return ((srcNumericalValue >> (-powerOf2 - 1)) + 1) >> 1;
		}

		@Override
		public long targetFloor(long srcNumericalValue) {
			// Floor division by shifting
			return srcNumericalValue >> -powerOf2;
		}

		@Override
		public BinaryScaleFactor invert() {
			return Cache.POS_POWERS[-powerOf2];
		}
	}

	private static class Cache {
		private static final BinaryScaleFactor[] POS_POWERS;
		private static final BinaryScaleFactor[] NEG_POWERS;

		static {
			BinaryScaleFactor[] posPowers = new BinaryScaleFactor[64];
			BinaryScaleFactor[] negPowers = new BinaryScaleFactor[64];

			posPowers[0] = negPowers[0] = new Unity();

			for (int i = 1; i < 64; i++) {
				posPowers[i] = new UpSmall(i);
				negPowers[i] = new DownSmall(-i);
			}
			POS_POWERS = posPowers;
			NEG_POWERS = negPowers;
		}
	}

	public static BinaryScaleFactor get(int powerOf2) {
		if (powerOf2 >= 0) {
			if (powerOf2 < Cache.POS_POWERS.length) {
				return Cache.POS_POWERS[powerOf2];
			}
		} else {
			// NOTE: Think of Integer.MIN_VALUE.
			if (powerOf2 > -Cache.NEG_POWERS.length) {
				return Cache.NEG_POWERS[-powerOf2];
			}
		}
		return new BinaryScaleFactor(powerOf2);
	}

	public static BinaryScaleFactor getFloor2Factor(long value) {
		return get(BinaryPrefix.getFloorLog2(value));
	}

	public static BinaryScaleFactor getFloor1024Factor(double value) {
		return get(BinaryPrefix.getFloorLog1024(value) * 10);
	}

	private BinaryScaleFactor(int powerOf2) {
		this.powerOf2 = powerOf2;
	}

	@Override
	public double getMultiplier() {
		return Math.scalb(1.0, powerOf2);
	}

	public StringBuilder asExponentialStringBuilder(boolean multiplicationSign) {
		StringBuilder out = new StringBuilder(multiplicationSign ? "\u00d72" : "2"); //$NON-NLS-1$ //$NON-NLS-2$
		DecimalScaleFactor.appendExponentTo(powerOf2, out);
		return out;
	}

	@Override
	public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
		// NOTE: Overridden when -64 < powerOf2 < 64
		return (powerOf2 >= 0) && (srcNumericalValue != 0);
	}

	@Override
	public boolean targetOutOfRange(double srcNumericalValue, long maxAbsValue) {
		if (srcNumericalValue >= 0) {
			/*
			 * NOTE: A little nasty trick to handle when maxAbsValue = Long.MAX_VALUE
			 *
			 * Not sure if it is entirely correct in all cases.
			 */
			return srcNumericalValue >= -Math.scalb((double) ~maxAbsValue, -powerOf2);
		} else {
			return srcNumericalValue < Math.scalb((double) ~maxAbsValue, -powerOf2);
		}
	}

	@Override
	public long targetValue(long srcNumericalValue) {
		// NOTE: Overridden when -64 < powerOf2 < 64
		if ((powerOf2 >= 0) && (srcNumericalValue != 0)) {
			return (srcNumericalValue >= 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
		}
		return 0;
	}

	@Override
	public long targetFloor(long srcNumericalValue) {
		// NOTE: Overridden when -64 < powerOf2 < 64
		if (powerOf2 >= 0) {
			if (srcNumericalValue == 0) {
				return 0;
			}
			return (srcNumericalValue >= 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
		}
		return (srcNumericalValue >= 0) ? 0 : -1;
	}

	@Override
	public double targetValue(double srcNumericalValue) {
		return Math.scalb(srcNumericalValue, powerOf2);
	}

	@Override
	public boolean isUnity() {
		return (powerOf2 == 0);
	}

	@Override
	public boolean isInteger() {
		return (powerOf2 >= 0);
	}

	@Override
	public ScaleFactor concat(ScaleFactor innerFactor) {
		if (innerFactor.isUnity()) {
			return this;
		}
		if (innerFactor instanceof BinaryScaleFactor) {
			return get(powerOf2 + ((BinaryScaleFactor) innerFactor).powerOf2);
		}
		return new ImpreciseScaleFactor(innerFactor.targetValue(getMultiplier()));
	}

	@Override
	public BinaryScaleFactor invert() {
		return get(-powerOf2);
	}

	@Override
	public int compareTo(ScaleFactor other) {
		if (other instanceof BinaryScaleFactor) {
			return powerOf2 - ((BinaryScaleFactor) other).powerOf2;
		}
		return super.compareTo(other);
	}

	@Override
	public boolean equals(Object other) {
		if (powerOf2 == 0) {
			return (other instanceof ScaleFactor) && ((ScaleFactor) other).isUnity();
		}
		return (other instanceof BinaryScaleFactor) && (((BinaryScaleFactor) other).powerOf2 == powerOf2);
	}

	@Override
	public int hashCode() {
		// NOTE: Need to ensure that if isUnity() is true, the hash code is 0.
		return powerOf2;
	}
}
