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

import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;

/**
 * Provides information associated with an individual instance of some Collection class.
 */
public interface CollectionInstanceDescriptor {
	/** Returns the descriptor for this collection's class */
	CollectionClassDescriptor getClassDescriptor();

	/** Returns the number of elements in the associated collection */
	int getNumElements();

	/**
	 * Returns the size, in bytes, of this collection instance's implementation. That's the size of
	 * the collection's main object plus all of the objects implementing this collection that the
	 * main one points to, such as HashMap$Entry, etc. Obviously the size of objects contained by
	 * this collection is not factored in by this method.
	 * <p>
	 * A very important side-effect of this method is that it marks all of the collection
	 * implementation objects, such as HashMap$Entry instances, with
	 * JavaHeapObject.setVisitedAsCollectionImpl().
	 * <p>
	 * Since this method can be called repeatedly on the same descriptor instance, it should either
	 * be very fast or cache its result upon the first invocation.
	 */
	int getImplSize();

	/**
	 * If the underlying collection object is a list, iterates all its elements, calling
	 * cb.scanListElement() for each element, and cb.scanImplementationObject() for each internal
	 * implementation object, such as ArrayList.elementData[] array.
	 */
	void iterateList(ListIteratorCallback cb);

	/**
	 * If the underlying collection object is a map, iterates all its elements, calling
	 * cb.scanMapEntry() for each key-value pair, and cb.scanImplementationObject() for each
	 * internal implementation object, such as HashMap.entries[] array and HashMap$Entry objects.
	 */
	void iterateMap(MapIteratorCallback cb);

	/**
	 * If this descriptor represents a list, returns one of its elements. The goal of this method is
	 * to return the most likely type for the list's elements.
	 * <p>
	 * To obtain the result, several elements are taken randomly, and their types are compared. If
	 * all types are the same, one of the elements is returned. Otherwise, i.e. if types are not the
	 * same or the list is empty, null is returned.
	 */
	JavaHeapObject getSampleElement();

	/**
	 * If this descriptor represents a map, returns one of its key-value pairs. The goal of this
	 * method is to return the most likely types for the keys and values in this map.
	 * <p>
	 * To obtain the result, several map entries are taken randomly, and their types are compared.
	 * If all types within keys or values are the same, one of the entries is returned. Otherwise,
	 * i.e. if types are not the same or the map is empty, null is returned for key, value or both.
	 */
	JavaHeapObject[] getSampleKeyAndValue();

	/**
	 * If CollectionClassDescriptor.canDetermineModCount() returns true, this method returns the
	 * number of times this collection has been modified (usually stored in the 'modCount' field of
	 * the collection class).
	 */
	long getModCount();

	/**
	 * Returns true if this collection's class has "extra" object fields - that is, fields not
	 * directly responsible for its implementation and not otherwise handled when processing
	 * collections of this class. Such fields can be well-known, e.g. WeakHashMap.queue, which has
	 * always been there, but is not used to calculate impl-inclusive size or other collection
	 * metrics. Or they can be something we generally don't know about in advance, such as various
	 * optimizations that can be introduced in existing collection classes. An example is
	 * HashMap.frontCache, that may or may not be there depending on which version of HashMap is
	 * used. The information on extra fields is needed to properly handle them during breadth-first
	 * scan, which by default handles only known parts of collection classes and ignores other
	 * fields.
	 */
	boolean hasExtraObjFields();

	/**
	 * Given an array of this collection object's fields that has been already initialized by
	 * JavaObject.getFields(), nulls out all of the elements in it except those corresponding to
	 * "extra" object fields, as explained in {@link #hasExtraObjFields()}.
	 */
	void filterExtraObjFields(JavaThing[] fields);

	/**
	 * Provides additional methods for instances of Collection classes that are array-based
	 * internally, and where capacity can exceed the number of elements in the collection (size).
	 */
	public interface CapacityDifferentFromSize {

		/**
		 * If this collection instance is sparse, returns the overhead, in bytes, due to sparseness.
		 * Otherwise, returns -1.
		 * <p>
		 * A simple array-based collection is typically considered sparse if the number of elements
		 * in it is less than half its full capacity; the overhead would be the total size of the
		 * unused pointers in the array.
		 */
		int getSparsenessOverhead(int ptrSize);

		/**
		 * Returns the default capacity for instances of this Collection class. For example, for
		 * HashMap it's 16.
		 */
		int getDefaultCapacity();

		/**
		 * Returns the current capacity of this instance. It is greater or equal to the number of
		 * elements in the colleciton.
		 */
		int getCapacity();
	}

	/**
	 * Used for iterating elements of the collection that is a list, see
	 * {@link CollectionInstanceDescriptor#iterateList(ListIteratorCallback)}.
	 */
	public interface ListIteratorCallback {

		/**
		 * Called from inside iterateList() for each collection workload element. If the
		 * implementation returns true, it means "continue", false tells iterateList() that
		 * iterating should stop.
		 */
		boolean scanListElement(JavaHeapObject element);

		/**
		 * Called from inside iterateList() for each collection implementation object, e.g.
		 * ArrayList.elements array. If the implementation of this callback returns true, it means
		 * "continue". False signals iterateMap() that this object has already been seen, and thus
		 * iteration should either stop completely or skip some objects to ensure that this object
		 * is not visited again, i.e. endless looping is avoided.
		 */
		boolean scanImplementationObject(JavaHeapObject implObj);
	}

	/**
	 * Used for iterating elements of the collection that is a map, see
	 * {@link CollectionInstanceDescriptor#iterateMap(MapIteratorCallback)}.
	 */
	public interface MapIteratorCallback {

		/**
		 * Called from inside iterateMap() for each key-value pair in the collection. Note that
		 * existing implementations of iterateMap() work such that if one or both of objects are
		 * unresolved (usually that happens because the heap dump is corrupted), null is used
		 * instead of such an object. If the implementation of this callback returns true, it means
		 * "continue", false tells iterateMap() that iterating should stop.
		 */
		boolean scanMapEntry(JavaHeapObject key, JavaHeapObject value);

		/**
		 * Called from inside iterateMap() for each collection implementation object, e.g.
		 * HashMap.table array or HashMap$Entry object. If the implementation of this callback
		 * returns true, it means "continue". False signals iterateMap() that this object has
		 * already been seen, and thus iteration should either stop completely or skip some objects
		 * to ensure that this object is not visited again, i.e. endless looping is avoided.
		 */
		boolean scanImplementationObject(JavaHeapObject implObj);
	}
}
