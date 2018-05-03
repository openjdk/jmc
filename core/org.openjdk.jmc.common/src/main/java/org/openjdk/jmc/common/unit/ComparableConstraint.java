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

import static org.openjdk.jmc.common.unit.QuantitiesToolkit.maxPresent;
import static org.openjdk.jmc.common.unit.QuantitiesToolkit.minPresent;
import static org.openjdk.jmc.common.unit.QuantitiesToolkit.same;

import org.openjdk.jmc.common.unit.QuantityConversionException.Problem;

/**
 * A {@link IConstraint constraint} that wraps a {@link IPersister persister} for {@link Comparable}
 * values, and constrains the allowed values with minimum and maximum values.
 *
 * @param <T>
 *            the type of values that the constraint operates on
 */
public final class ComparableConstraint<T extends Comparable<T>> implements IConstraint<T>, IFormatter<T> {
	private final IPersister<T> persister;
	private final T min;
	private final T max;

	public static <U, T extends Comparable<T>> IConstraint<U> constrain(
		IConstraint<U> constraint, String persistedMin, String persistedMax) throws QuantityConversionException {
		U min;
		try {
			min = (persistedMin != null) ? constraint.parsePersisted(persistedMin) : null;
		} catch (QuantityConversionException e) {
			if (e.getProblem() != Problem.TOO_LOW) {
				throw e;
			}
			min = null;
		}
		U max;
		try {
			max = (persistedMax != null) ? constraint.parsePersisted(persistedMax) : null;
		} catch (QuantityConversionException e) {
			if (e.getProblem() != Problem.TOO_HIGH) {
				throw e;
			}
			max = null;
		}
		if ((min == null) && (max == null)) {
			return constraint;
		}
		if ((min instanceof Comparable) || (max instanceof Comparable)) {
			@SuppressWarnings("unchecked")
			IConstraint<U> result = (IConstraint<U>) constrainComparable((IConstraint<T>) constraint, (T) min, (T) max);
			return result;
		}
		return null;
	}

	public static <U, T extends Comparable<T>> IConstraint<U> constrain(IConstraint<U> constraint, U min, U max) {
		if ((min instanceof Comparable) || (max instanceof Comparable)) {
			@SuppressWarnings("unchecked")
			IConstraint<U> result = (IConstraint<U>) constrainComparable((IConstraint<T>) constraint, (T) min, (T) max);
			return result;
		}
		return null;
	}

	private static <T extends Comparable<T>> IConstraint<T> constrainComparable(
		IConstraint<T> constraint, T min, T max) {
		if ((min == null) && (max == null)) {
			return constraint;
		}
		if (constraint instanceof IPersister) {
			return new ComparableConstraint<>((IPersister<T>) constraint, min, max);
		}
		if (constraint instanceof ComparableConstraint) {
			ComparableConstraint<T> cc = (ComparableConstraint<T>) constraint;
			return cc.constrain(min, max);
		}
		return null;
	}

	public ComparableConstraint(IPersister<T> persister, T min, T max) {
		this.persister = persister;
		this.min = min;
		this.max = max;
	}

	private IConstraint<T> constrain(T otherMin, T otherMax) {
		T newMin = minPresent(min, otherMin);
		T newMax = maxPresent(max, otherMax);
		if ((newMin == null) || (newMax == null) || (newMin.compareTo(newMax) <= 0)) {
			if (same(newMin, min) && same(newMax, max)) {
				return this;
			}
			return new ComparableConstraint<>(persister, newMin, newMax);
		}
		return null;
	}

	@Override
	public ComparableConstraint<T> combine(IConstraint<?> other) {
		if ((other == this) || (other == persister)) {
			return this;
		}
		if ((other instanceof ComparableConstraint) && (((ComparableConstraint<?>) other).persister == persister)) {
			@SuppressWarnings("unchecked")
			ComparableConstraint<T> otherCC = (ComparableConstraint<T>) other;
			return combineNonSame(otherCC);
		}
		return null;
	}

	/**
	 * Combine with other ComparableConstraint known not to be the same instance, but having the
	 * same persister.
	 *
	 * @return a combined constraint, if possible, otherwise {@code null}
	 */
	// NOTE: If this class is ever made non-final, this method needs to be overridden in every subclass.
	protected ComparableConstraint<T> combineNonSame(ComparableConstraint<T> other) {
		T newMin = minPresent(min, other.min);
		T newMax = maxPresent(max, other.max);
		if ((newMin == null) || (newMax == null) || (newMin.compareTo(newMax) <= 0)) {
			// Optimization (premature?) to avoid creating new instances in many cases.
			if (same(newMin, min) && same(newMax, max)) {
				return this;
			}
			if (same(newMin, other.min) && same(newMax, other.max)) {
				return other;
			}
			return new ComparableConstraint<>(persister, newMin, newMax);
		}
		return null;
	}

	@Override
	public boolean validate(T value) throws QuantityConversionException {
		return persister.validate(value) || validateRange(value);
	}

	protected boolean validateRange(T value) throws QuantityConversionException {
		if (min != null) {
			if (min.compareTo(value) > 0) {
				throw QuantityConversionException.tooLow(value, min, persister);
			}
		}
		if (max != null) {
			if (max.compareTo(value) < 0) {
				throw QuantityConversionException.tooHigh(value, max, persister);
			}
		}
		return false;
	}

	@Override
	public String persistableString(T value) throws QuantityConversionException {
		validate(value);
		return persister.persistableString(value);
	}

	@Override
	public T parsePersisted(String persistedValue) throws QuantityConversionException {
		T value = persister.parsePersisted(persistedValue);
		validate(value);
		return value;
	}

	@Override
	public String interactiveFormat(T value) throws QuantityConversionException {
		validate(value);
		return persister.interactiveFormat(value);
	}

	@Override
	public T parseInteractive(String interactiveValue) throws QuantityConversionException {
		T value = persister.parseInteractive(interactiveValue);
		validate(value);
		return value;
	}

	@Override
	public String format(T value) {
		if (persister instanceof IFormatter) {
			@SuppressWarnings("unchecked")
			IFormatter<T> formatter = (IFormatter<T>) persister;
			return formatter.format(value);
		}
		return persister.interactiveFormat(value);
	}
}
