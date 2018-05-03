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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.support.Constants.ProblemKind;

/**
 * A temporary list of instances of {@link ClassAndOvhdCombo}, used during data collection and
 * aggregation.
 * <p>
 * When we aggregate entries that have the same class/field etc., we don't distinguish between class
 * versions, i.e. we consider two classes with the same name but different classloaders as a single
 * class. This fixed policy may need to be made flexible at some point.
 */
public class ClassAndOvhdComboList implements Cloneable {
	private LinkedList<ClassAndOvhdCombo> list;
	private int totalOverhead;

	public List<ClassAndOvhdCombo> getFinalList() {
		ArrayList<ClassAndOvhdCombo> result = new ArrayList<>(list.size());
		result.addAll(list);
		if (result.size() > 1) {
			Collections.sort(result);
		}
		return result;
	}

	/** Returns the total overhead of all entries in this list */
	public int getTotalOverhead() {
		return totalOverhead;
	}

	public void addCollectionInfo(JavaClass colClass, ProblemKind ovhdKind, int ovhd, int nInstances) {
		totalOverhead += ovhd;
		if (list == null) {
			list = new LinkedList<>();
			addNewEntry(colClass, ovhdKind, ovhd, nInstances);
			return;
		}

		addInfo(colClass, ovhdKind, ovhd, nInstances);
	}

	public void addCollectionInfoWithNumEls(
		JavaClass colClass, ProblemKind ovhdKind, int ovhd, int nInstances, long numElementsInCollection,
		int maxNumElements) {
		totalOverhead += ovhd;
		if (list == null) {
			list = new LinkedList<>();
			addNewEntryWithNumEls(colClass, ovhdKind, ovhd, nInstances, numElementsInCollection, maxNumElements);
			return;
		}

		addInfoWithNumEls(colClass, ovhdKind, ovhd, nInstances, numElementsInCollection, maxNumElements);
	}

	public void merge(ClassAndOvhdComboList other) {
		LinkedList<ClassAndOvhdCombo> otherList = other.list;
		if (otherList == null) {
			return; // Can happen if that list only contains good collections
		}
		for (ClassAndOvhdCombo entry : otherList) {
			if (entry instanceof ClassAndOvhdCombo.Extended) {
				ClassAndOvhdCombo.Extended extEntry = (ClassAndOvhdCombo.Extended) entry;
				addCollectionInfoWithNumEls(entry.getClazz(), entry.getProblemKind(), entry.getOverhead(),
						entry.getNumInstances(), extEntry.getTotalNumElements(), extEntry.getMaxNumElements());
			} else {
				addCollectionInfo(entry.getClazz(), entry.getProblemKind(), entry.getOverhead(),
						entry.getNumInstances());
			}
		}
	}

	@Override
	public ClassAndOvhdComboList clone() {
		ClassAndOvhdComboList result = new ClassAndOvhdComboList();
		if (list != null) { // Can happen if this cluster only contains good collections
			result.list = new LinkedList<>();
			for (ClassAndOvhdCombo entry : list) {
				result.list.add(entry.clone());
			}
		}
		result.totalOverhead = totalOverhead;
		return result;
	}

	private void addInfo(JavaClass colClass, ProblemKind ovhdKind, int ovhd, int nInstances) {
		for (ClassAndOvhdCombo entry : list) {
			if (entry.getClazz().getName().equals(colClass.getName()) && entry.getProblemKind() == ovhdKind) {
				entry.addInstances(nInstances, ovhd);
				return;
			}
		}

		addNewEntry(colClass, ovhdKind, ovhd, nInstances);
	}

	private void addNewEntry(JavaClass colClass, ProblemKind ovhdKind, int ovhd, int nInstances) {
		list.add(new ClassAndOvhdCombo(colClass, ovhdKind, nInstances, ovhd));
	}

	private void addInfoWithNumEls(
		JavaClass colClass, ProblemKind ovhdKind, int ovhd, int nInstances, long numElementsInCollection,
		int maxNumElements) {
		for (ClassAndOvhdCombo entry : list) {
			if (entry.getClazz().getName().equals(colClass.getName()) && entry.getProblemKind() == ovhdKind) {
				((ClassAndOvhdCombo.Extended) entry).addInstances(nInstances, ovhd, numElementsInCollection,
						maxNumElements);
				return;
			}
		}

		addNewEntryWithNumEls(colClass, ovhdKind, ovhd, nInstances, numElementsInCollection, maxNumElements);
	}

	private void addNewEntryWithNumEls(
		JavaClass colClass, ProblemKind ovhdKind, int ovhd, int nInstances, long numElementsInCollection,
		int maxNumElements) {
		list.add(new ClassAndOvhdCombo.Extended(colClass, ovhdKind, nInstances, ovhd, numElementsInCollection,
				maxNumElements));
	}
}
