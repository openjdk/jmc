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
package org.openjdk.jmc.joverflow.support;

/**
 * Container for basic statistics about short arrays or strings.
 */
public class ShortArrayStats {
	/** Number of zero-length objects */
	public final int n0LenObjs;
	/** Overhead of zero-length objects */
	public final long ovhd0LenObjs;
	/** Number of 1-slot objects */
	public final int n1LenObjs;
	/** Overhead of 1-slot objects */
	public final long ovhd1LenObjs;
	/** Number of 1..4-slot objects */
	public final int n4LenObjs;
	/** Overhead of 1..4-slot objects */
	public final long ovhd4LenObjs;
	/** Number of 4..8-slot objects */
	public final int n8LenObjs;
	/** Overhead of 4..8-slot objects */
	public final long ovhd8LenObjs;

	public ShortArrayStats(int n0LenObjs, long ovhd0LenObjs, int n1LenObjs, long ovhd1LenObjs, int n4LenObjs,
			long ovhd4LenObjs, int n8LenObjs, long ovhd8LenObjs) {
		this.n0LenObjs = n0LenObjs;
		this.ovhd0LenObjs = ovhd0LenObjs;
		this.n1LenObjs = n1LenObjs;
		this.ovhd1LenObjs = ovhd1LenObjs;
		this.n4LenObjs = n4LenObjs;
		this.ovhd4LenObjs = ovhd4LenObjs;
		this.n8LenObjs = n8LenObjs;
		this.ovhd8LenObjs = ovhd8LenObjs;
	}

}
