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

/**
 */
public interface Constants {
	// Collection classes
	String HASH_MAP = "java.util.HashMap";
	String LINKED_HASH_MAP = "java.util.LinkedHashMap";
	String HASH_SET = "java.util.HashSet";
	String LINKED_HASH_SET = "java.util.LinkedHashSet";
	String HASHTABLE = "java.util.Hashtable";
	String ARRAY_LIST = "java.util.ArrayList";
	String VECTOR = "java.util.Vector";
	String CONCURRENT_HASH_MAP = "java.util.concurrent.ConcurrentHashMap";
	String WEAK_HASH_MAP = "java.util.WeakHashMap";
	String STACK = "java.util.StaNonFinalck";
	String TREE_MAP = "java.util.TreeMap";
	String LINKED_LIST = "java.util.LinkedList";
	String IDENTITY_HASH_MAP = "java.util.IdentityHashMap";
	String ARRAY_BLOCKING_QUEUE = "java.util.concurrent.ArrayBlockingQueue";
	String ARRAY_DEQUE = "java.util.ArrayDeque";
	String PROPERTIES = "java.util.Properties";
	String ATTRIBUTE_LIST = "javax.management.AttributeList";
	String CONCURRENT_LINKED_QUEUE = "java.util.concurrent.ConcurrentLinkedQueue";
	String COPY_ON_WRITE_ARRAY_LIST = "java.util.concurrent.CopyOnWriteArrayList";
	String COPY_ON_WRITE_ARRAY_SET = "java.util.concurrent.CopyOnWriteArraySet";
	String PRIORITY_QUEUE = "java.util.PriorityQueue";

	// Collection implementation detail classes
	String HASH_MAP_ENTRY = "java.util.HashMap$Entry";
	String LINKED_HASH_MAP_ENTRY = "java.util.LinkedHashMap$Entry";
	String HASHTABLE_ENTRY = "java.util.Hashtable$Entry";
	String CONCURRENT_HASH_MAP_SEGMENT = "java.util.concurrent.ConcurrentHashMap$Segment";
	String CONCURRENT_HASH_MAP_ENTRY = "java.util.concurrent.ConcurrentHashMap$HashEntry";
	String CONCURRENT_HASH_MAP_NODE = "java.util.concurrent.ConcurrentHashMap$Node";
	String WEAK_HASH_MAP_ENTRY = "java.util.WeakHashMap$Entry";
	String TREE_MAP_ENTRY = "java.util.TreeMap$Entry";
	String TREE_MAP_NODE = "java.util.TreeMap$Node"; // Probably in older JDK versions
	String LINKED_LIST_ENTRY = "java.util.LinkedList$Entry"; // Before JDK7-update-something (2?)
	String LINKED_LIST_NODE = "java.util.LinkedList$Node"; // After JDK7-update-something (2?)
	String CONCURRENT_LINKED_QUEUE_NODE = "java.util.concurrent.ConcurrentLinkedQueue$Node";

	// Other useful classes
	String JAVA_LANG_OBJECT = "java.lang.Object";
	String JAVA_LANG_STRING = "java.lang.String";
	String WEAK_REFERENCE = "java.lang.ref.WeakReference";
	String OBJECT_ARRAY = "[Ljava.lang.Object;";

	String CHAR_ARRAY = "[C";

	// JVM in-memory pointer sizes, in bytes, in different modes
	static final int POINTER_SIZE_IN_32BIT_MODE = 4;
	static final int NARROW_POINTER_SIZE_IN_64BIT_MODE = 4;
	static final int WIDE_POINTER_SIZE_IN_64BIT_MODE = 8;

	// JVM in-memory object header sizes, in bytes, for various JVMs
	static final int STANDARD_32BIT_OBJ_HEADER_SIZE = 8;
	static final int HOTSPOT_64BIT_NARROW_REF_OBJ_HEADER_SIZE = 12;
	static final int HOTSPOT_64BIT_WIDE_REF_OBJ_HEADER_SIZE = 16;
	static final int JROCKIT_OBJ_HEADER_SIZE = 8;

	/**
	 * We assume that objects in memory are aligned at the byte granularity below. The value may be
	 * different if compressed references are used. However, it should be rare in practice - it
	 * requires an explicit JVM otpion -XX:ObjectAlignmentInBytes=<n>, and it makes sense to use it
	 * only to enable narrow pointers for heap sizes larger than ~32GB.
	 */
	static final int DEFAULT_OBJECT_ALIGNMENT_IN_MEMORY = 8;

	/**
	 * All problems that we recognize for collections and standalone arrays. Some problems can occur
	 * in both collections and arrays; others are specific to only one kind of data structures.
	 */
	static enum ProblemKind {
		/**
		 * Collection or standalone array that contains no elements, for which we cannot determine
		 * whether it has been used (i.e. whether it previously contained elements that have been
		 * deleted subsequently). For arrays that's always the case; for collections it should not
		 * be the case unless some collection doesn't have the 'modCount' field.
		 */
		EMPTY,
		/**
		 * Empty collection that has never contained any elements (because its modCount == 0)
		 */
		EMPTY_UNUSED,
		/**
		 * Empty collection that previously contained some elements (because its modCount != 0)
		 */
		EMPTY_USED,
		/**
		 * Array-based collection of default or smaller capacity, has less than half slots occupied
		 */
		SPARSE_SMALL,
		/**
		 * Array-based collection of larger than default capacity, has less than half slots occupied
		 */
		SPARSE_LARGE,
		/**
		 * Standalone array has less than half slots occupied
		 */
		SPARSE_ARRAY,
		/**
		 * Collection or standalone array contains boxed numbers
		 */
		BOXED,
		/**
		 * Standalone array of length 0
		 */
		LENGTH_ZERO,
		/**
		 * Standalone array of length 1
		 */
		LENGTH_ONE,
		/**
		 * Standalone primitive array of types short[], char[], int[] or long[] where some of the
		 * high bytes are unused in each element
		 */
		UNUSED_HI_BYTES,
		/**
		 * A WeakHashMap or subclass, where elements point back to keys
		 */
		WEAK_MAP_WITH_BACK_REFS,
		/**
		 * An array of subarrays, where the outer dimension is bigger than inner
		 */
		BAR,
		/**
		 * Long zero-tail - a primitive array that ends with a series of zeros that is half the
		 * array's length or longer
		 */
		LZT,
		/**
		 * Small collections, with impl overhead too big compared to workload. They can, in
		 * principle, be replaced with arrays (or pairs of arrays for maps)
		 */
		SMALL
	}

// FIXME: Unused arrays that in either case should not be exposed. Consider if they are useful in any way or if they can be removed. 
//	/**
//	 * Kinds of problems that collections can have. Note that sparseness and "vertical bar" shape
//	 * are only defined for array-based collections.
//	 */
//	static ProblemKind[] COL_SPECIFIC_PROBLEM_KINDS = new ProblemKind[] {ProblemKind.EMPTY_UNUSED,
//			ProblemKind.EMPTY_USED, ProblemKind.SPARSE_SMALL, ProblemKind.SPARSE_LARGE, ProblemKind.BOXED,
//			ProblemKind.BAR, ProblemKind.SMALL, ProblemKind.EMPTY};
//
//	/** Kinds of problems that standalone object arrays can have */
//	static ProblemKind[] OBJ_ARRAY_SPECIFIC_PROBLEM_KINDS = new ProblemKind[] {ProblemKind.LENGTH_ZERO,
//			ProblemKind.LENGTH_ONE, ProblemKind.EMPTY, ProblemKind.SPARSE_ARRAY, ProblemKind.BOXED, ProblemKind.BAR};
//
//	/** Kinds of problems that standalone primitive arrays can have */
//	static ProblemKind[] VALUE_ARRAY_SPECIFIC_PROBLEM_KINDS = new ProblemKind[] {ProblemKind.LENGTH_ZERO,
//			ProblemKind.LENGTH_ONE, ProblemKind.EMPTY, ProblemKind.LZT};

	/** Maximum size for small collections, see ProblemKind.SMALL */
	int SMALL_COL_MAX_SIZE = 4;

}
