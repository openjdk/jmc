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
package org.openjdk.jmc.common;

import org.openjdk.jmc.common.unit.IQuantity;

/**
 * Class for representing an object on the heap in Mission Control.
 */
// FIXME: Move IMC* classes to a subpackage
public interface IMCOldObject {

	/**
	 * The address of the object, used for identity purposes.
	 *
	 * @return the address of the object
	 */
	IQuantity getAddress();

	/**
	 * The type of the object, as an {@link IMCType}
	 *
	 * @return the type of the object
	 */
	IMCType getType();

	/**
	 * If the object is referred to by an object in an array it returns the information about that
	 * array, otherwise it returns {@code null}.
	 *
	 * @return a representation of the array data for the object referring to this old object
	 */
	IMCOldObjectArray getReferrerArray();

	/**
	 * If the object is referred to by an object in a field it returns a representation of the
	 * field, {@code null} otherwise.
	 *
	 * @return a representation of the field that refers to this object
	 */
	IMCOldObjectField getReferrerField();

	/**
	 * Returns the number of steps away in the reference chain this object is from the next object
	 * referring to it. If this is greater than 0, it means that there are objects between this one
	 * and the referrer that were omitted when committing the traces to the Flight Recording file.
	 *
	 * @return the number of steps between this object and the next one towards the root in the
	 *         reference chain
	 */
	int getReferrerSkip();

	/**
	 * Returns a description of the object.
	 *
	 * @return the object description
	 */
	String getDescription();

	/**
	 * Returns the object that refers to this object in the heap reference chain.
	 *
	 * @return the object that refers to this object
	 */
	IMCOldObject getReferrer();

}
