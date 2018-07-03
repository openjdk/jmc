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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmc.common.messages.internal.Messages;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity.LinearUnitSelector;

/**
 * Decimal prefixes, as standardized by SI. Extend functionality as needed, perhaps like
 * {@link TimeUnit}.
 *
 * @see BinaryPrefix
 */
// Localization is delegated to the Messages class. Warnings here would still not catch the cases that matter.
@SuppressWarnings("nls")
public enum DecimalPrefix implements IPrefix<DecimalPrefix> {
	YOCTO(-24, 'y'),
	ZEPTO(-21, 'z'),
	ATTO(-18, 'a'),
	FEMTO(-15, 'f'),
	PICO(-12, 'p'),
	NANO(-9, 'n'),
	MICRO(-6, "\u03bc", "u"), // \u03bc is the micron (my)
	MILLI(-3, 'm'),
	CENTI(-2, 'c'),
	DECI(-1, 'd'),
	NONE(0, ""), // Really include?
	DECA(1, "da"), // Bastard, not single char, ignore for now.
	HECTO(2, 'h'),
	KILO(3, 'k'), // Shouldn't allow capital 'K', but not everyone knows that.
	MEGA(6, 'M'),
	GIGA(9, 'G'),
	TERA(12, 'T'),
	PETA(15, 'P'),
	EXA(18, 'E'),
	ZETTA(21, 'Z'),
	YOTTA(24, 'Y');

	private DecimalPrefix(int powerOf10, char prefixChar) {
		this(powerOf10, "" + prefixChar, null);
	}

	private DecimalPrefix(int powerOf10, String prefix) {
		this(powerOf10, prefix, null);
	}

	private DecimalPrefix(int powerOf10, String prefix, String altPrefix) {
		powerOfTen = powerOf10;
		symbol = prefix;
		altSymbol = altPrefix;
		englishName = name().toLowerCase(Locale.ENGLISH);
		// NOTE: Using identifier() to choose between symbol and altSymbol, so these must be initialized.
		localizedName = (prefix.length() == 0) ? ""
				: Messages.getString("Prefix_" + identifier() + "_name", englishName);

		scaleFactor = DecimalScaleFactor.get(powerOf10);
		doubleMult = StrictMath.pow(10, powerOf10);
	}

	private final static DecimalPrefix[] THOUSANDS = {YOCTO, ZEPTO, ATTO, FEMTO, PICO, NANO, MICRO, MILLI, NONE, KILO,
			MEGA, GIGA, TERA, EXA, PETA, ZETTA, YOTTA};

	private final static Map<String, DecimalPrefix> PREFIX_BY_SYMBOL;

	static {
		Map<String, DecimalPrefix> symbolMap = new HashMap<>();
		for (DecimalPrefix prefix : values()) {
			DecimalPrefix old = symbolMap.put(prefix.symbol, prefix);
			assert old == null;
			if (prefix.altSymbol != null) {
				old = symbolMap.put(prefix.altSymbol, prefix);
				assert old == null;
			}
		}
		PREFIX_BY_SYMBOL = symbolMap;
	}

	private final int powerOfTen;
	private final DecimalScaleFactor scaleFactor;
	private final double doubleMult;
	private final String symbol;
	private final String altSymbol;
	private final String englishName;
	private transient String localizedName;

	public static DecimalPrefix getPrefix(String symbol) {
		return PREFIX_BY_SYMBOL.get(symbol);
	}

	public static int getFloorLog10(double value) {
		return (value == 0.0) ? 0 : (int) Math.floor(Math.log10(Math.abs(value)));
	}

	public static int getFloorLog1000(double value) {
		return (value == 0.0) ? 0 : (int) Math.floor(Math.log10(Math.abs(value)) / 3.0);
	}

	public static DecimalPrefix getEngFloorPrefix(double value) {
		int idx = Math.max(0, getFloorLog1000(value) - (YOCTO.powerOfTen / 3));
		return THOUSANDS[Math.min(idx, THOUSANDS.length - 1)];
	}

	public int powerOf10() {
		return powerOfTen;
	}

	@Override
	public DecimalScaleFactor scaleFactor() {
		return scaleFactor;
	}

	@Override
	public DecimalScaleFactor valueFactorTo(DecimalPrefix targetPrefix) {
		return DecimalScaleFactor.get(powerOfTen - targetPrefix.powerOfTen);
	}

	public double doubleMult() {
		return doubleMult;
	}

	@Override
	public final String symbol() {
		return symbol;
	}

	/**
	 * Intended to be used for parsing GUI strings where entering the micron character may be
	 * cumbersome.
	 *
	 * @return alternative symbol, or {@code null} if no other representation is available
	 */
	@Override
	public final String altSymbol() {
		return altSymbol;
	}

	@Override
	public final String identifier() {
		// The only altSymbol we have is "u" for micro, which is easier to type and persist, so use it.
		return (altSymbol != null) ? altSymbol : symbol;
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
	public String toString() {
		return englishName;
	}

	@Override
	public LinearUnitSelector createUnitSelector(
		LinearKindOfQuantity kindOfQuantity, Iterable<DecimalPrefix> prefixes) {
		return new DecimalUnitSelector(kindOfQuantity, prefixes);
	}
}
