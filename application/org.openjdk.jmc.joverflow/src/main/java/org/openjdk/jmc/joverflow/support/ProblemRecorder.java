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

import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;

/**
 * JOverflow core heap scanning code invokes methods of this interface when it comes across
 * problematic objects such as empty collections or duplicate strings. The implementation is
 * supposed to record these problems, likely in an aggregated form, and then provide the resulting
 * data to the user. There is an additional method that can be called by the heap scanner to report
 * a Java instance that does not have any problems. But since reporting non-problematic objects
 * requires additional time and memory and may not be needed in every scenario, that method is
 * called by the scanner only when it is configured to do so.
 */
public interface ProblemRecorder {

	/**
	 * Implementation of this method may initialize some internal data using information from the
	 * passed objects.
	 */
	public void initialize(Snapshot snapshot, HeapStats hs);

	/**
	 * Reports a problematic collection or object array class with the specified problem and
	 * overhead of specified kind, and the reference chain leading to that object from some GC root.
	 * An instance of CollectionClassDescriptor that is also passed to the method allows its
	 * implementation to find more information about the problematic collection, if needed, e.g. its
	 * implementation-inclusive size, number of elements, etc.
	 */
	public void recordProblematicCollection(
		JavaLazyReadObject col, CollectionInstanceDescriptor colDesc, Constants.ProblemKind ovhdKind, int ovhd,
		RefChainElement referer);

	/**
	 * Reports a good collection or object array, that does not have any problems, and a reference
	 * chain leading to it.
	 */
	public void recordGoodCollection(
		JavaLazyReadObject col, CollectionInstanceDescriptor colDesc, RefChainElement referer);

	/**
	 * Reports a duplicate string with the associated overhead and reference chain leading to it
	 * from some GC root. If hasDupBackingCharArray is true, the backing char array is duplicated;
	 * otherwise there are two or more String objects pointing at the same backing char array.
	 */
	public void recordDuplicateString(
		JavaObject strObj, String stringValue, int implInclusiveSize, int ovhd, boolean hasDupBackingCharArray,
		RefChainElement referer);

	/**
	 * Reports a good string, that does not have any duplicates, and a reference chain leading to
	 * it.
	 */
	public void recordNonDuplicateString(JavaObject strObj, int implInclusiveSize, RefChainElement referer);

	/**
	 * Reports a duplicate primitive array with the associated overhead and reference chain leading
	 * to it from some GC root.
	 */
	public void recordDuplicateArray(JavaValueArray ar, int ovhd, RefChainElement referer);

	/**
	 * Reports a good primitive array, that does not have any duplicates, and a reference chain
	 * leading to it from some GC root.
	 */
	public void recordNonDuplicateArray(JavaValueArray ar, RefChainElement referer);

	/**
	 * Reports a problematic instance of WeakHashMap or its subclass, that incurs the specified
	 * minimum overhead due to references from values pointing back to keys.
	 */
	public void recordWeakHashMapWithBackRefs(
		JavaObject col, CollectionInstanceDescriptor colDesc, int ovhd, String valueTypeAndFieldSample,
		RefChainElement referer);

	/**
	 * If this method returns true for the given object,
	 * {@link #recordGoodInstance(JavaObject, RefChainElement)} will be called for it next.
	 */
	public boolean shouldRecordGoodInstance(JavaObject obj);

	/**
	 * Reports a good Java instance, that does not have any problems, with the reference chain
	 * leading to it from some GC root. Will be called by the heap scanner only if previously
	 * {@link #shouldRecordGoodInstance(JavaObject)} returned true for it.
	 * <p>
	 * NOTE: currently objects that are good in principle, but belong to the implementation of some
	 * collection, such as HashMap$Entry, are not reported here. That is done in part to keep
	 * uniform our view of the heap, as it is generated by the core heap scanner, where
	 * implementation details of collections are not exposed in any way.
	 */
	public void recordGoodInstance(JavaObject obj, RefChainElement referer);
}
