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

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItem;

public interface IQuantity extends IItem, IDisplayable, Comparable<IQuantity> {

	/**
	 * Get the kind of this quantity.
	 */
	@Override
	KindOfQuantity<?> getType();

	/**
	 * @return the unit in which this quantity is expressed
	 */
	IUnit getUnit();

	/**
	 * Get this quantity expressed in the unit {@code targetUnit}. Note that as a result of this
	 * conversion, precision may be lost. Partly due to that fact, this method should generally not
	 * be used. If the ultimate goal is some pure numerical value, there are more suitable methods,
	 * some of which are listed below, which directly provides such values. In either case, the
	 * quantity returned from this method should not be kept alive, as the original quantity
	 * provides better precision.
	 *
	 * @return a quantity, with approximately the same value as this quantity, expressed in
	 *         {@code targetUnit}
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 * @see #doubleValueIn(IUnit)
	 * @see #numberValueIn(IUnit)
	 * @see #clampedLongValueIn(IUnit)
	 * @see #ratioTo(IQuantity)
	 */
	IQuantity in(IUnit targetUnit);

	/**
	 * Get the numerical quantity value that this quantity would have if expressed in the unit
	 * {@code targetUnit}, rounded to a mathematical integer, if that numerical value is in the
	 * range {@code [-maxAbsValue-1, maxAbsValue]}. Otherwise, an
	 * {@link QuantityConversionException} will be thrown, with the violation encoded. Note that as
	 * a result of this conversion, precision may be lost.
	 *
	 * @return the numerical quantity, as a {@code long}
	 * @throws QuantityConversionException
	 *             if the result would be out of range
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 * @see #numberValueIn(IUnit)
	 */
	long longValueIn(IUnit targetUnit, long maxAbsValue) throws QuantityConversionException;

	/**
	 * Get the numerical quantity value that this quantity would have if expressed in the unit
	 * {@code targetUnit}, rounded to a mathematical integer, if that numerical value can be
	 * represented in a {@code long}. Otherwise, an {@link QuantityConversionException} will be
	 * thrown, with the violation encoded. Note that as a result of this conversion, precision may
	 * be lost.
	 * <p>
	 * This method is equivalent to {@link #longValueIn(IUnit, long) longValueIn(IUnit,
	 * Long.MAX_VALUE)}.
	 *
	 * @return the numerical quantity, as a {@code long}
	 * @throws QuantityConversionException
	 *             if the result would be out of range
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 * @see #numberValueIn(IUnit)
	 */
	long longValueIn(IUnit targetUnit) throws QuantityConversionException;

	/**
	 * Get the {@code long} value closest to the numerical quantity value that this quantity would
	 * have if expressed in the unit {@code targetUnit}. This means that values outside the range
	 * {@code [} {@link Long#MIN_VALUE} {@code ,}{@link Long#MAX_VALUE}{@code ]} will be clamped to
	 * the closest extreme. Note that as a result of this conversion, precision may be lost.
	 * <p>
	 * This method is equivalent to {@link #in(IUnit)}@{@code .}{@link #longValue()}, but is
	 * preferred since it is both cheaper and its name more explicitly conveys the limitations
	 * involved.
	 *
	 * @return the numerical quantity, as a {@code long}
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 * @see #numberValueIn(IUnit)
	 */
	long clampedLongValueIn(IUnit targetUnit);

	/**
	 * Get the {@code long} value closest to the floor of the numerical quantity value that this
	 * quantity would have if expressed in the unit {@code targetUnit}. This means that values
	 * outside the range{@code [} {@link Long#MIN_VALUE} {@code , }{@link Long#MAX_VALUE} {@code ]}
	 * will be clamped to the closest extreme. Note that as a result of this conversion, precision
	 * may be lost.
	 *
	 * @return the floor of the numerical quantity, as a {@code long}
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 */
	long clampedFloorIn(IUnit targetUnit);

	/**
	 * Get the {@code int} value closest to the floor of the numerical quantity value that this
	 * quantity would have if expressed in the unit {@code targetUnit}. This means that values
	 * outside the range{@code [} {@link Integer#MIN_VALUE} {@code , }{@link Integer#MAX_VALUE}
	 * {@code ]} will be clamped to the closest extreme. Note that as a result of this conversion,
	 * precision may be lost.
	 *
	 * @return the floor of the numerical quantity, as an {@code int}
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 */
	int clampedIntFloorIn(IUnit targetUnit);

	/**
	 * Get the numerical quantity value that this quantity would have if expressed in the unit
	 * {@code targetUnit}, as a {@code double}. Note that as a result of this conversion, precision
	 * may be lost.
	 *
	 * @return the numerical quantity, as a {@code double}
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 */
	double doubleValueIn(IUnit targetUnit);

	/**
	 * Get the numerical quantity value that this quantity would have if expressed in the unit
	 * {@code targetUnit}, as either a {@link Long} or a {@link Number} with at least the precision
	 * of {@code double}. If this quantity is exact, the unit conversion is known to be exact, and
	 * the result can be exactly represented in a {@code long}, a {@link Long} will be returned.
	 * Otherwise, some other {@link Number} implementation, such as {@link Double} will be returned.
	 * Note that as a result of this conversion, precision may be lost.
	 *
	 * @return the numerical quantity, as a {@link Long} if exact, otherwise as some other
	 *         {@link Number}
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 * @see #clampedLongValueIn(IUnit)
	 */
	Number numberValueIn(IUnit targetUnit);

	/**
	 * Get the numerical quantity value of this quantity as a {@code long}. This may cause
	 * truncation and loss of precision.
	 *
	 * @return the numerical quantity, as a {@code long}
	 */
	long longValue();

	/**
	 * Get the numerical quantity value of this quantity as a {@code double}. This may cause loss of
	 * precision.
	 *
	 * @return the numerical quantity, as a {@code double}
	 */
	double doubleValue();

	/**
	 * Get the numerical quantity value that this quantity, as either a {@link Long} or a
	 * {@link Number} with at least the precision of {@code double}.
	 *
	 * @return the numerical quantity
	 */
	Number numberValue();

	/**
	 * A string representation independent of locale or internationalization, that when parsed using
	 * {@link KindOfQuantity#parsePersisted(String)} (by this quantity's kind of quantity) yields a
	 * value that is {@link Object#equals(Object) equal} to this quantity. That is, the exact
	 * representation must be preserved.
	 *
	 * @return a string representation independent of locale or internationalization.
	 */
	String persistableString();

	/**
	 * An exact string representation taking locale and internationalization into account. When
	 * parsed using {@link KindOfQuantity#parseInteractive(String)} (by this quantity's kind of
	 * quantity), in the same locale, yields a value that is {@link Object#equals(Object) equal} to
	 * this quantity. That is, the exact representation must be preserved.
	 *
	 * @return a string representation taking locale and internationalization into account.
	 */
	String interactiveFormat();

	/**
	 * If this quantity is linear. That is, if quantities of the same kind can be
	 * {@linkplain #add(IQuantity) added to} and {@linkplain #subtract(IQuantity) subtracted from}
	 * it, and the resulting quantity remains of the same kind.
	 *
	 * @return true iff the quantity is linear
	 */
	boolean isLinear();

	/**
	 * Returns a new quantity that is the arithmetic sum of this quantity and {@code addend}, if
	 * such an operation is defined for this kind of quantity. This operation is commutative, so
	 * that {@code a.add(b)} produces the same result, or throws the same kind of exception, as
	 * {@code b.add(a)}.
	 * <p>
	 * Note that if this quantity is {@linkplain #isLinear() linear}, adding a quantity of the same
	 * kind is guaranteed to succeed and produces a quantity of the same kind. Also, the sum of a
	 * non-linear quantity and a linear quantity of a specific related (delta) kind of quantity is
	 * defined and produces a non-linear quantity. However, two non-linear quantities may never be
	 * added.
	 *
	 * @return the sum of this quantity and {@code addend}
	 * @throws IllegalArgumentException
	 *             if {@code addend} is not of a compatible kind of quantity
	 */
	IQuantity add(IQuantity addend) throws IllegalArgumentException;

	/**
	 * Returns a new quantity that is the arithmetic difference of this quantity and
	 * {@code subtrahend}, if such an operation is defined for this kind of quantity. This operation
	 * is anti-commutative when this quantity and {@code subtrahend} are of the same kind. That is,
	 * when the result is {@linkplain #isLinear() linear}.
	 * <p>
	 * Quantities of the same kind may be subtracted, giving a linear result. Additionally, a linear
	 * quantity may be subtracted from a non-linear quantity, provided their kinds are related.
	 *
	 * @return the difference of this quantity and {@code subtrahend}
	 * @throws IllegalArgumentException
	 *             if {@code subtrahend} is not of a compatible kind of quantity
	 */
	IQuantity subtract(IQuantity subtrahend) throws IllegalArgumentException;

	/**
	 * Returns a new quantity that is this quantity multiplied with {@code factor}, if this is a
	 * linear quantity. Otherwise, this operation is undefined and
	 * {@link UnsupportedOperationException} will be thrown.
	 *
	 * @return this quantity multiplied with {@code factor}
	 * @throws UnsupportedOperationException
	 *             if this quantity is not linear
	 */
	IQuantity multiply(long factor) throws UnsupportedOperationException;

	/**
	 * Returns a new quantity that is this quantity multiplied with {@code factor}, if this is a
	 * linear quantity. Otherwise, this operation is undefined and
	 * {@link UnsupportedOperationException} will be thrown.
	 *
	 * @return this quantity multiplied with {@code factor}
	 * @throws UnsupportedOperationException
	 *             if this quantity is not linear
	 */
	IQuantity multiply(double factor) throws UnsupportedOperationException;

	/**
	 * Compute the ratio of this quantity to the given {@code consequent} quantity, if both are of
	 * the same linear kind. Otherwise, this operation is undefined and an exception is thrown.
	 *
	 * @return the ratio of this quantity to {@code consequent}, as a {@code double} fraction
	 * @throws UnsupportedOperationException
	 *             if this quantity is not linear
	 * @throws IllegalArgumentException
	 *             if {@code consequent} is not of a compatible kind of quantity
	 */
	double ratioTo(IQuantity consequent) throws UnsupportedOperationException, IllegalArgumentException;
}
