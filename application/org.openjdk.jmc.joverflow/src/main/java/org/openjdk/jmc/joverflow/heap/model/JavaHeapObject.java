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

import org.openjdk.jmc.joverflow.util.MiscUtils;
import org.openjdk.jmc.joverflow.util.StringInterner;

/**
 * Represents an object that's allocated out of the Java heap. It can be a JavaClass, a
 * JavaObjectArray, a JavaValueArray or a JavaObject.
 */
public abstract class JavaHeapObject extends JavaThing {

	/**
	 * Returns true if this object has been visited during detailed analysis. Uses the tags field in
	 * the object. After the object is marked visited, it cannot be scanned again. Depth-first and
	 * breadth-first heap scan algorithms fundamentally depend on this.
	 * <p>
	 * Long ago this info was kept in a separate 'IdentitySetOfObjects visited'. Replacing it with a
	 * bit directly in the objects resulted in about 15% improvement in both performance and memory
	 * usage.
	 */
	public abstract boolean isVisited();

	/** @see #isVisited() */
	public abstract void setVisited();

	/**
	 * Sets this object's "visited" tag. Returns true if it has not been set before, and false if
	 * this object has already been visited.
	 */
	public abstract boolean setVisitedIfNot();

	public abstract JavaClass getClazz();

	/**
	 * Returns the object's global index. This index is not equal to the object id returned by
	 * {@link #readId()}. Each JavaLazyReadObject (representing a Java instance, object array or
	 * primitive array) has a unique index that is &gt; 0. Each JavaClass (that represents a Java
	 * class) has a unique index that's &lt;= 0. The value returned for JavaClass is an index into the
	 * internal class list, and thus increments by one. The value returned for a JavaHeapObject is a
	 * position in the internal compact table, and increments by 3 or 4. In contrast, the long
	 * object id normally increments by comparatively large numbers.
	 */
	public abstract int getGlobalObjectIndex();

	/**
	 * Returns the object's heap ID (which is the value of that's object address in the machine
	 * memory at the time when the heap dump was taken; don't confuse it with the "internal id"
	 * returned by {@link JavaLazyReadObject#getInternalId()} - the latter is the object's offset in
	 * heap dump file. Note: for non-class objects, this is done by reading the ID from the mmapped
	 * file!
	 */
	public abstract long readId();

	@Override
	public String idAsString() {
		return StringInterner.internString(getClazz().getHumanFriendlyName() + '@' + MiscUtils.toHex(readId()));
	}

	@Override
	public String toString() {
		return idAsString();
	}

	/**
	 * Tell the visitor about all of the objects we refer to.
	 */
	public void visitReferencedObjects(JavaHeapObjectVisitor v) {
		v.visit(getClazz());
	}

	@Override
	public boolean isHeapAllocated() {
		return true;
	}
}
