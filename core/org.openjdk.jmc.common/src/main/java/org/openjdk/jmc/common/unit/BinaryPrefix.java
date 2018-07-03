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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmc.common.messages.internal.Messages;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity.LinearUnitSelector;

/**
 * Binary prefixes, as standardized by IEC. The corresponding SI prefixes are also provided to
 * reflect common usage. (JEDEC defines these to be used for powers of 1024 as well, except that
 * they too capitalize "k".) Extend functionality as needed, perhaps like {@link TimeUnit}.
 */
// These prefixes are defined to be language independent.
@SuppressWarnings("nls")
public enum BinaryPrefix implements IPrefix<BinaryPrefix> {
	NOBI,
	KIBI(1, 'k'),
	MEBI(2, 'M'),
	GIBI(3, 'G'),
	TEBI(4, 'T'),
	PEBI(5, 'P'),
	EXBI(6, 'E'),
	ZEBI(7, 'Z'),
	YOBI(8, 'Y');

	private static BinaryPrefix[] PREFIXES;
	static {
		PREFIXES = values();
	}

	private BinaryPrefix() {
		assert ordinal() == 0 : "Implementation error in enum: (ordinal = " + ordinal() + ") != 0";
		shift = 0;
		scaleFactor = BinaryScaleFactor.get(0);
		prefixSI = DecimalPrefix.NONE;
		symbolSI = "";
		symbolJEDEC = "";
		symbolIEC = "";
		englishName = "";
		localizedName = "";
	}

	private BinaryPrefix(int powerOf1024, char charSI) {
		assert powerOf1024 == ordinal() : "Implementation error in enum: (ordinal = " + ordinal()
				+ ") != (powerOf1024 = " + powerOf1024 + ')';
		shift = powerOf1024 * 10;
		scaleFactor = BinaryScaleFactor.get(shift);

		symbolSI = String.valueOf(charSI);
		prefixSI = DecimalPrefix.getPrefix(symbolSI);
		assert prefixSI != null;
		// Start with upper case according to JEDEC/IEC.
		symbolJEDEC = symbolSI.toUpperCase(Locale.ENGLISH);
		symbolIEC = symbolJEDEC + 'i';
		englishName = name().toLowerCase(Locale.ENGLISH);
		localizedName = Messages.getString("Prefix_" + symbolIEC + "_name", englishName);
	}

	private final int shift;
	private final BinaryScaleFactor scaleFactor;
	/**
	 * Temporary reference to the corresponding decimal prefix to get the full name, only to be used
	 * during the transition phase where JMC hasn't been completely converted to use quantities. Do
	 * not rely on this.
	 *
	 * @deprecated Accept that kilobytes and kibibytes are different units.
	 */
	@Deprecated
	public final DecimalPrefix prefixSI;
	public final String symbolSI;
	public final String symbolJEDEC;
	public final String symbolIEC;
	public final String englishName;
	public final String localizedName;

	public int shift() {
		return shift;
	}

	public int log1024() {
		// This is ensured by an assertion in the constructor.
		return ordinal();
	}

	public long convertTo(long value) {
		return (shift > 63) ? 0 : (value >> shift);
	}

	public float convertTo(float value) {
		return Math.scalb(value, -shift);
	}

	public double convertTo(double value) {
		return Math.scalb(value, -shift);
	}

	/**
	 * The base 2 logarithm. Note that for the value 0, 0 is returned in order to be consistent with
	 * {@link Math#floor(double)}.
	 *
	 * @return the floor of the base 2 logarithm of the absolute value of {@code value}
	 */
	public static int getFloorLog2(long value) {
		if (value == 0) {
			return 0;
		}
		return 63 - Long.numberOfLeadingZeros(Math.abs(value));
	}

	/**
	 * The base 1024 logarithm. Note that for the value 0, 0 is returned in order to be consistent
	 * with {@link Math#floor(double)}.
	 *
	 * @return the floor of the base 1024 logarithm of the absolute value of {@code value}
	 */
	public static int getFloorLog1024(long value) {
		/*
		 * NOTE: Java truncates towards zero, so -1/10 => 0. This means that we don't need to
		 * special treat 0 in order to be consistent with Math.floor().
		 */
		return (63 - Long.numberOfLeadingZeros(Math.abs(value))) / 10;
	}

	public static BinaryPrefix getFloorPrefix(long value) {
		return PREFIXES[getFloorLog1024(value)];
	}

	/**
	 * The base 2 logarithm of the binary alignment of {@code value}. Note that for the value 0, the
	 * return value is undefined.
	 *
	 * @return the base 2 logarithm of the alignment of {@code value}
	 */
	public static int getAlignmentLog2(long value) {
		return Long.numberOfTrailingZeros(value);
	}

	/**
	 * The base 1024 logarithm of the binary alignment of {@code value}. Note that for the value 0,
	 * the return value is undefined.
	 *
	 * @return the base 1024 logarithm of the alignment of {@code value}
	 */
	public static int getAlignmentLog1024(long value) {
		return getAlignmentLog2(value) / 10;
	}

	/**
	 * The base 2 logarithm. Note that for the value 0, 0 is returned, in order to be consistent
	 * with {@link Math#floor(double)}.
	 *
	 * @return the floor of the base 2 logarithm of the absolute value of {@code value}
	 */
	public static int getFloorLog2(double value) {
		if (value == 0.0) {
			return 0;
		}
		return Math.getExponent(value);
	}

	private static int floorDiv10(int value) {
		// FIXME: Replace with Math.floorDiv() when JDK 8 is allowed for this code.
		if (value < 0) {
			return ~(~value / 10);
		}
		return value / 10;
	}

	/**
	 * The base 1024 logarithm. Note that for the value 0, 0 is returned in order to be consistent
	 * with {@link Math#floor(double)}.
	 *
	 * @return the floor of the base 1024 logarithm of the absolute value of {@code value}
	 */
	public static int getFloorLog1024(double value) {
		return floorDiv10(getFloorLog2(value));
	}

	public static BinaryPrefix getFloorPrefix(double value) {
		int log1024 = getFloorLog1024(value);
		return PREFIXES[Math.max(0, Math.min(log1024, PREFIXES.length - 1))];
	}

	/**
	 * The base 2 logarithm of the binary alignment of {@code value}. Note that for zero, infinity,
	 * NaN, and denormalized values, the return value is currently undefined.
	 *
	 * @return the base 2 logarithm of the alignment of {@code value}
	 */
	public static int getAlignmentLog2(double value) {
		// FIXME: Handle double corner cases like NaN, infinity and denormalized numbers.
		long bits = Double.doubleToRawLongBits(value);
		long mantissa = (bits & ((1L << 52) - 1)) + (1L << 52);
		return Long.numberOfTrailingZeros(mantissa) - 52 + Math.getExponent(value);
	}

	/**
	 * The base 1024 logarithm of the binary alignment of {@code value}. Note that for zero,
	 * infinity, NaN, and denormalized values, the return value is currently undefined.
	 *
	 * @return the base 1024 logarithm of the alignment of {@code value}
	 */
	public static int getAlignmentLog1024(double value) {
		return floorDiv10(getAlignmentLog2(value));
	}

	@Override
	public String identifier() {
		return symbolIEC;
	}

	@Override
	public String symbol() {
		return symbolIEC;
	}

	@Override
	public String altSymbol() {
		/*
		 * FIXME: When quantities are used consistently all over JMC, change this to null?
		 *
		 * Basically, depends on if this is the place to decide whether MiB (2^20 B) and MB (10^6 B)
		 * could both be allowed for a given kind of quantity. It probably isn't.
		 */
		return symbolJEDEC;
	}

	@Override
	public String localizedName() {
		return localizedName;
	}

	@Override
	public StringBuilder asExponentialStringBuilder(boolean multiplicationSign) {
		return scaleFactor.asExponentialStringBuilder(multiplicationSign);
	}

	@Override
	public BinaryScaleFactor scaleFactor() {
		return scaleFactor;
	}

	@Override
	public BinaryScaleFactor valueFactorTo(BinaryPrefix targetPrefix) {
		return BinaryScaleFactor.get(shift - targetPrefix.shift);
	}

	@Override
	public LinearUnitSelector createUnitSelector(LinearKindOfQuantity kindOfQuantity, Iterable<BinaryPrefix> prefixes) {
		return new BinaryUnitSelector(kindOfQuantity, prefixes);
	}
}
