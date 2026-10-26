/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.agent.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmc.agent.Method;
import org.openjdk.jmc.agent.TransformDescriptor;

/**
 * A {@link TransformDescriptor} for one of the fixed, hidden collection resize instrumentation
 * points. Unlike the XML-driven JFR probes, these are not user configurable: the target class,
 * resize method, backing-array field and size accessor are all baked in (see the factory methods).
 * <p>
 * The {@code Transformer} recognizes this subtype and routes it to the dedicated collection resize
 * class visitor rather than the generic JFR path.
 */
public final class CollectionTransformDescriptor extends TransformDescriptor {

	private final String arrayFieldName;
	private final String arrayFieldDescriptor;
	private final String sizeAccessorName;
	private final boolean sizeIsMethod;

	private CollectionTransformDescriptor(String id, String className, Method method, String arrayFieldName,
			String arrayFieldDescriptor, String sizeAccessorName, boolean sizeIsMethod) {
		super(id, className, method, Collections.emptyMap());
		this.arrayFieldName = arrayFieldName;
		this.arrayFieldDescriptor = arrayFieldDescriptor;
		this.sizeAccessorName = sizeAccessorName;
		this.sizeIsMethod = sizeIsMethod;
	}

	/**
	 * {@link java.util.HashMap#resize()}. Also covers {@link java.util.LinkedHashMap},
	 * {@link java.util.HashSet} and {@link java.util.LinkedHashSet} since they reuse (or are backed
	 * by) {@code HashMap.resize()}.
	 */
	public static CollectionTransformDescriptor hashMap() {
		return new CollectionTransformDescriptor("collectiontracking.hashmap", "java/util/HashMap", //$NON-NLS-1$ //$NON-NLS-2$
				new Method("resize", "()[Ljava/util/HashMap$Node;"), "table", "[Ljava/util/HashMap$Node;", "size", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				false);
	}

	/**
	 * {@link java.util.ArrayList}'s internal {@code grow(int)}.
	 */
	public static CollectionTransformDescriptor arrayList() {
		return new CollectionTransformDescriptor("collectiontracking.arraylist", "java/util/ArrayList", //$NON-NLS-1$ //$NON-NLS-2$
				new Method("grow", "(I)[Ljava/lang/Object;"), "elementData", "[Ljava/lang/Object;", "size", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	/**
	 * {@link java.util.Vector}'s internal {@code grow(int)}. Note the size field is {@code
	 * elementCount}.
	 */
	public static CollectionTransformDescriptor vector() {
		return new CollectionTransformDescriptor("collectiontracking.vector", "java/util/Vector", //$NON-NLS-1$ //$NON-NLS-2$
				new Method("grow", "(I)[Ljava/lang/Object;"), "elementData", "[Ljava/lang/Object;", "elementCount", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				false);
	}

	/**
	 * {@link java.util.Hashtable#rehash()}. The size field is {@code count} and the backing array
	 * is {@code table}.
	 */
	public static CollectionTransformDescriptor hashtable() {
		return new CollectionTransformDescriptor("collectiontracking.hashtable", "java/util/Hashtable", //$NON-NLS-1$ //$NON-NLS-2$
				new Method("rehash", "()V"), "table", "[Ljava/util/Hashtable$Entry;", "count", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	/**
	 * {@link java.util.PriorityQueue}'s internal {@code grow(int)}. The backing array is {@code
	 * queue}.
	 */
	public static CollectionTransformDescriptor priorityQueue() {
		return new CollectionTransformDescriptor("collectiontracking.priorityqueue", "java/util/PriorityQueue", //$NON-NLS-1$ //$NON-NLS-2$
				new Method("grow", "(I)V"), "queue", "[Ljava/lang/Object;", "size", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	/**
	 * {@link java.util.ArrayDeque}'s internal {@code grow(int)}. ArrayDeque has no size field, so
	 * the size is read via the {@code size()} method.
	 */
	public static CollectionTransformDescriptor arrayDeque() {
		return new CollectionTransformDescriptor("collectiontracking.arraydeque", "java/util/ArrayDeque", //$NON-NLS-1$ //$NON-NLS-2$
				new Method("grow", "(I)V"), "elements", "[Ljava/lang/Object;", "size", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	/**
	 * @return all tracked collection instrumentation points.
	 */
	public static List<CollectionTransformDescriptor> all() {
		return Arrays.asList(hashMap(), arrayList(), vector(), hashtable(), priorityQueue(), arrayDeque());
	}

	/**
	 * @return the name of the backing-array field whose length is the collection capacity.
	 */
	public String getArrayFieldName() {
		return arrayFieldName;
	}

	/**
	 * @return the type descriptor of the backing-array field.
	 */
	public String getArrayFieldDescriptor() {
		return arrayFieldDescriptor;
	}

	/**
	 * @return the name of the field or (no-arg, int-returning) method used to read the collection
	 *         size (entry count).
	 */
	public String getSizeAccessorName() {
		return sizeAccessorName;
	}

	/**
	 * @return {@code true} if the size accessor is a method, {@code false} if it is a field.
	 */
	public boolean isSizeMethod() {
		return sizeIsMethod;
	}
}
