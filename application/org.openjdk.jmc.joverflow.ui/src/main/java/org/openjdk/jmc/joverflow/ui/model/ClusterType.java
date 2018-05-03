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
package org.openjdk.jmc.joverflow.ui.model;

import org.openjdk.jmc.joverflow.support.Constants.ProblemKind;

public enum ClusterType {
	EMPTY_USED_COLLECTION("Empty Used Collections"),
	EMPTY_UNUSED_COLLECTION("Empty Unused Collections"),
	EMPTY_ARRAY("Empty Arrays"),
	SPARSE_SMALL_COLLECTION("Sparse Small Collections"),
	SPARSE_LARGE_COLLECTION("Sparse Large Collections"),
	SPARSE_ARRAY("Sparse Arrays"),
	BOXED_COLLECTION("Boxed Collections"),
	ZERO_SIZE_ARRAY("Zero Size Arrays"),
	ONE_ELEMENT_ARRAY("Arrays with One Element"),
	UNUSED_HI_BYTES("Arrays with Underused Elements"),
	WEAK_MAP_WITH_BACK_REFS("Weak Maps with Back References"),
	VERTICAL_BAR_ARRAY("Vertical Bar Arrays"),
	LONG_ZERO_TAIL_ARRAY("Long Zero Tail Arrays"),
	SMALL_COLLECTION("Small Collections"),
	NULL_VALUE("Null Values"),
	DUPLICATE_STRING("Duplicate Strings"),
	DUPLICATE_ARRAY("Duplicate Arrays"),
	ALL_OBJECTS("All Objects");
	private String headline;

	ClusterType(String headline) {
		this.headline = headline;
	}

	public String getName() {
		return headline;
	}

	public static ClusterType fromProblemKind(ProblemKind pk) {
		switch (pk) {
		case EMPTY_USED:
			return ClusterType.EMPTY_USED_COLLECTION;
		case EMPTY_UNUSED:
			return ClusterType.EMPTY_UNUSED_COLLECTION;
		case EMPTY:
			return ClusterType.EMPTY_ARRAY;
		case SPARSE_SMALL:
			return ClusterType.SPARSE_SMALL_COLLECTION;
		case SPARSE_LARGE:
			return ClusterType.SPARSE_LARGE_COLLECTION;
		case SPARSE_ARRAY:
			return ClusterType.SPARSE_ARRAY;
		case BOXED:
			return ClusterType.BOXED_COLLECTION;
		case LENGTH_ZERO:
			return ClusterType.ZERO_SIZE_ARRAY;
		case LENGTH_ONE:
			return ClusterType.ONE_ELEMENT_ARRAY;
		case UNUSED_HI_BYTES:
			return ClusterType.UNUSED_HI_BYTES;
		case WEAK_MAP_WITH_BACK_REFS:
			return ClusterType.WEAK_MAP_WITH_BACK_REFS;
		case BAR:
			return ClusterType.VERTICAL_BAR_ARRAY;
		case LZT:
			return ClusterType.LONG_ZERO_TAIL_ARRAY;
		case SMALL:
			return ClusterType.SMALL_COLLECTION;
		default:
			throw new RuntimeException("Unknown problem kind " + pk);
		}
	}

	@Override
	public String toString() {
		return getName();
	}
}
