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
 * A unit of an affine unit system.
 */
public interface IUnit {
	String[] EMPTY_STRING_ARRAY = new String[0];

	/**
	 * Get the kind of quantity of this unit.
	 */
	KindOfQuantity<?> getContentType();

	/**
	 * Create a quantity expressed in this unit and with a numerical quantity value equal to
	 * {@code numericalValue}.
	 */
	IQuantity quantity(Number numericalValue);

	/**
	 * Create a quantity expressed in this unit and with a numerical quantity value equal to
	 * {@code numericalValue}.
	 */
	IQuantity quantity(long numericalValue);

	/**
	 * Create a quantity expressed in this unit and with a numerical quantity value equal to
	 * {@code numericalValue}.
	 */
	IQuantity quantity(double numericalValue);

	/**
	 * Get a transform for transforming numerical quantity values expressed in this unit to
	 * numerical quantity values expressed in {@code targetUnit}. This method is typically only used
	 * internally by the quantity implementations.
	 *
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 */
	IScalarAffineTransform valueTransformTo(IUnit targetUnit);

	/**
	 * If this unit is linear. That is, if quantities expressed in this unit and in units of the
	 * same kind can be {@linkplain IQuantity#add(IQuantity) added to} and
	 * {@linkplain IQuantity#subtract(IQuantity) subtracted from} each other, and the resulting
	 * quantity remains of the same kind.
	 *
	 * @return {@code true} if and only if the unit is linear
	 */
	boolean isLinear();

	/**
	 * Get the unit that the difference between two quantities in this unit will have. For linear
	 * units, this is always the unit itself.
	 *
	 * @return the linear unit in which deltas of this unit is expressed
	 */
	LinearUnit getDeltaUnit();

	/**
	 * Persistable identifier, not to show interactively.
	 *
	 * @return the persistable (locale independent) identifier, or (possibly) null if this unit
	 *         isn't persistable.
	 */
	String getIdentifier();

	/**
	 * Symbols for units are most often not locale dependent, but there are exceptions (like in
	 * French). Also, besides real persistable units, temporary units are sometimes constructed for
	 * display purposes, and they might vary due to internationalization, and conceivably language.
	 * <p>
	 * While symbols normally don't contain any white space, spacing occur in circumstances like
	 * custom units. In this case, it is important that they are non-breaking spaces
	 * ({@code \u00a0}).
	 *
	 * @return the localized symbol of this unit.
	 */
	String getLocalizedSymbol();

	/**
	 * Convenience method for (localized) formatters, to prepend a space to
	 * {@link #getLocalizedSymbol()}, if needed.
	 *
	 * @param useBreakingSpace
	 *            to use breaking space instead of the default, non-breaking space (only for edit)
	 * @return a localized string to be appended to a number when displaying a quantity (including
	 *         space, if non-empty).
	 */
	String getAppendableSuffix(boolean useBreakingSpace);

	/**
	 * In the real world, units are sometimes used ambiguously (like MB). This description is
	 * intended to remove such ambiguity where needed.
	 *
	 * @return a (possibly) localized unambiguous description of this unit
	 */
	String getLocalizedDescription();

	/**
	 * Get alternate names for content assist matching. Note that these should never contain
	 * non-breaking spaces, only regular (breaking) spaces.
	 *
	 * @return an array, possibly empty, of names with which to match for content assist, never
	 *         {@code null}.
	 */
	String[] getAltLocalizedNames();
}
