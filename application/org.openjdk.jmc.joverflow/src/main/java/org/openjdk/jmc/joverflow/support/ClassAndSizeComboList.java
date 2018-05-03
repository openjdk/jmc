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

/**
 * A temporary list of instances of {@link ClassAndSizeCombo}, used during data collection and
 * aggregation.
 */
public class ClassAndSizeComboList implements Cloneable {

	private LinkedList<ClassAndSizeCombo> list;
	private int totalSize;

	public ClassAndSizeComboList() {
		list = new LinkedList<>();
	}

	public List<ClassAndSizeCombo> getFinalList() {
		ArrayList<ClassAndSizeCombo> result = new ArrayList<>(list.size());
		result.addAll(list);
		if (result.size() > 1) {
			Collections.sort(result);
		}
		return result;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public void addInstanceInfo(JavaClass clazz, int size, int nInstances) {
		totalSize += size;

		for (ClassAndSizeCombo entry : list) {
			if (entry.getClazz() == clazz) {
				entry.addInstances(nInstances, size);
				return;
			}
		}

		list.add(new ClassAndSizeCombo(clazz, nInstances, size));
	}

	public void merge(ClassAndSizeComboList other) {
		LinkedList<ClassAndSizeCombo> otherList = other.list;
		for (ClassAndSizeCombo entry : otherList) {
			addInstanceInfo(entry.getClazz(), entry.getSizeOrOvhd(), entry.getNumInstances());
		}
	}

	@Override
	public ClassAndSizeComboList clone() {
		ClassAndSizeComboList result = new ClassAndSizeComboList();
		for (ClassAndSizeCombo entry : list) {
			result.list.add(entry.clone());
		}
		result.totalSize = totalSize;
		return result;
	}
}
