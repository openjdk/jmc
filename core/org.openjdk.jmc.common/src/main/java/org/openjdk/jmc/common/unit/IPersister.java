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
 * A specialized {@link IConstraint constraint} that allows all instances of {@code T}, but not
 * {@code null}. As a result, {@link #interactiveFormat(Object)} and
 * {@link #persistableString(Object)} cannot throw {@link QuantityConversionException}, but
 * {@link NullPointerException} and in some cases {@link IllegalArgumentException}. This interface
 * is typically implemented by {@link ContentType content types} where every allowed value readily
 * can be represented with a human editable string.
 */
public interface IPersister<T> extends IConstraint<T> {
	@Override
	boolean validate(T value);

	/**
	 * A string representation independent of locale or internationalization, that when parsed using
	 * {@link #parsePersisted(String)} (on this instance) yields a result that is
	 * {@link Object#equals(Object) equal} to the given {@code value}. That is, the exact
	 * representation must be preserved.
	 *
	 * @return a string representation independent of locale or internationalization.
	 * @throws NullPointerException
	 *             if {@code value} is null
	 * @throws IllegalArgumentException
	 *             if some type aspect of {@code value} prevents it from being valid
	 */
	@Override
	String persistableString(T value);

	/**
	 * An exact string representation taking locale and internationalization into account. When
	 * parsed using {@link #parseInteractive(String)} (on this instance) yields a result that is
	 * {@link Object#equals(Object) equal} to the given {@code value}. That is, the exact
	 * representation must be preserved.
	 *
	 * @return a string representation taking locale and internationalization into account.
	 * @throws NullPointerException
	 *             if {@code value} is null
	 * @throws IllegalArgumentException
	 *             if some type aspect of {@code value} prevents it from being valid
	 */
	@Override
	String interactiveFormat(T value);
}
