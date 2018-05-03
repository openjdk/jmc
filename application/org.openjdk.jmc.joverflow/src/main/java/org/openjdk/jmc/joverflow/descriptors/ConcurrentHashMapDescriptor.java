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

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaInt;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.UnresolvedObject;
import org.openjdk.jmc.joverflow.support.Constants;

/**
 * Descriptor for instances of ConcurrentHashMap.
 */
public class ConcurrentHashMapDescriptor extends AbstractCollectionDescriptor
		implements CollectionInstanceDescriptor.CapacityDifferentFromSize, Constants {
	private final Factory factory;
	private int cachedNumElements = -1;
	private int cachedTotalCapacity = -1;

	private ConcurrentHashMapDescriptor(JavaObject col, Factory factory) {
		super(col);
		this.factory = factory;
	}

	@Override
	public int getNumElements() {
		if (cachedNumElements != -1) {
			return cachedNumElements;
		}

		int result = 0;
		JavaThing segmentField = fields[factory.segmentsFieldIdx];
		if (segmentField == null || !(segmentField instanceof JavaObjectArray)) {
			// Likely unresolved object in a corrupted heap dump
			return 0;
		}
		JavaObjectArray segments = (JavaObjectArray) segmentField;
		JavaHeapObject[] segs = segments.getElements();
		for (JavaThing segThing : segs) {
			// Can be null in JDK7/8, where individual Segments are created lazily.
			if (segThing == null || !(segThing instanceof JavaObject)) {
				continue;
			}
			JavaObject seg = (JavaObject) segThing;

			result += getNumElementsInSegment(seg);
		}

		cachedNumElements = result;
		return result;
	}

	private int getNumElementsInSegment(JavaObject seg) {
		return ((JavaInt) seg.getField(factory.segLengthFieldIdx)).getValue();
	}

	@Override
	public int doGetImplSize() {
		col.setVisitedAsCollectionImpl();
		int result = col.getSize();
		// TODO: shall we also look at views here, like keySet etc.?
		JavaThing segmentField = fields[factory.segmentsFieldIdx];
		if (segmentField == null || !(segmentField instanceof JavaObjectArray)) {
			return result; // Likely unresolved object in a corrupted heap dump
		}

		JavaObjectArray segments = (JavaObjectArray) segmentField;
		segments.setVisitedAsCollectionImpl();
		result += segments.getSize();
		JavaHeapObject[] segs = segments.getElements();
		for (JavaThing segThing : segs) {
			// Can be null in JDK7/8, where individual Segments are created lazily.
			if (segThing == null || !(segThing instanceof JavaObject)) {
				continue;
			}
			JavaObject seg = (JavaObject) segThing;
			seg.setVisitedAsCollectionImpl();
			result += seg.getSize();
			// CHM$Segment extends ReentrantLock, which has one pointer field, 'Sync sync'
			JavaThing segSyncField = seg.getField(factory.segSyncFieldIdx);
			if (!(segSyncField == null || segSyncField instanceof UnresolvedObject)) {
				JavaObject segSync = (JavaObject) segSyncField;
				segSync.setVisitedAsCollectionImpl();
				result += segSync.getSize();
			}
			JavaThing segTableThing = seg.getField(factory.segTableFieldIdx);
			if (segTableThing == null || !(segTableThing instanceof JavaObjectArray)) {
				// Unresolved
				continue;
			}
			JavaObjectArray segTable = (JavaObjectArray) segTableThing;
			segTable.setVisitedAsCollectionImpl();
			result += segTable.getSize();
			int nElsInSeg = getNumElementsInSegment(seg);
			if (nElsInSeg == 0) {
				continue;
			}

			result += getSegmentEntriesSize(segTable);
		}

		return result;
	}

	private int getSegmentEntriesSize(JavaObjectArray entriesArray) {
		JavaHeapObject[] entries = entriesArray.getElements();
		int nextFieldIdx = factory.getEntryNextFieldIdx(entries);

		// Reusable fields, to reduce GC pressure
		JavaThing[] entryFields = null;

		int size = 0;
		for (JavaHeapObject entryThing : entries) {
			if (entryThing == null || !(entryThing instanceof JavaObject)) {
				// Unresolved or inconsistent
				continue;
			}
			JavaObject entry = (JavaObject) entryThing;
			while (true) {
				entry.setVisitedAsCollectionImpl();
				size += entry.getSize();
				entryFields = entry.getFields(entryFields);
				JavaObject prevEntry = entry;
				JavaThing entryThing1 = entryFields[nextFieldIdx];
				if (entryThing1 == null || !(entryThing1 instanceof JavaObject)) {
					break;
				}
				entry = (JavaObject) entryThing1;
				if (entry == prevEntry) {
					break;
				}
			}
		}

		return size;
	}

	@Override
	public int getSparsenessOverhead(int ptrSize) {
		int totalEls = 0;
		int totalCapacity = 0;
		int emptySegOverhead = 0;
		JavaThing segsThing = fields[factory.segmentsFieldIdx];
		if (segsThing == null || !(segsThing instanceof JavaObjectArray)) {
			return 0;
		}
		JavaObjectArray segments = (JavaObjectArray) segsThing;
		JavaHeapObject[] segs = segments.getElements();
		for (JavaHeapObject segThing : segs) {
			// Can be null in JDK7/8, where individual Segments are created lazily.
			if (segThing == null || !(segThing instanceof JavaObject)) {
				continue;
			}
			JavaObject seg = (JavaObject) segThing;
			JavaThing segTableField = seg.getField(factory.segTableFieldIdx);
			if (segTableField == null || segTableField instanceof UnresolvedObject) {
				continue;
			}
			int nElsInSeg = getNumElementsInSegment(seg);
			totalEls += nElsInSeg;
			JavaObjectArray segTable = (JavaObjectArray) segTableField;
			totalCapacity += segTable.getLength();
			if (nElsInSeg == 0) {
				emptySegOverhead += seg.getSize() + segTable.getSize();
			}
		}

		this.cachedTotalCapacity = totalCapacity;

		if (totalEls >= totalCapacity / 2) {
			return -1;
		}
		return (totalCapacity - totalEls) * ptrSize + emptySegOverhead;
	}

	@Override
	public int getDefaultCapacity() {
		return 16 * 16;
	}

	@Override
	public int getCapacity() {
		if (cachedTotalCapacity != -1) {
			return cachedTotalCapacity;
		}

		int totalCapacity = 0;
		JavaThing segsThing = fields[factory.segmentsFieldIdx];
		if (segsThing == null || !(segsThing instanceof JavaObjectArray)) {
			cachedTotalCapacity = 0;
			return cachedTotalCapacity;
		}
		JavaObjectArray segments = (JavaObjectArray) segsThing;
		JavaHeapObject[] segs = segments.getElements();
		for (JavaHeapObject segThing : segs) {
			// Can be null in JDK7/8, where individual Segments are created lazily.
			if (segThing == null || !(segThing instanceof JavaObject)) {
				continue;
			}
			JavaThing segTableField = ((JavaObject) segThing).getField(factory.segTableFieldIdx);
			if (segTableField == null || segTableField instanceof UnresolvedObject) {
				continue;
			}
			JavaObjectArray segTable = (JavaObjectArray) segTableField;
			totalCapacity += segTable.getLength();
		}

		this.cachedTotalCapacity = totalCapacity;
		return totalCapacity;
	}

	@Override
	public void iterateList(ListIteratorCallback cb) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void iterateMap(MapIteratorCallback cb) {
		JavaThing segsThing = fields[factory.segmentsFieldIdx];
		if (segsThing == null || !(segsThing instanceof JavaObjectArray)) {
			return;
		}

		JavaObjectArray segments = (JavaObjectArray) segsThing;
		if (!cb.scanImplementationObject(segments)) {
			return;
		}
		JavaHeapObject[] segs = segments.getElements();
		for (JavaHeapObject seg : segs) {
			if (seg == null) {
				continue;
			}
			cb.scanImplementationObject(seg);
		}

		int numElements = getNumElements();
		if (numElements == 0) {
			return;
		}

		int keyFieldIdx = -1, valueFieldIdx = -1;

		for (JavaHeapObject segThing : segs) {
			// Can be null in JDK7/8, where individual Segments are created lazily.
			if (segThing == null || !(segThing instanceof JavaObject)) {
				continue;
			}
			JavaObject seg = (JavaObject) segThing;

			JavaThing segTableField = seg.getField(factory.segTableFieldIdx);
			if (segTableField == null || segTableField instanceof UnresolvedObject) {
				continue;
			}
			JavaObjectArray segTable = (JavaObjectArray) segTableField;
			if (!cb.scanImplementationObject(segTable)) {
				continue;
			}

			int nElsInSeg = getNumElementsInSegment(seg);
			if (nElsInSeg == 0) {
				continue;
			}

			JavaHeapObject[] table = segTable.getElements();
			int nextFieldIdx = factory.getEntryNextFieldIdx(table);
			JavaThing[] entryFields = null;

			outerLoop: for (JavaHeapObject entryThing : table) {
				if (entryThing == null || !(entryThing instanceof JavaObject)) {
					continue;
				}
				JavaObject entry = (JavaObject) entryThing;

				while (true) {
					if (!cb.scanImplementationObject(entry)) {
						break;
					}

					if (keyFieldIdx == -1) {
						keyFieldIdx = factory.getKeyFieldIdx(entry);
						valueFieldIdx = factory.getValueFieldIdx(entry);
					}
					entryFields = entry.getFields(entryFields);
					JavaThing keyThing = entryFields[keyFieldIdx];
					JavaThing valueThing = entryFields[valueFieldIdx];

					JavaHeapObject key = null, value = null;
					if (keyThing instanceof JavaHeapObject) {
						key = (JavaHeapObject) keyThing;
					}
					if (valueThing instanceof JavaHeapObject) {
						value = (JavaHeapObject) valueThing;
					}
					if (!cb.scanMapEntry(key, value)) {
						break outerLoop;
					}

					JavaObject prevEntry = entry;
					JavaThing entryThing1 = entryFields[nextFieldIdx];
					if (entryThing1 == null || !(entryThing1 instanceof JavaObject)) {
						break;
					}
					entry = (JavaObject) entryThing1;
					if (entry == prevEntry) {
						break;
					}
				}
			}
		}
	}

	@Override
	public long getModCount() {
		JavaThing segsThing = fields[factory.segmentsFieldIdx];
		if (segsThing == null || !(segsThing instanceof JavaObjectArray)) {
			return 0;
		}
		JavaObjectArray segments = (JavaObjectArray) segsThing;
		JavaHeapObject[] segs = segments.getElements();
		int result = 0;

		for (JavaHeapObject segThing : segs) {
			// Can be null in JDK7/8, where individual Segments are created lazily.
			if (segThing == null || !(segThing instanceof JavaObject)) {
				continue;
			}
			JavaObject seg = (JavaObject) segThing;
			result += ((JavaInt) seg.getField(factory.segModCountFieldIdx)).getValue();
		}
		return result;
	}

	@Override
	AbstractCollectionDescriptor.Factory getFactory() {
		return factory;
	}

	static class Factory extends AbstractCollectionDescriptor.Factory {

		private static final String SEGMENTS_FIELD_NAME = "segments";

		/*
		 * TODO: this all gets problematic when fields are added/changed in CHM and CHM$Segment...
		 * Probably need to use "self-reflection" more actively, at least when it comes to fields
		 * like 'sync', that are only relevant for determining deep CHM size.
		 */
		private final int segmentsFieldIdx, segLengthFieldIdx, segTableFieldIdx, segSyncFieldIdx;
		private final int segModCountFieldIdx;

		Factory(JavaClass mapClazz, JavaClass segmentClazz, JavaClass[] allImplClasses) {
			super(mapClazz, true, allImplClasses, null, false, new String[] {SEGMENTS_FIELD_NAME});

			segmentsFieldIdx = mapClazz.getInstanceFieldIndex(SEGMENTS_FIELD_NAME);
			segLengthFieldIdx = segmentClazz.getInstanceFieldIndex("count");
			segTableFieldIdx = segmentClazz.getInstanceFieldIndex("table");
			segSyncFieldIdx = segmentClazz.getInstanceFieldIndex("sync");
			segModCountFieldIdx = segmentClazz.getInstanceFieldIndex("modCount");
		}

		private Factory(JavaClass clazz, AbstractCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);

			this.segmentsFieldIdx = ((Factory) superclassFactory).segmentsFieldIdx;
			this.segLengthFieldIdx = ((Factory) superclassFactory).segLengthFieldIdx;
			this.segTableFieldIdx = ((Factory) superclassFactory).segTableFieldIdx;
			this.segSyncFieldIdx = ((Factory) superclassFactory).segSyncFieldIdx;
			this.segModCountFieldIdx = ((Factory) superclassFactory).segModCountFieldIdx;
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		CollectionInstanceDescriptor get(JavaObject col) {
			return new ConcurrentHashMapDescriptor(col, this);
		}

		@Override
		protected boolean setModCountFieldIdx(JavaClass clazz) {
			// We know that modCount for HashSet can be found, though in non-standard way.
			// See ConcurrentHashMapDescriptor.getModCount().
			return true;
		}
	}
}
