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

import static org.openjdk.jmc.common.unit.DecimalPrefix.NONE;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.ScalarQuantity.LongStored;
import org.openjdk.jmc.common.unit.TypedUnit.UnitSelector;
import org.openjdk.jmc.common.util.FormatThreadLocal;

/**
 * The kind of a linear (scalar) physical quantity. Closely related to the dimension of a quantity,
 * but we separate them further to be able to use units specific to certain kinds of quantities. In
 * particular, Hertz for (clock) frequency. We are mainly interested in the commensurability of
 * quantities, to display them in the same graph. So when the dimension suffices we do not need to
 * introduce additional kinds of quantities. (Although we should strive to follow
 * <a href="http://www.bipm.org/en/publications/guides/vim.html">VIM</a>.)
 * <p>
 * Each kind of quantity has a single "atom unit" which may be used with prefixes. Where applicable,
 * this unit is the SI base unit with any prefix stripped (that is, "kg" is the base unit, but the
 * atom unit is "g"). According to BIPM/VIM above, units that are not formed by a prefix and the
 * atom unit may be permitted, but are treated as off-system units. This means that they cannot have
 * prefixes. (Also, SI only permits a specified set of off-system units to be used with SI, in order
 * to avoid ambiguity. For instance, since there's a unit "Pa", for Pascal, and prefixes "P" for
 * Peta and "E" for Exa, no in-system unit may be designated by "a", and no off-system unit may be
 * designated with "EPa".) To avoid future problems, where we might persist a unit without content
 * type, we should stick to this.
 * <p>
 * Note that there's a relevant specification for representing units, SI as well as conventional,
 * without ambiguity, in ASCII, with or without case sensitivity, see
 * <a href="http://unitsofmeasure.org/trac/">UCUM</a>. We should strive for "limited conformance"
 * with this specification. That is, it will hardly be worth the trouble of parsing every possible
 * UCUM expression. (There's an Eclipse project for that, UOMo, but it has too much dependencies.)
 * But what we produce when persisting quantities and units should be valid UCUM expressions. That
 * will allow us to export data into a standardized format. And also to switch to a full UCUM
 * implementation, should a suitable one become available.
 */
public class LinearKindOfQuantity extends KindOfQuantity<LinearUnit> {
	// Holder of interactive number formatter/parser, where no spaces are non-breaking.
	private static final FormatThreadLocal<NumberFormat> NUMBER_FORMAT_INTERACTIVE_HOLDER;
	// Holder of display number formatter/parser, which may contain non-breaking spaces.
	private static final FormatThreadLocal<NumberFormat> NUMBER_FORMAT_DISPLAY_HOLDER;

	protected final LinearUnit atomUnit;
	// Since units no longer have an explicit name, only a description, use this to generate prefixed names.
	protected final String atomUnitName;
	// FIXME: Should not use a SortedSet like TreeSet. It will prevent alias units.
	protected final Collection<LinearUnit> commonUnits = new TreeSet<>();
	protected final Collection<LinearUnit> allUnits = new TreeSet<>();
	protected final Map<String, LinearUnit> interactiveSymbolToUnitMap = new HashMap<>();
	protected LinearUnitSelector unitSelector;
	protected final Map<Object, LinearUnit> unitCache = new HashMap<>();

	static {
		NumberFormat formatter = NumberFormat.getNumberInstance();
		/*
		 * FIXME: Find out what this really should be. Might unfortunately be best to do manual
		 * conversion from Double.toString(), perhaps using parts of java.text.DigitList. We must
		 * ensure an exact representation, but would prefer scientific notation over hundreds of
		 * fraction digits.
		 */
		formatter.setMaximumFractionDigits(340);
		NUMBER_FORMAT_DISPLAY_HOLDER = new FormatThreadLocal<>(formatter);
		if (formatter instanceof DecimalFormat) {
			DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
			if (symbols.getGroupingSeparator() == '\u00a0') {
				symbols.setGroupingSeparator(' ');
			}
			((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);
		}
		NUMBER_FORMAT_INTERACTIVE_HOLDER = new FormatThreadLocal<>(formatter);
	}

	/**
	 * Get a thread local configured {@link NumberFormat} suitable for display or interactive
	 * formatting, or interactive parsing. The returned formatter mustn't be modified except as by
	 * the {@link NumberFormat#parse(String)} and {@link NumberFormat#format(Object)} family of
	 * methods.
	 *
	 * @param interactive
	 *            true to format/parse for interactive use, false otherwise
	 * @return a thread local configured {@link NumberFormat}
	 */
	public static NumberFormat getNumberFormat(boolean interactive) {
		return interactive ? NUMBER_FORMAT_INTERACTIVE_HOLDER.get() : NUMBER_FORMAT_DISPLAY_HOLDER.get();
	}

	public static interface LinearUnitSelector extends UnitSelector<LinearUnit> {
		/*
		 * FIXME: Specify "best", so that for example a lower value is preferred, and whether the
		 * upper or implicit lower limit is exclusive, to make the result well defined.
		 */
		ITypedQuantity<LinearUnit> snapToBestBetweenHalfAndEqual(ITypedQuantity<LinearUnit> upperLimit);
	}

	public static class AutoFormatter extends DisplayFormatter<IQuantity> {
		protected final double minNumericalValue;
		protected final double maxNumericalValue;
		protected final int nominalValueDigits;

		protected AutoFormatter(LinearKindOfQuantity kindOfQuantity, String name) {
			this(kindOfQuantity, IDisplayable.AUTO, name, 1.0, 1000, 3);
		}

		protected AutoFormatter(LinearKindOfQuantity kindOfQuantity, String name, double minNumericalValue,
				double maxNumericalValue) {
			this(kindOfQuantity, IDisplayable.AUTO, name, minNumericalValue, maxNumericalValue, 3);
		}

		protected AutoFormatter(LinearKindOfQuantity kindOfQuantity, String id, String name, double minNumericalValue,
				double maxNumericalValue, int nominalValueDigits) {
			super(kindOfQuantity, id, name);
			this.minNumericalValue = minNumericalValue;
			this.maxNumericalValue = maxNumericalValue;
			this.nominalValueDigits = nominalValueDigits;
		}

		@Override
		public LinearKindOfQuantity getContentType() {
			return (LinearKindOfQuantity) super.getContentType();
		}

		@Override
		public String format(IQuantity quantity) {
			LinearUnit preferredUnit = getContentType().getPreferredUnit(quantity, minNumericalValue,
					maxNumericalValue);
			return formatInUnit(quantity, preferredUnit, nominalValueDigits);
		}

		public static String formatInUnit(IQuantity quantity, LinearUnit customUnit, int nominalValueDigits) {
			return formatWithUnit(quantity.numberValueIn(customUnit), customUnit, nominalValueDigits);
		}

		protected static String formatWithUnit(Number numValue, LinearUnit customUnit, int nominalValueDigits) {
			NumberFormat formatter = NumberFormat.getNumberInstance();
			int intDigits = DecimalPrefix.getFloorLog10(numValue.doubleValue()) + 1;
			formatter.setMaximumFractionDigits(nominalValueDigits - intDigits);
			return formatter.format(numValue) + customUnit.getAppendableSuffix(false);
		}

		protected static String formatWithFixedFraction(Number numValue, LinearUnit customUnit, int numFractionDigits) {
			NumberFormat formatter = NumberFormat.getNumberInstance();
			formatter.setMinimumFractionDigits(numFractionDigits);
			formatter.setMaximumFractionDigits(numFractionDigits);
			return formatter.format(numValue) + customUnit.getAppendableSuffix(false);
		}
	}

	public static class DualUnitFormatter extends DisplayFormatter<IQuantity> {
		/*
		 * Related to (the mantissa of) Double.MIN_VALUE but with safety factor above 1000.
		 * 
		 * This value is equal to 0x0.0000000001P0 but since Fortify can't handle hexadecimal
		 * doubles we use longBitsToDouble instead.
		 */
		private static final double PRECISION_LIMIT = Double.longBitsToDouble(0x3d70000000000000L);
		private final TypedUnit.UnitSelector<LinearUnit> unitSelector;

		// This is the smallest unit at which to still show two units.
		private final LinearUnit cutoffUnit;

		protected DualUnitFormatter(LinearKindOfQuantity kindOfQuantity, String id, String name) {
			this(kindOfQuantity, id, name, null, null);
		}

		protected DualUnitFormatter(LinearKindOfQuantity kindOfQuantity, String id, String name,
				LinearUnit cutoffUnit) {
			this(kindOfQuantity, id, name, null, cutoffUnit);
		}

		protected DualUnitFormatter(LinearKindOfQuantity kindOfQuantity, String id, String name,
				TypedUnit.UnitSelector<LinearUnit> unitSelector) {
			this(kindOfQuantity, id, name, unitSelector, null);
		}

		protected DualUnitFormatter(LinearKindOfQuantity kindOfQuantity, String id, String name,
				TypedUnit.UnitSelector<LinearUnit> unitSelector, LinearUnit cutoffUnit) {
			super(kindOfQuantity, id, name);
			this.unitSelector = unitSelector;
			this.cutoffUnit = cutoffUnit;
		}

		private TypedUnit.UnitSelector<LinearUnit> getSelector() {
			return (unitSelector != null) ? unitSelector : ((LinearKindOfQuantity) getContentType()).unitSelector;
		}

		@Override
		public String format(IQuantity quantity) {
			@SuppressWarnings("unchecked")
			ITypedQuantity<LinearUnit> typedQuantity = (ITypedQuantity<LinearUnit>) quantity;
			TypedUnit.UnitSelector<LinearUnit> selector = getSelector();
			LinearUnit bigUnit = selector.getPreferredUnit(typedQuantity, 1, 1000);
			if (bigUnit.getIdentifier() == null) {
				// Temporary unit (with x10^23 or so): Only use that one.
				return AutoFormatter.formatInUnit(quantity, bigUnit, 3);
			}
			double valueInBigs = quantity.doubleValueIn(bigUnit);
			// Truncate towards zero.
			double bigs = (valueInBigs < 0) ? Math.ceil(valueInBigs) : Math.floor(valueInBigs);
			double absRest = Math.abs(valueInBigs - bigs);
			// Check with respect to double precision (and some margin) so we don't show "1 year 1 ns".
			if ((absRest < PRECISION_LIMIT) || (absRest > (1.0 - PRECISION_LIMIT))) {
				return AutoFormatter.formatWithUnit(valueInBigs, bigUnit, 3);
			}
			ITypedQuantity<LinearUnit> restQuantity = bigUnit.quantity(absRest);
			LinearUnit smallUnit = selector.getPreferredUnit(restQuantity, 1, 1000);
			if (cutoffUnit != null && smallUnit.compareTo(cutoffUnit) < 0) {
				return AutoFormatter.formatWithUnit(valueInBigs, bigUnit, 0);
			}
			if ((smallUnit.getIdentifier() == null) || smallUnit.equals(bigUnit)) {
				// Temporary unit (with x10^23 or so) or same as big(!): Use the big one.
				return AutoFormatter.formatWithUnit(valueInBigs, bigUnit, 3);
			}
			ScaleFactor scale = bigUnit.valueTransformTo(smallUnit);
			if (scale instanceof DecimalScaleFactor) {
				// Power of ten relation. Just use the decimal number with the big unit.
				return AutoFormatter.formatWithFixedFraction(valueInBigs, bigUnit, 3);
			}
			double smalls = Math.round(restQuantity.doubleValueIn(smallUnit));
			// Check if this rounded value pushes the total quantity above another big unit. (Otherwise, 13.5 d => 1 wk 7 d.)
			if (smallUnit.valueTransformTo(bigUnit).targetValue(smalls) >= 1) {
				return AutoFormatter.formatWithUnit(bigs + Math.signum(bigs), bigUnit, 3);
			}
			return AutoFormatter.formatWithUnit(bigs, bigUnit, 3) + '\u00a0'
					+ AutoFormatter.formatWithUnit(smalls, smallUnit, 3);
		}
	}

	/**
	 * Create a new linear kind of quantity, with content type identifier {@code id} and sole unit
	 * identifier {@code atomUnitId}.
	 */
	public LinearKindOfQuantity(String id, String atomUnitId) {
		this(id, atomUnitId, EnumSet.of(NONE));
	}

	/**
	 * Create a new linear kind of quantity, with content type identifier {@code id} and atom unit
	 * identifier {@code atomUnitId}, using prefixes in the range {@code minPrefix} to
	 * {@code maxPrefix}.
	 */
	public <P extends Enum<P> & IPrefix<P>> LinearKindOfQuantity(String id, String atomUnitId, P minPrefix,
			P maxPrefix) {
		this(id, atomUnitId, EnumSet.range(minPrefix, maxPrefix));
	}

	/**
	 * Create a new linear kind of quantity, with content type identifier {@code id} and atom unit
	 * identifier {@code atomUnitId}, using the prefixes in {@code prefixes}.
	 */
	<P extends IPrefix<P>> LinearKindOfQuantity(String id, String atomUnitId, Collection<P> prefixes) {
		this(id, atomUnitId, prefixes, prefixes);
	}

	/**
	 * Create a new linear kind of quantity, with content type identifier {@code id} and atom unit
	 * identifier {@code atomUnitId}, using the prefixes in {@code allPrefixes} but only designating
	 * those in {@code commonPrefixes} as common.
	 */
	<P extends IPrefix<P>> LinearKindOfQuantity(String id, String atomUnitId, Collection<P> commonPrefixes,
			Collection<P> allPrefixes) {
		super(id);
		String unitSymbol = resolveLocalizedSymbol(NONE, atomUnitId, atomUnitId);
		atomUnitName = resolveLocalizedName(NONE, atomUnitId, null);
		String appendableUnitSymbol = (unitSymbol.length() == 0) ? "" : ("\u00a0" + unitSymbol); //$NON-NLS-1$ //$NON-NLS-2$
		if (atomUnitName != null) {
			atomUnit = new LinearUnit(this, atomUnitId, DecimalScaleFactor.get(0), unitSymbol,
					"1" + appendableUnitSymbol + " (= 1\u00a0" + atomUnitName + ')', atomUnitName); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			atomUnit = new LinearUnit(this, atomUnitId, DecimalScaleFactor.get(0), unitSymbol,
					"1" + appendableUnitSymbol); //$NON-NLS-1$
		}

		for (P prefix : allPrefixes) {
			LinearUnit unit = getUnit(prefix);
			addUnit(unit, commonPrefixes.contains(prefix));
		}

		if (allPrefixes.isEmpty()) {
			// This shouldn't really happen, but if it does, this would be the most reasonable selector to use.
			unitSelector = new DecimalUnitSelector(this, atomUnit);
		} else {
			// Get an IPrefix implementation to delegate to.
			P anyPrefix = allPrefixes.iterator().next();
			unitSelector = anyPrefix.createUnitSelector(this, allPrefixes);
		}
	}

	protected void setDefaultSelector(LinearUnitSelector unitSelector) {
		this.unitSelector = unitSelector;
	}

	@Override
	public KindOfQuantity<LinearUnit> getDeltaKind() {
		return this;
	}

	@Override
	public LinearUnit getDefaultUnit() {
		return atomUnit;
	}

	protected String resolveLocalizedName(IPrefix<?> prefix) {
		return resolveLocalizedName(prefix, atomUnit.getIdentifier(), atomUnitName);
	}

	protected String resolveLocalizedSymbol(IPrefix<?> prefix) {
		return resolveLocalizedSymbol(prefix, atomUnit.getIdentifier(), atomUnit.getLocalizedSymbol());
	}

	public LinearUnit getUnit(IPrefix<?> prefix) {
		LinearUnit unit = getCachedUnit(prefix);
		if (unit == null) {
			if (prefix.scaleFactor().isUnity()) {
				unitCache.put(prefix, atomUnit);
				return atomUnit;
			}
			String id = prefix.identifier() + atomUnit.getIdentifier();
			String symbol = resolveLocalizedSymbol(prefix);
			String name = resolveLocalizedName(prefix);
			StringBuilder descBuf = new StringBuilder("1\u00a0"); //$NON-NLS-1$
			descBuf.append(symbol);
			if (name != null) {
				descBuf.append(" (= 1\u00a0"); //$NON-NLS-1$
				descBuf.append(name);
				descBuf.append(" = "); //$NON-NLS-1$
			} else {
				descBuf.append(" (= "); //$NON-NLS-1$
			}
			descBuf.append(prefix.asExponentialStringBuilder(false));
			descBuf.append(atomUnit.getAppendableSuffix(false));
			if (prefix instanceof BinaryPrefix) {
				BinaryPrefix binPrefix = (BinaryPrefix) prefix;
				@SuppressWarnings("deprecation")
				DecimalPrefix prefixSI = binPrefix.prefixSI;
				String altSymbol = resolveLocalizedSymbol(prefixSI);
				String altName = resolveLocalizedName(prefixSI);
				descBuf.append(" \u2248 1\u00a0"); //$NON-NLS-1$
				descBuf.append(altSymbol);
				if (altName != null) {
					descBuf.append(" = 1\u00a0"); //$NON-NLS-1$
					descBuf.append(altName);
				}
				descBuf.append(" = "); //$NON-NLS-1$
				descBuf.append(prefixSI.asExponentialStringBuilder(false));
				descBuf.append(atomUnit.getAppendableSuffix(false));
				descBuf.append(')');
				if (altName != null) {
					unit = new LinearUnit(this, id, prefix.scaleFactor(), symbol, descBuf.toString(), name, altSymbol,
							altName);
				} else {
					unit = new LinearUnit(this, id, prefix.scaleFactor(), symbol, descBuf.toString(), name, altSymbol);
				}
			} else if (prefix.altSymbol() != null) {
				String altSymbol = prefix.altSymbol() + atomUnit.getLocalizedSymbol();
				descBuf.append(')');
				unit = new LinearUnit(this, id, prefix.scaleFactor(), symbol, descBuf.toString(), name, altSymbol);
			} else {
				descBuf.append(')');
				unit = new LinearUnit(this, id, prefix.scaleFactor(), symbol, descBuf.toString(), name);
			}
			unitCache.put(prefix, unit);
		}
		return unit;
	}

	@Override
	public LinearUnit getUnit(String id) {
		return super.getUnit(id);
	}

	public LinearUnit getCachedUnit(Object key) {
		return unitCache.get(key);
	}

	protected ScaleFactor getBestScaleFactorFor(long value) {
		int log2 = BinaryPrefix.getFloorLog2(value);
		if ((1 << log2) == value) {
			return BinaryScaleFactor.get(log2);
		}

		ScaleFactor factor = DecimalScaleFactor.getSciFloorFactor(value);
		long mult = (long) factor.getMultiplier();
		if (mult == value) {
			return factor;
		}
		return new LongScaleFactor(value);
	}

	/**
	 * Create an off-system unit with no default localized name.
	 *
	 * @param id
	 *            unit identifier which also is used as the unit symbol
	 */
	public LinearUnit makeUnit(String id, ITypedQuantity<LinearUnit> quantity) {
		return makeUnit(id, id, quantity, null, false);
	}

	public LinearUnit makeUnit(String id, ITypedQuantity<LinearUnit> quantity, String localizedName) {
		return makeUnit(id, id, quantity, localizedName, false);
	}

	/**
	 * Create a custom unit. That is, one that normally isn't available for parsing (persisted or
	 * interactive).
	 */
	public LinearUnit makeCustomUnit(ITypedQuantity<LinearUnit> quantity) {
		// Disallow custom units aliasing well known units.
		if ((quantity instanceof LongStored) && (quantity.longValue() == 1)) {
			return quantity.getUnit();
		}

		String id = quantity.persistableString();
		String symbol = "\u00d7" + quantity.localizedFormat(false, false); //$NON-NLS-1$
		return makeUnit(id, symbol, quantity, null, true);
	}

	/**
	 * Create an off-system unit.
	 *
	 * @param localizedSymbol
	 *            Default unit symbol. May be an empty string and may be overridden by translation.
	 * @param localizedName
	 *            Default unit name. May be {@code null} and may be overridden by translation.
	 */
	private LinearUnit makeUnit(
		String id, String localizedSymbol, ITypedQuantity<LinearUnit> quantity, String localizedName, boolean custom) {
		String symbol = resolveLocalizedSymbol(NONE, id, localizedSymbol);
		String name = resolveLocalizedName(NONE, id, localizedName);

		boolean isFactor = symbol.startsWith("\u00d7"); //$NON-NLS-1$
		StringBuilder descBuf = new StringBuilder();
		if (!isFactor) {
			if (symbol.length() == 0) {
				descBuf.append('1');
			} else {
				descBuf.append("1\u00a0"); //$NON-NLS-1$
				descBuf.append(symbol);
			}
			if (name != null) {
				descBuf.append(" (= 1\u00a0"); //$NON-NLS-1$
				descBuf.append(name);
				descBuf.append(" = "); //$NON-NLS-1$
			} else {
				descBuf.append(" (= "); //$NON-NLS-1$
			}
		}

		ScaleFactor factorToAtom;
		ScaleFactor factorToDefinition;
		String relStr;
		if (quantity instanceof LongStored) {
			descBuf.append(quantity.localizedFormat(false, false));
			relStr = " = "; //$NON-NLS-1$
			// FIXME: Throw QuantityConversionException if quantity is 0?
			factorToDefinition = getBestScaleFactorFor(quantity.longValue());
			factorToAtom = quantity.getUnit().valueTransformTo(atomUnit).concat(factorToDefinition);
		} else {
			descBuf.append(AutoFormatter.formatInUnit(quantity, quantity.getUnit(), 3));
			relStr = " \u2248 "; //$NON-NLS-1$
			factorToDefinition = new ImpreciseScaleFactor(quantity.doubleValue());
			factorToAtom = new ImpreciseScaleFactor(quantity.doubleValueIn(atomUnit));
		}

		if (!atomUnit.equals(quantity.getUnit())) {
			descBuf.append(relStr);
			descBuf.append(AutoFormatter.formatInUnit(quantity, atomUnit, 3));
		}

		if (!isFactor) {
			descBuf.append(')');
		}

		LinearUnit unit;
		String[] altNames;
		if (isFactor) {
			// NOTE: Will ignore the name even if one was resolved.
			String qStr = quantity.interactiveFormat();
			String suffix = quantity.getUnit().getAppendableSuffix(true);
			if (suffix.startsWith(" ")) { //$NON-NLS-1$
				// AltNames without space before original unit, and with "x" instead of multiplication sign.
				String trimmed = qStr.replace(suffix, quantity.getUnit().getLocalizedSymbol());
				String[] names = {"\u00d7" + qStr, "x" + qStr, "\u00d7" + trimmed, "x" + trimmed}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				altNames = names;
			} else {
				String[] names = {"\u00d7" + qStr, "x" + qStr}; //$NON-NLS-1$ //$NON-NLS-2$
				altNames = names;
			}
		} else if (name != null) {
			String[] names = {name};
			altNames = names;
		} else {
			altNames = new String[0];
		}

		if (custom) {
			unit = new LinearUnit.Custom(this, id, factorToDefinition, quantity.getUnit(), symbol, descBuf.toString(),
					altNames);
		} else {
			unit = new LinearUnit(this, id, factorToAtom, symbol, descBuf.toString(), altNames);
		}

		if (id != null) {
			LinearUnit oldUnit = unitCache.put(id, unit);
			if (oldUnit != null) {
				UnitLookup.getLogger().log(Level.FINE, "Replaced cached unit " + oldUnit + " with " + unit); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return unit;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString() {
		return "Linear(" + m_identifier + ')';
	}

	@Override
	public LinearUnit getPreferredUnit(IQuantity quantity, double minNumericalValue, double maxNumericalValue) {
		@SuppressWarnings("unchecked")
		ITypedQuantity<LinearUnit> typedQuantity = (ITypedQuantity<LinearUnit>) quantity;
		return unitSelector.getPreferredUnit(typedQuantity, minNumericalValue, maxNumericalValue);
	}

	@Override
	public LinearUnit getLargestExactUnit(IQuantity quantity) {
		@SuppressWarnings("unchecked")
		ITypedQuantity<LinearUnit> typedQuantity = (ITypedQuantity<LinearUnit>) quantity;
		return unitSelector.getLargestExactUnit(typedQuantity);
	}

	/*
	 * FIXME: Specify "best", so that for example a lower value is preferred, and whether the upper
	 * or implicit lower limit is exclusive, to make the result well defined.
	 */
	public ITypedQuantity<LinearUnit> snapToBestBetweenHalfAndEqual(ITypedQuantity<LinearUnit> upperLimit) {
		return unitSelector.snapToBestBetweenHalfAndEqual(upperLimit);
	}

	@Override
	public IFormatter<IQuantity> getFormatterResolving(IRange<IQuantity> range) {
		// FIXME: Implement properly!
		return getFormatter(IDisplayable.AUTO);
	}

	// FIXME: Reimplement parsing
	private static final Pattern NUMBER_UNIT_PATTERN = Pattern
			.compile("^(-?\\d+)(\\.?\\d*(?:E-?\\d+)?)\\s*([a-zA-Z%]*)$"); //$NON-NLS-1$

	@Override
	public ITypedQuantity<LinearUnit> parsePersisted(String persistedQuantity) throws QuantityConversionException {
		Matcher m = NUMBER_UNIT_PATTERN.matcher(persistedQuantity.trim());
		if (m.matches()) {
			LinearUnit unit = getUnit(m.group(3));
			if (unit != null) {
				try {
					if (m.group(2).length() == 0) {
						return unit.quantity(Long.parseLong(m.group(1)));
					} else {
						return unit.quantity(Double.parseDouble(m.group(1) + m.group(2)));
					}
				} catch (RuntimeException e) {

				}
			} else if (m.group(3).length() == 0) {
				throw QuantityConversionException.noUnit(persistedQuantity, getDefaultUnit().quantity(1234.0));
			} else {
				throw QuantityConversionException.unknownUnit(persistedQuantity, getDefaultUnit().quantity(1234.0));
			}
		}
		throw QuantityConversionException.unparsable(persistedQuantity, getDefaultUnit().quantity(1234.0));
	}

	@Override
	public Collection<LinearUnit> getAllUnits() {
		return allUnits;
	}

	@Override
	public Collection<LinearUnit> getCommonUnits() {
		return commonUnits;
	}

	@Override
	protected void addUnit(LinearUnit unit) {
		addUnit(unit, true);
	}

	/**
	 * Add a unit so that it is available for parsing and content assist, optionally also for direct
	 * selection by users.
	 *
	 * @param common
	 *            whether to include when showing most common units to users
	 */
	protected void addUnit(LinearUnit unit, boolean common) {
		super.addUnit(unit);
		allUnits.add(unit);
		if (common) {
			commonUnits.add(unit);
		}
		interactiveSymbolToUnitMap.put(unit.getIdentifier(), unit);
		interactiveSymbolToUnitMap.put(unit.getLocalizedSymbol().replace('\u00a0', ' '), unit);
	}

	@Override
	public ITypedQuantity<LinearUnit> parseInteractive(String interactiveQuantity) throws QuantityConversionException {
		return parseInteractive(interactiveQuantity, null);
	}

	/**
	 * @param interactiveQuantity
	 *            string to parse, interactive style
	 * @param symbolToUnitMap
	 *            map of additional symbols to parse into units, or null
	 * @return the parsed quantity
	 * @throws QuantityConversionException
	 *             containing detailed structured information, if parsing failed
	 */
	public ITypedQuantity<LinearUnit> parseInteractive(
		String interactiveQuantity, Map<String, ? extends LinearUnit> symbolToUnitMap)
			throws QuantityConversionException {
		/*
		 * Treat non-breaking spaces as regular breaking spaces.
		 * 
		 * Note that non-breaking space should be used between number and unit in regular (display)
		 * formatters, and also between digit groups for some locales. However, for interactive
		 * parsing or content assist purposes, we shouldn't distinguish between them, since they
		 * cannot be visually distinguished.
		 * 
		 * (Since display formatting outnumbers interactive formatting/parsing by at least several
		 * magnitudes, and contains a superset of the information, it makes sense to keep
		 * information used both for display and interactively with non-braking space, where
		 * applicable.)
		 */
		interactiveQuantity = interactiveQuantity.replace('\u00a0', ' ');
		Number num = null;
		ParsePosition pos = null;
		if (isHexString(interactiveQuantity)) {
			String numberString = getHexNumberString(interactiveQuantity);
			pos = new ParsePosition(2 + numberString.length());
			num = parseHexNumberString(numberString);
		} else {
			NumberFormat formatter = NUMBER_FORMAT_INTERACTIVE_HOLDER.get();
			pos = new ParsePosition(0);
			num = formatter.parse(interactiveQuantity, pos);
		}
		if (num != null) {
			String rest = interactiveQuantity.substring(pos.getIndex()).trim();
			LinearUnit unit = interactiveSymbolToUnitMap.get(rest);
			if ((unit == null) && (symbolToUnitMap != null)) {
				unit = symbolToUnitMap.get(rest);
			}
			if (unit == null) {
				if (rest.length() == 0) {
					throw QuantityConversionException.noUnit(interactiveQuantity, getDefaultUnit().quantity(num));
				}
				// A little ad-hoc to make tests (symmetric with persistable) happy. Should probably be removed.
				if (rest.contains(" ") && !rest.startsWith("\u00d7")) { //$NON-NLS-1$ //$NON-NLS-2$
					throw QuantityConversionException.unparsable(rest, getDefaultUnit().quantity(num));
				}
				throw QuantityConversionException.unknownUnit(rest, getDefaultUnit().quantity(num));
			}
			return unit.quantity(num);
		}
		throw QuantityConversionException.unparsable(interactiveQuantity, getDefaultUnit().quantity(1234.0));
	}

	private boolean isHexString(String interactiveQuantity) {
		return interactiveQuantity.startsWith("0x") || interactiveQuantity.startsWith("0X"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Number parseHexNumberString(String numberString) throws QuantityConversionException {
		try {
			return Long.parseLong(numberString, 16);
		} catch (NumberFormatException e) {
			throw QuantityConversionException.unparsable(numberString, getDefaultUnit().quantity(0x1ABC));
		}
	}

	private static String getHexNumberString(String interactiveQuantity) {
		int i = 2;
		while (i < interactiveQuantity.length() && isHexDigit(interactiveQuantity.charAt(i))) {
			i++;
		}
		return interactiveQuantity.substring(2, i);
	}

	private static boolean isHexDigit(char character) {
		return (character >= '0' && character <= '9') || (character >= 'A' && character <= 'F')
				|| (character >= 'a' && character <= 'f');
	}
}
