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

import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.descriptors.WeakHashMapDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.util.SimpleIdentitySet;

/**
 * Inspects an instance of WeakHashMap or its subclass for a special problem: elements that point
 * back at keys in the same map. That breaks the weakness property and prevents entries where keys
 * are not referenced from anywhere (except their own elements) from being GCed.
 */
class WeakMapHandler {
	private final WeakHashMapDescriptor colDesc;

	/**
	 * Returns an instance of WeakMapHandler if the given collection is a WeakHashMap or its
	 * subclass. Otherwise, returns null.
	 */
	static WeakMapHandler createInstance(CollectionInstanceDescriptor colDesc) {
		JavaClass clazz = colDesc.getClassDescriptor().getClazz();
		if (!clazz.isOrSubclassOf(Constants.WEAK_HASH_MAP)) {
			return null;
		}
		if (colDesc.getNumElements() == 0) {
			return null;
		}

		return new WeakMapHandler(colDesc);
	}

	private WeakMapHandler(CollectionInstanceDescriptor colDesc) {
		this.colDesc = (WeakHashMapDescriptor) colDesc;
	}

	/**
	 * Checks if WeakHashMap associated with this object has the problem. If so, returns a
	 * combination of the overhead for problematic entries, and a sample value type/field, like
	 * "Foo.bar", for an object that references a key in this table.
	 */
	Result calculateOverhead() {
		JavaHeapObject keysAndValues[][] = colDesc.getKeysAndValues();
		if (keysAndValues[0].length == 0) {
			return null;
		}

		JavaHeapObject[] keys = keysAndValues[0];
		SimpleIdentitySet<JavaHeapObject> keySet = new SimpleIdentitySet<>(keys.length);
		for (JavaHeapObject key : keys) {
			if (key != null) {
				keySet.add(key);
			}
		}
		// Important: this operation can only be performed if the empty check above is done!
		// Let's comment it out. Safety is more important than a little saved memory
		// keysAndValues[0] = null;   // Help the GC
		JavaHeapObject[] values = keysAndValues[1];

		int ovhd = 0;
		String valueTypeAndFieldSample = null;
		for (JavaHeapObject value : values) {
			if (value instanceof JavaObject) {
				JavaObject valueObj = (JavaObject) value;
				// A weak reference back to key is ok
				if (valueObj.getClazz().isOrSubclassOf(Constants.WEAK_REFERENCE)) {
					break;
				}

				JavaThing[] fields = valueObj.getFields(false);
				for (int i = 0; i < fields.length; i++) {
					JavaThing fieldThing = fields[i];
					if (fieldThing == null || !(fieldThing instanceof JavaHeapObject)) {
						continue;
					}
					JavaHeapObject field = (JavaHeapObject) fieldThing;
					if (keySet.contains(field)) {
						ovhd += field.getSize() + valueObj.getSize();
						if (valueTypeAndFieldSample == null) {
							valueTypeAndFieldSample = getStringForValueAndField(valueObj, i);
						}
						break;
					}
				}
			} else if (value instanceof JavaObjectArray) {
				JavaHeapObject[] elements = ((JavaObjectArray) value).getElements();
				for (JavaHeapObject element : elements) {
					if (element == null) {
						continue;
					}
					// A weak reference back to key is ok
					if (element.getClazz().isOrSubclassOf(Constants.WEAK_REFERENCE)) {
						continue;
					}

					if (keySet.contains(element)) {
						ovhd += element.getSize() + value.getSize();
						if (valueTypeAndFieldSample == null) {
							valueTypeAndFieldSample = value.getClazz().getName();
						}
						break;
					}
				}
			}
		}

		if (ovhd > 0) {
			return new Result(ovhd, valueTypeAndFieldSample);
		} else {
			return null;
		}
	}

	private static String getStringForValueAndField(JavaObject value, int fieldIdx) {
		JavaClass clazz = value.getClazz();
		return clazz.getName() + '.' + clazz.getFieldForInstance(fieldIdx).getName();
	}

	static class Result {
		final int overhead;
		final String valueTypeAndFieldSample;

		Result(int overhead, String valueTypeAndFieldSample) {
			this.overhead = overhead;
			this.valueTypeAndFieldSample = valueTypeAndFieldSample;
		}
	}
}
