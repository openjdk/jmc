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
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.UnresolvedObject;

/**
 * Descriptor for java.util.TreeMap. It is somewhat complicated due to the fact that JRockit seems
 * to use its own version of this class, where each node has Object keys[], values[] rather than
 * Object key, value.
 */
public class TreeMapDescriptor extends AbstractLinkedCollectionDescriptor {
	private static final JavaHeapObject[] EMPTY_OBJ_ARRAY = new JavaHeapObject[0];

	private int keyFieldIdx, valueFieldIdx, leftFieldIdx, rightFieldIdx;

	private TreeMapDescriptor(JavaObject col, Factory factory) {
		super(col, factory);
	}

	@Override
	public int doGetImplSize() {
		col.setVisitedAsCollectionImpl();
		int result = col.getSize();

		JavaThing rootThing = col.getField(factory.rootFieldIdx);
		if (rootThing == null || rootThing instanceof UnresolvedObject) {
			return result;
		}
		JavaObject rootEntry = (JavaObject) rootThing;

		Factory thisFactory = (Factory) factory;
		leftFieldIdx = thisFactory.getLeftFieldIdx(rootEntry);
		rightFieldIdx = thisFactory.getRightFieldIdx(rootEntry);
		keyFieldIdx = thisFactory.getKeyFieldIdx(rootEntry);
		valueFieldIdx = thisFactory.getValueFieldIdx(rootEntry);

		result += getDeepEntrySize(rootThing, null);
		return result;
	}

	private int getDeepEntrySize(JavaThing entryThing, JavaThing[] entryFields) {
		if (entryThing == null || entryThing instanceof UnresolvedObject) {
			return 0;
		}

		JavaObject entry = (JavaObject) entryThing;
		if (entry.isVisitedAsCollectionImpl()) {
			// Just in case there is a corruption
			return 0;
		}
		entry.setVisitedAsCollectionImpl();

		// Reuse fields to reduce GC pressure
		entryFields = entry.getFields(entryFields);

		// It looks like in some heap dumps, an instance of TreeMap$Node may be
		// reachable from *outside* its encapsulating TreeMap. So far I observed it
		// once (with default2011-10-25_11_27_00.hprof), and the ref chain looked
		// like something really outside a normal TreeMap. If such a node is reached
		// first, it ends up with impl-inclusive size updated for its own class,
		// so if we subsequently update impl-inclusive size for TreeMap here, the
		// sum total of inclusive sizes for all classess will end up being greater
		// than the sum total of shallow sizes. So here we take measures to fix
		// the sizes properly
		int entrySize = entry.getSize();
		if (entry.isVisited() && entry.getClazz().getSnapshot().isCalculatingStats()) {
			entry.getClazz().updateInclusiveInstanceSize(-entrySize);
		}
		int result = entrySize;
		if (((Factory) factory).isJRockitVersion) {
			JavaObjectArray keys = (JavaObjectArray) entryFields[keyFieldIdx];
			keys.setVisitedAsCollectionImpl();
			result += keys.getSize();
			JavaObjectArray values = (JavaObjectArray) entryFields[valueFieldIdx];
			values.setVisitedAsCollectionImpl();
			result += values.getSize();
		}

		JavaThing leftThing = entryFields[leftFieldIdx];
		JavaThing rightThing = entryFields[rightFieldIdx];
		result += getDeepEntrySize(leftThing, entryFields);
		result += getDeepEntrySize(rightThing, entryFields);
		return result;
	}

	@Override
	public void iterateList(ListIteratorCallback cb) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void iterateMap(MapIteratorCallback cb) {
		JavaThing rootThing = col.getField(factory.rootFieldIdx);
		if (rootThing == null || !(rootThing instanceof JavaObject)) {
			return;
		}
		JavaObject rootEntry = (JavaObject) rootThing;

		Factory thisFactory = (Factory) factory;
		keyFieldIdx = thisFactory.getKeyFieldIdx(rootEntry);
		valueFieldIdx = thisFactory.getValueFieldIdx(rootEntry);
		leftFieldIdx = thisFactory.getLeftFieldIdx(rootEntry);
		rightFieldIdx = thisFactory.getRightFieldIdx(rootEntry);

		scanEntry(rootEntry, null, cb);
	}

	private void scanEntry(JavaThing entryThing, JavaThing[] entryFields, MapIteratorCallback cb) {
		if (entryThing == null || entryThing instanceof UnresolvedObject) {
			return;
		}
		JavaObject entry = (JavaObject) entryThing;
		if (!cb.scanImplementationObject(entry)) {
			return;
		}

		entryFields = entry.getFields(entryFields);
		JavaThing keyThing = entryFields[keyFieldIdx];
		JavaThing valueThing = entryFields[valueFieldIdx];
		JavaHeapObject key = null, value = null;

		if (((Factory) factory).isJRockitVersion) {
			JavaObjectArray keysArr = (keyThing != null && keyThing instanceof JavaObjectArray)
					? (JavaObjectArray) keyThing : null;
			JavaObjectArray valuesArr = (valueThing != null && valueThing instanceof JavaObjectArray)
					? (JavaObjectArray) valueThing : null;
			JavaHeapObject[] keys = EMPTY_OBJ_ARRAY;
			if (keysArr != null) {
				if (!cb.scanImplementationObject(keysArr)) {
					return;
				}
				keys = keysArr.getElements();
			}
			JavaHeapObject[] values = EMPTY_OBJ_ARRAY;
			if (valuesArr != null) {
				if (!cb.scanImplementationObject(valuesArr)) {
					return;
				}
				values = valuesArr.getElements();
			}
			int maxLen = (keys.length > values.length) ? keys.length : values.length;

			for (int i = 0; i < maxLen; i++) {
				key = (i < keys.length) ? keys[i] : null;
				value = (i < values.length) ? values[i] : null;
				if (!cb.scanMapEntry(key, value)) {
					return;
				}
			}

		} else {
			if (keyThing != null && keyThing instanceof JavaHeapObject) {
				key = (JavaHeapObject) keyThing;
			}
			if (valueThing != null && valueThing instanceof JavaHeapObject) {
				value = (JavaHeapObject) valueThing;
			}

			if (!cb.scanMapEntry(key, value)) {
				return;
			}
		}

		JavaThing leftThing = entryFields[leftFieldIdx];
		JavaThing rightThing = entryFields[rightFieldIdx];
		scanEntry(leftThing, entryFields, cb);
		scanEntry(rightThing, entryFields, cb);
	}

	@Override
	protected int getSizeByCountingElements() {
		throw new UnsupportedOperationException("Should never be called");
	}

	static class Factory extends AbstractLinkedCollectionDescriptor.Factory {

		private int leftFieldIdx = -1, rightFieldIdx = -1;
		private boolean isJRockitVersion, keyFieldIdxInitialized;

		Factory(JavaClass clazz, JavaClass[] implClasses) {
			super(clazz, true, "size", "root", "value", implClasses);
		}

		private Factory(JavaClass clazz, AbstractCollectionDescriptor.Factory superclassFactory) {
			super(clazz, superclassFactory);
		}

		@Override
		AbstractCollectionDescriptor.Factory cloneForSubclass(JavaClass clazz) {
			return new Factory(clazz, this);
		}

		@Override
		CollectionInstanceDescriptor get(JavaObject col) {
			return new TreeMapDescriptor(col, this);
		}

		@Override
		protected int getKeyFieldIdx(JavaObject entry) {
			if (keyFieldIdxInitialized) {
				return super.getKeyFieldIdx(entry);
			}

			keyFieldIdxInitialized = true;

			// Check if this is a JRockit version, that has Object keys[] instead of Object key
			if (entry.getField("key") != null) {
				// Normal version, do the usual thing
				isJRockitVersion = false;
				return super.getKeyFieldIdx(entry);
			}

			isJRockitVersion = true;
			super.setMapKeyFieldName("keys");
			super.setValueFieldName("values");
			return super.getKeyFieldIdx(entry);
		}

		protected int getLeftFieldIdx(JavaObject entry) {
			if (leftFieldIdx == -1) {
				leftFieldIdx = entry.getClazz().getInstanceFieldIndex("left");
			}
			return leftFieldIdx;
		}

		protected int getRightFieldIdx(JavaObject entry) {
			if (rightFieldIdx == -1) {
				rightFieldIdx = entry.getClazz().getInstanceFieldIndex("right");
			}
			return rightFieldIdx;
		}
	}
}
