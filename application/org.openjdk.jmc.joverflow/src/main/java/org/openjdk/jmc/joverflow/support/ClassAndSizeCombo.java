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

/**
 * Represents a bunch of objects, all of which belong to the same class.
 * <p>
 * Note that this class implements compareTo() but has no implementation of equals(). In other
 * words, it's currently not guaranteed that compareTo() returns zero if and only if equals()
 * returns true. However, that only matters if instances of a class are used in classes like
 * PriorityQueue, which is highly unlikely for this class and its subclasses.
 */
public class ClassAndSizeCombo implements Cloneable, Comparable<ClassAndSizeCombo> {
	private final JavaClass clazz;
	private int numInstances;
	private int sizeOrOvhd;

	ClassAndSizeCombo(JavaClass clazz, int numInstances, int sizeOrOvhd) {
		this.clazz = clazz;
		this.numInstances = numInstances;
		this.sizeOrOvhd = sizeOrOvhd;
	}

	public JavaClass getClazz() {
		return clazz;
	}

	public int getNumInstances() {
		return numInstances;
	}

	public int getSizeOrOvhd() {
		return sizeOrOvhd;
	}

	// Methods used only when collecting and aggregating data

	void addInstances(int nInstances, int ovhd) {
		this.numInstances += nInstances;
		this.sizeOrOvhd += ovhd;
	}

	@Override
	public ClassAndSizeCombo clone() {
		try {
			return (ClassAndSizeCombo) super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public int compareTo(ClassAndSizeCombo other) {
		return other.getSizeOrOvhd() - this.getSizeOrOvhd();
	}
}
