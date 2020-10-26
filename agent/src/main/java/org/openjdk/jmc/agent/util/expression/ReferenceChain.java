/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.util.expression;

import org.objectweb.asm.Type;

import java.util.LinkedList;
import java.util.List;

/**
 * A ReferenceChain instance presents a field reference expression. eg.
 * <code>OuterClass.this.field.STATIC_FIELD</code> is a reference chain consisting elements: a
 * qualified-this reference and two field reference (<code>field</code> and
 * <code>STATIC_FIELD</code>).
 */
public final class ReferenceChain {
	private final Class<?> callerClass;
	private final List<ReferenceChainElement> references;

	/**
	 * @param callerClass
	 *            the caller class making this reference
	 */
	public ReferenceChain(Class<?> callerClass) {
		this.callerClass = callerClass;
		this.references = new LinkedList<>();
	}

	/**
	 * @return the caller class making this reference
	 */
	public Class<?> getCallerClass() {
		return callerClass;
	}

	/**
	 * @return all elements on the reference chain
	 */
	public List<ReferenceChainElement> getReferences() {
		return references;
	}

	/**
	 * Reduces the reference chain to prepend "this" or qualified-this references if necessary, and
	 * short-circuits on static references
	 * 
	 * @return the normalized reference chain
	 */
	public ReferenceChain normalize() {
		List<ReferenceChainElement> oldRefs = getReferences();
		List<ReferenceChainElement> newRefs = new LinkedList<>();

		// Take shortcuts on static references
		for (ReferenceChainElement ref : oldRefs) {
			if (ref.isStatic()) {
				newRefs.clear();
			}

			newRefs.add(ref);
		}

		// Don't reduce static final references to constants. The value could be different, or even stochastic, if 
		// loaded via different class loaders. (eg. logic in static initializers)

		// prepend "this" if starts with non-static field reference
		if (newRefs.isEmpty()) {
			newRefs.add(0, new ReferenceChainElement.ThisReference(callerClass)); // implicit "this"
		} else if (newRefs.get(0) instanceof ReferenceChainElement.FieldReference && !newRefs.get(0).isStatic()) {
			newRefs.add(0, new ReferenceChainElement.ThisReference(callerClass)); // prop => this.prop
		}

		ReferenceChain ret = new ReferenceChain(callerClass);
		ret.references.addAll(newRefs);
		return ret;
	}

	/**
	 * @return the type of the last reference element
	 */
	public Type getType() {
		if (references.isEmpty()) {
			return Type.getType(callerClass);
		}
		return references.get(references.size() - 1).getReferencedType();
	}

	/**
	 * Appends a ReferenceChainElement to the chain
	 * 
	 * @param ref
	 *            ReferenceChainElement to be appended
	 */
	public void append(ReferenceChainElement ref) {
		references.add(ref);
	}

	/**
	 * @return whether the reference is valid from a static context
	 */
	public boolean isStatic() {
		if (references.isEmpty()) {
			return false;
		}

		return references.get(0).isStatic();
	}
}
