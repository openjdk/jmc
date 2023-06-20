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
package org.openjdk.jmc.rjmx.util.internal;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.preferences.PreferencesKeys;
import org.openjdk.jmc.ui.common.tree.IParent;

public class PartitionedList<T> implements IParent<T>, Comparable<PartitionedList<T>> {
	private final int firstObjectIndex;
	private final int lastObjectIndex;
	private final List<T> list;
	private final int fromIndex;
	private final int toIndex;

	private PartitionedList(int firstObjectIndex, int lastObjectIndex, List<T> list, int fromIndex, int toIndex) {
		this.firstObjectIndex = firstObjectIndex;
		this.lastObjectIndex = lastObjectIndex;
		this.list = list;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	public List<T> getChildren() {
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public String toString() {
		return "[" + firstObjectIndex + "..." + lastObjectIndex + ']'; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public int compareTo(PartitionedList<T> o) {
		return fromIndex - o.fromIndex;
	}

	public static <T> List<?> create(List<T> list) {
		int maxChildCount = RJMXPlugin.getDefault().getRJMXPreferences()
				.getInt(PreferencesKeys.PROPERTY_LIST_AGGREGATE_SIZE, PreferencesKeys.DEFAULT_LIST_AGGREGATE_SIZE);
		return create(list, 1, list.size(), maxChildCount);
	}

	static <T> List<?> create(List<T> list, int levelMultiplier, int totalObjects, int maxChildCount) {
		int size = list.size();
		if (size > maxChildCount) {
			int partitionCount = (int) Math.ceil(size / (double) maxChildCount);
			List<PartitionedList<T>> partitions = new ArrayList<>(partitionCount);
			for (int i = 0; i < partitionCount; i++) {
				int fromIndex = i * maxChildCount;
				int toIndex = Math.min((i + 1) * maxChildCount, size);
				int firstObjectIndex = levelMultiplier * fromIndex;
				int lastObjectIndex = Math.min(totalObjects, levelMultiplier * toIndex) - 1;
				partitions.add(new PartitionedList<>(firstObjectIndex, lastObjectIndex, list, fromIndex, toIndex));
			}
			return create(partitions, levelMultiplier * maxChildCount, totalObjects, maxChildCount);
		} else {
			return list;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + firstObjectIndex;
		result = prime * result + fromIndex;
		result = prime * result + lastObjectIndex;
		result = prime * result + ((list == null) ? 0 : list.hashCode());
		result = prime * result + toIndex;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		PartitionedList<T> other = (PartitionedList<T>) obj;
		if (firstObjectIndex != other.firstObjectIndex) {
			return false;
		}
		if (fromIndex != other.fromIndex) {
			return false;
		}
		if (lastObjectIndex != other.lastObjectIndex) {
			return false;
		}
		if (list == null) {
			if (other.list != null) {
				return false;
			}
		} else if (!list.equals(other.list)) {
			return false;
		}
		if (toIndex != other.toIndex) {
			return false;
		}
		return true;
	}

}
