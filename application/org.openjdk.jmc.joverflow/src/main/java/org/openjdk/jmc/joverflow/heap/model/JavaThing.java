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
package org.openjdk.jmc.joverflow.heap.model;

/**
 * Represents a java "Thing". A thing is anything that can be the value of a field. This includes
 * JavaHeapObject, JavaObjectRef, and JavaValue.
 */
public abstract class JavaThing {

	protected JavaThing() {
	}

	/**
	 * Are we the same type as other?
	 *
	 * @see JavaObject#isSameTypeAs(JavaThing)
	 */
	public boolean isSameTypeAs(JavaThing other) {
		return getClass() == other.getClass();
	}

	/**
	 * Returns true iff this represents a heap-allocated object
	 */
	abstract public boolean isHeapAllocated();

	/**
	 * Returns the size of this object, in bytes, including VM overhead. For primitive types,
	 * returns the size of the value in memory, e.g. 1 for boolean, 4 for int, etc.
	 */
	abstract public int getSize();

	/**
	 * Returns implementation-inclusive size for this object. Currently, this size is different
	 * (higher than) getSize() value for known Collections and Strings. For collections, it is the
	 * size of the object itself plus all of its internal implementation objects, such as
	 * HashMap$Entry - but not the size of the "workload", i.e. collection elements. For Strings,
	 * it's the size of the object itself plus the size of its char[] array.
	 * <p>
	 * Note that unlike {@link JavaClass#getTotalInclusiveInstanceSize()} this calculates only the
	 * "local" implementation-inclusive size for the given object. Thus, for example, it will return
	 * the same size as getSize() when called for a char[] array that belongs to some String. That
	 * is in contrast with the total inclusive size in JavaClass, which is 0 for all char[] arrays
	 * that belong to Strings.
	 * <p>
	 * Also note that this calculation may be relatively expensive, as it may require traversing all
	 * of the implementation objects for a given collection.
	 */
	public abstract int getImplInclusiveSize();

	/**
	 * Returns a string that uniquely identifies this thing. For objects, it's typically a
	 * combination of class name and numeric object ID (its memory address). For primitive values,
	 * it's the value itself.
	 */
	abstract public String idAsString();

	/**
	 * Returns a human-readable string representation of the value of this thing
	 */
	abstract public String valueAsString();

	// Use Comparator instead of implementing Comparable if sorting is needed 
//	/**
//	 * Compare our string representation to other's
//	 *
//	 * @see java.lang.String#compareTo(String)
//	 */
//	public int compareTo(JavaThing other) {
//		return toString().compareTo(other.toString());
//	}
}
