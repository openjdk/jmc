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
 * Type parameterized extension of {@link IQuantity}. This construction exists to reduce clutter for
 * casual users of IQuantity, while still providing type safety for internal implementations.
 * (Proposed "self-variance" extensions to Java, in JDK 9 or beyond, may directly support this with
 * a single interface.)
 */
public interface ITypedQuantity<U extends TypedUnit<U>> extends IQuantity {

	@Override
	KindOfQuantity<U> getType();

	@Override
	U getUnit();

	/**
	 * Get this quantity expressed in the unit {@code targetUnit}. Note that as a result of this
	 * conversion, precision may be lost. Note that this method differs from {@link #in(IUnit)} only
	 * by stricter typing.
	 *
	 * @return a quantity, with approximately the same value as this quantity, expressed in
	 *         {@code targetUnit}
	 * @throws IllegalArgumentException
	 *             if {@code targetUnit} is not of the same kind of quantity
	 */
	ITypedQuantity<U> in(U targetUnit);

	ITypedQuantity<U> add(ITypedQuantity<LinearUnit> addend) throws IllegalArgumentException;

	ITypedQuantity<LinearUnit> subtract(ITypedQuantity<U> subtrahend) throws IllegalArgumentException;

	@Override
	ITypedQuantity<U> multiply(long factor) throws UnsupportedOperationException;

	@Override
	ITypedQuantity<U> multiply(double factor) throws UnsupportedOperationException;

	ITypedQuantity<U> floorQuantize(ITypedQuantity<LinearUnit> quanta);

	String interactiveFormat(boolean allowCustomUnit);

	String localizedFormat(boolean useBreakingSpace, boolean allowCustomUnit);
}
