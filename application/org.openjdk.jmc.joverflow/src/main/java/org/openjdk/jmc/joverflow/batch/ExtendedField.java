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
package org.openjdk.jmc.joverflow.batch;

import java.util.List;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl;

/**
 * Extended field element. Used internally to temporarily represent a field referencing a cluster of
 * problematic objects. The field can reference it directly, or via one or more intermediate
 * compound objects (collection, array, etc.) So this class provides an API of RefChainElement, but
 * internally is more like a reference chain, and is eventually converted into one.
 */
public class ExtendedField implements RefChainElement {

	private final List<RefChainElement> fieldAndExt;
	private final String valueAsString;

	@Override
	public JavaClass getJavaClass() {
		return fieldAndExt.get(0).getJavaClass();
	}

	@Override
	public RefChainElement getReferer() {
		return null;
	}

	ExtendedField(List<RefChainElement> fieldAndExt) {
		this.fieldAndExt = fieldAndExt;
		this.valueAsString = valueAsString();
	}

	public RefChainElement toReferenceChain() {
		RefChainElement referer = null;
		for (RefChainElement curElement : fieldAndExt) {
			RefChainElement newElement;
			if (curElement instanceof RefChainElementImpl.InstanceFieldOrLinkedList) {
				RefChainElementImpl.InstanceFieldOrLinkedList field = (RefChainElementImpl.InstanceFieldOrLinkedList) curElement;
				newElement = RefChainElementImpl.createInstanceFieldOrLinkedListElementInFinalForm(field.getJavaClass(),
						field.getFieldIdx(), referer, field.isInstanceField());
			} else if (curElement instanceof RefChainElementImpl.StaticField) {
				RefChainElementImpl.StaticField field = (RefChainElementImpl.StaticField) curElement;
				newElement = RefChainElementImpl.createStaticFieldElementInFinalForm(field.getJavaClass(),
						field.getFieldIdx(), referer);
			} else if (curElement instanceof RefChainElementImpl.Collection) {
				RefChainElementImpl.Collection col = (RefChainElementImpl.Collection) curElement;
				newElement = RefChainElementImpl.createCompoundCollectionElementInFinalForm(col.getJavaClass(),
						referer);
			} else if (curElement instanceof RefChainElementImpl.Array) {
				RefChainElementImpl.Array ar = (RefChainElementImpl.Array) curElement;
				newElement = RefChainElementImpl.createCompoundArrayElementInFinalForm(ar.getJavaClass(), referer);
			} else {
				throw new RuntimeException("Unsupported ref chain element type: " + curElement.getClass());
			}
			referer = newElement;
		}
		return referer;
	}

	@Override
	public String toString() {
		return valueAsString;
	}

	private String valueAsString() {
		if (fieldAndExt.size() == 1) {
			return fieldAndExt.get(0).toString();
		}

		StringBuilder sb = new StringBuilder(32);
		for (RefChainElement ext : fieldAndExt) {
			sb.append(ext.toString());
			sb.append("-->");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object otherObj) {
		if (!(otherObj instanceof ExtendedField)) {
			return false;
		}

		ExtendedField other = (org.openjdk.jmc.joverflow.batch.ExtendedField) otherObj;
		return (this.valueAsString.equals(other.valueAsString));
	}

	@Override
	public int hashCode() {
		return valueAsString.hashCode();
	}

	@Override
	public boolean shallowEquals(Object other) {
		return equals(other);
	}

	@Override
	public int shallowHashCode() {
		return hashCode();
	}
}
