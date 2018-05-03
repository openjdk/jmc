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
package org.openjdk.jmc.rjmx.ui.celleditors;

import java.lang.reflect.Array;

import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.rjmx.ui.internal.Messages;

public class ArrayLengthCellEditor extends NumberCellEditor<Integer> {

	private final Class<?> arrayType;
	private Object oldArray;

	public ArrayLengthCellEditor(Composite parent, Class<?> arrayType) {
		super(parent, Integer.class, true, false);
		this.arrayType = arrayType;
		if (!arrayType.isArray()) {
			throw new IllegalArgumentException(arrayType + " is not an array type"); //$NON-NLS-1$
		}
	}

	@Override
	protected void doSetValue(Object value) {
		oldArray = value;
		if (value == null) {
			super.doSetValue(null);
		} else if (arrayType.isAssignableFrom(value.getClass())) {
			super.doSetValue(Array.getLength(value));
		}
	}

	@Override
	protected Object doGetValue() {
		Object newLength = super.doGetValue();
		if (!(newLength instanceof Integer)) {
			return null;
		} else if (oldArray != null && newLength.equals(Array.getLength(oldArray))) {
			return oldArray;
		} else {
			return Array.newInstance(arrayType.getComponentType(), (Integer) newLength);
		}
	}

	@Override
	public String getInfoText() {
		return Messages.ArrayLengthCellEditor_ENTER_THE_LENGTH_OF_THE_ARRAY + "\n" + super.getInfoText(); //$NON-NLS-1$
	}
}
