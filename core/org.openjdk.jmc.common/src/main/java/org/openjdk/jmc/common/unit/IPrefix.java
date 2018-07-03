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

import org.openjdk.jmc.common.unit.LinearKindOfQuantity.LinearUnitSelector;

public interface IPrefix<P extends IPrefix<P>> {
	/**
	 * @return identifier usable for constructing persistable identifiers for units.
	 */
	String identifier();

	/**
	 * @return the symbol normally used to present this prefix in a GUI.
	 */
	String symbol();

	/**
	 * An alternative symbol. Intended to be used for interactive parsing where entering the micron
	 * character may be cumbersome, or catching typing of MB when intending MiB. (These are very
	 * different cases, and should ideally not be handled by the same mechanism. However, a single
	 * mechanism seems to work right now.)
	 *
	 * @return alternative symbol, or {@code null} if no other representation is available
	 */
	String altSymbol();

	String localizedName();

	StringBuilder asExponentialStringBuilder(boolean multiplicationSign);

	ScaleFactor scaleFactor();

	ScaleFactor valueFactorTo(P targetPrefix);

	LinearUnitSelector createUnitSelector(LinearKindOfQuantity kindOfQuantity, Iterable<P> prefixes);
}
