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
package org.openjdk.jmc.joverflow.ui.viewers;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;

import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

/**
 * Class holding an aggregate of overhead/memory/size for a number of {@code ObjectCluster}. The group is identified by
 * {@code id}
 */
class MemoryStatisticsItem {

	private final Object id;
	private Integer index;
	private final LongProperty ovhdP = new SimpleLongProperty();
	private final LongProperty memoryP = new SimpleLongProperty();
	private final IntegerProperty sizeP = new SimpleIntegerProperty();
	private long ovhd;
	private long memory;
	private int size;

	public MemoryStatisticsItem(Object id, long memory, long ovhd, int size) {
		this.id = id;
		memoryP.set(memory);
		ovhdP.set(ovhd);
		sizeP.set(size);
	}

	public void reset() {
		ovhd = 0;
		memory = 0;
		size = 0;
	}

	public String getName() {
		return id == null ? "N/A" : id.toString();
	}

	public LongProperty ovhdProperty() {
		return ovhdP;
	}

	public LongProperty memoryProperty() {
		return memoryP;
	}

	public IntegerProperty sizeProperty() {
		return sizeP;
	}

	public void addObjectCluster(ObjectCluster oc) {
		ovhd += oc.getOverhead();
		memory += oc.getMemory();
		size += oc.getObjectCount();
	}

	public boolean update() {
		if (size == 0) {
			return false;
		}
		memoryP.set(memory);
		sizeP.set(size);
		ovhdP.set(ovhd);
		return true;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Integer getIndex() {
		return index;
	}

	public Object getId() {
		return id;
	}

	@Override
	public String toString() {
		return getName();
	}

}
