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

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.support.Constants.ProblemKind;

/**
 * Represents a bunch of problematic collections, all of which have the same class and problem, e.g.
 * empty HashMaps. Other information available is the number of collections and their total
 * overhead. So example information in this instance may look like "HashMap: 100 EMPTY instances,
 * overhead 1000 bytes".
 */
public class ClassAndOvhdCombo extends ClassAndSizeCombo {

	private final Constants.ProblemKind problemKind;

	ClassAndOvhdCombo(JavaClass clazz, ProblemKind problemKind, int numInstances, int ovhd) {
		super(clazz, numInstances, ovhd);
		this.problemKind = problemKind;
	}

	public Constants.ProblemKind getProblemKind() {
		return problemKind;
	}

	public int getOverhead() {
		return getSizeOrOvhd();
	}

	@Override
	public ClassAndOvhdCombo clone() {
		return (ClassAndOvhdCombo) super.clone();
	}

	/**
	 * Instances of this class are created only for certain kinds of problematic collections:
	 * currently that's SMALL, SPARSE_SMALL and SPARSE_LARGE. It contains additional aggregated
	 * information about the number of elements of the said collections.
	 */
	public static class Extended extends ClassAndOvhdCombo {
		private long totalNumElements;
		private int maxNumElements;

		Extended(JavaClass collectionClazz, ProblemKind problemKind, int numInstances, int ovhd,
				long numElementsInCollection, int maxNumElements) {
			super(collectionClazz, problemKind, numInstances, ovhd);
			totalNumElements = numElementsInCollection;
			this.maxNumElements = maxNumElements;
		}

		void addInstances(int nInstances, int ovhd, long totalNumElements, int maxNumElements) {
			super.addInstances(nInstances, ovhd);
			this.totalNumElements += totalNumElements;
			if (maxNumElements > this.maxNumElements) {
				this.maxNumElements = maxNumElements;
			}
		}

		public long getTotalNumElements() {
			return totalNumElements;
		}

		public float getAverageNumElements() {
			return (float) (((double) totalNumElements) / getNumInstances());
		}

		public int getMaxNumElements() {
			return maxNumElements;
		}
	}
}
