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

@SuppressWarnings("nls")
public class DecimalScaleFactor extends ScaleFactor {
	final int powerOf10;

	private final static char SUPERSCRIPT_MINUS = '\u207B';
	private final static char[] SUPERSCRIPT_DIGITS = "\u2070\u00B9\u00B2\u00B3\u2074\u2075\u2076\u2077\u2078\u2079"
			.toCharArray();

	public static void appendExponentTo(int exp, StringBuilder out) {
		if (exp == 0) {
			out.append(SUPERSCRIPT_DIGITS[0]);
			return;
		}
		if (exp < 0) {
			out.append(SUPERSCRIPT_MINUS);
			exp = -exp;
		}
		if (exp < 10) {
			out.append(SUPERSCRIPT_DIGITS[exp]);
		} else if (exp < 100) {
			// A good compiler realizes that one instruction gives both the quotient and remainder.
			out.append(SUPERSCRIPT_DIGITS[exp / 10]);
			out.append(SUPERSCRIPT_DIGITS[exp % 10]);
		} else {
			// Uncommon case, just be lazy.
			char[] orgDigits = Integer.toString(exp).toCharArray();
			for (char orgDigit : orgDigits) {
				out.append(SUPERSCRIPT_DIGITS[orgDigit - '0']);
			}
		}
	}

	private static class Unity extends DecimalScaleFactor {
		public Unity() {
			super(0);
		}

		@Override
		public ScaleFactor concat(ScaleFactor innerFactor) {
			return innerFactor;
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
		public DecimalScaleFactor invert() {
			return this;
		}
	}

	private static class UpSmall extends DecimalScaleFactor {
		// Transient just to silence SpotBugs.
		private final transient long longMultiplier;

		public UpSmall(int powerOf10, long multiplier) {
			super(powerOf10);
			assert powerOf10 > 0;
			assert powerOf10 < Math.log10(Long.MAX_VALUE);
			assert Math.pow(10, powerOf10) == multiplier;
			longMultiplier = multiplier;
		}

		@Override
		public ScaleFactor concat(ScaleFactor innerFactor) {
			if (innerFactor.isUnity()) {
				return this;
			}
			if (innerFactor instanceof DecimalScaleFactor) {
				return get(powerOf10 + ((DecimalScaleFactor) innerFactor).powerOf10);
			}
			Number multiplier = innerFactor.targetNumber(longMultiplier);
			if (multiplier instanceof Long) {
				return new LongScaleFactor(multiplier.longValue());
			}
			return new ImpreciseScaleFactor(multiplier);
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
	}

	private static class DownSmall extends DecimalScaleFactor {
		// Transient just to silence SpotBugs.
		private final transient long longDivisor;

		public DownSmall(int powerOf10, long divisor) {
			super(powerOf10);
			assert powerOf10 < 0;
			assert powerOf10 > -Math.log10(Long.MAX_VALUE);
			assert Math.pow(10, -powerOf10) == divisor;
			longDivisor = divisor;
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
			// Round by adding half of the divisor and then floor divide (by shifting).
			// Need to floor divide down first, almost all the way, before adding the half
			// in order to avoid overflow.
			if (srcNumericalValue >= 0) {
				return ((srcNumericalValue / (longDivisor / 2)) + 1) >> 1;
			} else {
				return (~(~srcNumericalValue / (longDivisor / 2)) + 1) >> 1;
			}
		}

		@Override
		public long targetFloor(long srcNumericalValue) {
			if (srcNumericalValue >= 0) {
				return srcNumericalValue / longDivisor;
			} else {
				// Trick to do floor divide although Java uses symmetric divide.
				return ~(~srcNumericalValue / longDivisor);
			}
		}
	}

	private static class Cache {
		private static final DecimalScaleFactor[] POS_POWERS;
		private static final DecimalScaleFactor[] NEG_POWERS;

		static {
			int maxLongPower = (int) Math.floor(Math.log10(Long.MAX_VALUE));
			DecimalScaleFactor[] posPowers = new DecimalScaleFactor[maxLongPower + 1];
			DecimalScaleFactor[] negPowers = new DecimalScaleFactor[maxLongPower + 1];

			posPowers[0] = negPowers[0] = new Unity();

			long multiplier = 1;
			for (int i = 1; i <= maxLongPower; i++) {
				multiplier *= 10;
				posPowers[i] = new UpSmall(i, multiplier);
				negPowers[i] = new DownSmall(-i, multiplier);
			}
			POS_POWERS = posPowers;
			NEG_POWERS = negPowers;
		}
	}

	public static DecimalScaleFactor get(int powerOf10) {
		if (powerOf10 >= 0) {
			if (powerOf10 < Cache.POS_POWERS.length) {
				return Cache.POS_POWERS[powerOf10];
			}
		} else {
			// NOTE: Think of Integer.MIN_VALUE.
			if (powerOf10 > -Cache.NEG_POWERS.length) {
				return Cache.NEG_POWERS[-powerOf10];
			}
		}
		return new DecimalScaleFactor(powerOf10);
	}

	public static DecimalScaleFactor getSciFloorFactor(double value) {
		return get(DecimalPrefix.getFloorLog10(value));
	}

	public static DecimalScaleFactor getEngFloorFactor(double value) {
		return get(DecimalPrefix.getFloorLog1000(value) * 3);
	}

	public DecimalScaleFactor(int powerOf10) {
		this.powerOf10 = powerOf10;
	}

	@Override
	public double getMultiplier() {
		return Math.pow(10.0, powerOf10);
	}

	public StringBuilder asExponentialStringBuilder(boolean multiplicationSign) {
		StringBuilder out = new StringBuilder(multiplicationSign ? "\u00d710" : "10");
		DecimalScaleFactor.appendExponentTo(powerOf10, out);
		return out;
	}

	@Override
	public boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue) {
		// NOTE: Overridden when the multiplier or divisor fits in a long.
		if (powerOf10 >= 0) {
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
		if (powerOf10 >= 0) {
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
	public long targetValue(long srcNumericalValue) {
		// NOTE: Overridden when the multiplier or divisor fits in a long.
		return Math.round(srcNumericalValue * getMultiplier());
	}

	@Override
	public long targetFloor(long srcNumericalValue) {
		// NOTE: Overridden when the multiplier or divisor fits in a long.
		return (long) Math.floor(srcNumericalValue * getMultiplier());
	}

	@Override
	public double targetValue(double srcNumericalValue) {
		return powerOf10 < 0 ? srcNumericalValue / Math.pow(10.0, -powerOf10) : srcNumericalValue * getMultiplier();
	}

	@Override
	public boolean isUnity() {
		return (powerOf10 == 0);
	}

	@Override
	public boolean isInteger() {
		return (powerOf10 >= 0);
	}

	@Override
	public ScaleFactor concat(ScaleFactor innerFactor) {
		if (innerFactor.isUnity()) {
			return this;
		}
		if (innerFactor instanceof DecimalScaleFactor) {
			return get(powerOf10 + ((DecimalScaleFactor) innerFactor).powerOf10);
		}
		return new ImpreciseScaleFactor(innerFactor.targetValue(getMultiplier()));
	}

	@Override
	public DecimalScaleFactor invert() {
		return get(-powerOf10);
	}

	@Override
	public int compareTo(ScaleFactor other) {
		if (other instanceof DecimalScaleFactor) {
			return powerOf10 - ((DecimalScaleFactor) other).powerOf10;
		}
		return super.compareTo(other);
	}

	@Override
	public boolean equals(Object other) {
		if (powerOf10 == 0) {
			return (other instanceof ScaleFactor) && ((ScaleFactor) other).isUnity();
		}
		return (other instanceof DecimalScaleFactor) && (((DecimalScaleFactor) other).powerOf10 == powerOf10);
	}

	@Override
	public int hashCode() {
		// NOTE: Need to ensure that if isUnity() is true, the hash code is 0.
		return powerOf10;
	}
}
