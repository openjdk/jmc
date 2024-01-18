/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER_UNITY;

public class QuantitiesToolkit {
	private final static ITypedQuantity<LinearUnit> ZERO = NUMBER_UNITY.quantity(0);

	public static <M extends Comparable<? super M>> M min(M a, M b) {
		return a.compareTo(b) < 0 ? a : b;
	}

	public static <M extends Comparable<? super M>> M max(M a, M b) {
		return a.compareTo(b) > 0 ? a : b;
	}

	public static <M extends Comparable<? super M>> M minPresent(M a, M b) {
		return (a == null) ? b : (b == null) ? a : min(a, b);
	}

	public static <M extends Comparable<? super M>> M maxPresent(M a, M b) {
		return (a == null) ? b : (b == null) ? a : max(a, b);
	}

	public static <M extends Comparable<? super M>> boolean same(M a, M b) {
		return (a == null) ? (b == null) : ((b == null) ? false : (a.compareTo(b) == 0));
	}

	public static IQuantity nullSafe(IQuantity quantity) {
		if (quantity != null) {
			return quantity;
		} else {
			return ZERO;
		}
	}
}
