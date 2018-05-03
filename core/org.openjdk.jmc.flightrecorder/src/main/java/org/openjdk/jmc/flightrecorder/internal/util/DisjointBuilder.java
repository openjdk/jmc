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
package org.openjdk.jmc.flightrecorder.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;

/**
 * An object collector used to place ranged objects in multiple arrays, where the objects in each
 * array are sorted and disjunct
 */
public class DisjointBuilder<T> {

	public interface ArrayFactory<U> {
		U[] createArray(int size);
	}

	private static class DisjointArray<T> {
		private Object[] array = new Object[3];
		final IQuantity start;
		IQuantity end;
		int size = 0;

		DisjointArray(T e, IQuantity start, IQuantity end) {
			this.start = start;
			this.end = end;
			array[size++] = e;
		}

		boolean accept(T e, IQuantity start, IQuantity end) {
			if (size >= array.length) {
				int newCapacity = array.length < 100 ? array.length * 4 : (array.length * 3) / 2 + 1;
				array = Arrays.copyOf(array, newCapacity);
			}
			if (start.compareTo(this.end) >= 0) {
				array[size++] = e;
				this.end = end;
				return true;
			}
			return false;
		}

		private T getElement(int index) {
			@SuppressWarnings("unchecked")
			T t = (T) array[index];
			return t;
		}
	}

	private final static IMemberAccessor<IQuantity, DisjointArray<?>> DA_START = new IMemberAccessor<IQuantity, DisjointArray<?>>() {

		@Override
		public IQuantity getMember(DisjointArray<?> inObject) {
			return inObject.start;
		}
	};

	private final static IMemberAccessor<IQuantity, DisjointArray<?>> DA_END = new IMemberAccessor<IQuantity, DisjointArray<?>>() {

		@Override
		public IQuantity getMember(DisjointArray<?> inObject) {
			return inObject.end;
		}
	};

	private int noLanes = 0;
	@SuppressWarnings("unchecked")
	private DisjointArray<T>[] lanes = new DisjointArray[1];
	private final IMemberAccessor<IQuantity, ? super T> startAccessor;
	private final IMemberAccessor<IQuantity, ? super T> endAccessor;

	public DisjointBuilder(IMemberAccessor<IQuantity, ? super T> startAccessor,
			IMemberAccessor<IQuantity, ? super T> endAccessor) {
		this.startAccessor = startAccessor;
		this.endAccessor = endAccessor;
	}

	public void add(T e) {
		IQuantity start = startAccessor.getMember(e);
		IQuantity end = endAccessor.getMember(e);
		if (noLanes == 0) {
			addToNewLane(e, start, end);
		} else if (!lanes[0].accept(e, start, end)) {
			int changedLane = addToOtherLane(e, start, end);
			sortLanes(changedLane);
		}
	}

	private void sortLanes(int fromIndex) {
		// Sorting the lanes by descending end time
		for (int i = fromIndex; i > 0; i--) {
			if (lanes[i].end.compareTo(lanes[i - 1].end) > 0) {
				DisjointArray<T> tmp = lanes[i - 1];
				lanes[i - 1] = lanes[i];
				lanes[i] = tmp;
			}
		}
	}

	private int addToOtherLane(T e, IQuantity start, IQuantity end) {
		// Try with the other existing lanes
		for (int i = 1; i < noLanes; i++) {
			if (lanes[i].accept(e, start, end)) {
				return i;
			}
		}
		return addToNewLane(e, start, end);
	}

	private int addToNewLane(T e, IQuantity start, IQuantity end) {
		if (noLanes >= lanes.length) {
			lanes = Arrays.copyOf(lanes, (lanes.length * 3) / 2 + 2);
		}
		lanes[noLanes] = new DisjointArray<>(e, start, end);
		return noLanes++;
	}

	public static <U> Collection<U[]> toArrays(
		Iterable<? extends DisjointBuilder<U>> collections, ArrayFactory<U> arrayFactory) {
		ArrayList<DisjointArray<U>> allLanes = new ArrayList<>();
		for (DisjointBuilder<U> c : collections) {
			for (int i = 0; i < c.noLanes; i++) {
				allLanes.add(c.lanes[i]);
			}
		}
		if (allLanes.size() == 0) {
			return Collections.emptyList(); // No input time ranges
		}
		Collections.sort(allLanes, new Comparator<DisjointArray<?>>() {

			@Override
			public int compare(DisjointArray<?> o1, DisjointArray<?> o2) {
				return o1.end.compareTo(o2.end);
			}

		});

		DisjointBuilder<DisjointArray<U>> lanesCombiner = new DisjointBuilder<>(DA_START, DA_END);
		for (DisjointArray<U> l : allLanes) {
			lanesCombiner.add(l);
		}
		List<U[]> result = new ArrayList<>(lanesCombiner.noLanes);
		for (int i = 0; i < lanesCombiner.noLanes; i++) {
			DisjointArray<DisjointArray<U>> laneOfLanes = lanesCombiner.lanes[i];
			int totalSize = 0;
			for (int j = 0; j < laneOfLanes.size; j++) {
				DisjointArray<U> lane = laneOfLanes.getElement(j);
				totalSize += lane.size;
			}
			U[] resultArray = arrayFactory.createArray(totalSize);
			int offset = 0;
			for (int j = 0; j < laneOfLanes.size; j++) {
				DisjointArray<U> lane = laneOfLanes.getElement(j);
				System.arraycopy(lane.array, 0, resultArray, offset, lane.size);
				offset += lane.size;
			}
			result.add(resultArray);
		}
		return result;
	}
}
