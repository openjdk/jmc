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
 * A one dimensional affine transform, to be used on numerical quantity values (that is, the numbers
 * in front of a unit). In other words, a transformation for numbers consisting of a linear scaling
 * followed by the addition of an offset. Typically, you request a value transform from a source
 * unit to a target unit.
 * <p>
 * The transform is fully described (apart from numerical precision) by the {@link #getMultiplier()
 * multiplier} and the {@link #getOffset() offset}. These methods are intended for when the
 * transform is to be performed on a large number of primitives and possibly in conjunction with
 * other transformations. A coalesced transformation can then be applied to every primitive,
 * optimally by specialized hardware (like a GPU).
 */
public interface IScalarAffineTransform {
	/**
	 * @return the offset to be added (after the source value has been multiplied with the
	 *         {@link #getMultiplier() multiplier})
	 */
	Number getOffset();

	/**
	 * @return the multiplier which source values should be multiplied with (before the
	 *         {@link #getOffset() offset} is added)
	 */
	double getMultiplier();

	// FIXME: Return QuantityConversionException.Problem instead? Handle underflow too?
	boolean targetOutOfRange(long srcNumericalValue, long maxAbsValue);

	boolean targetOutOfRange(double srcNumericalValue, long maxAbsValue);

	/**
	 * @param srcNumericalValue
	 *            a numerical quantity value, expressed in the source unit
	 * @return the corresponding numerical quantity value, when expressed in the target unit,
	 *         rounded to the closest integer that can be represented by a {@code long}
	 */
	long targetValue(long srcNumericalValue);

	/**
	 * @param srcNumericalValue
	 *            a numerical quantity value, expressed in the source unit
	 * @return the floor of the corresponding numerical quantity value, when expressed in the target
	 *         unit, clamped to a {@code long}
	 */
	long targetFloor(long srcNumericalValue);

	/**
	 * @param srcNumericalValue
	 *            a numerical quantity value, expressed in the source unit
	 * @return the floor of the corresponding numerical quantity value, when expressed in the target
	 *         unit
	 */
	double targetFloor(double srcNumericalValue);

	/**
	 * @param srcNumericalValue
	 *            a numerical quantity value, expressed in the source unit
	 * @return the floor of the corresponding numerical quantity value, when expressed in the target
	 *         unit, clamped to an {@code int}
	 */
	int targetIntFloor(Number srcNumericalValue);

	/**
	 * @param srcNumericalValue
	 *            an exact numerical quantity value, expressed in the source unit
	 * @return the corresponding numerical quantity value, when expressed in the target unit, as a
	 *         {@link Long} if it can exactly be represented in one, otherwise as some other
	 *         {@link Number} with at least the precision of {@code double}
	 */
	Number targetNumber(long srcNumericalValue);

	/**
	 * @param srcNumericalValue
	 *            an exact or inexact numerical quantity value, expressed in the source unit
	 * @return the corresponding numerical quantity value, when expressed in the target unit, as a
	 *         {@link Long} if it can exactly be represented in one, otherwise as some other
	 *         {@link Number} with at least the precision of {@code double}
	 */
	Number targetNumber(Number srcNumericalValue);

	/**
	 * @param srcNumericalValue
	 *            a numerical quantity value, expressed in the source unit
	 * @return the corresponding numerical quantity value, when expressed in the target unit
	 */
	double targetValue(double srcNumericalValue);

	/**
	 * @return the inverse transform
	 */
	IScalarAffineTransform invert();

	/**
	 * @return true iff this represents the identity transform
	 */
	boolean isUnity();

	/**
	 * @return true iff this transform can exactly be described by an integer multiplier followed by
	 *         an integer offset
	 */
	boolean isInteger();

	/**
	 * Concatenate this transform with {@code innerTransform}, such that applying the resulting
	 * transform is equivalent to first applying {@code innerTransform} and then applying this
	 * transform on the resulting value. That is, R(v) = T(I(v)), where R(v) is the resulting
	 * transform, T(v) is this transform, and I(v) is {@code innerTransform}.
	 * <p>
	 * In this snippet, {@code v1} and {@code v2} should be equal, apart from numerical precision,
	 * for any {@code v}.
	 *
	 * <pre>
	 * IScalarAffineTransform R, T = ..., I = ...;
	 * double v = ...;
	 *
	 * R = T.concat(I);
	 * double v1 = R.targetValue(v);
	 * double v2 = T.targetValue(I.targetValue(v));
	 * </pre>
	 *
	 * @param innerTransform
	 *            the transform that should be applied before this transform
	 * @return the concatenated transform
	 */
	IScalarAffineTransform concat(IScalarAffineTransform innerTransform);

	/**
	 * Concatenate the inverse of this transform with {@code innerTransform}, such that applying the
	 * resulting transform is equivalent to first applying {@code innerTransform} and then applying
	 * this inverse of this transform on the resulting value. That is, R(v) = T<sup>-1</sup>(I(v)),
	 * where R(v) is the resulting transform, T<sup>-1</sup>(v) is the inverse of this transform,
	 * and I(v) is {@code innerTransform}.
	 * <p>
	 * In this snippet, {@code v1} and {@code v2} should be equal, apart from numerical precision,
	 * for any {@code v}.
	 *
	 * <pre>
	 * IScalarAffineTransform R, T = ..., I = ...;
	 * double v = ...;
	 *
	 * R = T.invertAndConcat(I);
	 * double v1 = R.targetValue(v);
	 * double v2 = T.invert().targetValue(I.targetValue(v));
	 * </pre>
	 *
	 * @param innerTransform
	 *            the transform that should be applied before this transform
	 * @return the concatenated transform
	 */
	IScalarAffineTransform invertAndConcat(IScalarAffineTransform innerTransform);
}
