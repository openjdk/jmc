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
package org.openjdk.jmc.joverflow.descriptors;

import org.openjdk.jmc.joverflow.heap.model.CollectionClassProperties;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.Constants.ProblemKind;

/**
 * Provides information associated with a single Collection class.
 */
public class CollectionClassDescriptor implements CollectionClassProperties {
	private final JavaClass clazz;
	private final boolean isMap;
	private final boolean canDetermineModCount;
	private final boolean hasOtherCollectionInImpl;
	private final String[] implClassNames;
	private final String[] parentColClassNames;

	private static final int NUM_ANTIPATTERNS = ProblemKind.values().length;

	private int nProblematicCols[] = new int[NUM_ANTIPATTERNS];
	private int problematicColsOverhead[] = new int[NUM_ANTIPATTERNS];

	CollectionClassDescriptor(JavaClass clazz, boolean isMap, boolean canDetermineModCount, JavaClass[] implClasses,
			String[] parentColClassNames, boolean hasOtherCollectionInImpl) {
		this(clazz, isMap, canDetermineModCount, getImplClassNames(implClasses), parentColClassNames,
				hasOtherCollectionInImpl);
	}

	/**
	 * Returns a new CollectionClassDescriptor, where all properties are the same as in this
	 * descriptor, except for the class name. It is intended to be used for generating class
	 * descriptors for subclasses of known collection classes, where the superclass' collection
	 * implementation is reused.
	 */
	CollectionClassDescriptor cloneForSubclass(JavaClass subClazz) {
		return new CollectionClassDescriptor(subClazz, isMap, canDetermineModCount, implClassNames, parentColClassNames,
				hasOtherCollectionInImpl);
	}

	private CollectionClassDescriptor(JavaClass clazz, boolean isMap, boolean canDetermineModCount,
			String[] implClassNames, String[] parentColClassNames, boolean hasOtherCollectionInImpl) {
		this.clazz = clazz;
		this.isMap = isMap;
		this.canDetermineModCount = canDetermineModCount;
		this.implClassNames = implClassNames;
		this.parentColClassNames = parentColClassNames;
		this.hasOtherCollectionInImpl = hasOtherCollectionInImpl;
		if (!clazz.isArray()) { // Not a pseudo-collection descriptor for array type
			clazz.setCollectionClassProperties(this);
		}
	}

	/** Returns JavaClass for this descriptor */
	public JavaClass getClazz() {
		return clazz;
	}

	/** Returns class name for this descriptor */
	public String getClassName() {
		return clazz.getName();
	}

	@Override
	public boolean isMap() {
		return isMap;
	}

	@Override
	public boolean hasOtherCollectionInImpl() {
		return hasOtherCollectionInImpl;
	}

	/**
	 * Returns true if for this collection class the information on whether it has been used
	 * (modified) is available (typically through the 'modCount' data field).
	 */
	public boolean canDetermineModCount() {
		return canDetermineModCount;
	}

	/**
	 * Returns true if the given class belongs to the implementation of this collection. For
	 * example, if this instance describes java.util.HashMap, returns true for HashMap$Entry.
	 */
	public boolean isImplClassName(String className) {
		if (implClassNames == null) {
			return false;
		}

		for (String implClassName : implClassNames) {
			if (className.equals(implClassName)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns true if this collection's class is in implementation of the given class. For example,
	 * should return true if this instance describes java.util.HashMap and className is
	 * java.util.HashSet.
	 */
	public boolean isInImplementationOf(String className) {
		if (parentColClassNames == null) {
			return false;
		}

		for (String parentColClassName : parentColClassNames) {
			if (className.equals(parentColClassName)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns true if this descriptor represents a primitive array type.
	 */
	public boolean isPrimitiveArray() {
		return clazz.isAnyDimPrimitiveArray();
	}

	// Functionality for keeping track of overhead stats of all instances of the
	// associated collection class.

	public void addProblematicCollection(ProblemKind kind, int ovhd) {
		int kindIdx = kind.ordinal();
		nProblematicCols[kindIdx]++;
		problematicColsOverhead[kindIdx] += ovhd;
	}

	public int getNumProblematicCollections(ProblemKind kind) {
		return nProblematicCols[kind.ordinal()];
	}

	public int getProblematicCollectionsOverhead(Constants.ProblemKind kind) {
		return problematicColsOverhead[kind.ordinal()];
	}

	public long getTotalOverhead() {
		long totalOvhd = 0;
		for (Constants.ProblemKind kind : ProblemKind.values()) {
			totalOvhd += getProblematicCollectionsOverhead(kind);
		}
		return totalOvhd;
	}

	@Override
	public String toString() {
		return clazz.getName();
	}

	private static String[] getImplClassNames(JavaClass[] implClasses) {
		String[] implClassNames = new String[implClasses.length];
		for (int i = 0; i < implClasses.length; i++) {
			implClassNames[i] = implClasses[i].getName();
		}
		return implClassNames;
	}
}
