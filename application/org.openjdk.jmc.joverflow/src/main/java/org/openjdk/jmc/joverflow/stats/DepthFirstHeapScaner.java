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
package org.openjdk.jmc.joverflow.stats;

import org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;
import org.openjdk.jmc.joverflow.util.FastStack;

/**
 * The heap scanner impl-n that scans a heap dump in depth-first order starting from GC roots. It
 * takes an instance of ProblemChecker, that should implement methods that check objects given to it
 * for problems and other interesting properties.
 */
class DepthFirstHeapScaner extends HeapScaner {

	private final ProblemChecker objHandler;
	private final FastStack<JavaThing[]> fieldsOrArrayElsStack;
	private final InterimRefChainStack refChain;

	// When this is true, getNextObjToScan() tries to find the next object to
	// scan such that it's located in the heap dump close to the previously
	// scanned object. That may reduce the number of page swaps in CachedReadBuffer
	// by 3-5%, thus reducing the number of disk reads and ultimately time.
	// However, for smaller heap dumps this may result in a significant increase
	// in CPU cycles with no benefit. We probably need to turn this option on
	// adaptively, when it becomes clear that page swapping takes too much time.
	private boolean optimizeForLocality = false;

	DepthFirstHeapScaner(Snapshot snapshot, ProblemChecker objHandler, ProblemRecorder problemRecorder,
			CollectionDescriptors colDescriptors) {
		super(snapshot, new InterimRefChainStack(problemRecorder, colDescriptors));
		this.refChain = (InterimRefChainStack) getRefChain();
		this.objHandler = objHandler;
		fieldsOrArrayElsStack = new FastStack<>(256);
	}

	/**
	 * Scans all of the objects reachable from obj in depth-first order. We don't use recursion here
	 * (which is easier to write and understand), because for long reference chains, such as those
	 * found in linked lists, deep recursion can lead to StackOverflowError.
	 */
	@Override
	protected void scanObjectsFromRootObj(JavaHeapObject obj) {
		while (obj != null) {
			if (obj.setVisitedIfNot()) {
				currentProcessedObjNo++;
				if (cancelled) {
					throw new HprofParsingCancelledException.Runtime();
				}

				JavaClass clazz = obj.getClazz();
				refChain.push(obj);
				if (clazz.isString()) {
					objHandler.handleString((JavaObject) obj);
					refChain.pop();
				} else {
					if (obj instanceof JavaObject) {
						JavaObject javaObj = (JavaObject) obj;
						// Note that in principle the call below could be replaced with the variant
						// with false boolean arg, which will initialize only reference fields. This
						// would speed up field parsing in JavaObject and field scanning below, but
						// may cause problems in other parts of the system, that expect JavaObjects
						// to have all fields set properly - like checking for all-zero fields.
						JavaThing[] fields = javaObj.getFields();

						objHandler.handleInstance(javaObj, fields);

						// Nullify various fields like those for auxiliary linked list in LinkedHashMap,
						// so that scanObjectFields does not have problems with long lists, etc.
						int[] bannedFieldIndices = clazz.getBannedFieldIndices();
						if (bannedFieldIndices != null) {
							for (int bannedFieldIdx : bannedFieldIndices) {
								fields[bannedFieldIdx] = null;
							}
						}

						refChain.pushIndexContainer(new TwoHandIndexContainer());
						fieldsOrArrayElsStack.push(fields);
					} else if (obj instanceof JavaClass) {
						JavaThing[] staticFields = ((JavaClass) obj).getStaticValues();
						refChain.pushIndexContainer(new TwoHandIndexContainer());
						fieldsOrArrayElsStack.push(staticFields);
					} else if (obj instanceof JavaObjectArray) {
						JavaObjectArray objArray = (JavaObjectArray) obj;
						JavaHeapObject[] elements = objArray.getElements();
						objHandler.handleObjectArray(objArray, elements);

						refChain.pushIndexContainer(new TwoHandIndexContainer());
						fieldsOrArrayElsStack.push(elements);
					} else {
						objHandler.handleValueArray((JavaValueArray) obj);
						refChain.pop();
					}
				}
			}

			// Now determine the next object to scan
			obj = getNextObjToScan(obj);
		}
	}

	/**
	 * Most of the complexity in this method is due to experimental functionality where we try to
	 * find next object to scan that is close enough to the current object inside the hep dump, in
	 * order to minimize our disk cache misses.
	 */
	private JavaHeapObject getNextObjToScan(JavaHeapObject oldObj) {
		long oldObjOfsInFile = -1;
		JavaHeapObject obj = null;

		while (!fieldsOrArrayElsStack.isEmpty()) {
			JavaThing[] fieldsOrElements = fieldsOrArrayElsStack.peek();
			TwoHandIndexContainer curIdxContainer = (TwoHandIndexContainer) refChain.getCurrentIndexContainer();
			int nextIdx = curIdxContainer.incrementAndGetBase();
			while (nextIdx < fieldsOrElements.length) {
				JavaThing objThing = fieldsOrElements[nextIdx];
				// Ignore null, primitive and already visited fields
				if (objThing != null && objThing instanceof JavaHeapObject) {
					obj = (JavaHeapObject) objThing;
					if (!obj.isVisited()) {
						if (optimizeForLocality && (obj instanceof JavaLazyReadObject)) {
							oldObjOfsInFile = (oldObj instanceof JavaLazyReadObject)
									? ((JavaLazyReadObject) oldObj).getObjOfsInFile() : -1;
							if (oldObjOfsInFile != -1) {
								curIdxContainer.setBase(nextIdx - 1);
								break; // We'll attempt to look for a closer located object
							}
						}

						curIdxContainer.setBase(nextIdx);
						curIdxContainer.set(nextIdx);
						return obj;
					} else {
						obj = null;
					}
				}
				nextIdx++;
			}

			if (obj == null) { // Fields or elements of the top object exhausted
				fieldsOrArrayElsStack.pop();
				refChain.pop2(); // Pop the index holder and object
				continue;
			}

			// Look through the next few elements to see if any of them is closer to old obj
			long curObjOfsInFile = ((JavaLazyReadObject) obj).getObjOfsInFile();
			long minDistance = Math.abs(curObjOfsInFile - oldObjOfsInFile);
			int bestIdx = nextIdx;
			int nCheckedFields = 0;
			nextIdx++;
			while (nextIdx < fieldsOrElements.length && nCheckedFields < 8) {
				JavaThing objThing = fieldsOrElements[nextIdx];
				if (objThing != null && objThing instanceof JavaHeapObject) {
					obj = (JavaHeapObject) objThing;
					if (!obj.isVisited()) {
						if (!(obj instanceof JavaLazyReadObject)) {
							curIdxContainer.set(nextIdx);
							return obj;
						}
						nCheckedFields++;
						curObjOfsInFile = ((JavaLazyReadObject) obj).getObjOfsInFile();
						long distance = Math.abs(curObjOfsInFile - oldObjOfsInFile);
						if (distance < minDistance) {
							bestIdx = nextIdx;
							minDistance = distance;
						}
					}
				}
				nextIdx++;
			}

			curIdxContainer.set(bestIdx);
			return (JavaHeapObject) fieldsOrElements[bestIdx];
		}

		return obj;
	}

}
