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
 * A constraint on allowed instances of an existing type {@code T}, including constrained
 * conversions to and from persistable and interactive strings.
 *
 * @param <T>
 *            the type of values that the constraint operates on
 */
/*
 * FIXME: Separate persistence to a has-a, rather than a is-a? Or maybe only in subclasses (and
 * delegate)? Same with interactive? That is, make two narrowed down interfaces.
 */
public interface IConstraint<T> {
	/**
	 * Return a constraint that honors both this constraint and {@code other}, if such a constraint
	 * would accept anything except {@code null}. Otherwise, return {@code null}.
	 *
	 * @return a constraint or {@code null}
	 */
	IConstraint<T> combine(IConstraint<?> other);

	/**
	 * Fundamentally, check that {@code value} satisfies this constraint and throw an exception
	 * otherwise. As long as the method returns normally, {@code value} is a valid value, regardless
	 * of the return value. However, when wrapping a persister in a constraint, it is possible that
	 * the persister treats some magic values differently. If the constraint isn't aware of these
	 * magical values it should typically not try to validate them. This is signaled by the
	 * persister by returning true from this method.
	 *
	 * @return true if this value is considered magical and further validation should be skipped,
	 *         false otherwise. Any return value mean that the {@code value} is valid.
	 * @throws NullPointerException
	 *             if {@code value} is null and this constraint doesn't allow it
	 * @throws IllegalArgumentException
	 *             if some type aspect of {@code value} prevents it from being valid
	 * @throws QuantityConversionException
	 *             if the constraint isn't satisfied in some other way
	 */
	boolean validate(T value) throws QuantityConversionException;

	/**
	 * A string representation independent of locale or internationalization, that when parsed using
	 * {@link #parsePersisted(String)} (on this instance) yields a result that is
	 * {@link Object#equals(Object) equal} to the given {@code value}. That is, the exact
	 * representation must be preserved.
	 *
	 * @return a string representation independent of locale or internationalization.
	 * @throws NullPointerException
	 *             if {@code value} is null and this constraint doesn't allow it
	 * @throws IllegalArgumentException
	 *             if some type aspect of {@code value} prevents it from being valid
	 * @throws QuantityConversionException
	 *             if the constraint isn't satisfied in some other way
	 */
	String persistableString(T value) throws QuantityConversionException;

	/**
	 * Parse a persisted string. Only guaranteed to be able to parse strings produced by
	 * {@link #persistableString(Object)} on this instance. Only use this on persisted strings,
	 * never for interactive input.
	 *
	 * @return a valid value for this instance
	 * @throws NullPointerException
	 *             if {@code persistedValue} is null
	 * @throws QuantityConversionException
	 *             if {@code persistedValue} couldn't be parsed or didn't satisfy the constraint
	 */
	T parsePersisted(String persistedValue) throws QuantityConversionException;

	/**
	 * An exact string representation taking locale and internationalization into account. When
	 * parsed using {@link #parseInteractive(String)} (on this instance) yields a result that is
	 * {@link Object#equals(Object) equal} to the given {@code value}. That is, the exact
	 * representation must be preserved.
	 *
	 * @return a string representation taking locale and internationalization into account.
	 * @throws NullPointerException
	 *             if {@code value} is null and this constraint doesn't allow it
	 * @throws IllegalArgumentException
	 *             if some type aspect of {@code value} prevents it from being valid
	 * @throws QuantityConversionException
	 *             if {@code value} doesn't satisfy the constraint
	 */
	String interactiveFormat(T value) throws QuantityConversionException;

	/**
	 * Parse an interactive string. Only guaranteed to be able to parse strings produced by
	 * {@link #interactiveFormat(Object)} on this instance and in the same locale. Only use this for
	 * interactive input, never for persisted strings.
	 *
	 * @return a valid value for this instance
	 * @throws NullPointerException
	 *             if {@code interactiveValue} is null
	 * @throws QuantityConversionException
	 *             if {@code interactiveValue} couldn't be parsed or didn't satisfy the constraint
	 */
	T parseInteractive(String interactiveValue) throws QuantityConversionException;
}
