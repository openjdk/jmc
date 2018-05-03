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

import java.util.Arrays;

class ObjectClusterImpl implements ObjectCluster {

	private static final int NONE = -1;
	private int[] indexArray;
	private int lastObj = NONE;
	private final ClusterType type;
	private int memory;
	private int ovhd;
	private final String qualifier;
	private final String className;
	private int elementsInArray;

	ObjectClusterImpl(ClusterType type, String className, String qualifier) {
		this.type = type;
		this.className = className;
		this.qualifier = qualifier;
	}

	@Override
	public int getObjectCount() {
		return elementsInArray + 1;
	}

	@Override
	public int getMemory() {
		return memory;
	}

	@Override
	public int getOverhead() {
		return ovhd;
	}

	@Override
	public ClusterType getType() {
		return type;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	int addObject(int globalObjectIndex, int memory, int ovhd) {
		if (lastObj != globalObjectIndex) {
			if (lastObj != NONE) {
				addtoArray(lastObj);
			}
			lastObj = globalObjectIndex;
			this.memory += memory;
			this.ovhd += ovhd;
			return memory;
		}
		return 0;
	}

	private void addtoArray(int globalObjectIndex) {
		if (indexArray == null) {
			indexArray = new int[5];
		} else if (elementsInArray == indexArray.length) {
			indexArray = Arrays.copyOf(indexArray, indexArray.length + (indexArray.length >> 1));
		}
		indexArray[elementsInArray++] = globalObjectIndex;
	}

	void trim() {
		if (indexArray != null) {
			indexArray = Arrays.copyOf(indexArray, elementsInArray);
		}
	}

	@Override
	public int getGlobalObjectIndex(int indexInCluster) {
		if (indexInCluster == elementsInArray) {
			return lastObj;
		} else {
			return indexArray[indexInCluster];
		}
	}

}
