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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.messages.internal.Messages;

public abstract class KindOfQuantity<U extends TypedUnit<U>> extends ContentType<IQuantity>
		implements IPersister<IQuantity> {

	private static IMemberAccessor<Number, IQuantity> DOUBLE_ACCESSOR = new IMemberAccessor<Number, IQuantity>() {
		@Override
		public Number getMember(IQuantity inObject) {
			return inObject.numberValue();
		}
	};

	private static IMemberAccessor<IUnit, IQuantity> UNIT_ACCESSOR = new IMemberAccessor<IUnit, IQuantity>() {
		@Override
		public IUnit getMember(IQuantity inObject) {
			return inObject.getUnit();
		}
	};

	private final Map<String, U> m_units = new LinkedHashMap<>();

	public static class ExactFormatter<U extends TypedUnit<U>> extends DisplayFormatter<IQuantity> {
		protected ExactFormatter(KindOfQuantity<U> kindOfQuantity) {
			this(kindOfQuantity, "Exact"); //$NON-NLS-1$
		}

		protected ExactFormatter(KindOfQuantity<U> kindOfQuantity, String name) {
			super(kindOfQuantity, IDisplayable.EXACT, name);
		}

		@Override
		public String format(IQuantity quantity) {
			@SuppressWarnings("unchecked")
			ITypedQuantity<U> typedQuantity = (ITypedQuantity<U>) quantity;
			return typedQuantity.localizedFormat(false, true);
		}
	}

	public static class VerboseFormatter<U extends TypedUnit<U>> extends DisplayFormatter<IQuantity> {
		protected VerboseFormatter(KindOfQuantity<U> kindOfQuantity) {
			this(kindOfQuantity, "Verbose"); //$NON-NLS-1$
		}

		protected VerboseFormatter(KindOfQuantity<U> kindOfQuantity, String name) {
			super(kindOfQuantity, IDisplayable.VERBOSE, name);
		}

		@Override
		public String format(IQuantity quantity) {
			/*
			 * NOTE: Custom units are currently only supported (and hidden by default) for
			 * LinearKindOfQuantity, but might in future be supported for any KindOfQuantity, which
			 * is why this class is here.
			 */
			@SuppressWarnings("unchecked")
			ITypedQuantity<U> typedQuantity = (ITypedQuantity<U>) quantity;
			if (quantity.getUnit() instanceof LinearUnit) {
				LinearUnit unit = (LinearUnit) quantity.getUnit();
				if (unit.isCustom()) {
					// FIXME: Currently using "~=" sign. Figure out if it would be better to use "=".
					return typedQuantity.localizedFormat(false, true) + " \u2248 " //$NON-NLS-1$
							+ typedQuantity.localizedFormat(false, false);
				}
			}
			return typedQuantity.localizedFormat(false, false);
		}
	}

	/**
	 * @param defaultAtomUnitName
	 *            fallback atom name to use with the prefix, or null for no fallback
	 * @return the best known localized name, or null if none could be found or constructed
	 */
	String resolveLocalizedName(IPrefix<?> prefix, String atomUnitId, String defaultAtomUnitName) {
		String name = Messages.getString("Unit_" + getIdentifier() + '_' + prefix.identifier() + atomUnitId + "_name", //$NON-NLS-1$ //$NON-NLS-2$
				null);
		if ((name == null) && (defaultAtomUnitName != null)) {
			name = prefix.localizedName() + defaultAtomUnitName;
		}
		// Never return empty names. Use null to represent that we have no name, which is acceptable.
		return "".equals(name) ? null : name; //$NON-NLS-1$
	}

	String resolveLocalizedSymbol(IPrefix<?> prefix, String atomUnitId, String defaultAtomUnitSymbol) {
		String symbol = Messages
				.getString("Unit_" + getIdentifier() + '_' + prefix.identifier() + atomUnitId + "_symbol", null); //$NON-NLS-1$ //$NON-NLS-2$
		if (symbol == null) {
			symbol = prefix.symbol() + defaultAtomUnitSymbol;
		}
		return symbol;
	}

	/**
	 * Convenience method (possibly temporary) until type parameters has settled.
	 *
	 * @param <U>
	 *            Unit type. Inferred from the {@code unit} argument.
	 * @param number
	 *            numerical quantity value
	 * @param unit
	 *            quantity unit
	 * @return a string representing a formatted version of the number with unit
	 */
	public static <U extends TypedUnit<U>> String format(Number number, IUnit unit) {
		@SuppressWarnings("unchecked")
		U typedUnit = (U) unit;
		ITypedQuantity<U> quantity = typedUnit.quantity(number);
		return typedUnit.getContentType().getDefaultFormatter().format(quantity);
	}

	KindOfQuantity(String identifier) {
		super(identifier);
	}

	KindOfQuantity(String identifier, String localizedName) {
		super(identifier, localizedName);
	}

	public abstract KindOfQuantity<LinearUnit> getDeltaKind();

	public abstract U getDefaultUnit();

	/**
	 * Add a common unit (displayed to the user when selecting a unit).
	 */
	protected void addUnit(U unit) {
		assert unit.getIdentifier() != null;
		Object existing = m_units.put(unit.getIdentifier(), unit);
		assert existing == null;
	}

	/**
	 * @return the most common units, suitable to display a unit selection to the user.
	 * @see #getAllUnits()
	 */
	public Collection<? extends U> getCommonUnits() {
		return m_units.values();
	}

	/**
	 * @return all units, suitable for parsing, content assist and similar.
	 * @see #getCommonUnits()
	 */
	public Collection<? extends U> getAllUnits() {
		return m_units.values();
	}

	public U getUnit(String id) {
		return m_units.get(id);
	}

	@Override
	public IPersister<IQuantity> getPersister() {
		return this;
	}

	@Override
	public IConstraint<IQuantity> combine(IConstraint<?> other) {
		if (this == other) {
			return this;
		}
		// Mustn't cause infinite delegation.
		if (other instanceof ContentType) {
			return null;
		}
		@SuppressWarnings("unchecked")
		IConstraint<IQuantity> combo = (IConstraint<IQuantity>) other.combine(this);
		return combo;
	}

	@Override
	public boolean validate(IQuantity value) {
		if (value.getType() != this) {
			throw new IllegalArgumentException(value.persistableString() + " isn't of the kind " + getIdentifier()); //$NON-NLS-1$
		}
		return false;
	}

	@Override
	public String persistableString(IQuantity value) {
		validate(value);
		return value.persistableString();
	}

	/**
	 * Parse a persisted string. Only guaranteed to be able to parse strings produced by
	 * {@link IQuantity#persistableString()} for quantities of this kind of quantity. Only use this
	 * on persisted strings, never for interactive input.
	 *
	 * @param persistedQuantity
	 *            persisted string to parse
	 * @return a valid quantity for this kind of quantity
	 * @throws QuantityConversionException
	 *             if parsing failed
	 */
	@Override
	abstract public ITypedQuantity<U> parsePersisted(String persistedQuantity) throws QuantityConversionException;

	@Override
	public String interactiveFormat(IQuantity value) {
		validate(value);
		return value.interactiveFormat();
	}

	/**
	 * Parse an interactive string. Only guaranteed to be able to parse strings produced by
	 * {@link IQuantity#interactiveFormat()} for quantities of this kind of quantity and in the same
	 * locale. Only use this for interactive input, never for persisted strings.
	 *
	 * @param interactiveQuantity
	 *            interactive string to parse
	 * @return a valid quantity for this kind of quantity
	 * @throws QuantityConversionException
	 *             if parsing failed
	 */
	@Override
	abstract public ITypedQuantity<U> parseInteractive(String interactiveQuantity) throws QuantityConversionException;

	abstract public U getPreferredUnit(IQuantity quantity, double minNumericalValue, double maxNumericalValue);

	/**
	 * Get the largest unit, if any, in which this quantity can be expressed exactly, typically with
	 * an integer. If the quantity has zero magnitude
	 * (<code>{@link IQuantity#doubleValue() quantity.doubleValue()} == 0.0</code>),
	 * {@link IQuantity#getUnit() quantity.getUnit()} will be returned. Thus, if you want to find
	 * out a maximum common unit for a set of quantities (not recommended), only use the non-zero
	 * quantities.
	 * <p>
	 * Note that this may be a fairly expensive operation, and isn't intended to be used
	 * excessively. The only valid use case is for guessing the original unit in which a quantity
	 * was expressed, after it has been stored or transmitted using a legacy mechanism with a fixed
	 * unit.
	 *
	 * @return a unit or {@code null}
	 */
	abstract public U getLargestExactUnit(IQuantity quantity);

	/**
	 * Divide the given range into at most {@code maxBuckets} "naturally" aligned buckets, and
	 * return the first one. This can be used to create tick marks in charts or buckets for
	 * histograms. The number of buckets will typically be between {@code maxBuckets/2} and
	 * {@code maxBuckets}, but this should be better specified.
	 * <p>
	 * Note that {@code start} is included in the first bucket.
	 *
	 * @param start
	 *            range start value
	 * @param end
	 *            range end value
	 * @param maxBuckets
	 *            maximum number of buckets to divide range into
	 * @return the first bucket, as described above
	 */
	public final IRange<IQuantity> getFirstBucket(IQuantity start, IQuantity end, double maxBuckets) {
		if ((start.getType() != this) || (end.getType() != this)) {
			throw new IllegalArgumentException(
					start.persistableString() + " and " + end.persistableString() + " both needs to be of the kind " //$NON-NLS-1$ //$NON-NLS-2$
							+ getIdentifier());
		}
		@SuppressWarnings("unchecked")
		ITypedQuantity<U> typedStart = (ITypedQuantity<U>) start;
		@SuppressWarnings("unchecked")
		ITypedQuantity<U> typedEnd = (ITypedQuantity<U>) end;
		return getFirstBucket(typedStart, typedEnd, maxBuckets);
	}

	protected IRange<IQuantity> getFirstBucket(ITypedQuantity<U> start, ITypedQuantity<U> end, double maxBuckets) {

		assert maxBuckets > 0;
		ITypedQuantity<LinearUnit> delta = end.subtract(start);
		assert delta.doubleValue() > 0;
		/*
		 * Ensure that the extent of buckets depend only on the delta, and not on the exact phase
		 * start is in, to avoid tick marks that jump around in scrolling graphs.
		 */
		ITypedQuantity<LinearUnit> maxExtent = delta.multiply(2.0 / maxBuckets);
		/*
		 * This should ideally be an instance method on linear quantities, but since we no longer
		 * have a subclass for those, we have to go via LinearUnit.
		 */
		ITypedQuantity<LinearUnit> extent = maxExtent.getUnit().getContentType()
				.snapToBestBetweenHalfAndEqual(maxExtent);
		ITypedQuantity<U> alignedStart = start.floorQuantize(extent);

		return QuantityRange.createWithExtent(alignedStart, extent);
	}

	/**
	 * Temporary helper to format quantity ranges.
	 *
	 * @param formatHint
	 *            A format hint. See {@link IDisplayable#displayUsing(String)}.
	 * @return a quantity range formatter
	 */
	public IFormatter<IRange<IQuantity>> getRangeFormatter(final String formatHint) {
		if (IDisplayable.EXACT.equals(formatHint) || IDisplayable.VERBOSE.equals(formatHint)) {
			return new IFormatter<IRange<IQuantity>>() {
				@Override
				public String format(IRange<IQuantity> range) {
					IFormatter<IQuantity> formatter = getFormatterResolving(range);
					if (formatter instanceof IIncrementalFormatter) {
						IIncrementalFormatter changeFormat = (IIncrementalFormatter) formatter;
						IQuantity start = range.getStart();
						return changeFormat.formatAdjacent(null, start) + " \u2013 (" //$NON-NLS-1$
								+ range.getExtent().displayUsing(formatHint) + ") \u2013 " //$NON-NLS-1$
								+ changeFormat.formatAdjacent(start, range.getEnd());
					}
					return formatter.format(range.getStart()) + " \u2013 (" + range.getExtent().displayUsing(formatHint) //$NON-NLS-1$
							+ ") \u2013 " + formatter.format(range.getEnd()); //$NON-NLS-1$
				}
			};
		}
		return new IFormatter<IRange<IQuantity>>() {
			@Override
			public String format(IRange<IQuantity> range) {
				IFormatter<IQuantity> formatter = getFormatterResolving(range);
				if (formatter instanceof IIncrementalFormatter) {
					IIncrementalFormatter changeFormat = (IIncrementalFormatter) formatter;
					IQuantity start = range.getStart();
					return changeFormat.formatAdjacent(null, start) + " \u2013 " //$NON-NLS-1$
							+ changeFormat.formatAdjacent(start, range.getEnd());
				}
				return formatter.format(range.getStart()) + " \u2013 " + formatter.format(range.getEnd()); //$NON-NLS-1$
			}
		};
	}

	/**
	 * Get a formatter with sufficient resolution to produce unique strings for both ends of
	 * {@code range}, and consecutive equally spaced quantities. The returned formatter might
	 * implement {@link IIncrementalFormatter}, in which case its method can be used to reduce
	 * redundant information between adjacent quantities.
	 */
	public abstract IFormatter<IQuantity> getFormatterResolving(IRange<IQuantity> range);

	@Override
	public List<IAttribute<?>> getAttributes() {
		return Arrays.<IAttribute<?>> asList(UnitLookup.NUMERICAL_ATTRIBUTE, UnitLookup.UNIT_ATTRIBUTE);
	}

	@Override
	public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
		Map<IAccessorKey<?>, IDescribable> keys = new HashMap<>();
		keys.put(UnitLookup.NUMERICAL_ATTRIBUTE.getKey(), UnitLookup.NUMERICAL_ATTRIBUTE);
		keys.put(UnitLookup.UNIT_ATTRIBUTE.getKey(), UnitLookup.UNIT_ATTRIBUTE);
		return keys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M> IMemberAccessor<M, IQuantity> getAccessor(IAccessorKey<M> attribute) {
		// Types are manually enforced below
		if (UnitLookup.NUMERICAL_ATTRIBUTE.getKey().equals(attribute)) {
			return (IMemberAccessor<M, IQuantity>) DOUBLE_ACCESSOR;
		}
		if (UnitLookup.UNIT_ATTRIBUTE.getKey().equals(attribute)) {
			return (IMemberAccessor<M, IQuantity>) UNIT_ACCESSOR;
		}
		return null;
	}
}
